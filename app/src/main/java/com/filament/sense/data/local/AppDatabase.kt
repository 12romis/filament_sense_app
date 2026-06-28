package com.filament.sense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.filament.sense.data.local.dao.MeasurementDao
import com.filament.sense.data.local.dao.SpoolDao
import com.filament.sense.data.local.entity.MeasurementEntity
import com.filament.sense.data.local.entity.SpoolEntity

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spools ADD COLUMN grossWeightGrams REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE spools ADD COLUMN remainingGrams REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE spools ADD COLUMN hasFilament INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE spools ADD COLUMN syncTimestamp INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE spools ADD COLUMN baselineTimestamp INTEGER DEFAULT NULL")
    }
}

@Database(
    entities = [SpoolEntity::class, MeasurementEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spoolDao(): SpoolDao
    abstract fun measurementDao(): MeasurementDao
}
