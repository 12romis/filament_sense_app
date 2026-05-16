package com.filament.sense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

private const val COLOR_WHITE_ARGB = -1

@Entity(tableName = "spools")
data class SpoolEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val colorArgb: Int = COLOR_WHITE_ARGB,
    val nominalWeightGrams: Int = 1000,
    /**
     * Snapshot повної (брутто) ваги котушка+філамент на момент натискання baseline на ESP32.
     * Оновлюється через SpoolRepositoryImpl при підключеній BLE-сесії.
     */
    val baselineWeight: Float = 0f,
    val isActive: Boolean = false,
    val startDate: Long? = null,
)
