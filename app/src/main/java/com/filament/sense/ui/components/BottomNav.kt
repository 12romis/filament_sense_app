package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.filament.sense.R

private data class NavItem(
    val label: String,
    val route: String,
    val iconRes: Int,
)

private val navItems = listOf(
    NavItem("Пристрій",  "home",      R.drawable.ic_nav_device),
    NavItem("Котушки",   "spools",    R.drawable.ic_nav_spools),
    NavItem("Аналітика", "analytics", R.drawable.ic_nav_analytics),
    NavItem("Налашт.",   "settings",  R.drawable.ic_nav_settings),
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.route
                val contentColor = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable { onItemClick(item.route) },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Top indicator line — always 2dp to avoid layout shift
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                ),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Icon(
                            imageVector = ImageVector.vectorResource(item.iconRes),
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}
