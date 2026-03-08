package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubbleLegendItem
import com.magimon.eq.bubble.BubbleLegendMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.bubble.BubbleScaleOverride
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val BUBBLE_GOLDEN_ANGLE = 2.3999633f

private data class BubbleLayout(
    val datum: BubbleDatum,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

private data class BubbleNumericRange(
    val min: Double,
    val max: Double,
) {
    val span: Double
        get() = max - min
}

private data class BubbleChartComputed(
    val plotRect: Rect,
    val titleY: Float,
    val title: String,
    val layouts: List<BubbleLayout>,
    val xTicks: List<Double>,
    val yTicks: List<Double>,
    val xRange: BubbleNumericRange,
    val yRange: BubbleNumericRange,
    val legendItems: List<BubbleLegendItem>,
)

@Composable
fun BubbleChart(
    data: List<BubbleDatum>,
    modifier: Modifier = Modifier,
    axisOptions: BubbleAxisOptions = BubbleAxisOptions(),
    presentationOptions: BubblePresentationOptions = BubblePresentationOptions(),
    scaleOverride: BubbleScaleOverride? = null,
    layoutMode: BubbleLayoutMode = BubbleLayoutMode.SCATTER,
    legendItems: List<BubbleLegendItem> = emptyList(),
    onBubbleClick: ((BubbleDatum) -> Unit)? = null,
) {
    val density = LocalDensity.current
    var selectedDatum by remember { mutableStateOf<BubbleDatum?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val computed = remember(
            data,
            axisOptions,
            presentationOptions,
            scaleOverride,
            layoutMode,
            legendItems,
            widthPx,
            heightPx,
        ) {
            computeBubbleChart(
                width = widthPx,
                height = heightPx,
                data = data,
                axisOptions = axisOptions,
                presentationOptions = presentationOptions,
                scaleOverride = scaleOverride,
                layoutMode = layoutMode,
                explicitLegendItems = legendItems,
                density = density.density,
                scaledDensity = density.fontScale * density.density,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.layouts) {
                    detectTapGestures { tap ->
                        val hit = computed.layouts.asReversed().firstOrNull { bubble ->
                            val dx = tap.x - bubble.centerX
                            val dy = tap.y - bubble.centerY
                            (dx * dx + dy * dy) <= (bubble.radius * bubble.radius)
                        }
                        selectedDatum = hit?.datum
                        if (hit != null) {
                            onBubbleClick?.invoke(hit.datum)
                        }
                    }
                },
        ) {
            drawRect(androidx.compose.ui.graphics.Color(0xFFE6E6E6))

            val titlePaint = newTextPaint(
                color = presentationOptions.titleColor,
                textSizePx = with(density) { presentationOptions.titleTextSizeSp.sp.toPx() },
                align = Paint.Align.LEFT,
                bold = true,
            )
            val legendTextPaint = newTextPaint(
                color = presentationOptions.legendTextColor,
                textSizePx = with(density) { presentationOptions.legendTextSizeSp.sp.toPx() },
                align = Paint.Align.LEFT,
            )
            val tickPaint = newTextPaint(
                color = 0xFFC6D2DE.toInt(),
                textSizePx = with(density) { 10.sp.toPx() },
            )

            if (computed.title.isNotBlank()) {
                drawContext.canvas.nativeCanvas.drawText(
                    computed.title,
                    computed.plotRect.left,
                    computed.titleY,
                    titlePaint,
                )
            }

            if (axisOptions.showGrid && layoutMode == BubbleLayoutMode.SCATTER) {
                computed.xTicks.forEach { tick ->
                    val x = bubbleMapX(tick, computed.xRange, computed.plotRect)
                    drawLine(
                        color = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                        start = Offset(x, computed.plotRect.top),
                        end = Offset(x, computed.plotRect.bottom),
                        strokeWidth = with(density) { 1.dp.toPx() },
                    )
                }
                computed.yTicks.forEach { tick ->
                    val y = bubbleMapY(tick, computed.yRange, computed.plotRect)
                    drawLine(
                        color = androidx.compose.ui.graphics.Color(0x33FFFFFF),
                        start = Offset(computed.plotRect.left, y),
                        end = Offset(computed.plotRect.right, y),
                        strokeWidth = with(density) { 1.dp.toPx() },
                    )
                }
            }

            if (axisOptions.showAxes && layoutMode == BubbleLayoutMode.SCATTER) {
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFF8D9AA8),
                    start = Offset(computed.plotRect.left, computed.plotRect.bottom),
                    end = Offset(computed.plotRect.right, computed.plotRect.bottom),
                    strokeWidth = with(density) { 1.dp.toPx() },
                )
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0xFF8D9AA8),
                    start = Offset(computed.plotRect.left, computed.plotRect.top),
                    end = Offset(computed.plotRect.left, computed.plotRect.bottom),
                    strokeWidth = with(density) { 1.dp.toPx() },
                )
            }

            if (axisOptions.showTicks && layoutMode == BubbleLayoutMode.SCATTER) {
                tickPaint.textAlign = Paint.Align.CENTER
                computed.xTicks.forEach { tick ->
                    val x = bubbleMapX(tick, computed.xRange, computed.plotRect)
                    drawContext.canvas.nativeCanvas.drawText(
                        axisOptions.xLabelFormatter(tick),
                        x,
                        computed.plotRect.bottom + with(density) { 18.dp.toPx() },
                        tickPaint,
                    )
                }

                tickPaint.textAlign = Paint.Align.RIGHT
                computed.yTicks.forEach { tick ->
                    val y = bubbleMapY(tick, computed.yRange, computed.plotRect)
                    drawContext.canvas.nativeCanvas.drawText(
                        axisOptions.yLabelFormatter(tick),
                        computed.plotRect.left - with(density) { 8.dp.toPx() },
                        y + with(density) { 4.dp.toPx() },
                        tickPaint,
                    )
                }
            }

            computed.layouts.forEach { bubble ->
                drawCircle(
                    color = bubble.datum.color.toComposeColor(),
                    radius = bubble.radius,
                    center = Offset(bubble.centerX, bubble.centerY),
                )

                if (selectedDatum === bubble.datum) {
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = bubble.radius,
                        center = Offset(bubble.centerX, bubble.centerY),
                        style = Stroke(width = with(density) { 2.dp.toPx() }),
                    )
                }

                val label = bubble.datum.label?.trim().orEmpty()
                if (label.isNotEmpty() && bubble.radius > with(density) { 14.dp.toPx() }) {
                    val labelPaint = newTextPaint(
                        color = android.graphics.Color.WHITE,
                        textSizePx = with(density) { 10.sp.toPx() },
                        align = Paint.Align.CENTER,
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        bubble.centerX,
                        bubble.centerY - ((labelPaint.ascent() + labelPaint.descent()) * 0.5f),
                        labelPaint,
                    )
                }
            }

            if (presentationOptions.showLegend && computed.legendItems.isNotEmpty()) {
                val markerSize = with(density) { presentationOptions.legendMarkerSizeDp.dp.toPx() }
                val rowGap = with(density) { presentationOptions.legendItemSpacingDp.dp.toPx() }
                val startX = computed.plotRect.left + with(density) { presentationOptions.legendLeftMarginDp.dp.toPx() }
                var baselineY =
                    computed.plotRect.bottom +
                    with(density) { presentationOptions.legendSectionTopPaddingDp.dp.toPx() } +
                    abs(legendTextPaint.fontMetrics.ascent)

                computed.legendItems.forEach { item ->
                    val markerTop = baselineY - abs(legendTextPaint.fontMetrics.ascent)
                    drawRect(
                        color = item.color.toComposeColor(),
                        topLeft = Offset(startX, markerTop),
                        size = androidx.compose.ui.geometry.Size(markerSize, markerSize),
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        item.label,
                        startX + markerSize + with(density) { 6.dp.toPx() },
                        baselineY,
                        legendTextPaint,
                    )
                    baselineY += max(markerSize, legendTextPaint.fontSpacing) + rowGap
                }
            }
        }
    }
}

