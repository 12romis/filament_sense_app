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

    /**
     * Повертає усереднені вимірювання згруповані по [bucketMs]-кошиках.
     * Для кожного кошика — середнє remainingGrams і середній timestamp.
     */
    @Query(
        """SELECT spoolId,
                  CAST(AVG(remainingGrams) AS REAL)    AS remainingGrams,
                  CAST(AVG(timestamp)     AS INTEGER)  AS timestamp
           FROM measurements
           WHERE spoolId = :spoolId AND timestamp >= :sinceMs
           GROUP BY (timestamp / :bucketMs)
           ORDER BY (timestamp / :bucketMs) ASC"""
    )
    fun getBucketedMeasurements(
        spoolId: Int,
        sinceMs: Long,
        bucketMs: Long,
    ): Flow<List<MeasurementBucket>>

    /**
     * Видаляє зайві записи в кошиках, що старіші за [cutoffMs]:
     * залишає тільки перший запис кожного [bucketMs]-кошика (MIN id).
     * Це дозволяє зберігати 30+ днів history в межах ліміту рядків.
     */
    @Query(
        """DELETE FROM measurements
           WHERE spoolId = :spoolId
             AND timestamp < :cutoffMs
             AND id NOT IN (
                 SELECT MIN(id)
                 FROM measurements
                 WHERE spoolId = :spoolId AND timestamp < :cutoffMs
                 GROUP BY (timestamp / :bucketMs)
             )"""
    )
    suspend fun compactOldMeasurements(spoolId: Int, cutoffMs: Long, bucketMs: Long)
}
