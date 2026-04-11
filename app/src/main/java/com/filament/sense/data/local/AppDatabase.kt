package com.filament.sense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.filament.sense.data.local.dao.MeasurementDao
import com.filament.sense.data.local.dao.SpoolDao
import com.filament.sense.data.local.entity.MeasurementEntity
import com.filament.sense.data.local.entity.SpoolEntity

@Database(
    entities = [SpoolEntity::class, MeasurementEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spoolDao(): SpoolDao
    abstract fun measurementDao(): MeasurementDao
}
