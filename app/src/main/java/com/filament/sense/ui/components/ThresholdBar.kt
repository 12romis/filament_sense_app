package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filament.sense.ui.theme.StatusConnected

private val TRACK_HEIGHT = 5.dp
private val THUMB_SIZE = 14.dp

/**
 * Прогрес-бар залишку філаменту у стилі Material3:
 * тонкий трек + кругла крапка на кінці заповненої частини.
 *
 * @param progress  0f..1f
 * @param fillColor колір заповнення (за замовч. зелений StatusConnected)
 */
@Composable
fun ThresholdBar(
    progress: Float,
    fillColor: Color = StatusConnected,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val clampedProgress = progress.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(THUMB_SIZE),
        contentAlignment = Alignment.CenterStart,
    ) {
        val totalWidth = maxWidth

        // Трек — фон
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
                .background(trackColor),
        )

        if (clampedProgress > 0f) {
            val fillWidth = totalWidth * clampedProgress

            // Заповнена частина треку
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .height(TRACK_HEIGHT)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
                    .background(fillColor),
            )

            // Крапка (thumb) на кінці прогресу
            val thumbOffset = (fillWidth - THUMB_SIZE)
                .coerceIn(0.dp, totalWidth - THUMB_SIZE)
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .offset(x = thumbOffset)
                    .clip(CircleShape)
                    .background(fillColor),
            )
        }
    }
}
