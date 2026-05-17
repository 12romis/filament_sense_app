package com.filament.sense.data.local.dao

/** Результат GROUP BY запиту — усереднене вимірювання за часовий кошик. */
data class MeasurementBucket(
    val spoolId: Int,
    val remainingGrams: Float,
    val timestamp: Long,
)
