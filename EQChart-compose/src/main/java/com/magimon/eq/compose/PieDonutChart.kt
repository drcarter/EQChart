package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class PieSegment(
    val index: Int,
    val slice: PieSlice,
    val ratio: Float,
    val start: Float,
    val sweep: Float,
    val mid: Float,
)

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    styleOptions: PieDonutStyleOptions = PieDonutStyleOptions(),
    presentationOptions: PieDonutPresentationOptions = PieDonutPresentationOptions(centerText = null, centerSubText = null),
    onSliceClick: ((sliceIndex: Int, slice: PieSlice, payload: Any?) -> Unit)? = null,
) {
    PieDonutChartInternal(
        slices = slices,
        modifier = modifier,
        styleOptions = styleOptions,
        presentationOptions = presentationOptions,
        donutInnerRatio = 0f,
        onSliceClick = onSliceClick,
    )
}

@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    styleOptions: PieDonutStyleOptions = PieDonutStyleOptions(),
    presentationOptions: PieDonutPresentationOptions = PieDonutPresentationOptions(),
    innerRadiusRatio: Float = 0.58f,
    onSliceClick: ((sliceIndex: Int, slice: PieSlice, payload: Any?) -> Unit)? = null,
) {
    PieDonutChartInternal(
        slices = slices,
        modifier = modifier,
        styleOptions = styleOptions,
        presentationOptions = presentationOptions,
        donutInnerRatio = innerRadiusRatio,
        onSliceClick = onSliceClick,
    )
}

