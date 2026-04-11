package com.filament.sense.ui.screen.spool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.filament.sense.domain.model.Measurement
import com.filament.sense.ui.components.BottomNav
import com.filament.sense.ui.components.DataRow
import com.filament.sense.ui.components.ThresholdBar
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ThresholdType(val label: String) {
    WARNING("Попередження"),
    CRITICAL("Критичний"),
    EMPTY("Порожньо"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolDetailScreen(
    id: Int,
    navController: NavController,
    viewModel: SpoolDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id) { viewModel.loadSpool(id) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNav(
                currentRoute = "spools",
                onItemClick = { route -> navController.navigate(route) },
            )
        },
    ) { innerPadding ->
        val spool = state.spool

        if (spool == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Завантаження...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Котушка",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { navController.navigate("spools/${id}/edit") }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Редагувати",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Color circle + name ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .background(Color(spool.colorArgb)),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = spool.name.ifEmpty { "Котушка #${id}" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // ── Main data card ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = "Основні дані",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${spool.remainingGrams.toInt()} г",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThresholdBar(progress = spool.remainingPercent)
                Spacer(modifier = Modifier.height(16.dp))
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                DataRow(icon = "⚖️", label = "Вага брутто поточна", value = "${spool.grossWeightGrams.toInt()} г")
                Spacer(modifier = Modifier.height(8.dp))
                DataRow(icon = "⚖️", label = "Вага брутто початкова", value = "${(spool.grossWeightGrams + spool.baselineWeight).toInt()} г")
                Spacer(modifier = Modifier.height(8.dp))
                val dateStr = spool.startDate?.let {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                } ?: "—"
                DataRow(icon = "📅", label = "Дата початку", value = dateStr)
                Spacer(modifier = Modifier.height(8.dp))
                DataRow(icon = "📦", label = "Номінальна вага", value = "${spool.nominalWeightGrams} г")
            }

            Spacer(modifier = Modifier.height(12.dp))

            var editingThreshold by remember { mutableStateOf<ThresholdType?>(null) }
            var editingValue by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                Text(
                    text = "Пороги сповіщень",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                DataRow(
                    icon = "⚠️", label = ThresholdType.WARNING.label, value = "${state.thresholdWarning} г",
                    onEdit = { editingThreshold = ThresholdType.WARNING; editingValue = state.thresholdWarning.toString() },
                )
                Spacer(modifier = Modifier.height(6.dp))
                DataRow(
                    icon = "🔴", label = ThresholdType.CRITICAL.label, value = "${state.thresholdCritical} г",
                    onEdit = { editingThreshold = ThresholdType.CRITICAL; editingValue = state.thresholdCritical.toString() },
                )
                Spacer(modifier = Modifier.height(6.dp))
                DataRow(
                    icon = "💀", label = ThresholdType.EMPTY.label, value = "${state.thresholdEmpty} г",
                    onEdit = { editingThreshold = ThresholdType.EMPTY; editingValue = state.thresholdEmpty.toString() },
                )
            }

            if (editingThreshold != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { editingThreshold = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp),
                    ) {
                        Text(
                            text = "Зміна значення порогу «${editingThreshold!!.label}»",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editingValue,
                            onValueChange = { v -> if (v.length <= 5) editingValue = v },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Значення") },
                            suffix = { Text("г", style = MaterialTheme.typography.bodyMedium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { editingThreshold = null },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Скасувати")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val newVal = editingValue.toIntOrNull() ?: return@Button
                                    val w = if (editingThreshold == ThresholdType.WARNING) newVal else state.thresholdWarning
                                    val c = if (editingThreshold == ThresholdType.CRITICAL) newVal else state.thresholdCritical
                                    val e = if (editingThreshold == ThresholdType.EMPTY) newVal else state.thresholdEmpty
                                    viewModel.updateThresholds(w, c, e)
                                    editingThreshold = null
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Зберегти")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── History chart ────────────────────────────────────────────
            WeightHistoryChart(
                measurements = state.measurements,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Active toggle ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Активна котушка для пристрою",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = spool.isActive,
                    onCheckedChange = { enabled ->
                        if (spool.isActive && !enabled) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Недоступно. Активуйте іншу котушку — ця стане неактивною автоматично"
                                )
                            }
                        } else if (!spool.isActive && enabled) {
                            viewModel.onToggleActive(id, spool.isActive)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── "Зробити активною?" AlertDialog ──────────────────────────────
        if (state.showSetActiveDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = {
                    Text(
                        text = "Зробити активною?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Text(
                        text = "Котушку «${spool.name.ifEmpty { "Котушка #${id}" }}» буде встановлено як активну для пристрою.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmSetActive) {
                        Text("Так", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text("Скасувати", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
            )
        }
    }
}

// ── Weight history chart (Vico) ───────────────────────────────────────────────

@Composable
private fun WeightHistoryChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(measurements) {
        if (measurements.size >= 2) {
            modelProducer.runTransaction {
                lineSeries { series(measurements.map { it.remainingGrams.toDouble() }) }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "Динаміка зміни залишку",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (measurements.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Недостатньо даних\n(вимірювання кожні 5 хв)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
        }
    }
}
