package com.magimon.eq.matrixheatmap

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Rectangular region expressed in pixels relative to the chart canvas.
 */
data class MatrixHeatmapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

/**
 * Pixel-based layout inputs consumed by [resolveMatrixHeatmapChartLayout].
 *
 * @property widthPx Full canvas width in pixels.
 * @property heightPx Full canvas height in pixels.
 * @property contentPaddingPx Outer chart padding in pixels.
 * @property xAxisLabelHeightPx Reserved height for X-axis labels.
 * @property yAxisLabelWidthPx Reserved width for Y-axis labels.
 * @property axisLabelGapPx Gap between labels and the heatmap grid.
 * @property cellGapPx Gap between adjacent cells.
 */
data class MatrixHeatmapChartLayoutConfig(
    val widthPx: Float,
    val heightPx: Float,
    val contentPaddingPx: Float,
    val xAxisLabelHeightPx: Float,
    val yAxisLabelWidthPx: Float,
    val axisLabelGapPx: Float,
    val cellGapPx: Float,
)

/**
 * Render-ready axis label placement produced by [resolveMatrixHeatmapChartLayout].
 */
data class MatrixHeatmapAxisLabelLayout(
    val index: Int,
    val label: String,
    val rect: MatrixHeatmapRect,
)

/**
 * Render-ready cell placement produced by [resolveMatrixHeatmapChartLayout].
 *
 * Empty cells represent missing `(x, y)` intersections inside an otherwise renderable grid.
 */
data class MatrixHeatmapCellLayout(
    val columnIndex: Int,
    val rowIndex: Int,
    val xLabel: String,
    val yLabel: String,
    val value: Double?,
    val cell: MatrixHeatmapCell?,
    val rect: MatrixHeatmapRect,
    val fillColor: Int,
    val resolvedText: String?,
)

/**
 * Shared layout result for View and Compose matrix heatmap renderers.
 */
data class MatrixHeatmapChartLayoutResult(
    val chartRect: MatrixHeatmapRect,
    val xAxisLabels: List<MatrixHeatmapAxisLabelLayout>,
    val yAxisLabels: List<MatrixHeatmapAxisLabelLayout>,
    val cellLayouts: List<MatrixHeatmapCellLayout>,
    val minValue: Double?,
    val maxValue: Double?,
    val isRenderable: Boolean,
    val emptyReason: String? = null,
)

private const val RANGE_EPSILON = 1e-12

/**
 * Resolves validated axis labels, cell placements, and fill colors for a matrix heatmap.
 *
 * Blank axis labels are dropped while preserving the first unique occurrence order.
 * Cells with unknown keys, duplicate coordinates, or non-finite values are sanitized before layout.
 */
