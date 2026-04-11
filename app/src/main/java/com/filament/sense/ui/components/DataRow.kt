package com.filament.sense.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Рядок даних: іконка (teal) + мітка (OnSurfaceVariant) + значення (OnSurface) right-aligned.
 *
 * Відповідає рядкам "Залишок / 1247 г", "Вага брутто / 2247 г" тощо з Figma 17:2.
 *
 * @param icon  один emoji або символ, відображається кольором Secondary (teal)
 * @param label назва параметра
 * @param value значення параметра
 */
@Composable
fun DataRow(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconStyle: TextStyle? = null,
    onEdit: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            style = iconStyle ?: MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (onEdit != null) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (onEdit != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Редагувати",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onEdit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}