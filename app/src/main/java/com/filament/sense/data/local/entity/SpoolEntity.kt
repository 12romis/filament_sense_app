package com.filament.sense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 0xFFFFFFFF = White in ARGB
private const val COLOR_WHITE_ARGB = -1

@Entity(tableName = "spools")
data class SpoolEntity(
    @PrimaryKey val index: Int,
    val name: String = "",
    val colorArgb: Int = COLOR_WHITE_ARGB,
    val nominalWeightGrams: Int = 1000,
    val baselineWeight: Float = 0f,
    val isActive: Boolean = false,
    val startDate: Long? = null,
)