fun resolveMatrixHeatmapChartLayout(
    data: MatrixHeatmapData,
    config: MatrixHeatmapChartLayoutConfig,
    style: MatrixHeatmapChartStyleOptions = MatrixHeatmapChartStyleOptions(),
    presentation: MatrixHeatmapChartPresentationOptions = MatrixHeatmapChartPresentationOptions(),
): MatrixHeatmapChartLayoutResult {
    val emptyRect = MatrixHeatmapRect(0f, 0f, 0f, 0f)
    if (config.widthPx <= 0f || config.heightPx <= 0f) {
        return MatrixHeatmapChartLayoutResult(
            chartRect = emptyRect,
            xAxisLabels = emptyList(),
            yAxisLabels = emptyList(),
            cellLayouts = emptyList(),
            minValue = null,
            maxValue = null,
            isRenderable = false,
            emptyReason = "non-positive size",
        )
    }

    val xLabels = sanitizeAxisLabels(data.xLabels)
    val yLabels = sanitizeAxisLabels(data.yLabels)
    if (xLabels.isEmpty() || yLabels.isEmpty()) {
        return MatrixHeatmapChartLayoutResult(
            chartRect = emptyRect,
            xAxisLabels = emptyList(),
            yAxisLabels = emptyList(),
            cellLayouts = emptyList(),
            minValue = null,
            maxValue = null,
            isRenderable = false,
            emptyReason = "no valid axes",
        )
    }

    val xIndexByKey = xLabels.withIndex().associate { it.value to it.index }
    val yIndexByKey = yLabels.withIndex().associate { it.value to it.index }
    val cellsByKey = LinkedHashMap<Pair<String, String>, MatrixHeatmapCell>()
    data.cells.forEach { cell ->
        val xKey = cell.xKey.trim()
        val yKey = cell.yKey.trim()
        if (!cell.value.isFinite()) return@forEach
        if (xKey !in xIndexByKey || yKey !in yIndexByKey) return@forEach
        cellsByKey[xKey to yKey] = cell.copy(
            xKey = xKey,
            yKey = yKey,
            label = cell.label?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
    if (cellsByKey.isEmpty()) {
        return MatrixHeatmapChartLayoutResult(
            chartRect = emptyRect,
            xAxisLabels = emptyList(),
            yAxisLabels = emptyList(),
            cellLayouts = emptyList(),
            minValue = null,
            maxValue = null,
            isRenderable = false,
            emptyReason = "no valid cells",
        )
    }

    val xAxisGap = if (config.xAxisLabelHeightPx > 0f) config.axisLabelGapPx else 0f
    val yAxisGap = if (config.yAxisLabelWidthPx > 0f) config.axisLabelGapPx else 0f
    val chartRect = MatrixHeatmapRect(
        left = config.contentPaddingPx + config.yAxisLabelWidthPx + yAxisGap,
        top = config.contentPaddingPx + config.xAxisLabelHeightPx + xAxisGap,
        right = config.widthPx - config.contentPaddingPx,
        bottom = config.heightPx - config.contentPaddingPx,
    )
    if (chartRect.width <= 0f || chartRect.height <= 0f) {
        return MatrixHeatmapChartLayoutResult(
            chartRect = chartRect,
            xAxisLabels = emptyList(),
            yAxisLabels = emptyList(),
            cellLayouts = emptyList(),
            minValue = null,
            maxValue = null,
            isRenderable = false,
            emptyReason = "no drawable bounds",
        )
    }

    val columnWidth = chartRect.width / xLabels.size.toFloat()
    val rowHeight = chartRect.height / yLabels.size.toFloat()
    if (columnWidth <= 0f || rowHeight <= 0f) {
        return MatrixHeatmapChartLayoutResult(
            chartRect = chartRect,
            xAxisLabels = emptyList(),
            yAxisLabels = emptyList(),
            cellLayouts = emptyList(),
            minValue = null,
            maxValue = null,
            isRenderable = false,
            emptyReason = "non-positive cell size",
        )
    }

    val values = cellsByKey.values.map { it.value }
    val minValue = values.minOrNull()
    val maxValue = values.maxOrNull()
    val hasNegative = values.any { it < 0.0 }
    val hasPositive = values.any { it > 0.0 }

    val xAxisLabels = buildXAxisLayouts(xLabels, chartRect, config, columnWidth)
    val yAxisLabels = buildYAxisLayouts(yLabels, chartRect, config, rowHeight)
    val cellLayouts = buildCellLayouts(
        xLabels = xLabels,
        yLabels = yLabels,
        chartRect = chartRect,
        columnWidth = columnWidth,
        rowHeight = rowHeight,
        cellGapPx = config.cellGapPx,
        cellsByKey = cellsByKey,
        style = style,
        presentation = presentation,
        minValue = minValue ?: 0.0,
        maxValue = maxValue ?: 0.0,
        hasNegative = hasNegative,
        hasPositive = hasPositive,
    )

    return MatrixHeatmapChartLayoutResult(
        chartRect = chartRect,
        xAxisLabels = xAxisLabels,
        yAxisLabels = yAxisLabels,
        cellLayouts = cellLayouts,
        minValue = minValue,
        maxValue = maxValue,
        isRenderable = cellLayouts.isNotEmpty(),
        emptyReason = if (cellLayouts.isEmpty()) "no renderable cells" else null,
    )
}

/**
 * Finds the topmost populated cell that contains the given point.
 */
fun findMatrixHeatmapHit(
    layout: MatrixHeatmapChartLayoutResult,
    x: Float,
    y: Float,
): MatrixHeatmapCellLayout? {
    return layout.cellLayouts.lastOrNull { cell ->
        cell.value != null && cell.rect.contains(x, y)
    }
}

/**
 * Default numeric formatter used by [MatrixHeatmapChartPresentationOptions].
 */
fun formatMatrixHeatmapValue(value: Double): String {
    if (!value.isFinite()) return "0"
    val formatted = String.format(Locale.US, "%.2f", value)
    return formatted.trimEnd('0').trimEnd('.')
}

private fun sanitizeAxisLabels(labels: List<String>): List<String> {
    val seen = LinkedHashSet<String>()
    labels.forEach { raw ->
        val trimmed = raw.trim()
        if (trimmed.isNotEmpty()) {
            seen.add(trimmed)
        }
    }
    return seen.toList()
}

private fun buildXAxisLayouts(
    labels: List<String>,
    chartRect: MatrixHeatmapRect,
    config: MatrixHeatmapChartLayoutConfig,
    columnWidth: Float,
): List<MatrixHeatmapAxisLabelLayout> {
    if (config.xAxisLabelHeightPx <= 0f) return emptyList()
    val bottom = chartRect.top - config.axisLabelGapPx
    val top = max(config.contentPaddingPx, bottom - config.xAxisLabelHeightPx)
    return labels.mapIndexed { index, label ->
        val left = chartRect.left + columnWidth * index
        MatrixHeatmapAxisLabelLayout(
            index = index,
            label = label,
            rect = MatrixHeatmapRect(
                left = left,
                top = top,
                right = left + columnWidth,
                bottom = bottom,
            ),
        )
    }
}

private fun buildYAxisLayouts(
    labels: List<String>,
    chartRect: MatrixHeatmapRect,
    config: MatrixHeatmapChartLayoutConfig,
    rowHeight: Float,
): List<MatrixHeatmapAxisLabelLayout> {
    if (config.yAxisLabelWidthPx <= 0f) return emptyList()
    val right = chartRect.left - config.axisLabelGapPx
    val left = max(config.contentPaddingPx, right - config.yAxisLabelWidthPx)
    return labels.mapIndexed { index, label ->
        val top = chartRect.top + rowHeight * index
        MatrixHeatmapAxisLabelLayout(
            index = index,
            label = label,
            rect = MatrixHeatmapRect(
                left = left,
                top = top,
                right = right,
                bottom = top + rowHeight,
            ),
        )
    }
}

private fun buildCellLayouts(
    xLabels: List<String>,
    yLabels: List<String>,
    chartRect: MatrixHeatmapRect,
    columnWidth: Float,
    rowHeight: Float,
    cellGapPx: Float,
    cellsByKey: Map<Pair<String, String>, MatrixHeatmapCell>,
    style: MatrixHeatmapChartStyleOptions,
    presentation: MatrixHeatmapChartPresentationOptions,
    minValue: Double,
    maxValue: Double,
    hasNegative: Boolean,
    hasPositive: Boolean,
): List<MatrixHeatmapCellLayout> {
    val layouts = ArrayList<MatrixHeatmapCellLayout>(xLabels.size * yLabels.size)
    val horizontalInset = min(cellGapPx * 0.5f, ((columnWidth * 0.5f) - 0.5f)).coerceAtLeast(0f)
    val verticalInset = min(cellGapPx * 0.5f, ((rowHeight * 0.5f) - 0.5f)).coerceAtLeast(0f)
    yLabels.forEachIndexed { rowIndex, yLabel ->
        xLabels.forEachIndexed { columnIndex, xLabel ->
            val rawLeft = chartRect.left + (columnWidth * columnIndex)
            val rawTop = chartRect.top + (rowHeight * rowIndex)
            val rect = MatrixHeatmapRect(
                left = rawLeft + horizontalInset,
                top = rawTop + verticalInset,
                right = max(rawLeft + horizontalInset + 1f, rawLeft + columnWidth - horizontalInset),
                bottom = max(rawTop + verticalInset + 1f, rawTop + rowHeight - verticalInset),
            )
            val source = cellsByKey[xLabel to yLabel]
            val value = source?.value
            layouts.add(
                MatrixHeatmapCellLayout(
                    columnIndex = columnIndex,
                    rowIndex = rowIndex,
                    xLabel = xLabel,
                    yLabel = yLabel,
                    value = value,
                    cell = source,
                    rect = rect,
                    fillColor = if (value == null) {
                        style.emptyCellColor
                    } else {
                        resolveMatrixHeatmapFillColor(
                            value = value,
                            minValue = minValue,
                            maxValue = maxValue,
                            hasNegative = hasNegative,
                            hasPositive = hasPositive,
                            style = style,
                        )
                    },
                    resolvedText = source?.label ?: source?.value?.let(presentation.valueFormatter),
                ),
            )
        }
    }
    return layouts
}

private fun resolveMatrixHeatmapFillColor(
    value: Double,
    minValue: Double,
    maxValue: Double,
    hasNegative: Boolean,
    hasPositive: Boolean,
    style: MatrixHeatmapChartStyleOptions,
): Int {
    if (hasNegative && hasPositive) {
        return when {
            value > 0.0 -> {
                val positiveMax = max(maxValue, RANGE_EPSILON)
                interpolateMatrixHeatmapColor(
                    from = style.neutralColor,
                    to = style.maxColor,
                    progress = (value / positiveMax).toFloat(),
                )
            }

            value < 0.0 -> {
                val negativeMin = min(minValue, -RANGE_EPSILON)
                interpolateMatrixHeatmapColor(
                    from = style.neutralColor,
                    to = style.minColor,
                    progress = (value / negativeMin).toFloat(),
                )
            }

            else -> style.neutralColor
        }
    }

    if (abs(maxValue - minValue) <= RANGE_EPSILON) {
        return when {
            value > 0.0 -> style.maxColor
            value < 0.0 -> style.minColor
            else -> style.neutralColor
        }
    }

    val ratio = ((value - minValue) / (maxValue - minValue)).toFloat()
    return interpolateMatrixHeatmapColor(
        from = style.minColor,
        to = style.maxColor,
        progress = ratio,
    )
}

private fun interpolateMatrixHeatmapColor(from: Int, to: Int, progress: Float): Int {
    val clamped = progress.coerceIn(0f, 1f)
    val alpha = (colorAlpha(from) + (colorAlpha(to) - colorAlpha(from)) * clamped).toInt()
    val red = (colorRed(from) + (colorRed(to) - colorRed(from)) * clamped).toInt()
    val green = (colorGreen(from) + (colorGreen(to) - colorGreen(from)) * clamped).toInt()
    val blue = (colorBlue(from) + (colorBlue(to) - colorBlue(from)) * clamped).toInt()
    return colorArgb(alpha, red, green, blue)
}

private fun colorAlpha(color: Int): Int = color ushr 24 and 0xFF

private fun colorRed(color: Int): Int = color ushr 16 and 0xFF

private fun colorGreen(color: Int): Int = color ushr 8 and 0xFF

private fun colorBlue(color: Int): Int = color and 0xFF

private fun colorArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return ((alpha and 0xFF) shl 24) or
        ((red and 0xFF) shl 16) or
        ((green and 0xFF) shl 8) or
        (blue and 0xFF)
}
