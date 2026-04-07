package com.filament.sense.data.repository

import com.filament.sense.data.ble.BleDataParser
import com.filament.sense.data.ble.BleManager
import com.filament.sense.data.ble.GattConstants
import com.filament.sense.data.local.dao.MeasurementDao
import com.filament.sense.data.local.dao.SpoolDao
import com.filament.sense.data.local.entity.MeasurementEntity
import com.filament.sense.data.local.entity.SpoolEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val MEASUREMENT_INTERVAL_MS = 5 * 60 * 1000L   // 5 хвилин
private const val RETENTION_PERIOD_MS = 30L * 24 * 60 * 60 * 1000L // 30 днів

@Singleton
class SpoolRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
    private val spoolDao: SpoolDao,
    private val measurementDao: MeasurementDao,
) : SpoolRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val slotCount = GattConstants.SPOOL_DATA_UUIDS.size

    // Ключовий in-memory стан — зливає BLE-дані та конфіг з Room
    private val _slots = MutableStateFlow(
        List(slotCount) { index -> SpoolSlot(index = index) }
    )

    private val _activeSpoolIndex = MutableStateFlow<Int?>(null)
    override val activeSpoolIndex: StateFlow<Int?> = _activeSpoolIndex.asStateFlow()

    override val envData: StateFlow<EnvData?> = bleManager.envData

    override val spools: Flow<List<SpoolSlot>>
        get() = bleManager.spoolUpdates
            .onStart { _slots.value.forEach { emit(it) } }
            .map { update ->
                val current = _slots.value.toMutableList()
                val activeIdx = _activeSpoolIndex.value
                current[update.index] = current[update.index].copy(
                    remainingGrams = update.remainingGrams,
                    grossWeightGrams = update.grossWeightGrams,
                    hasFilament = update.hasFilament,
                    isActive = update.index == activeIdx,
                )
                _slots.value = current
                current.toList()
            }

    init {
        // Завантажуємо збережений конфіг котушок з Room при старті
        scope.launch {
            val entities = spoolDao.getAllSpools().first()
            if (entities.isNotEmpty()) {
                val current = _slots.value.toMutableList()
                entities.forEach { entity ->
                    val idx = entity.index
                    if (idx in current.indices) {
                        current[idx] = current[idx].copy(
                            name = entity.name,
                            colorArgb = entity.colorArgb,
                            nominalWeightGrams = entity.nominalWeightGrams,
                            baselineWeight = entity.baselineWeight,
                            isActive = entity.isActive,
                            startDate = entity.startDate,
                        )
                        if (entity.isActive) _activeSpoolIndex.value = idx
                    }
                }
                _slots.value = current
            }
        }

        // Записуємо вимірювання кожні 5 хвилин
        scope.launch {
            while (true) {
                delay(MEASUREMENT_INTERVAL_MS)
                val env = envData.value
                _slots.value.forEach { spool ->
                    if (spool.hasFilament) {
                        measurementDao.insert(
                            MeasurementEntity(
                                spoolIndex = spool.index,
                                remainingGrams = spool.remainingGrams,
                                temperature = env?.temperature,
                                humidity = env?.humidity,
                            )
                        )
                    }
                }
                // Очищаємо старі записи (старіші за 30 днів)
                measurementDao.deleteOlderThan(System.currentTimeMillis() - RETENTION_PERIOD_MS)
            }
        }
    }

    override suspend fun setActiveSpool(index: Int) {
        _activeSpoolIndex.value = index
        val current = _slots.value.toMutableList()
        for (i in current.indices) {
            val isActive = i == index
            current[i] = current[i].copy(isActive = isActive)
            spoolDao.upsert(current[i].toEntity())
        }
        _slots.value = current
    }

    override suspend fun saveBaseline(index: Int) {
        bleManager.sendCommand(BleDataParser.buildSaveBaselineCmd(index))
    }

    override suspend fun updateSpoolConfig(
        index: Int,
        name: String,
        colorArgb: Int,
        nominalWeight: Int,
        baselineWeight: Float,
    ) {
        val current = _slots.value.toMutableList()
        current[index] = current[index].copy(
            name = name,
            colorArgb = colorArgb,
            nominalWeightGrams = nominalWeight,
            baselineWeight = baselineWeight,
        )
        _slots.value = current
        spoolDao.upsert(current[index].toEntity())
        bleManager.sendCommand(BleDataParser.buildSetNameCmd(index, name))
        if (baselineWeight > 0f) {
            bleManager.sendCommand(BleDataParser.buildSetTareCmd(index, baselineWeight))
        }
    }

    override suspend fun setThresholds(warning: Int, critical: Int, empty: Int) {
        bleManager.sendCommand(BleDataParser.buildSetThresholdCmd(warning, critical, empty))
    }

    override fun getMeasurements(spoolIndex: Int, sinceMs: Long): Flow<List<Measurement>> =
        measurementDao.getMeasurementsForSpool(spoolIndex, sinceMs)
            .map { entities -> entities.map { it.toDomain() } }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun SpoolSlot.toEntity() = SpoolEntity(
        index = index,
        name = name,
        colorArgb = colorArgb,
        nominalWeightGrams = nominalWeightGrams,
        baselineWeight = baselineWeight,
        isActive = isActive,
        startDate = startDate,
    )

    private fun MeasurementEntity.toDomain() = Measurement(
        spoolIndex = spoolIndex,
        remainingGrams = remainingGrams,
        temperature = temperature,
        humidity = humidity,
        timestamp = timestamp,
    )
}