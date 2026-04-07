package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filament.sense.ui.theme.StatusConnected

/**
 * Прогрес-бар залишку філаменту.
 *
 * З Figma 17:2: висота 8dp, cornerRadius 4dp, трек SurfaceVariant, заповнення динамічне.
 * Права мітка показує номінальну вагу ("3000 г"), ліва — % залишку —
 * виводяться зовні цього composable батьківським layout.
 *
 * @param progress   0f..1f — частина залишку від номіналу
 * @param fillColor  колір заповнення (за замовч. зелений StatusConnected)
 */
@Composable
fun ThresholdBar(
    progress: Float,
    fillColor: Color = StatusConnected,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(fillColor),
        )
    }
}