private fun computeBubbleChart(
    width: Float,
    height: Float,
    data: List<BubbleDatum>,
    axisOptions: BubbleAxisOptions,
    presentationOptions: BubblePresentationOptions,
    scaleOverride: BubbleScaleOverride?,
    layoutMode: BubbleLayoutMode,
    explicitLegendItems: List<BubbleLegendItem>,
    density: Float,
    scaledDensity: Float,
): BubbleChartComputed {
    val validData = data.filter { it.x.isFinite() && it.y.isFinite() && it.size.isFinite() }
    val legend = resolveBubbleLegend(validData, presentationOptions.legendMode, explicitLegendItems)

    val title = presentationOptions.title?.trim().orEmpty()
    val titleReserved = if (title.isEmpty()) {
        0f
    } else {
        presentationOptions.titleTextSizeSp * scaledDensity +
            presentationOptions.titleBottomSpacingDp * density
    }

    val legendReserved = if (!presentationOptions.showLegend || legend.isEmpty()) {
        0f
    } else {
        val marker = presentationOptions.legendMarkerSizeDp * density
        val line = max(marker, presentationOptions.legendTextSizeSp * scaledDensity)
        val itemsHeight = (line * legend.size) + ((legend.size - 1).coerceAtLeast(0) * presentationOptions.legendItemSpacingDp * density)
        presentationOptions.legendSectionTopPaddingDp * density +
            itemsHeight +
            presentationOptions.legendBottomMarginDp * density
    }

    val outerPadding = 12f * density
    val axisGutterLeft = 48f * density
    val axisGutterBottom = 36f * density
    val axisGutterTop = 12f * density
    val axisGutterRight = 12f * density

    val baseRect = Rect(
        left = outerPadding,
        top = outerPadding + titleReserved,
        right = (width - outerPadding).coerceAtLeast(outerPadding),
        bottom = (height - outerPadding - legendReserved).coerceAtLeast(outerPadding + titleReserved),
    )

    val useAxis = axisOptions.showAxes || axisOptions.showTicks
    val plotRect = if (useAxis && layoutMode == BubbleLayoutMode.SCATTER) {
        Rect(
            left = baseRect.left + axisGutterLeft,
            top = baseRect.top + axisGutterTop,
            right = baseRect.right - axisGutterRight,
            bottom = baseRect.bottom - axisGutterBottom,
        )
    } else {
        baseRect
    }

    if (plotRect.width <= 1f || plotRect.height <= 1f || validData.isEmpty()) {
        return BubbleChartComputed(
            plotRect = plotRect,
            titleY = baseRect.top + presentationOptions.titleTextSizeSp * scaledDensity,
            title = title,
            layouts = emptyList(),
            xTicks = emptyList(),
            yTicks = emptyList(),
            xRange = BubbleNumericRange(0.0, 1.0),
            yRange = BubbleNumericRange(0.0, 1.0),
            legendItems = legend,
        )
    }

    val minRadius = max(4f * density, min(plotRect.width, plotRect.height) * 0.01f)
    val maxRadius = min(plotRect.width, plotRect.height) * 0.12f

    val sizeRange = bubbleResolveRange(validData.map { it.size }, scaleOverride?.sizeMin, scaleOverride?.sizeMax)

    val (layouts, xTicks, yTicks, xRange, yRange) = if (layoutMode == BubbleLayoutMode.PACKED) {
        val packed = buildPackedBubbleLayouts(validData, plotRect, sizeRange, minRadius, maxRadius, density)
        BubbleTuple(
            layouts = packed,
            xTicks = emptyList(),
            yTicks = emptyList(),
            xRange = BubbleNumericRange(0.0, 1.0),
            yRange = BubbleNumericRange(0.0, 1.0),
        )
    } else {
        val xr = bubbleResolveRange(validData.map { it.x }, scaleOverride?.xMin, scaleOverride?.xMax)
        val yr = bubbleResolveRange(validData.map { it.y }, scaleOverride?.yMin, scaleOverride?.yMax)
        val xt = bubbleTickValues(xr, 5)
        val yt = bubbleTickValues(yr, 5)
        val scatter = validData.map { datum ->
            val radius = bubbleMapRadius(datum.size, sizeRange, minRadius, maxRadius)
            val mappedX = bubbleMapX(datum.x, xr, plotRect).coerceIn(plotRect.left + radius, plotRect.right - radius)
            val mappedY = bubbleMapY(datum.y, yr, plotRect).coerceIn(plotRect.top + radius, plotRect.bottom - radius)
            BubbleLayout(datum, mappedX, mappedY, radius)
        }
        BubbleTuple(scatter, xt, yt, xr, yr)
    }

    return BubbleChartComputed(
        plotRect = plotRect,
        titleY = baseRect.top + presentationOptions.titleTextSizeSp * scaledDensity,
        title = title,
        layouts = layouts.sortedByDescending { it.radius },
        xTicks = xTicks,
        yTicks = yTicks,
        xRange = xRange,
        yRange = yRange,
        legendItems = legend,
    )
}

