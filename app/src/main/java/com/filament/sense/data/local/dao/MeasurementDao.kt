package com.filament.sense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.filament.sense.data.local.entity.MeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Insert
    suspend fun insert(measurement: MeasurementEntity)

    @Query(
        """SELECT * FROM measurements
           WHERE spoolIndex = :spoolIndex AND timestamp >= :sinceMs
           ORDER BY timestamp ASC"""
    )
    fun getMeasurementsForSpool(spoolIndex: Int, sinceMs: Long): Flow<List<MeasurementEntity>>

    @Query("DELETE FROM measurements WHERE timestamp < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}