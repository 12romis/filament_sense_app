package com.filament.sense.domain.repository

import com.filament.sense.domain.model.EnvData
import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.model.SpoolSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SpoolRepository {
    val spools: Flow<List<SpoolSlot>>
    val envData: StateFlow<EnvData?>
    val activeSpoolIndex: StateFlow<Int?>

    suspend fun setActiveSpool(index: Int)
    suspend fun saveBaseline(index: Int)
    suspend fun updateSpoolConfig(index: Int, name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float)
    suspend fun setThresholds(warning: Int, critical: Int, empty: Int)

    /** Вимірювання залишку для конкретного слоту за вказаний період. */
    fun getMeasurements(spoolIndex: Int, sinceMs: Long): Flow<List<Measurement>>
}