@Composable
private fun PieDonutChartInternal(
    slices: List<PieSlice>,
    modifier: Modifier,
    styleOptions: PieDonutStyleOptions,
    presentationOptions: PieDonutPresentationOptions,
    donutInnerRatio: Float,
    onSliceClick: ((sliceIndex: Int, slice: PieSlice, payload: Any?) -> Unit)?,
) {
    val density = LocalDensity.current
    val segments = remember(slices, presentationOptions.startAngleDeg, presentationOptions.clockwise) {
        buildPieSegments(slices, presentationOptions.startAngleDeg, presentationOptions.clockwise)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier.pointerInput(segments, donutInnerRatio) {
            detectTapGestures { tap ->
                if (segments.isEmpty()) return@detectTapGestures
                val legendPaint = newTextPaint(
                    color = styleOptions.legendTextColor,
                    textSizePx = with(density) { styleOptions.legendTextSizeSp.sp.toPx() },
                )
                val legendReserved = resolvePieLegendReservedHeight(
                    availableWidth = size.width.toFloat(),
                    segments = segments,
                    styleOptions = styleOptions,
                    presentationOptions = presentationOptions,
                    density = density,
                    legendTextPaint = legendPaint,
                )
                val chartBottom = (size.height.toFloat() - legendReserved).coerceAtLeast(0f)
                val center = Offset(size.width.toFloat() * 0.5f, chartBottom * 0.5f)
                val contentPadding = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
                val radius = (min(size.width.toFloat(), chartBottom) * 0.5f - contentPadding).coerceAtLeast(0f)
                val inner = radius * donutInnerRatio.coerceIn(0f, 0.92f)
                val dx = tap.x - center.x
                val dy = tap.y - center.y
                val dist = hypot(dx, dy)
                if (dist < inner || dist > radius) {
                    selectedIndex = null
                    return@detectTapGestures
                }
                val angle = normalizeAngle(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
                val hit = segments.firstOrNull { isAngleInSweep(angle, it.start, it.sweep) }
                selectedIndex = hit?.index
                if (hit != null) {
                    onSliceClick?.invoke(hit.index, hit.slice, hit.slice.payload)
                }
            }
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (segments.isEmpty()) {
                val emptyText = presentationOptions.emptyText?.trim().orEmpty()
                if (emptyText.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        newTextPaint(
                            color = styleOptions.centerSubTextColor,
                            textSizePx = with(density) { 13f.sp.toPx() },
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            val legendTextPaint = newTextPaint(
                color = styleOptions.legendTextColor,
                textSizePx = with(density) { styleOptions.legendTextSizeSp.sp.toPx() },
                align = Paint.Align.LEFT,
            )
            val legendReserved = resolvePieLegendReservedHeight(
                availableWidth = size.width,
                segments = segments,
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
                density = density,
                legendTextPaint = legendTextPaint,
            )
            val chartBottom = (size.height - legendReserved).coerceAtLeast(0f)
            val chartPadding = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
            val center = Offset(size.width * 0.5f, chartBottom * 0.5f)
            val radius = (min(size.width, chartBottom) * 0.5f - chartPadding).coerceAtLeast(0f)
            val inner = radius * donutInnerRatio.coerceIn(0f, 0.92f)
            val strokeWidth = if (inner > 0f) radius - inner else 0f
            val strokeStyle = if (inner > 0f) Stroke(width = strokeWidth.coerceAtLeast(1f)) else null

            segments.forEach { segment ->
                val selected = selectedIndex == segment.index
                val offsetDistance = if (selected && presentationOptions.enableSelectionExpand) {
                    with(density) { presentationOptions.selectedSliceExpandDp.dp.toPx() }
                } else {
                    0f
                }
                val offset = degreeToOffset(segment.mid, offsetDistance)
                val drawCenter = center + offset
                val drawRect = Rect(drawCenter - Offset(radius, radius), Size(radius * 2f, radius * 2f))

                if (strokeStyle != null) {
                    drawArc(
                        color = segment.slice.color.toComposeColor(),
                        startAngle = segment.start,
                        sweepAngle = segment.sweep,
                        useCenter = false,
                        topLeft = drawRect.topLeft,
                        size = drawRect.size,
                        style = strokeStyle,
                    )
                } else {
                    drawArc(
                        color = segment.slice.color.toComposeColor(),
                        startAngle = segment.start,
                        sweepAngle = segment.sweep,
                        useCenter = true,
                        topLeft = drawRect.topLeft,
                        size = drawRect.size,
                    )
                }
            }

            if (styleOptions.sliceStrokeWidthDp > 0f) {
                val strokePx = with(density) { styleOptions.sliceStrokeWidthDp.dp.toPx() }
                segments.forEach { segment ->
                    val selected = selectedIndex == segment.index
                    val offsetDistance = if (selected && presentationOptions.enableSelectionExpand) {
                        with(density) { presentationOptions.selectedSliceExpandDp.dp.toPx() }
                    } else {
                        0f
                    }
                    val offset = degreeToOffset(segment.mid, offsetDistance)
                    val drawCenter = center + offset
                    val drawRect = Rect(drawCenter - Offset(radius, radius), Size(radius * 2f, radius * 2f))
                    if (strokeStyle != null) {
                        drawArc(
                            color = styleOptions.sliceStrokeColor.toComposeColor(),
                            startAngle = segment.start,
                            sweepAngle = segment.sweep,
                            useCenter = false,
                            topLeft = drawRect.topLeft,
                            size = drawRect.size,
                            style = Stroke(width = strokePx * if (selected) 1.8f else 1f),
                        )
                    } else {
                        drawArc(
                            color = styleOptions.sliceStrokeColor.toComposeColor(),
                            startAngle = segment.start,
                            sweepAngle = segment.sweep,
                            useCenter = true,
                            topLeft = drawRect.topLeft,
                            size = drawRect.size,
                            style = Stroke(width = strokePx * if (selected) 1.8f else 1f),
                        )
                    }
                }
            }

            if (presentationOptions.showLabels) {
                val labelPaint = newTextPaint(
                    color = styleOptions.labelTextColor,
                    textSizePx = with(density) { styleOptions.labelTextSizeSp.sp.toPx() },
                    align = Paint.Align.CENTER,
                )
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = styleOptions.labelLineColor
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = with(density) { 1.dp.toPx() }
                }

                segments.forEach { segment ->
                    val label = "${segment.slice.label} ${(segment.ratio * 100f).roundToInt()}%"
                    val sweepAbs = abs(segment.sweep)
                    if (sweepAbs <= 0.4f) return@forEach

                    val selected = selectedIndex == segment.index
                    val offsetDistance = if (selected && presentationOptions.enableSelectionExpand) {
                        with(density) { presentationOptions.selectedSliceExpandDp.dp.toPx() }
                    } else {
                        0f
                    }
                    val offset = degreeToOffset(segment.mid, offsetDistance)
                    val drawCenter = center + offset

                    val midRadius = if (inner > 0f) (inner + radius) * 0.5f else radius * 0.6f
                    val point = drawCenter + degreeToOffset(segment.mid, midRadius)
                    val availableArc = Math.toRadians(sweepAbs.toDouble()).toFloat() * midRadius.coerceAtLeast(1f)
                    val textWidth = labelPaint.measureText(label)
                    val inside = when (presentationOptions.labelPosition) {
                        PieLabelPosition.INSIDE -> true
                        PieLabelPosition.OUTSIDE -> false
                        PieLabelPosition.AUTO -> sweepAbs >= 20f && textWidth <= availableArc * 0.95f
                    }

                    if (inside) {
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            point.x,
                            point.y - ((labelPaint.ascent() + labelPaint.descent()) * 0.5f),
                            labelPaint,
                        )
                    } else {
                        val anchor = drawCenter + degreeToOffset(segment.mid, radius)
                        val elbow = drawCenter + degreeToOffset(segment.mid, radius + with(density) { 12.dp.toPx() })
                        val rightSide = normalizeAngle(segment.mid) <= 90f || normalizeAngle(segment.mid) >= 270f
                        val endX = if (rightSide) elbow.x + with(density) { 8.dp.toPx() } else elbow.x - with(density) { 8.dp.toPx() }
                        drawContext.canvas.nativeCanvas.drawLine(anchor.x, anchor.y, elbow.x, elbow.y, linePaint)
                        drawContext.canvas.nativeCanvas.drawLine(elbow.x, elbow.y, endX, elbow.y, linePaint)
                        labelPaint.textAlign = if (rightSide) Paint.Align.LEFT else Paint.Align.RIGHT
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            if (rightSide) endX + with(density) { 2.dp.toPx() } else endX - with(density) { 2.dp.toPx() },
                            elbow.y - ((labelPaint.ascent() + labelPaint.descent()) * 0.5f),
                            labelPaint,
                        )
                    }
                }
            }

            if (inner > 0f) {
                val centerMain = presentationOptions.centerText?.trim().orEmpty()
                val centerSub = presentationOptions.centerSubText?.trim().orEmpty()
                if (centerMain.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        centerMain,
                        center.x,
                        center.y - with(density) { 4.dp.toPx() },
                        newTextPaint(
                            color = styleOptions.centerTextColor,
                            textSizePx = with(density) { styleOptions.centerTextSizeSp.sp.toPx() },
                            align = Paint.Align.CENTER,
                            bold = true,
                        ),
                    )
                }
                if (centerSub.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        centerSub,
                        center.x,
                        center.y + with(density) { 14.dp.toPx() },
                        newTextPaint(
                            color = styleOptions.centerSubTextColor,
                            textSizePx = with(density) { styleOptions.centerSubTextSizeSp.sp.toPx() },
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
            }

            if (presentationOptions.showLegend) {
                drawPieLegend(
                    segments = segments,
                    styleOptions = styleOptions,
                    presentationOptions = presentationOptions,
                    density = density,
                    legendTextPaint = legendTextPaint,
                    chartBottom = chartBottom,
                )
            }
        }
    }
}

private fun buildPieSegments(
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPieLegend(
    segments: List<PieSegment>,
    styleOptions: PieDonutStyleOptions,
    presentationOptions: PieDonutPresentationOptions,
    density: Density,
    legendTextPaint: Paint,
    chartBottom: Float,
) {
    if (segments.isEmpty()) return

    val markerSize = with(density) { styleOptions.legendMarkerSizeDp.dp.toPx() }
    val markerTextGap = with(density) { styleOptions.legendMarkerTextGapDp.dp.toPx() }
    val itemGap = with(density) { styleOptions.legendItemSpacingDp.dp.toPx() }
    val rowGap = with(density) { styleOptions.legendRowSpacingDp.dp.toPx() }

    val startX = with(density) { styleOptions.contentPaddingDp.dp.toPx() + presentationOptions.legendLeftMarginDp.dp.toPx() }
    val maxX = size.width - with(density) { styleOptions.contentPaddingDp.dp.toPx() }
    val rowHeight = max(markerSize, legendTextPaint.fontSpacing)
    var cursorX = startX
    var baselineY = chartBottom + with(density) { presentationOptions.legendTopMarginDp.dp.toPx() } + abs(legendTextPaint.fontMetrics.ascent)

    segments.forEach { segment ->
        val text = segment.slice.label.ifBlank { "Slice" }
        val textWidth = legendTextPaint.measureText(text)
        val itemWidth = markerSize + markerTextGap + textWidth

        if (cursorX + itemWidth > maxX && cursorX > startX) {
            cursorX = startX
            baselineY += rowHeight + rowGap
        }

        val markerTop = baselineY - abs(legendTextPaint.fontMetrics.ascent) + (rowHeight - markerSize) * 0.5f
        drawRect(
            color = segment.slice.color.toComposeColor(),
            topLeft = Offset(cursorX, markerTop),
            size = Size(markerSize, markerSize),
        )
        drawContext.canvas.nativeCanvas.drawText(
            text,
            cursorX + markerSize + markerTextGap,
            baselineY,
            legendTextPaint,
        )
        cursorX += itemWidth + itemGap
    }
}

private fun resolvePieLegendReservedHeight(
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
