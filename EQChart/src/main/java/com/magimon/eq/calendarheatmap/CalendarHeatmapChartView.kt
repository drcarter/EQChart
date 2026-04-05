package com.magimon.eq.calendarheatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * Android View renderer for single-year calendar heatmap charts.
 */
class CalendarHeatmapChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var data = CalendarHeatmapData(year = 2026, days = emptyList())
    private var styleOptions = CalendarHeatmapChartStyleOptions()
    private var presentationOptions = CalendarHeatmapChartPresentationOptions()
    private var layout = resolveCalendarHeatmapChartLayout(
        data = data,
        config = emptyLayoutConfig(),
        style = styleOptions,
        presentation = presentationOptions,
    )
    private var onDayClickListener: ((CalendarHeatmapDay) -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cellFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val monthLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val weekdayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    init {
        applyStyle()
    }

    fun setData(data: CalendarHeatmapData) {
        this.data = data
        recomputeLayout(width, height)
        invalidate()
    }

    fun setStyleOptions(options: CalendarHeatmapChartStyleOptions) {
        styleOptions = options
        applyStyle()
        recomputeLayout(width, height)
        invalidate()
    }

    fun setPresentationOptions(options: CalendarHeatmapChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        recomputeLayout(width, height)
        invalidate()
    }

    fun setOnDayClickListener(listener: (CalendarHeatmapDay) -> Unit) {
        onDayClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (960 * density).toInt()
        val desiredHeight = (280 * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recomputeLayout(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (!layout.isRenderable) {
            if (presentationOptions.emptyText.isNotBlank()) {
                canvas.drawText(
                    presentationOptions.emptyText,
                    width * 0.5f,
                    height * 0.5f,
                    emptyTextPaint,
                )
            }
            return
        }

        val radius = styleOptions.cellCornerRadiusDp * density
        layout.dayCells.forEach { cell ->
            val rect = RectF(cell.rect.left, cell.rect.top, cell.rect.right, cell.rect.bottom)
            cellFillPaint.color = cell.fillColor
            canvas.drawRoundRect(rect, radius, radius, cellFillPaint)
            canvas.drawRoundRect(rect, radius, radius, cellBorderPaint)
        }

        monthLabelPaint.textAlign = Paint.Align.LEFT
        layout.monthLabels.forEach { label ->
            val text = fitTextToWidth(
                label.label,
                monthLabelPaint,
                label.rect.width,
            )
            if (text.isNotEmpty()) {
                canvas.drawText(
                    text,
                    label.rect.left,
                    label.rect.top + label.rect.height * 0.5f + monthLabelPaint.textSize * 0.35f,
                    monthLabelPaint,
                )
            }
        }

        weekdayLabelPaint.textAlign = Paint.Align.RIGHT
        layout.weekdayLabels.forEach { label ->
            canvas.drawText(
                label.label,
                label.rect.right,
                label.rect.top + label.rect.height * 0.5f + weekdayLabelPaint.textSize * 0.35f,
                weekdayLabelPaint,
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                findCalendarHeatmapHit(layout, event.x, event.y)?.day?.let { day ->
                    onDayClickListener?.invoke(day)
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun applyStyle() {
        backgroundPaint.color = styleOptions.backgroundColor
        cellBorderPaint.color = styleOptions.cellBorderColor
        cellBorderPaint.strokeWidth = 1f * density
        monthLabelPaint.color = styleOptions.labelTextColor
        monthLabelPaint.textSize = presentationOptions.monthLabelTextSizeSp * scaledDensity
        weekdayLabelPaint.color = styleOptions.labelTextColor
        weekdayLabelPaint.textSize = presentationOptions.weekdayLabelTextSizeSp * scaledDensity
        emptyTextPaint.color = styleOptions.labelTextColor
        emptyTextPaint.textSize = presentationOptions.monthLabelTextSizeSp * scaledDensity
    }

    private fun recomputeLayout(w: Int, h: Int) {
        layout = resolveCalendarHeatmapChartLayout(
            data = data,
            config = CalendarHeatmapChartLayoutConfig(
                widthPx = w.toFloat(),
                heightPx = h.toFloat(),
                contentPaddingPx = styleOptions.contentPaddingDp * density,
                monthLabelHeightPx = if (presentationOptions.showMonthLabels) monthLabelPaint.fontSpacing else 0f,
                weekdayLabelWidthPx = if (presentationOptions.showWeekdayLabels) {
                    max(
                        weekdayLabelPaint.measureText("Mon"),
                        max(weekdayLabelPaint.measureText("Wed"), weekdayLabelPaint.measureText("Fri")),
                    )
                } else {
                    0f
                },
                labelGapPx = styleOptions.labelGapDp * density,
                cellGapPx = styleOptions.cellGapDp * density,
            ),
            style = styleOptions,
            presentation = presentationOptions,
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

    private fun emptyLayoutConfig(): CalendarHeatmapChartLayoutConfig {
        return CalendarHeatmapChartLayoutConfig(
            widthPx = 0f,
            heightPx = 0f,
            contentPaddingPx = 0f,
            monthLabelHeightPx = 0f,
            weekdayLabelWidthPx = 0f,
            labelGapPx = 0f,
            cellGapPx = 0f,
        )
    }
}
