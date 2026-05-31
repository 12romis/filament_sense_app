package com.filament.sense.ui.screen.printer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.filament.sense.R
import com.filament.sense.domain.model.DeviceState
import com.filament.sense.domain.model.PrinterStatus
import com.filament.sense.ui.components.BottomNav
import com.filament.sense.ui.theme.StatusConnected
import kotlin.math.roundToInt

private fun formatSyncTime(timeMs: Long?): String {
    if (timeMs == null) return "Немає даних"
    val diffSec = (System.currentTimeMillis() - timeMs) / 1000
    return when {
        diffSec < 10   -> "Щойно"
        diffSec < 60   -> "${diffSec} сек тому"
        diffSec < 3600 -> "${diffSec / 60} хв тому"
        else           -> "${diffSec / 3600} год тому"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(
    navController: NavController,
    viewModel: PrinterViewModel = hiltViewModel(),
) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val status by viewModel.printerStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val filesList by viewModel.filesList.collectAsStateWithLifecycle()
    val isConnected = deviceState == DeviceState.CONNECTED

    var showHeatSheet by remember { mutableStateOf(false) }
    var showReprintSheet by remember { mutableStateOf(false) }

    // Request fresh data whenever connection is established
    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNav(
                currentRoute = "printer",
                onItemClick = { route ->
                    if (route != "printer") navController.navigate(route)
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
            // ── Header ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Принтер",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // ── Disconnection warning ────────────────────────────────────────
            if (!isConnected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Пристрій FilamentSense не підключений. З'єднайтесь для керування принтером.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Printer identity card ────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.p1s_printer),
                        contentDescription = "Bambu P1S",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(160.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Bambu P1S",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isConnected && status != null) {
                            GcodeStateBadge(status!!.gcodeState)
                        }
                        Spacer(Modifier.height(4.dp))
                        DeviceConnectionRow(deviceState = deviceState)
                        // LastSyncRow(lastSyncMs = lastSyncTime)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Print status card ────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Файл",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    val fileName = if (isConnected) status?.fileName?.ifEmpty { "—" } ?: "—" else "—"
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Progress bar
                    val progress = if (isConnected && status != null) status!!.progress / 100f else 0f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            // color = StatusConnected,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                        val pctText = if (isConnected && status != null) "${status!!.progress}%" else "—"
                        Text(
                            text = pctText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.End,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Layers and time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Layers
                        Column {
                            Text(
                                text = "Шари",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val layerText = if (isConnected && status != null)
                                "${status!!.layerNum} / ${status!!.totalLayers}" else "—"
                            Text(
                                text = layerText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        // Remaining time
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Залишилось",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val timeText = if (isConnected && status != null)
                                formatRemainingTime(status!!.remainingMinutes) else "—"
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Temperature card ─────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TempRow(
                        label = "Сопло",
                        current = if (isConnected) status?.nozzleTemp else null,
                        target = if (isConnected) status?.nozzleTarget else null,
                    )
                    TempRow(
                        label = "Стіл",
                        current = if (isConnected) status?.bedTemp else null,
                        target = if (isConnected) status?.bedTarget else null,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Refresh button ───────────────────────────────────────────────
            OutlinedButton(
                onClick = { viewModel.refresh() },
                enabled = isConnected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("Оновити")
            }

            Spacer(Modifier.height(8.dp))

            // ── Heat bed button ──────────────────────────────────────────────
            OutlinedButton(
                onClick = { showHeatSheet = true },
                enabled = isConnected,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (isConnected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("Підняти температуру столу")
            }

            Spacer(Modifier.height(8.dp))

            // ── Reprint button ───────────────────────────────────────────────
            OutlinedButton(
                onClick = { showReprintSheet = true },
                enabled = isConnected,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, if (isConnected) MaterialTheme.colorScheme.secondary
                           else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("Повторити друк")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Heat bed bottom sheet ────────────────────────────────────────────────
    if (showHeatSheet) {
        HeatBedBottomSheet(
            isPrinting = status?.gcodeState?.uppercase() == "RUNNING",
            currentBedTemp = status?.bedTemp?.roundToInt(),
            onConfirm = { target ->
                viewModel.heatBed(target)
                showHeatSheet = false
            },
            onDismiss = { showHeatSheet = false },
        )
    }

    // ── Reprint confirmation bottom sheet ────────────────────────────────────
    if (showReprintSheet) {
        ReprintBottomSheet(
            fileName = status?.fileName ?: "",
            isPrinting = status?.gcodeState?.uppercase() == "RUNNING",
            filesList = filesList,
            onOpen = { viewModel.requestFilesList() },
            onConfirm = { fileOverride ->
                viewModel.reprint(fileOverride)
                showReprintSheet = false
            },
            onDismiss = { showReprintSheet = false },
        )
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun GcodeStateBadge(gcodeState: String) {
    val (label, color) = when (gcodeState.uppercase()) {
        "RUNNING" -> "Друкується" to MaterialTheme.colorScheme.primary
        "FINISH", "FINISHED" -> "Завершено" to StatusConnected
        "PAUSE", "PAUSED" -> "Пауза" to MaterialTheme.colorScheme.secondary
        "FAILED", "ERROR", "STOPPED" -> "Помилка" to MaterialTheme.colorScheme.error
        "IDLE" -> "Очікує" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> return
    }
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DeviceConnectionRow(deviceState: DeviceState) {
    val (stateLabel, dotColor) = when (deviceState) {
        DeviceState.CONNECTED    -> "Підключено"    to StatusConnected
        DeviceState.CONNECTING   -> "Підключення…"  to MaterialTheme.colorScheme.secondary
        DeviceState.SCANNING     -> "Пошук…"        to MaterialTheme.colorScheme.secondary
        DeviceState.DISCONNECTED -> "Не підключено" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = dotColor, shape = CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "FilamentSense",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = dotColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun LastSyncRow(lastSyncMs: Long?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "⏱",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "Синхронізація",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatSyncTime(lastSyncMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TempRow(label: String, current: Float?, target: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        val currentStr = current?.let { "%.1f°C".format(it) } ?: "—"
        val targetStr = target?.let { "${it}°C" } ?: "—"
        Text(
            text = currentStr,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "→ $targetStr",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeatBedBottomSheet(
    isPrinting: Boolean,
    currentBedTemp: Int?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sliderValue by remember { mutableFloatStateOf(61f) }
    val target = sliderValue.roundToInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Температура столу",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            val stepHint = if (currentBedTemp != null)
                "Нагрів від поточних ${currentBedTemp}°C по 9°C кожні 15 сек"
            else
                "Нагрів від поточної температури по 9°C кожні 15 сек"
            Text(
                text = stepHint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Warning when printer is actively printing
            if (isPrinting) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ℹ️", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Принтер зараз друкує. Зміна температури може вплинути на поточний друк.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "$target°C",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 0f..110f,
                steps = 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0°C", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("110°C", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(target) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Нагріти до $target°C")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скасувати")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReprintBottomSheet(
    fileName: String,
    isPrinting: Boolean,
    filesList: List<String>,
    onOpen: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var fileOverride by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onOpen() }
    LaunchedEffect(filesList) {
        if (fileOverride.isEmpty() && filesList.isNotEmpty()) {
            fileOverride = filesList.first()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isPrinting) {
                // ── Blocked state: printer is currently printing ──────────────
                Text(
                    text = "Повторний друк недоступний",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("🚫", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Принтер зараз друкує",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Зупиніть або дочекайтесь завершення поточного друку, після чого повторний друк буде доступний.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Зрозуміло")
                }
            } else {
                // ── Normal confirmation state ─────────────────────────────────
                Text(
                    text = "Повторити друк",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                if (fileName.isNotEmpty()) {
                    Text(
                        text = "Файл:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = "Стіл буде поступово прогрітий до 55°C перед початком друку.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded && filesList.isNotEmpty(),
                    onExpandedChange = { if (filesList.isNotEmpty()) dropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = fileOverride,
                        onValueChange = { fileOverride = it; dropdownExpanded = false },
                        label = { Text("Файл для друку") },
                        placeholder = { Text("myfile_plate_2.gcode") },
                        singleLine = true,
                        trailingIcon = {
                            if (filesList.isNotEmpty())
                                ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    if (filesList.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                        ) {
                            filesList.forEach { file ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = file,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                    onClick = {
                                        fileOverride = file
                                        dropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onConfirm(fileOverride.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Підтвердити")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Скасувати")
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatRemainingTime(minutes: Int): String {
    if (minutes <= 0) return "0 хв"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h} год ${m} хв"
        h > 0 -> "${h} год"
        else -> "${m} хв"
    }
}
