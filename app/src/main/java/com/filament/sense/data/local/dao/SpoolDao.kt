package com.filament.sense.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.filament.sense.data.local.entity.SpoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpoolDao {

    @Query("SELECT * FROM spools ORDER BY id ASC")
    fun getAllSpools(): Flow<List<SpoolEntity>>

    @Query("SELECT * FROM spools WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): SpoolEntity?

    @Query("SELECT * FROM spools WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSpool(): SpoolEntity?

    @Upsert
    suspend fun upsert(spool: SpoolEntity)

    @Upsert
    suspend fun upsertAll(spools: List<SpoolEntity>)

    @Query("DELETE FROM spools WHERE id = :id")
    suspend fun deleteById(id: Int)
}
