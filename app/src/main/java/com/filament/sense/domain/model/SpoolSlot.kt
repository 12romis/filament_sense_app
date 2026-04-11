package com.filament.sense.domain.model

private const val COLOR_WHITE_ARGB = -1

data class SpoolSlot(
    val id: Int = 0,
    val name: String = "",
    val material: String = "",
    val colorArgb: Int = COLOR_WHITE_ARGB,
    val nominalWeightGrams: Int = 1000,
    val baselineWeight: Float = 0f,
    val remainingGrams: Float = 0f,
    val grossWeightGrams: Float = 0f,
    val hasFilament: Boolean = false,
    val isActive: Boolean = false,
    val startDate: Long? = null,
) {
    val remainingPercent: Float
        get() = if (nominalWeightGrams > 0) {
            (remainingGrams / nominalWeightGrams).coerceIn(0f, 1f)
        } else 0f
}
