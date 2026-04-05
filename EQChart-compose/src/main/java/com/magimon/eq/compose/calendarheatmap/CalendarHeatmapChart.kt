package com.magimon.eq.compose.calendarheatmap

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartLayoutConfig
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartLayoutResult
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartPresentationOptions
import com.magimon.eq.calendarheatmap.CalendarHeatmapChartStyleOptions
import com.magimon.eq.calendarheatmap.CalendarHeatmapData
import com.magimon.eq.calendarheatmap.CalendarHeatmapDay
import com.magimon.eq.calendarheatmap.findCalendarHeatmapHit
import com.magimon.eq.calendarheatmap.resolveCalendarHeatmapChartLayout
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor

/**
 * Compose calendar heatmap chart for single-year contribution-style data.
 */
@Composable
fun CalendarHeatmapChart(
    data: CalendarHeatmapData,
    modifier: Modifier = Modifier,
    styleOptions: CalendarHeatmapChartStyleOptions = CalendarHeatmapChartStyleOptions(),
    presentationOptions: CalendarHeatmapChartPresentationOptions = CalendarHeatmapChartPresentationOptions(),
    onDayClick: ((CalendarHeatmapDay) -> Unit)? = null,
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
            computeCalendarHeatmapLayout(
                widthPx = widthPx,
                heightPx = heightPx,
                data = data,
                style = styleOptions,
                presentation = presentationOptions,
                density = densityPx,
                scaledDensity = scaledDensity,
            )
        }

        val monthLabelPaint = newTextPaint(
            color = styleOptions.labelTextColor,
            textSizePx = presentationOptions.monthLabelTextSizeSp * scaledDensity,
            align = Paint.Align.LEFT,
        )
        val weekdayLabelPaint = newTextPaint(
            color = styleOptions.labelTextColor,
            textSizePx = presentationOptions.weekdayLabelTextSizeSp * scaledDensity,
            align = Paint.Align.RIGHT,
        )
        val emptyTextPaint = newTextPaint(
            color = styleOptions.labelTextColor,
            textSizePx = presentationOptions.monthLabelTextSizeSp * scaledDensity,
            align = Paint.Align.CENTER,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.dayCells, onDayClick) {
                    detectTapGestures { tap ->
                        findCalendarHeatmapHit(computed, tap.x, tap.y)?.day?.let { day ->
                            onDayClick?.invoke(day)
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

            val radius = styleOptions.cellCornerRadiusDp * densityPx
            computed.dayCells.forEach { cell ->
                drawRoundRect(
                    color = cell.fillColor.toComposeColor(),
                    topLeft = Offset(cell.rect.left, cell.rect.top),
                    size = Size(cell.rect.width, cell.rect.height),
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    color = styleOptions.cellBorderColor.toComposeColor(),
                    topLeft = Offset(cell.rect.left, cell.rect.top),
                    size = Size(cell.rect.width, cell.rect.height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 1f),
                )
            }

            computed.monthLabels.forEach { label ->
                drawContext.canvas.nativeCanvas.drawText(
                    fitTextToWidth(label.label, monthLabelPaint, label.rect.width),
                    label.rect.left,
                    label.rect.top + label.rect.height * 0.5f + monthLabelPaint.textSize * 0.35f,
                    monthLabelPaint,
                )
            }

            computed.weekdayLabels.forEach { label ->
                drawContext.canvas.nativeCanvas.drawText(
                    label.label,
                    label.rect.right,
                    label.rect.top + label.rect.height * 0.5f + weekdayLabelPaint.textSize * 0.35f,
                    weekdayLabelPaint,
                )
            }
        }
    }
}

internal fun computeCalendarHeatmapLayout(
    widthPx: Float,
    heightPx: Float,
    data: CalendarHeatmapData,
    style: CalendarHeatmapChartStyleOptions,
    presentation: CalendarHeatmapChartPresentationOptions,
    density: Float,
    scaledDensity: Float,
): CalendarHeatmapChartLayoutResult {
    val monthLabelPaint = newTextPaint(
        color = style.labelTextColor,
        textSizePx = presentation.monthLabelTextSizeSp * scaledDensity,
        align = Paint.Align.LEFT,
    )
    val weekdayLabelPaint = newTextPaint(
        color = style.labelTextColor,
        textSizePx = presentation.weekdayLabelTextSizeSp * scaledDensity,
        align = Paint.Align.RIGHT,
    )
    return resolveCalendarHeatmapChartLayout(
        data = data,
        config = CalendarHeatmapChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = style.contentPaddingDp * density,
            monthLabelHeightPx = if (presentation.showMonthLabels) monthLabelPaint.fontSpacing else 0f,
            weekdayLabelWidthPx = if (presentation.showWeekdayLabels) {
                maxOf(
                    weekdayLabelPaint.measureText("Mon"),
                    weekdayLabelPaint.measureText("Wed"),
                    weekdayLabelPaint.measureText("Fri"),
                )
            } else {
                0f
            },
            labelGapPx = style.labelGapDp * density,
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
