package com.filament.sense.domain.model

// 0xFFFFFFFF = White in ARGB
private const val COLOR_WHITE_ARGB = -1

data class SpoolSlot(
    val index: Int,
    val name: String = "",
    val material: String = "",
    val colorArgb: Int = COLOR_WHITE_ARGB,
    val nominalWeightGrams: Int = 1000,
    val baselineWeight: Float = 0f,       // вага порожньої котушки (tare)
    val remainingGrams: Float = 0f,       // залишок філаменту (з BLE)
    val grossWeightGrams: Float = 0f,     // вага брутто (з BLE)
    val hasFilament: Boolean = false,     // датчик наявності філаменту
    val isActive: Boolean = false,        // активна котушка
    val startDate: Long? = null,          // дата початку використання (ms)
) {
    val remainingPercent: Float
        get() = if (nominalWeightGrams > 0) {
            (remainingGrams / nominalWeightGrams).coerceIn(0f, 1f)
        } else 0f
}