package com.filament.sense.domain.model

data class Measurement(
    val spoolIndex: Int,
    val remainingGrams: Float,
    val temperature: Float?,
    val humidity: Float?,
    val timestamp: Long,
)