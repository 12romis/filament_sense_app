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
           WHERE spoolId = :spoolId AND timestamp >= :sinceMs
           ORDER BY timestamp ASC"""
    )
    fun getMeasurementsForSpool(spoolId: Int, sinceMs: Long): Flow<List<MeasurementEntity>>

    /** Видаляє найстаріші записи для конкретної котушки, залишаючи не більше [keepCount]. */
    @Query(
        """DELETE FROM measurements
           WHERE spoolId = :spoolId
           AND id NOT IN (
               SELECT id FROM measurements
               WHERE spoolId = :spoolId
               ORDER BY timestamp DESC
               LIMIT :keepCount
           )"""
    )
    suspend fun trimSpoolToLimit(spoolId: Int, keepCount: Int)
}
