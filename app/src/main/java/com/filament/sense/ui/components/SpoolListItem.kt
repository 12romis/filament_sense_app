package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.ui.theme.PrimaryContainer
import com.filament.sense.ui.theme.StatusConnected
import com.filament.sense.ui.util.formatWeight

/**
 * Рядок котушки у списку. Figma node 18:43+.
 * - Кольорова крапка (Ellipse)
 * - Назва + залишок
 * - Прогрес-бар (5dp)
 * - Бейдж "✓ Активна" якщо isActive
 * - Шеврон "›"
 */
@Composable
fun SpoolListItem(
    spool: SpoolSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = spool.isActive
    val remainingText = if (spool.hasFilament) {
        "${spool.remainingGrams.toInt().formatWeight()} г залишилось"
    } else {
        "Порожня"
    }
    val progressColor = when {
        spool.remainingPercent > 0.3f -> StatusConnected
        spool.remainingPercent > 0.1f -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isActive) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Кольорова крапка
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .background(Color(spool.colorArgb)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = spool.name.ifEmpty { "Котушка #${spool.id}" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                        ),
                        color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(PrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "✓ Активна",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThresholdBar(
                    progress = spool.remainingPercent,
                    fillColor = progressColor,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}