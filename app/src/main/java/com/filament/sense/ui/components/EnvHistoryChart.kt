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

private val sdfDateGlobal = SimpleDateFormat("dd.MM", Locale.getDefault())
private val sdfHourGlobal = SimpleDateFormat("HH", Locale.getDefault())

private const val BUCKET_3H_MS = 3L * 60 * 60 * 1000L

// Розрив лінії якщо між бакетами пробіл більший за 1.5 бакети (4.5 год)
private const val GAP_THRESHOLD_BUCKETS = 1.5

private data class EnvBucket(
    val bucketIndex: Long,   // порядковий номер бакету (timestamp / BUCKET_3H_MS)
    val bucketMs: Long,      // мілісекунди початку бакету (для підпису осі X)
    val temperature: Float?,
    val humidity: Float?,
)

@Composable
fun EnvHistoryChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier,
) {
    val buckets = remember(measurements) {
        measurements
            .groupBy { m -> (m.timestamp / BUCKET_3H_MS) * BUCKET_3H_MS }
            .map { (bucketMs, list) ->
                val temps = list.mapNotNull { it.temperature }
                val hums = list.mapNotNull { it.humidity }
                EnvBucket(
                    bucketIndex = bucketMs / BUCKET_3H_MS,
                    bucketMs = bucketMs,
                    temperature = if (temps.isNotEmpty()) temps.average().toFloat() else null,
                    humidity = if (hums.isNotEmpty()) hums.average().toFloat() else null,
                )
            }
            .sortedBy { it.bucketMs }
    }

    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            val ts = value.toLong() * BUCKET_3H_MS
            val date = Date(ts)
            "${sdfDateGlobal.format(date)}·${sdfHourGlobal.format(date)}"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SingleEnvChart(
            title = "Температура",
            buckets = buckets,
            getValue = { it.temperature },
            yFormatter = CartesianValueFormatter { _, v, _ -> "${v.toInt()}°C" },
            xFormatter = xFormatter,
        )
        Spacer(modifier = Modifier.height(12.dp))
        SingleEnvChart(
            title = "Вологість",
            buckets = buckets,
            getValue = { it.humidity },
            yFormatter = CartesianValueFormatter { _, v, _ -> "${v.toInt()}%" },
            xFormatter = xFormatter,
        )
    }
}

@Composable
private fun SingleEnvChart(
    title: String,
    buckets: List<EnvBucket>,
    getValue: (EnvBucket) -> Float?,
    yFormatter: CartesianValueFormatter,
    xFormatter: CartesianValueFormatter,
) {
    val validBuckets = remember(buckets) { buckets.filter { getValue(it) != null } }

    val segments = remember(validBuckets) {
        if (validBuckets.isEmpty()) return@remember emptyList<List<EnvBucket>>()
        val result = mutableListOf<List<EnvBucket>>()
        var current = mutableListOf(validBuckets.first())
        for (i in 1 until validBuckets.size) {
            val gap = (validBuckets[i].bucketIndex - validBuckets[i - 1].bucketIndex).toDouble()
            if (gap > GAP_THRESHOLD_BUCKETS) {
                result += current.toList()
                current = mutableListOf()
            }
            current += validBuckets[i]
        }
        result += current.toList()
        result
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(segments) {
        if (validBuckets.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    segments.forEach { segment ->
                        series(
                            x = segment.map { it.bucketIndex.toDouble() },
                            y = segment.map { getValue(it)!!.toDouble() },
                        )
                    }
                }
            }
        }
    }

    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    val line = LineCartesianLayer.rememberLine()
    val lineProvider = remember(line) { LineCartesianLayer.LineProvider.series(line) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (validBuckets.size < 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Недостатньо даних",
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
                    .height(160.dp),
            )
        }
    }
}