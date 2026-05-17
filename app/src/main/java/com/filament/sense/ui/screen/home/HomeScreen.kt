package com.filament.sense.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.filament.sense.R
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.ui.components.BottomNav
import com.filament.sense.ui.components.DataRow
import com.filament.sense.ui.components.EnvDataCard
import com.filament.sense.ui.components.ThresholdBar
import com.filament.sense.ui.components.WeightHistoryChart
import com.filament.sense.ui.navigation.Screen
import com.filament.sense.ui.theme.PrimaryContainer
import com.filament.sense.ui.theme.StatusConnected
import com.filament.sense.ui.theme.StatusConnectedBg
import com.filament.sense.ui.util.formatWeight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNav(
                currentRoute = "home",
                onItemClick = { route ->
                    if (route != "home") navController.navigate(route)
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── App bar ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "FilamentSense",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // ── Connection status banner (тільки коли пристрій відомий системі) ──
            val showBanner = state.deviceState == DeviceState.CONNECTED ||
                             state.deviceState == DeviceState.CONNECTING
            if (showBanner) {
                ConnectionBanner(deviceState = state.deviceState)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // SCANNING також вважається "немає пристрою" — запобігає флешу при
            // поверненні зі ScanScreen поки StateFlow ще не оновився
            val isEmptyState = state.deviceState == DeviceState.DISCONNECTED ||
                               state.deviceState == DeviceState.SCANNING

            if (isEmptyState) {
                // ── Empty / disconnected state ───────────────────────────
                if (state.hasLastMac) {
                    ReconnectState(
                        onReconnect = { viewModel.triggerReconnect() },
                        onScan = { navController.navigate(Screen.Scan.route) },
                    )
                } else {
                    EmptyDeviceState(onAddDevice = { navController.navigate(Screen.Scan.route) })
                }
            } else {
                // ── Device card ──────────────────────────────────────────
                DeviceCard(
                    deviceName = state.deviceName.ifEmpty { "FilamentSense" },
                    deviceState = state.deviceState,
                    lastSyncTimestamp = state.activeSpool?.syncTimestamp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ── Active spool card ────────────────────────────────────
                state.activeSpool?.let { spool ->
                    ActiveSpoolCard(
                        spool = spool,
                        onSpoolClick = {
                            navController.navigate(Screen.SpoolDetail.createRoute(spool.id))
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    state.envData?.let { env ->
                        EnvDataCard(
                            envData = env,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    WeightHistoryChart(
                        measurements = state.activeMeasurements,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        emptyHint = "Недостатньо даних\n(накопичується кожні 5 хв,\nусереднено по 8 год)",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (!isEmptyState) {
                // ── "All spools" link ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.SpoolList.route) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Всі котушки  →",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Reconnect state (last MAC відомий, але пристрій недоступний) ─────────────

@Composable
private fun ReconnectState(
    onReconnect: () -> Unit,
    onScan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.img_empty_ring_outer),
                contentDescription = null,
                modifier = Modifier.size(280.dp).alpha(0.5f),
            )
            Image(
                painter = painterResource(R.drawable.img_empty_ring_inner),
                contentDescription = null,
                modifier = Modifier.size(240.dp).alpha(0.3f),
            )
            DeviceIllustration(modifier = Modifier.size(200.dp))
        }
        Text(
            text = "Немає пристрою",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "FilamentSense недоступний.\nПереконайтесь що пристрій увімкнено.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(56.dp))
        Button(
            onClick = onReconnect,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Підключити", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text("Шукати пристрій", style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
        }
    }
}

// ── Connection banner ────────────────────────────────────────────────────────

@Composable
private fun ConnectionBanner(deviceState: DeviceState) {
    val bgColor by animateColorAsState(
        targetValue = when (deviceState) {
            DeviceState.CONNECTED -> StatusConnectedBg
            DeviceState.CONNECTING, DeviceState.SCANNING -> Color(0xFF1A2A1A)
            DeviceState.DISCONNECTED -> Color(0xFF2A1010)
        },
        label = "bannerBg",
    )
    val dotColor = when (deviceState) {
        DeviceState.CONNECTED -> StatusConnected
        DeviceState.CONNECTING, DeviceState.SCANNING -> Color(0xFFFFB300)
        DeviceState.DISCONNECTED -> MaterialTheme.colorScheme.error
    }
    val label = when (deviceState) {
        DeviceState.CONNECTED -> "Підключено"
        DeviceState.CONNECTING -> "Підключення..."
        DeviceState.SCANNING -> "Пошук..."
        DeviceState.DISCONNECTED -> "Не підключено"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = dotColor,
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyDeviceState(onAddDevice: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Три шари: зовнішнє кільце → внутрішнє кільце → ілюстрація пристрою
        // Позиції per Figma: outer 280dp (op=50%), inner 240dp (op=30%), illustration 200dp
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.img_empty_ring_outer),
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .alpha(0.5f),
            )
            Image(
                painter = painterResource(R.drawable.img_empty_ring_inner),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .alpha(0.3f),
            )
            DeviceIllustration(modifier = Modifier.size(200.dp))
        }

        // Текст починається трохи вище нижнього краю кілець (per Figma позиції)
        Text(
            text = "Немає пристрою",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Додайте пристрій FilamentSense\nщоб розпочати моніторинг котушок",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(56.dp))
        Button(
            onClick = onAddDevice,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "+ Додати пристрій",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            )
        }
    }
}

// Ілюстрація пристрою — перебудована з SVG (Figma 17:76)
@Composable
private fun DeviceIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val s = size.minDimension / 200f           // scale factor (SVG viewBox = 200×200)
        val cx = size.width / 2
        val cy = size.height / 2

        // Clip всього малювання до кола щоб бар не виходив за межі
        val circlePath = Path().apply { addOval(Rect(center = Offset(cx, cy), radius = size.minDimension / 2)) }
        clipPath(circlePath) {
            // Зовнішнє коло (#2C2C2C = surfaceVariant), r=100
            drawCircle(color = surfaceVariant, radius = size.minDimension / 2, center = Offset(cx, cy))
            // Середнє коло (#383838), r=67
            drawCircle(color = Color(0xFF383838), radius = 67f * s, center = Offset(cx, cy))
            // PrimaryContainer (темно-бурштиновий), r=33
            drawCircle(color = primaryContainer, radius = 33f * s, center = Offset(cx, cy))
            // Центральна бурштинова крапка, r=16
            drawCircle(color = primary, radius = 16f * s, center = Offset(cx, cy))
            // Нижня бурштинова смужка — "підставка" котушки (clipPath не дає їй виходити за коло)
            drawRoundRect(
                color = primary,
                topLeft = Offset(16f * s, 182f * s),
                size = Size(168f * s, 18f * s),
                cornerRadius = CornerRadius(3f * s),
            )
        } // end clipPath
    }
}

// ── Device card ──────────────────────────────────────────────────────────────

@Composable
private fun DeviceCard(
    deviceName: String,
    deviceState: DeviceState,
    lastSyncTimestamp: Long?,
    modifier: Modifier = Modifier,
) {
    val syncStr = lastSyncTimestamp?.let {
        val date = Date(it)
        val today = Calendar.getInstance()
        val syncCal = Calendar.getInstance().apply { time = date }
        val isToday = today.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                      today.get(Calendar.DAY_OF_YEAR) == syncCal.get(Calendar.DAY_OF_YEAR)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        if (isToday) "сьогодні $timeStr"
        else SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(date)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeviceIllustration(modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ESP32-C6 · IoT Device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (syncStr != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Востаннє: $syncStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val badgeBg = if (deviceState == DeviceState.CONNECTED) StatusConnectedBg
                          else Color(0xFF1A2A1A)
            val badgeDot = if (deviceState == DeviceState.CONNECTED) StatusConnected
                           else Color(0xFFFFB300)
            val badgeText = if (deviceState == DeviceState.CONNECTED) "Активний"
                            else "Підключається..."
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeDot),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    color = badgeDot,
                )
            }
        }
    }
}

// ── Active spool card ─────────────────────────────────────────────────────────

@Composable
private fun ActiveSpoolCard(
    spool: SpoolSlot,
    onSpoolClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateStr = spool.baselineTimestamp?.let {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
    } ?: "—"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onSpoolClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Активна котушка",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(spool.colorArgb)),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = spool.name.ifEmpty { "Котушка #${spool.id}" },
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        ThresholdBar(progress = spool.remainingPercent)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${(spool.remainingPercent * 100).toInt()}% залишилось",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${spool.nominalWeightGrams.formatWeight()} г",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(8.dp))
        DataRow(icon = "⚖️", label = "Залишок", value = "${spool.remainingGrams.toInt().formatWeight()} г", fontSize = 14)
        Spacer(modifier = Modifier.height(10.dp))
        DataRow(icon = "📦", label = "Вага брутто", value = "${spool.grossWeightGrams.toInt().formatWeight()} г", fontSize = 14)
        Spacer(modifier = Modifier.height(10.dp))
        val baselineStr = if (spool.baselineWeight > 0f) "${spool.baselineWeight.toInt().formatWeight()} г" else "—"
        DataRow(icon = "📫", label = "Початкова вага брутто", value = baselineStr, fontSize = 14)
        Spacer(modifier = Modifier.height(10.dp))
        val syncStr = spool.syncTimestamp?.let {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "Не синхронізовано"
        DataRow(icon = "🔄", label = "Синхронізовано", value = syncStr, fontSize = 14)
        Spacer(modifier = Modifier.height(10.dp))
        DataRow(icon = "📅", label = "Дата початку", value = dateStr, fontSize = 14)
        Spacer(modifier = Modifier.height(10.dp))
        DataRow(icon = "📦", label = "Номінал", value = "${spool.nominalWeightGrams.formatWeight()} г", fontSize = 14)
    }
}