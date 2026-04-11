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
import com.filament.sense.ui.navigation.Screen
import com.filament.sense.ui.theme.PrimaryContainer
import com.filament.sense.ui.theme.StatusConnected
import com.filament.sense.ui.theme.StatusConnectedBg
import java.text.SimpleDateFormat
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
                // ── Empty state ──────────────────────────────────────────
                EmptyDeviceState(onAddDevice = { navController.navigate(Screen.Scan.route) })
            } else {
                // ── Device card ──────────────────────────────────────────
                DeviceCard(
                    deviceName = state.deviceName.ifEmpty { "FilamentSense" },
                    deviceState = state.deviceState,
                    activeSpool = state.activeSpool,
                    onSpoolClick = {
                        state.activeSpool?.let {
                            navController.navigate(Screen.SpoolDetail.createRoute(it.id))
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Active spool card ────────────────────────────────────
                state.activeSpool?.let { spool ->
                    ActiveSpoolCard(
                        spool = spool,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Env data ─────────────────────────────────────────────
                state.envData?.let { env ->
                    EnvDataCard(
                        envData = env,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
    activeSpool: SpoolSlot?,
    onSpoolClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = deviceName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "ESP32-C6 · IoT Device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(StatusConnectedBg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StatusConnected),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Підключено",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = StatusConnected,
            )
        }
        activeSpool?.let { spool ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Котушка: ${spool.name.ifEmpty { "Котушка #${spool.id}" }}  →",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSpoolClick),
            )
        }
    }
}

// ── Active spool card ─────────────────────────────────────────────────────────

@Composable
private fun ActiveSpoolCard(
    spool: SpoolSlot,
    modifier: Modifier = Modifier,
) {
    val dateStr = spool.startDate?.let {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
    } ?: "—"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "Активна котушка",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(spool.colorArgb)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = spool.name.ifEmpty { "Котушка #${spool.id}" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(PrimaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "✓ Активна",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ThresholdBar(progress = spool.remainingPercent)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${(spool.remainingPercent * 100).toInt()}% залишилось",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${spool.nominalWeightGrams} г",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(8.dp))
        DataRow(icon = "⚖", label = "Залишок", value = "${spool.remainingGrams.toInt()} г")
        Spacer(modifier = Modifier.height(8.dp))
        DataRow(icon = "◎", label = "Вага брутто", value = "${spool.grossWeightGrams.toInt()} г")
        Spacer(modifier = Modifier.height(8.dp))
        DataRow(icon = "📅", label = "Дата початку", value = dateStr)
        Spacer(modifier = Modifier.height(8.dp))
        DataRow(icon = "📦", label = "Номінал", value = "${spool.nominalWeightGrams} г")
    }
}