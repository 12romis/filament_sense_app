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
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BUCKET_3H_MS = 3L * 60 * 60 * 1000L

// Розрив лінії, якщо між сусідніми точками пробіл більший за 12 год.
// Береться в реальному часі (а не в кількості бакетів графіка), бо стара історія
// в БД ущільнюється до одного запису на 8-год кошик (SpoolRepositoryImpl.compactAllSpools) —
// поріг має бути суттєво більшим за 8 год, інакше й звичайна ущільнена історія
// (без жодного реального розриву підключення) хибно рвалась би на шматки.
private const val GAP_THRESHOLD_MS = 12L * 60 * 60 * 1000L

private data class BucketData(
    val bucketIndex: Long,  // порядковий номер бакету (timestamp / BUCKET_3H_MS)
    val bucketMs: Long,     // мілісекунди початку бакету
    val weight: Float,      // середній залишок за бакет
)

@Composable
fun WeightHistoryChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier,
    emptyHint: String = "Недостатньо даних",
) {
    val buckets = remember(measurements) {
        measurements
            .groupBy { m -> (m.timestamp / BUCKET_3H_MS) * BUCKET_3H_MS }
            .map { (bucketMs, list) ->
                BucketData(
                    bucketIndex = bucketMs / BUCKET_3H_MS,
                    bucketMs = bucketMs,
                    weight = list.maxBy { it.timestamp }.remainingGrams,
                )
            }
            .sortedBy { it.bucketMs }
    }

    val segments = remember(buckets) {
        if (buckets.isEmpty()) return@remember emptyList<List<BucketData>>()
        val result = mutableListOf<List<BucketData>>()
        var current = mutableListOf(buckets.first())
        for (i in 1 until buckets.size) {
            val gap = buckets[i].bucketMs - buckets[i - 1].bucketMs
            if (gap > GAP_THRESHOLD_MS) {
                result += current.toList()
                current = mutableListOf()
            }
            current += buckets[i]
        }
        result += current.toList()
        result
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(segments) {
        if (buckets.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    segments.forEach { segment ->
                        series(
                            x = segment.map { it.bucketIndex.toDouble() },
                            y = segment.map { it.weight.toDouble() },
                        )
                    }
                }
            }
        }
    }

    val sdfDate = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val sdfHour = remember { SimpleDateFormat("HH", Locale.getDefault()) }

    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val ts = value.toLong() * BUCKET_3H_MS
            val date = Date(ts)
            "${sdfDate.format(date)}·${sdfHour.format(date)}"
        }
    }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt().formatWeight()} г" }
    }

    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    val line = LineCartesianLayer.rememberLine()
    val lineProvider = remember(line) { LineCartesianLayer.LineProvider.series(line) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = "Залишок філаменту",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (buckets.size < 2) {
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
                    rememberLineCartesianLayer(lineProvider = lineProvider),
                    startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = xFormatter,
                        labelRotationDegrees = -45f,
                        itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 2 }) },
                    ),
                ),
                modelProducer = modelProducer,
                scrollState = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
    }
}