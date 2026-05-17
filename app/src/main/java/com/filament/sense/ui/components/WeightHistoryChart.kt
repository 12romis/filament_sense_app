package com.filament.sense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filament.sense.domain.model.Measurement
import com.filament.sense.ui.util.formatWeight
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DAY_MS = 24L * 60 * 60 * 1000

@Composable
fun WeightHistoryChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier,
    emptyHint: String = "Недостатньо даних",
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(measurements) {
        if (measurements.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = measurements.indices.map { it.toDouble() },
                        y = measurements.map { it.remainingGrams.toDouble() },
                    )
                }
            }
        }
    }

    val xFormatter = remember(measurements) {
        if (measurements.size < 2) {
            CartesianValueFormatter.decimal()
        } else {
            val rangeMs = measurements.last().timestamp - measurements.first().timestamp
            val sdf = if (rangeMs < DAY_MS) {
                SimpleDateFormat("HH:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("dd.MM", Locale.getDefault())
            }
            CartesianValueFormatter { _, value, _ ->
                val idx = value.toInt().coerceIn(measurements.indices)
                sdf.format(Date(measurements[idx].timestamp))
            }
        }
    }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt().formatWeight()} г" }
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
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
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
                    text = emptyHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = xFormatter,
                        labelRotationDegrees = -45f,
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }
}