private data class BubbleTuple(
    val layouts: List<BubbleLayout>,
    val xTicks: List<Double>,
    val yTicks: List<Double>,
    val xRange: BubbleNumericRange,
    val yRange: BubbleNumericRange,
)

private fun buildPackedBubbleLayouts(
    data: List<BubbleDatum>,
    plotRect: Rect,
    sizeRange: BubbleNumericRange,
    minRadius: Float,
    maxRadius: Float,
    density: Float,
): List<BubbleLayout> {
    data class Node(
        val datum: BubbleDatum,
        val radius: Float,
        var x: Float,
        var y: Float,
    )

    val centerX = plotRect.center.x
    val centerY = plotRect.center.y
    val spiralStep = maxRadius * 1.45f
    val gap = 2f * density

    val nodes = data
        .map { datum ->
            Node(
                datum = datum,
                radius = bubbleMapRadius(datum.size, sizeRange, minRadius, maxRadius),
                x = centerX,
                y = centerY,
            )
        }
        .sortedByDescending { it.radius }

    nodes.forEachIndexed { index, node ->
        if (index == 0) {
            node.x = centerX
            node.y = centerY
        } else {
            val angle = BUBBLE_GOLDEN_ANGLE * index
            val distance = sqrt(index.toFloat()) * spiralStep
            node.x = centerX + (cos(angle) * distance)
            node.y = centerY + (sin(angle) * distance)
        }
    }

    repeat(220) {
        nodes.forEach { node ->
            node.x += (centerX - node.x) * 0.045f
            node.y += (centerY - node.y) * 0.045f
        }

        for (i in 0 until nodes.lastIndex) {
            val a = nodes[i]
            for (j in i + 1 until nodes.size) {
                val b = nodes[j]
                var dx = b.x - a.x
                var dy = b.y - a.y
                var distance = hypot(dx, dy)
                val minDistance = a.radius + b.radius + gap
                if (distance < minDistance) {
                    if (distance < 0.001f) {
                        val seedAngle = ((i * 73 + j * 37) % 360) * (PI.toFloat() / 180f)
                        dx = cos(seedAngle)
                        dy = sin(seedAngle)
                        distance = 1f
                    }
                    val overlap = (minDistance - distance) * 0.5f
                    val ux = dx / distance
                    val uy = dy / distance
                    a.x -= ux * overlap
                    a.y -= uy * overlap
                    b.x += ux * overlap
                    b.y += uy * overlap
                }
            }
        }
    }

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    nodes.forEach { node ->
        minX = min(minX, node.x - node.radius)
        maxX = max(maxX, node.x + node.radius)
        minY = min(minY, node.y - node.radius)
        maxY = max(maxY, node.y + node.radius)
    }

    val clusterWidth = (maxX - minX).coerceAtLeast(1f)
    val clusterHeight = (maxY - minY).coerceAtLeast(1f)
    val scale = min((plotRect.width * 0.94f) / clusterWidth, (plotRect.height * 0.94f) / clusterHeight)
    val clusterCenterX = (minX + maxX) * 0.5f
    val clusterCenterY = (minY + maxY) * 0.5f

    return nodes.map { node ->
        val tx = centerX + (node.x - clusterCenterX) * scale
        val ty = centerY + (node.y - clusterCenterY) * scale
        val tr = node.radius * scale
        BubbleLayout(
            datum = node.datum,
            centerX = tx.coerceIn(plotRect.left + tr, plotRect.right - tr),
            centerY = ty.coerceIn(plotRect.top + tr, plotRect.bottom - tr),
            radius = tr,
        )
    }
}

