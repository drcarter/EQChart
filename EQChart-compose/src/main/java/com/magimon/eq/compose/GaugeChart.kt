package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeChartStyleOptions
import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class ComposeResolvedGaugeValue(
    val clampedValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val progress: Float,
    val label: String?,
)

internal data class ComposeResolvedGaugeRange(
    val startRatio: Float,
    val endRatio: Float,
    val color: Int,
)

internal data class ComposeGaugeGeometry(
    val center: Offset,
    val radius: Float,
    val arcRect: Rect,
    val valueY: Float,
    val labelY: Float,
    val minMaxBaselineOffset: Float,
)

/**
 * Compose gauge chart for a single [GaugeValue].
 *
 * This composable mirrors the View-based gauge behavior:
 * - invalid values fall back to [GaugeChartPresentationOptions.emptyText]
 * - the rendered value is clamped into its domain
 * - optional [ranges] are drawn behind the progress arc
 * - value updates animate when enabled in [presentationOptions]
 *
 * @param value Current gauge reading and domain definition
 * @param modifier Standard Compose modifier for size and layout
 * @param ranges Optional threshold bands mapped onto the same domain as [value]
 * @param styleOptions Colors and dimensions for the rendered gauge
 * @param presentationOptions Layout, label visibility, and animation behavior
 */
