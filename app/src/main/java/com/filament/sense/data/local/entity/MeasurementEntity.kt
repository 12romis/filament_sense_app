package com.filament.sense.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = SpoolEntity::class,
            parentColumns = ["index"],
            childColumns = ["spoolIndex"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("spoolIndex"), Index("timestamp")],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spoolIndex: Int,
    val remainingGrams: Float,
    val temperature: Float?,
    val humidity: Float?,
    val timestamp: Long = System.currentTimeMillis(),
)