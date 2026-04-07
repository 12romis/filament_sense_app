package com.filament.sense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.filament.sense.data.local.entity.SpoolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpoolDao {

    @Query("SELECT * FROM spools ORDER BY `index` ASC")
    fun getAllSpools(): Flow<List<SpoolEntity>>

    @Query("SELECT * FROM spools WHERE `index` = :index LIMIT 1")
    suspend fun getByIndex(index: Int): SpoolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(spool: SpoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(spools: List<SpoolEntity>)
}