@Composable
fun GaugeChart(
    value: GaugeValue,
    modifier: Modifier = Modifier,
    ranges: List<GaugeRange> = emptyList(),
    styleOptions: GaugeChartStyleOptions = GaugeChartStyleOptions(),
    presentationOptions: GaugeChartPresentationOptions = GaugeChartPresentationOptions(),
) {
    val density = LocalDensity.current
    val resolvedValue = remember(value) { resolveComposeGaugeValue(value) }
    val resolvedRanges = remember(ranges, resolvedValue) {
        resolvedValue?.let { resolveComposeGaugeRanges(ranges, it.minValue, it.maxValue) } ?: emptyList()
    }
    val progress = remember { Animatable(resolvedValue?.progress ?: 0f) }

    LaunchedEffect(
        resolvedValue?.progress,
        presentationOptions.animateOnValueChange,
        presentationOptions.animationDurationMs,
    ) {
        val target = resolvedValue?.progress ?: 0f
        if (resolvedValue != null && presentationOptions.animateOnValueChange) {
            progress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = presentationOptions.animationDurationMs.toInt().coerceAtLeast(0)),
            )
        } else {
            progress.snapTo(target)
        }
    }

    Canvas(modifier = modifier) {
        drawRect(styleOptions.backgroundColor.toComposeColor())

        if (resolvedValue == null) {
            val text = presentationOptions.emptyText?.trim().orEmpty()
            if (text.isNotEmpty()) {
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    size.width * 0.5f,
                    size.height * 0.5f,
                    newTextPaint(
                        color = styleOptions.labelTextColor,
                        textSizePx = with(density) { 13f.sp.toPx() },
                        align = Paint.Align.CENTER,
                    ),
                )
            }
            return@Canvas
        }

        val geometry = resolveComposeGaugeGeometry(size.width, size.height, density, styleOptions, presentationOptions)
        if (geometry.radius <= 0f) return@Canvas

        val strokeStyle = Stroke(
            width = with(density) { styleOptions.progressThicknessDp.dp.toPx() },
            cap = StrokeCap.Round,
        )

        drawArc(
            color = styleOptions.trackColor.toComposeColor(),
            startAngle = presentationOptions.startAngleDeg,
            sweepAngle = presentationOptions.sweepAngleDeg,
            useCenter = false,
            topLeft = geometry.arcRect.topLeft,
            size = geometry.arcRect.size,
            style = strokeStyle,
        )

        resolvedRanges.forEach { range ->
            val start = composeGaugeAngle(range.startRatio, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
            val sweep = presentationOptions.sweepAngleDeg * (range.endRatio - range.startRatio)
            if (sweep > 0f) {
                drawArc(
                    color = range.color.toComposeColor(),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = geometry.arcRect.topLeft,
                    size = geometry.arcRect.size,
                    style = strokeStyle,
                )
            }
        }

        if (progress.value > 0f) {
            drawArc(
                color = styleOptions.progressColor.toComposeColor(),
                startAngle = presentationOptions.startAngleDeg,
                sweepAngle = presentationOptions.sweepAngleDeg * progress.value.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = geometry.arcRect.topLeft,
                size = geometry.arcRect.size,
                style = strokeStyle,
            )
        }

        if (presentationOptions.showTicks) {
            val tickOuter = geometry.radius + (with(density) { styleOptions.progressThicknessDp.dp.toPx() } * 0.15f)
            val tickInner = tickOuter - with(density) { styleOptions.tickLengthDp.dp.toPx() }
            val tickCount = presentationOptions.tickCount.coerceAtLeast(1)
            repeat(tickCount + 1) { index ->
                val ratio = index.toFloat() / tickCount.toFloat()
                val angle = composeGaugeAngle(ratio, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
                val start = composeGaugePoint(geometry.center, tickOuter, angle)
                val end = composeGaugePoint(geometry.center, tickInner, angle)
                drawLine(
                    color = styleOptions.tickColor.toComposeColor(),
                    start = start,
                    end = end,
                    strokeWidth = with(density) { styleOptions.tickThicknessDp.dp.toPx() },
                    cap = StrokeCap.Round,
                )
            }
        }

        val indicatorAngle = composeGaugeAngle(progress.value, presentationOptions.startAngleDeg, presentationOptions.sweepAngleDeg)
        val indicatorEnd = composeGaugePoint(
            geometry.center,
            (geometry.radius - with(density) { styleOptions.progressThicknessDp.dp.toPx() * 0.75f }).coerceAtLeast(0f),
            indicatorAngle,
        )
        drawLine(
            color = styleOptions.indicatorColor.toComposeColor(),
            start = geometry.center,
            end = indicatorEnd,
            strokeWidth = with(density) { styleOptions.indicatorThicknessDp.dp.toPx() },
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = styleOptions.indicatorCenterColor.toComposeColor(),
            radius = with(density) { styleOptions.indicatorCenterRadiusDp.dp.toPx() },
            center = geometry.center,
        )

        if (presentationOptions.showValueText) {
            drawContext.canvas.nativeCanvas.drawText(
                formatComposeGaugeNumber(resolvedValue.clampedValue),
                geometry.center.x,
                geometry.valueY,
                newTextPaint(
                    color = styleOptions.valueTextColor,
                    textSizePx = with(density) { styleOptions.valueTextSizeSp.sp.toPx() },
                    align = Paint.Align.CENTER,
                    bold = true,
                ),
            )
        }

        if (presentationOptions.showCenterLabel) {
            val label = resolvedValue.label?.takeIf { it.isNotBlank() }
            if (label != null) {
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    geometry.center.x,
                    geometry.labelY,
                    newTextPaint(
                        color = styleOptions.labelTextColor,
                        textSizePx = with(density) { styleOptions.labelTextSizeSp.sp.toPx() },
                        align = Paint.Align.CENTER,
                    ),
                )
            }
        }

        if (presentationOptions.showMinMaxLabels) {
            val textPaint = newTextPaint(
                color = styleOptions.minMaxTextColor,
                textSizePx = with(density) { styleOptions.minMaxTextSizeSp.sp.toPx() },
                align = Paint.Align.CENTER,
            )
            val radius = geometry.radius + with(density) { styleOptions.progressThicknessDp.dp.toPx() * 0.7f }
            val startPoint = composeGaugePoint(geometry.center, radius, presentationOptions.startAngleDeg)
            val endPoint = composeGaugePoint(geometry.center, radius, presentationOptions.startAngleDeg + presentationOptions.sweepAngleDeg)
            drawContext.canvas.nativeCanvas.drawText(
                formatComposeGaugeNumber(resolvedValue.minValue),
                startPoint.x,
                startPoint.y + geometry.minMaxBaselineOffset,
                textPaint,
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatComposeGaugeNumber(resolvedValue.maxValue),
                endPoint.x,
                endPoint.y + geometry.minMaxBaselineOffset,
                textPaint,
            )
        }
    }
}

