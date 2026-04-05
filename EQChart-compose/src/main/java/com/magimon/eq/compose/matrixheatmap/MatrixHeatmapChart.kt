package com.magimon.eq.compose.matrixheatmap

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.matrixheatmap.MatrixHeatmapCell
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartLayoutConfig
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartLayoutResult
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartPresentationOptions
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartStyleOptions
import com.magimon.eq.matrixheatmap.MatrixHeatmapData
import com.magimon.eq.matrixheatmap.findMatrixHeatmapHit
import com.magimon.eq.matrixheatmap.resolveMatrixHeatmapChartLayout

/**
 * Compose matrix heatmap chart for categorical X/Y grid data.
 *
 * @param data Matrix heatmap input where [MatrixHeatmapCell.xKey] and
 * [MatrixHeatmapCell.yKey] must match the provided axis labels.
 * @param modifier Compose modifier applied to the chart container.
 * @param styleOptions Shared visual styling for the grid and labels.
 * @param presentationOptions Shared label and empty-state behavior.
 * @param onCellClick Optional callback invoked when a populated cell is tapped.
 */
@Composable
fun MatrixHeatmapChart(
    data: MatrixHeatmapData,
    modifier: Modifier = Modifier,
    styleOptions: MatrixHeatmapChartStyleOptions = MatrixHeatmapChartStyleOptions(),
    presentationOptions: MatrixHeatmapChartPresentationOptions = MatrixHeatmapChartPresentationOptions(),
    onCellClick: ((MatrixHeatmapCell) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val densityPx = density.density
    val scaledDensity = density.fontScale * densityPx

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val computed = remember(
            data,
            widthPx,
            heightPx,
            styleOptions,
            presentationOptions,
            densityPx,
            scaledDensity,
        ) {
            computeMatrixHeatmapLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                data = data,
                style = styleOptions,
                presentation = presentationOptions,
                density = densityPx,
                scaledDensity = scaledDensity,
            )
        }

        val axisLabelPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
        )
        val cellTextPaint = newTextPaint(
            color = styleOptions.cellTextColor,
            textSizePx = presentationOptions.cellTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.axisLabelColor,
            textSizePx = presentationOptions.axisLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.cellLayouts, onCellClick) {
                    detectTapGestures { tap ->
                        findMatrixHeatmapHit(computed, tap.x, tap.y)?.cell?.let { cell ->
                            onCellClick?.invoke(cell)
                        }
                    }
                },
        ) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            if (!computed.isRenderable) {
                if (presentationOptions.emptyText.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        emptyTextPaint,
                    )
                }
                return@Canvas
            }

            val chartRect = Rect(
                left = computed.chartRect.left,
                top = computed.chartRect.top,
                right = computed.chartRect.right,
                bottom = computed.chartRect.bottom,
            )
            drawRect(
                color = styleOptions.axisLineColor.toComposeColor(),
                topLeft = Offset(chartRect.left, chartRect.top),
                size = Size(chartRect.width, chartRect.height),
                style = Stroke(width = 1f),
            )

            val cellRadius = styleOptions.cellCornerRadiusDp * densityPx
            computed.cellLayouts.forEach { cell ->
                val rect = Rect(
                    left = cell.rect.left,
                    top = cell.rect.top,
                    right = cell.rect.right,
                    bottom = cell.rect.bottom,
                )
                drawRoundRect(
                    color = cell.fillColor.toComposeColor(),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(cellRadius, cellRadius),
                )
                drawRoundRect(
                    color = styleOptions.cellBorderColor.toComposeColor(),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(cellRadius, cellRadius),
                    style = Stroke(width = 1f),
                )

                if (
                    presentationOptions.showCellText &&
                    cell.value != null &&
                    rect.width >= presentationOptions.minCellTextWidthDp * densityPx &&
                    rect.height >= presentationOptions.minCellTextHeightDp * densityPx
                ) {
                    val text = fitTextToWidth(
                        text = cell.resolvedText.orEmpty(),
                        paint = cellTextPaint,
                        maxWidth = rect.width - 8f * densityPx,
                    )
                    if (text.isNotEmpty()) {
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            rect.center.x,
                            rect.center.y + cellTextPaint.textSize * 0.35f,
                            cellTextPaint,
                        )
                    }
                }
            }

            axisLabelPaint.textAlign = Paint.Align.CENTER
            computed.xAxisLabels.forEach { axisLabel ->
                val text = fitTextToWidth(
                    text = axisLabel.label,
                    paint = axisLabelPaint,
                    maxWidth = axisLabel.rect.width - 4f * densityPx,
                )
                if (text.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        axisLabel.rect.left + axisLabel.rect.width * 0.5f,
                        axisLabel.rect.top + axisLabel.rect.height * 0.5f + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }

            axisLabelPaint.textAlign = Paint.Align.RIGHT
            computed.yAxisLabels.forEach { axisLabel ->
                val text = fitTextToWidth(
                    text = axisLabel.label,
                    paint = axisLabelPaint,
                    maxWidth = axisLabel.rect.width - 4f * densityPx,
                )
                if (text.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        axisLabel.rect.right,
                        axisLabel.rect.top + axisLabel.rect.height * 0.5f + axisLabelPaint.textSize * 0.35f,
                        axisLabelPaint,
                    )
                }
            }
        }
    }
}

internal fun computeMatrixHeatmapLayout(
    widthPx: Float,
    heightPx: Float,
    data: MatrixHeatmapData,
    style: MatrixHeatmapChartStyleOptions,
    presentation: MatrixHeatmapChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): MatrixHeatmapChartLayoutResult {
    val axisLabelPaint = newTextPaint(
        color = style.axisLabelColor,
        textSizePx = presentation.axisLabelTextSizeSp * scaledDensity,
    )
    val xAxisLabelHeightPx = if (presentation.showXAxisLabels && data.xLabels.isNotEmpty()) {
        axisLabelPaint.fontSpacing
    } else {
        0f
    }
    val yAxisLabelWidthPx = if (presentation.showYAxisLabels && data.yLabels.isNotEmpty()) {
        data.yLabels.maxOfOrNull { label -> axisLabelPaint.measureText(label.trim()) } ?: 0f
    } else {
        0f
    }
    return resolveMatrixHeatmapChartLayout(
        data = data,
        config = MatrixHeatmapChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = style.contentPaddingDp * density,
            xAxisLabelHeightPx = xAxisLabelHeightPx,
            yAxisLabelWidthPx = yAxisLabelWidthPx,
            axisLabelGapPx = style.axisLabelGapDp * density,
            cellGapPx = style.cellGapDp * density,
        ),
        style = style,
        presentation = presentation,
    )
}

private fun fitTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
    if (text.isBlank() || maxWidth <= 0f) return ""
    if (paint.measureText(text) <= maxWidth) return text
    val suffix = "..."
    val suffixWidth = paint.measureText(suffix)
    if (suffixWidth >= maxWidth) return ""
    var end = text.length
    while (end > 0 && paint.measureText(text, 0, end) + suffixWidth > maxWidth) {
        end--
    }
    return if (end <= 0) "" else text.substring(0, end) + suffix
}
