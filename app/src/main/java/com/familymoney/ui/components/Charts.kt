package com.familymoney.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familymoney.ui.theme.CategoryPalette
import com.familymoney.util.Money
import kotlin.math.max
import kotlin.math.min

/**
 * Hand-rolled Compose charts. No charting dependency: the spec asks for simple
 * graphs (section 51) and Canvas keeps the APK small and the styling on-brand.
 */

data class BarPoint(val label: String, val value: Double, val secondary: Double = 0.0)

/** Grouped income/expense bars, one pair per month. */
@Composable
fun GroupedBarChart(
    points: List<BarPoint>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.tertiary,
    secondaryColor: Color = MaterialTheme.colorScheme.error,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    if (points.isEmpty()) return
    val maxValue = points.maxOf { max(it.value, it.secondary) }.coerceAtLeast(1.0)
    val progress by animateFloatAsState(1f, tween(700), label = "bars")

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val slotWidth = size.width / points.size
            val barWidth = (slotWidth * 0.28f).coerceAtMost(22f)
            val gap = barWidth * 0.35f
            val bottom = size.height - 4f

            points.forEachIndexed { index, point ->
                // RTL: first month renders on the right.
                val center = size.width - (index + 0.5f) * slotWidth
                val h1 = ((point.value / maxValue) * (size.height - 16f) * progress).toFloat()
                val h2 = ((point.secondary / maxValue) * (size.height - 16f) * progress).toFloat()

                drawRoundBar(center + (barWidth + gap) / 2f, bottom, barWidth, h1, primaryColor)
                if (point.secondary > 0) {
                    drawRoundBar(center - (barWidth + gap) / 2f, bottom, barWidth, h2, secondaryColor)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            points.reversed().forEach { p ->
                Text(
                    p.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundBar(
    centerX: Float,
    bottom: Float,
    width: Float,
    barHeight: Float,
    color: Color
) {
    val h = barHeight.coerceAtLeast(3f)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.65f))),
        topLeft = Offset(centerX - width / 2f, bottom - h),
        size = Size(width, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2.2f, width / 2.2f)
    )
}

/** Smooth net-worth / projection line with a soft fill underneath. */
@Composable
fun LineChart(
    values: List<Double>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 170.dp,
    showDots: Boolean = true
) {
    if (values.size < 2) return
    val minV = values.min()
    val maxV = values.max()
    val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val progress by animateFloatAsState(1f, tween(800), label = "line")
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val padH = 10f
            val usableH = size.height - padH * 2
            val step = size.width / (values.size - 1)

            repeat(4) { i ->
                val y = padH + usableH * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }

            // RTL: oldest point on the right, newest on the left.
            val pts = values.mapIndexed { i, v ->
                val x = size.width - i * step
                val y = padH + usableH - (((v - minV) / span) * usableH).toFloat()
                Offset(x, y)
            }

            val visible = (pts.size * progress).toInt().coerceAtLeast(2)
            val shown = pts.take(visible)

            val path = Path().apply {
                moveTo(shown[0].x, shown[0].y)
                for (i in 1 until shown.size) {
                    val prev = shown[i - 1]
                    val cur = shown[i]
                    val midX = (prev.x + cur.x) / 2f
                    cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
            }

            val fill = Path().apply {
                addPath(path)
                lineTo(shown.last().x, size.height)
                lineTo(shown[0].x, size.height)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                )
            )
            drawPath(path, lineColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))

            if (showDots) {
                shown.forEach { p ->
                    drawCircle(Color.White, radius = 5f, center = p)
                    drawCircle(lineColor, radius = 5f, center = p, style = Stroke(width = 2.5f))
                }
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                labels.reversed().forEach { l ->
                    Text(
                        l,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

data class DonutSlice(val label: String, val value: Double, val color: Color, val emoji: String = "")

/** Spending-by-category ring with the total in the middle. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 190.dp
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) return
    val progress by animateFloatAsState(1f, tween(800), label = "donut")

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.17f
            val inset = stroke / 2f
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = ((slice.value / total) * 360f * progress).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                centerValue,
                style = com.familymoney.ui.theme.MoneyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DonutLegend(slices: List<DonutSlice>, modifier: Modifier = Modifier, maxItems: Int = 6) {
    val total = slices.sumOf { it.value }.takeIf { it > 0 } ?: return
    Column(modifier.fillMaxWidth()) {
        slices.take(maxItems).forEach { slice ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                Spacer(Modifier.width(10.dp))
                Text(
                    "${slice.emoji} ${slice.label}".trim(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    Money.percent(slice.value / total * 100),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    Money.format(slice.value),
                    style = com.familymoney.ui.theme.MoneySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Stacked bar showing where the money sits (liquid / savings / investments). */
@Composable
fun CompositionBar(
    segments: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 14.dp
) {
    val total = segments.sumOf { it.second }
    if (total <= 0.0) return
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        segments.forEachIndexed { i, (_, value) ->
            if (value <= 0.0) return@forEachIndexed
            Box(
                Modifier
                    .weight((value / total).toFloat().coerceAtLeast(0.02f))
                    .height(height)
                    .clip(CircleShape)
                    .background(CategoryPalette[i % CategoryPalette.size])
            )
        }
    }
}

/** Compact 7-point sparkline for tiles. */
@Composable
fun Sparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (values.size < 2) return
    val minV = values.min()
    val span = (values.max() - minV).takeIf { it > 0 } ?: 1.0
    Canvas(modifier.height(34.dp).fillMaxWidth()) {
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width - i * step
            val y = size.height - (((v - minV) / span) * size.height * 0.85f).toFloat() - 3f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

/** Semi-circular gauge for savings rate. */
@Composable
fun GaugeArc(
    percent: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary
) {
    val p = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(p, tween(800), label = "gauge")
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    Canvas(modifier) {
        val stroke = size.minDimension * 0.14f
        val inset = stroke / 2f
        drawArc(
            color = track,
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height * 2 - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)),
            startAngle = 180f, sweepAngle = 180f * animated, useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height * 2 - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

internal fun paletteFor(index: Int): Color = CategoryPalette[index % CategoryPalette.size]

internal fun niceAxisLabel(value: Double): String = Money.compact(value)

internal fun clampChartValues(values: List<Double>, maxPoints: Int = 12): List<Double> =
    if (values.size <= maxPoints) values else values.takeLast(maxPoints)

internal fun safeRatio(a: Double, b: Double): Float =
    if (b == 0.0) 0f else min(1f, max(0f, (a / b).toFloat()))
