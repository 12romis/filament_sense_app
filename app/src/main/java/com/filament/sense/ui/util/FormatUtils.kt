package com.filament.sense.ui.util

/**
 * Форматує ціле число з розділенням тисяч вузьким нерозривним пробілом (U+202F).
 * Приклади: 1234 → "1 234", 12345 → "12 345", 999 → "999"
 */
fun Int.formatWeight(): String {
    if (this < 1000) return this.toString()
    val s = this.toString()
    val offset = s.length % 3
    return buildString {
        s.forEachIndexed { i, c ->
            if (i > 0 && (i - offset) % 3 == 0 && i >= offset) append('\u202F')
            append(c)
        }
    }
}
