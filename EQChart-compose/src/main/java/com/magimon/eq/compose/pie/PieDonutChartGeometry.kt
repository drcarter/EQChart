package com.magimon.eq.compose.pie

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.magimon.eq.compose.internal.isAngleInSweep
import com.magimon.eq.compose.internal.normalizeAngle
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal data class PieSegment(
    val index: Int,
    val slice: PieSlice,
    val ratio: Float,
    val start: Float,
    val sweep: Float,
    val mid: Float,
)

internal data class PieChartGeometry(
    val center: Offset,
    val radius: Float,
    val innerRadius: Float,
    val chartBottom: Float,
    val legendReservedHeight: Float,
)

internal fun buildPieSegments(
    slices: List<PieSlice>,
    startAngle: Float,
    clockwise: Boolean,
): List<PieSegment> {
    val valid = slices.filter { it.value.isFinite() && it.value > 0.0 }
    if (valid.isEmpty()) return emptyList()
    val total = valid.sumOf { it.value }
    if (!total.isFinite() || total <= 0.0) return emptyList()

    val sign = if (clockwise) 1f else -1f
    var cursor = startAngle
    var consumed = 0f

    return valid.mapIndexed { index, slice ->
        val ratio = (slice.value / total).toFloat().coerceAtLeast(0f)
        val sweepAbs = if (index == valid.lastIndex) {
            (360f - consumed).coerceAtLeast(0f)
        } else {
            (ratio * 360f).coerceAtLeast(0f)
        }
        consumed += sweepAbs
        val sweep = sweepAbs * sign
        val mid = cursor + (sweep * 0.5f)
        PieSegment(
            index = index,
            slice = slice,
            ratio = ratio,
            start = cursor,
            sweep = sweep,
            mid = mid,
        ).also {
            cursor += sweep
        }
    }
}

internal fun resolvePieLegendReservedHeight(
    availableWidth: Float,
    segments: List<PieSegment>,
    styleOptions: PieDonutStyleOptions,
    presentationOptions: PieDonutPresentationOptions,
    density: Density,
    legendTextPaint: Paint,
): Float {
    if (!presentationOptions.showLegend || segments.isEmpty()) return 0f

    val markerSize = with(density) { styleOptions.legendMarkerSizeDp.dp.toPx() }
    val markerTextGap = with(density) { styleOptions.legendMarkerTextGapDp.dp.toPx() }
    val itemGap = with(density) { styleOptions.legendItemSpacingDp.dp.toPx() }
    val rowGap = with(density) { styleOptions.legendRowSpacingDp.dp.toPx() }
    val rowHeight = max(markerSize, legendTextPaint.fontSpacing)

    val startX = with(density) { styleOptions.contentPaddingDp.dp.toPx() + presentationOptions.legendLeftMarginDp.dp.toPx() }
    val maxX = availableWidth - with(density) { styleOptions.contentPaddingDp.dp.toPx() }

    var cursorX = startX
    var rowCount = 1
    segments.forEach { segment ->
        val text = segment.slice.label.ifBlank { "Slice" }
        val textWidth = legendTextPaint.measureText(text)
        val itemWidth = markerSize + markerTextGap + textWidth
        if (cursorX + itemWidth > maxX && cursorX > startX) {
            rowCount += 1
            cursorX = startX
        }
        cursorX += itemWidth + itemGap
    }

    return with(density) { presentationOptions.legendTopMarginDp.dp.toPx() } +
        (rowCount * rowHeight) +
        ((rowCount - 1).coerceAtLeast(0) * rowGap) +
        with(density) { presentationOptions.legendBottomMarginDp.dp.toPx() }
}

internal fun computePieChartGeometry(
    availableWidth: Float,
    availableHeight: Float,
    segments: List<PieSegment>,
    styleOptions: PieDonutStyleOptions,
    presentationOptions: PieDonutPresentationOptions,
    density: Density,
    legendTextPaint: Paint,
    donutInnerRatio: Float,
): PieChartGeometry {
    val legendReserved = resolvePieLegendReservedHeight(
        availableWidth = availableWidth,
        segments = segments,
        styleOptions = styleOptions,
        presentationOptions = presentationOptions,
        density = density,
        legendTextPaint = legendTextPaint,
    )
    val chartBottom = (availableHeight - legendReserved).coerceAtLeast(0f)
    val contentPadding = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
    val radius = (min(availableWidth, chartBottom) * 0.5f - contentPadding).coerceAtLeast(0f)
    return PieChartGeometry(
        center = Offset(availableWidth * 0.5f, chartBottom * 0.5f),
        radius = radius,
        innerRadius = radius * donutInnerRatio.coerceIn(0f, 0.92f),
        chartBottom = chartBottom,
        legendReservedHeight = legendReserved,
    )
}

internal fun hitTestPieSegment(
    tap: Offset,
    geometry: PieChartGeometry,
    segments: List<PieSegment>,
): PieSegment? {
    if (segments.isEmpty()) return null
    val dx = tap.x - geometry.center.x
    val dy = tap.y - geometry.center.y
    val dist = hypot(dx, dy)
    if (dist < geometry.innerRadius || dist > geometry.radius) return null

    val angle = normalizeAngle(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
    return segments.firstOrNull { isAngleInSweep(angle, it.start, it.sweep) }
}

internal fun shouldPlacePieLabelInside(
    segment: PieSegment,
    labelPosition: PieLabelPosition,
    midRadius: Float,
    textWidth: Float,
): Boolean {
    val sweepAbs = kotlin.math.abs(segment.sweep)
    val availableArc = Math.toRadians(sweepAbs.toDouble()).toFloat() * midRadius.coerceAtLeast(1f)
    return when (labelPosition) {
        PieLabelPosition.INSIDE -> true
        PieLabelPosition.OUTSIDE -> false
        PieLabelPosition.AUTO -> sweepAbs >= 20f && textWidth <= availableArc * 0.95f
    }
}

internal fun isPieLabelOnRightSide(angle: Float): Boolean {
    val normalized = normalizeAngle(angle)
    return normalized <= 90f || normalized >= 270f
}
