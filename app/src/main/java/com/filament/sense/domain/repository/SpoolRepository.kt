package com.filament.sense.domain.repository

import com.filament.sense.domain.model.ConfigData
import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SpoolRepository {
    val spools: Flow<List<SpoolSlot>>
    fun getSpoolById(id: Int): Flow<SpoolSlot?>
    val envData: StateFlow<EnvData?>
    val thresholds: StateFlow<Triple<Int, Int, Int>>
    val configData: StateFlow<ConfigData?>

    suspend fun setActiveSpool(id: Int)
    suspend fun saveBaseline(id: Int)
    suspend fun createSpool(name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float)
    suspend fun updateSpoolConfig(id: Int, name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float)
    suspend fun deleteSpool(id: Int)
    suspend fun setThresholds(warning: Int, critical: Int, empty: Int)
    suspend fun setMqttHost(host: String)
    suspend fun sendManualReport()

    fun getMeasurements(spoolId: Int, sinceMs: Long): Flow<List<Measurement>>
}
