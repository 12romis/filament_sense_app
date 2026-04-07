package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val icon: String,
    val label: String,
    val route: String,
)

val bottomNavItems = listOf(
    BottomNavItem("⊙", "Пристрій", "home"),
    BottomNavItem("≡", "Котушки", "spools"),
    BottomNavItem("◎", "Аналітика", "analytics"),
    BottomNavItem("⚙", "Налашт.", "settings"),
)

@Composable
fun BottomNav(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable { onItemClick(item.route) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Amber indicator bar on top for active tab
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .align(Alignment.CenterHorizontally),
                            )
                        }
                        Text(
                            text = item.icon,
                            style = MaterialTheme.typography.titleMedium,
                            color = color,
                            modifier = Modifier.padding(top = if (isSelected) 6.dp else 10.dp),
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}