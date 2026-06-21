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
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DAY_MS = 24L * 60 * 60 * 1000L

// Якщо між двома сусідніми днями пробіл > порогу — розриваємо лінію
private const val GAP_THRESHOLD_DAYS = 1.5

private data class DayData(
    val dayIndex: Long,  // днів з епохи (вісь X)
    val dayMs: Long,     // timestamp опівночі (підпис)
    val weight: Float,   // останній вимір дня (не середнє)
)

@Composable
fun WeightHistoryChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier,
    emptyHint: String = "Недостатньо даних",
) {
    // Групуємо по календарному дню; беремо останній вимір дня (найактуальніше значення)
    val dailyData = remember(measurements) {
        measurements
            .groupBy { m ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = m.timestamp
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .map { (dayMs, list) ->
                DayData(
                    dayIndex = dayMs / DAY_MS,
                    dayMs = dayMs,
                    weight = list.maxBy { it.timestamp }.remainingGrams,
                )
            }
            .sortedBy { it.dayMs }
    }

    // Розбиваємо на безперервні сегменти: кожен пробіл > 1.5 дня → розрив лінії
    val segments = remember(dailyData) {
        if (dailyData.isEmpty()) return@remember emptyList<List<DayData>>()
        val result = mutableListOf<List<DayData>>()
        var current = mutableListOf(dailyData.first())
        for (i in 1 until dailyData.size) {
            val gap = (dailyData[i].dayIndex - dailyData[i - 1].dayIndex).toDouble()
            if (gap > GAP_THRESHOLD_DAYS) {
                result += current.toList()
                current = mutableListOf()
            }
            current += dailyData[i]
        }
        result += current.toList()
        result
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(segments) {
        if (dailyData.size >= 2) {
            modelProducer.runTransaction {
                lineSeries {
                    // Кожен сегмент — окрема series(); між ними Vico не малює лінію
                    segments.forEach { segment ->
                        series(
                            x = segment.map { it.dayIndex.toDouble() },
                            y = segment.map { it.weight.toDouble() },
                        )
                    }
                }
            }
        }
    }

    val sdf = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }

    // X — реальний номер дня (dayIndex), форматуємо у дату
    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            sdf.format(Date(value.toLong() * DAY_MS))
        }
    }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "${value.toInt().formatWeight()} г" }
    }

    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)

    // Одна Line для всіх сегментів → однаковий колір, розриви лише між series()
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
            text = "Динаміка зміни залишку",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (dailyData.size < 2) {
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
