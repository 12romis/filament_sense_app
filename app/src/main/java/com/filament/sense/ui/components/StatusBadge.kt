package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pill-badge з кольоровою крапкою та текстом.
 *
 * Використовується для:
 * - статусу з'єднання на смузі (великий, без фону-пілюлі)
 * - міні-бейджу "Підключено" всередині картки пристрою
 * - бейджу "✓ Активна" на картці активної котушки
 *
 * @param label    текст бейджу
 * @param dotColor колір крапки
 * @param textColor колір тексту
 * @param containerColor фон пілюлі (Color.Transparent — без фону)
 * @param showDot  показувати крапку чи ні (для "✓ Активна" крапка не потрібна)
 */
@Composable
fun StatusBadge(
    label: String,
    dotColor: Color,
    textColor: Color,
    containerColor: Color = Color.Transparent,
    showDot: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}