private fun resolveBubbleLegend(
    data: List<BubbleDatum>,
    mode: BubbleLegendMode,
    explicitItems: List<BubbleLegendItem>,
): List<BubbleLegendItem> {
    return when (mode) {
        BubbleLegendMode.AUTO -> bubbleAutoLegendItems(data)
        BubbleLegendMode.EXPLICIT -> explicitItems
        BubbleLegendMode.AUTO_WITH_OVERRIDE -> if (explicitItems.isNotEmpty()) explicitItems else bubbleAutoLegendItems(data)
    }
}

private fun bubbleAutoLegendItems(data: List<BubbleDatum>): List<BubbleLegendItem> {
    val entries = LinkedHashMap<String, Int>()
    data.forEach { datum ->
        val label = datum.legendGroup?.takeIf { it.isNotBlank() }
            ?: String.format(Locale.US, "#%06X", datum.color and 0x00FFFFFF)
        if (!entries.containsKey(label)) {
            entries[label] = datum.color
        }
    }
    return entries.map { BubbleLegendItem(label = it.key, color = it.value) }
}

private fun bubbleResolveRange(
    values: List<Double>,
    overrideMin: Double?,
    overrideMax: Double?,
    paddingRatio: Double = 0.05,
): BubbleNumericRange {
    val baseMin = values.minOrNull() ?: 0.0
    val baseMax = values.maxOrNull() ?: 1.0

    var minValue = overrideMin ?: baseMin
    var maxValue = overrideMax ?: baseMax

    if (minValue > maxValue) {
        val temp = minValue
        minValue = maxValue
        maxValue = temp
    }

    if (minValue == maxValue) {
        val pad = max(1.0, abs(minValue) * paddingRatio)
        minValue -= pad
        maxValue += pad
    }

    return BubbleNumericRange(minValue, maxValue)
}

private fun bubbleNormalize(value: Double, range: BubbleNumericRange): Double {
    if (range.span <= 0.0) return 0.5
    return ((value - range.min) / range.span).coerceIn(0.0, 1.0)
}

private fun bubbleMapX(value: Double, range: BubbleNumericRange, plotRect: Rect): Float {
    val t = bubbleNormalize(value, range).toFloat()
    return plotRect.left + (plotRect.width * t)
}

private fun bubbleMapY(value: Double, range: BubbleNumericRange, plotRect: Rect): Float {
    val t = bubbleNormalize(value, range).toFloat()
    return plotRect.bottom - (plotRect.height * t)
}

private fun bubbleMapRadius(
    sizeValue: Double,
    sizeRange: BubbleNumericRange,
    minRadius: Float,
    maxRadius: Float,
): Float {
    val t = bubbleNormalize(sizeValue, sizeRange)
    val eased = sqrt(t).toFloat()
    return minRadius + (maxRadius - minRadius) * eased
}

private fun bubbleTickValues(range: BubbleNumericRange, tickCount: Int): List<Double> {
    if (tickCount <= 1) return listOf(range.min, range.max)
    val step = range.span / (tickCount - 1)
    return (0 until tickCount).map { index ->
        range.min + step * index
    }
}