internal fun resolveComposeGaugeValue(value: GaugeValue?): ComposeResolvedGaugeValue? {
    value ?: return null
    if (!value.value.isFinite() || !value.minValue.isFinite() || !value.maxValue.isFinite()) return null
    if (value.maxValue <= value.minValue) return null
    val clamped = value.value.coerceIn(value.minValue, value.maxValue)
    val span = value.maxValue - value.minValue
    val progress = if (span <= 0.0) 0f else ((clamped - value.minValue) / span).toFloat().coerceIn(0f, 1f)
    return ComposeResolvedGaugeValue(
        clampedValue = clamped,
        minValue = value.minValue,
        maxValue = value.maxValue,
        progress = progress,
        label = value.label,
    )
}

internal fun resolveComposeGaugeRanges(
    ranges: List<GaugeRange>,
    minValue: Double,
    maxValue: Double,
): List<ComposeResolvedGaugeRange> {
    if (!minValue.isFinite() || !maxValue.isFinite() || maxValue <= minValue) return emptyList()
    val span = maxValue - minValue
    return ranges.mapNotNull { range ->
        if (!range.startValue.isFinite() || !range.endValue.isFinite()) return@mapNotNull null
        val start = range.startValue.coerceIn(minValue, maxValue)
        val end = range.endValue.coerceIn(minValue, maxValue)
        if (end <= start) return@mapNotNull null
        ComposeResolvedGaugeRange(
            startRatio = ((start - minValue) / span).toFloat().coerceIn(0f, 1f),
            endRatio = ((end - minValue) / span).toFloat().coerceIn(0f, 1f),
            color = range.color,
        )
    }
}

internal fun resolveComposeGaugeGeometry(
    width: Float,
    height: Float,
    density: Density,
    styleOptions: GaugeChartStyleOptions,
    presentationOptions: GaugeChartPresentationOptions,
): ComposeGaugeGeometry {
    with(density) {
        val padding = styleOptions.contentPaddingDp.dp.toPx()
        val thickness = styleOptions.progressThicknessDp.dp.toPx()
        val valueTextBlock = styleOptions.valueTextSizeSp.sp.toPx() + styleOptions.labelTextSizeSp.sp.toPx() + 20.dp.toPx()
        val minMaxReserve = if (presentationOptions.showMinMaxLabels) {
            styleOptions.minMaxTextSizeSp.sp.toPx() + 14.dp.toPx()
        } else {
            8.dp.toPx()
        }

        val availableWidth = width - (padding * 2f)
        val centerCandidateY = height - padding - minMaxReserve
        var radius = min((availableWidth * 0.5f) - (thickness * 0.5f), centerCandidateY - padding - (thickness * 0.5f))
        var centerY = max(padding + thickness, height - padding - minMaxReserve - valueTextBlock)
        val maxAllowedRadius = min((availableWidth * 0.5f) - (thickness * 0.5f), centerY - padding - (thickness * 0.5f))
        radius = min(radius, maxAllowedRadius).coerceAtLeast(0f)
        centerY = max(centerY, padding + radius + (thickness * 0.5f))

        val center = Offset(width * 0.5f, centerY)
        return ComposeGaugeGeometry(
            center = center,
            radius = radius,
            arcRect = Rect(
                offset = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            ),
            valueY = centerY - (radius * 0.18f),
            labelY = centerY - (radius * 0.18f) + styleOptions.labelTextSizeSp.sp.toPx() + 10.dp.toPx(),
            minMaxBaselineOffset = styleOptions.minMaxTextSizeSp.sp.toPx() + 4.dp.toPx(),
        )
    }
}

internal fun composeGaugeAngle(ratio: Float, startAngleDeg: Float, sweepAngleDeg: Float): Float {
    return startAngleDeg + (sweepAngleDeg * ratio.coerceIn(0f, 1f))
}

internal fun composeGaugePoint(center: Offset, radius: Float, angleDeg: Float): Offset {
    return center + degreeToOffset(angleDeg, radius)
}

internal fun formatComposeGaugeNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format("%.1f", value)
    }
}
