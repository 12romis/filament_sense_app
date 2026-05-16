package com.filament.sense.data.repository

import android.content.SharedPreferences
import com.filament.sense.data.ble.BleDataParser
import com.filament.sense.data.ble.BleManager
import com.filament.sense.data.local.dao.MeasurementDao
import com.filament.sense.data.local.dao.SpoolDao
import com.filament.sense.data.local.entity.MeasurementEntity
import com.filament.sense.data.local.entity.SpoolEntity
import com.filament.sense.domain.model.ConfigData
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val MEASUREMENT_INTERVAL_MS = 5 * 60 * 1000L
private const val MEASUREMENTS_PER_SPOOL = 1000
private const val PREF_THRESHOLD_WARNING = "threshold_warning"
private const val PREF_THRESHOLD_CRITICAL = "threshold_critical"
private const val PREF_THRESHOLD_EMPTY = "threshold_empty"

private data class BleSpoolData(
    val remainingGrams: Float,
    val grossWeightGrams: Float,
    val hasFilament: Boolean,
    val baselineWeight: Float,
    val baselineTimestamp: Long?,
    val syncTimestamp: Long,
)

@Singleton
class SpoolRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
    private val spoolDao: SpoolDao,
    private val measurementDao: MeasurementDao,
    private val prefs: SharedPreferences,
) : SpoolRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val envData: StateFlow<EnvData?> = bleManager.envData
    override val configData: StateFlow<ConfigData?> = bleManager.configData

    // Live BLE дані для активної котушки (null = немає підключення)
    private val _liveBleData = MutableStateFlow<BleSpoolData?>(null)

    // DB — джерело правди; BLE live дані накладаються на активну котушку
    override val spools: Flow<List<SpoolSlot>> =
        spoolDao.getAllSpools().combine(_liveBleData) { entities, live ->
            entities.map { entity ->
                val domain = entity.toDomain()
                if (entity.isActive && live != null) {
                    domain.copy(
                        remainingGrams = live.remainingGrams,
                        grossWeightGrams = live.grossWeightGrams,
                        hasFilament = live.hasFilament,
                        baselineWeight = live.baselineWeight,
                        baselineTimestamp = live.baselineTimestamp,
                        syncTimestamp = live.syncTimestamp,
                    )
                } else domain
            }
        }

    private val _thresholds = MutableStateFlow(
        Triple(
            prefs.getInt(PREF_THRESHOLD_WARNING, 500),
            prefs.getInt(PREF_THRESHOLD_CRITICAL, 100),
            prefs.getInt(PREF_THRESHOLD_EMPTY, 10),
        )
    )
    override val thresholds: StateFlow<Triple<Int, Int, Int>> = _thresholds.asStateFlow()

    init {
        scope.launch {
            bleManager.spoolUpdates.collect { update ->
                // grossWeightGrams == 0 означає неготовність GATT (ESP32 ще не виміряв
                // після reconnect). Ігноруємо такі пакети щоб не затирати валідні дані.
                if (update.grossWeightGrams <= 0f) return@collect
                _liveBleData.value = BleSpoolData(
                    remainingGrams = update.remainingGrams,
                    grossWeightGrams = update.grossWeightGrams,
                    hasFilament = update.hasFilament,
                    baselineWeight = update.baselineWeight,
                    baselineTimestamp = update.baselineTimestamp,
                    syncTimestamp = System.currentTimeMillis(),
                )
            }
        }

        scope.launch {
            while (true) {
                delay(MEASUREMENT_INTERVAL_MS)
                val live = _liveBleData.value ?: continue
                val active = spoolDao.getActiveSpool() ?: continue
                val env = envData.value
                measurementDao.insert(
                    MeasurementEntity(
                        spoolId = active.id,
                        remainingGrams = live.remainingGrams,
                        temperature = env?.temperature,
                        humidity = env?.humidity,
                    )
                )
                measurementDao.trimSpoolToLimit(active.id, MEASUREMENTS_PER_SPOOL)
            }
        }
    }

    override suspend fun setActiveSpool(id: Int) {
        val entities = spoolDao.getAllSpools().first()
        entities.forEach { entity ->
            spoolDao.upsert(entity.copy(isActive = entity.id == id))
        }
    }

    override suspend fun saveBaseline(id: Int) {
        bleManager.sendCommand(BleDataParser.buildSaveBaselineCmd(0))
    }

    override suspend fun setMqttHost(host: String) {
        bleManager.writeConfig(BleDataParser.buildSetMqttHostJson(host))
    }

    override suspend fun sendManualReport() {
        bleManager.sendCommand(BleDataParser.buildManualReportCmd())
    }

    override suspend fun createSpool(
        name: String,
        colorArgb: Int,
        nominalWeight: Int,
        baselineWeight: Float,
    ) {
        spoolDao.upsert(
            SpoolEntity(
                id = 0,
                name = name,
                colorArgb = colorArgb,
                nominalWeightGrams = nominalWeight,
                baselineWeight = baselineWeight,
                startDate = System.currentTimeMillis(),
            )
        )
        bleManager.sendCommand(BleDataParser.buildSetNameCmd(0, name))
        bleManager.sendCommand(BleDataParser.buildSetTareCmd(0, baselineWeight, nominalWeight))
    }

    override suspend fun updateSpoolConfig(
        id: Int,
        name: String,
        colorArgb: Int,
        nominalWeight: Int,
        baselineWeight: Float,
    ) {
        val existing = spoolDao.getById(id)
        spoolDao.upsert(
            SpoolEntity(
                id = id,
                name = name,
                colorArgb = colorArgb,
                nominalWeightGrams = nominalWeight,
                baselineWeight = baselineWeight,
                isActive = existing?.isActive ?: false,
                startDate = existing?.startDate ?: System.currentTimeMillis(),
            )
        )
        bleManager.sendCommand(BleDataParser.buildSetNameCmd(0, name))
        bleManager.sendCommand(BleDataParser.buildSetTareCmd(0, baselineWeight, nominalWeight))
    }

    override suspend fun deleteSpool(id: Int) {
        spoolDao.deleteById(id)
    }

    override suspend fun setThresholds(warning: Int, critical: Int, empty: Int) {
        prefs.edit()
            .putInt(PREF_THRESHOLD_WARNING, warning)
            .putInt(PREF_THRESHOLD_CRITICAL, critical)
            .putInt(PREF_THRESHOLD_EMPTY, empty)
            .apply()
        _thresholds.value = Triple(warning, critical, empty)
        bleManager.sendCommand(BleDataParser.buildSetThresholdCmd(warning, critical, empty))
    }

    override fun getSpoolById(id: Int): Flow<SpoolSlot?> =
        spools.map { list -> list.find { it.id == id } }

    override fun getMeasurements(spoolId: Int, sinceMs: Long): Flow<List<Measurement>> =
        measurementDao.getMeasurementsForSpool(spoolId, sinceMs)
            .map { entities -> entities.map { it.toDomain() } }

    private fun SpoolEntity.toDomain() = SpoolSlot(
        id = id,
        name = name,
        colorArgb = colorArgb,
        nominalWeightGrams = nominalWeightGrams,
        baselineWeight = baselineWeight,
        isActive = isActive,
        startDate = startDate,
    )

    private fun MeasurementEntity.toDomain() = Measurement(
        spoolIndex = spoolId,
        remainingGrams = remainingGrams,
        temperature = temperature,
        humidity = humidity,
        timestamp = timestamp,
    )
}
