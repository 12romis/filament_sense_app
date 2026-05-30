package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    NavItem("Принтер",   "printer",   R.drawable.ic_nav_printer),
    NavItem("Налашт.",   "settings",  R.drawable.ic_nav_settings),
)

@Composable
fun BottomNav(
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Column дозволяє фону surface заходити під системний nav bar,
    // а Spacer(navigationBarsPadding) розширює зону нижче контенту.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.route
                val contentColor = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { role = Role.Tab }
                        .clickable(
                            onClickLabel = item.label,
                            onClick = { onItemClick(item.route) },
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pill-індикатор: 60×4dp, rounded 2dp — per Figma 6:62
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                ),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Icon(
                            imageVector = ImageVector.vectorResource(item.iconRes),
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}