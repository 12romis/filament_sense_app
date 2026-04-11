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
            parentColumns = ["id"],
            childColumns = ["spoolId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("spoolId"), Index("timestamp")],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spoolId: Int,
    val remainingGrams: Float,
    val temperature: Float?,
    val humidity: Float?,
    val timestamp: Long = System.currentTimeMillis(),
)
