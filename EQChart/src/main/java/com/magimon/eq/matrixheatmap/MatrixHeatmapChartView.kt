package com.magimon.eq.matrixheatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * Android View renderer for categorical matrix heatmap charts.
 *
 * The view renders a fixed X/Y grid using the shared
 * [resolveMatrixHeatmapChartLayout] engine so its geometry matches the Compose
 * implementation.
 *
 * @see MatrixHeatmapData
 * @see MatrixHeatmapCell
 * @see MatrixHeatmapChartStyleOptions
 * @see MatrixHeatmapChartPresentationOptions
 */
class MatrixHeatmapChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private var data = MatrixHeatmapData(emptyList(), emptyList(), emptyList())
    private var styleOptions = MatrixHeatmapChartStyleOptions()
    private var presentationOptions = MatrixHeatmapChartPresentationOptions()
    private var layout = resolveMatrixHeatmapChartLayout(
        data = data,
        config = emptyLayoutConfig(),
        style = styleOptions,
        presentation = presentationOptions,
    )
    private var onCellClickListener: ((MatrixHeatmapCell) -> Unit)? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cellFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    init {
        applyStyle()
    }

    /**
     * Replaces the matrix data rendered by this view.
     */
    fun setData(data: MatrixHeatmapData) {
        this.data = data
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Applies updated visual styling.
     */
    fun setStyleOptions(options: MatrixHeatmapChartStyleOptions) {
        styleOptions = options
        applyStyle()
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Applies updated label and empty-state behavior.
     */
    fun setPresentationOptions(options: MatrixHeatmapChartPresentationOptions) {
        presentationOptions = options
        applyStyle()
        recomputeLayout(width, height)
        invalidate()
    }

    /**
     * Registers a click callback for rendered cells.
     */
    fun setOnCellClickListener(listener: (MatrixHeatmapCell) -> Unit) {
        onCellClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (840 * density).toInt()
        val desiredHeight = (520 * density).toInt()
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

        val chartRect = RectF(
            layout.chartRect.left,
            layout.chartRect.top,
            layout.chartRect.right,
            layout.chartRect.bottom,
        )
        canvas.drawRect(chartRect, axisLinePaint)

        val radius = styleOptions.cellCornerRadiusDp * density
        layout.cellLayouts.forEach { cell ->
            val rect = RectF(cell.rect.left, cell.rect.top, cell.rect.right, cell.rect.bottom)
            cellFillPaint.color = cell.fillColor
            canvas.drawRoundRect(rect, radius, radius, cellFillPaint)
            canvas.drawRoundRect(rect, radius, radius, cellBorderPaint)

            if (
                presentationOptions.showCellText &&
                cell.value != null &&
                rect.width() >= presentationOptions.minCellTextWidthDp * density &&
                rect.height() >= presentationOptions.minCellTextHeightDp * density
            ) {
                val text = fitTextToWidth(
                    text = cell.resolvedText.orEmpty(),
                    paint = cellTextPaint,
                    maxWidth = rect.width() - 8f * density,
                )
                if (text.isNotEmpty()) {
                    cellTextPaint.textAlign = Paint.Align.CENTER
                    canvas.drawText(
                        text,
                        rect.centerX(),
                        rect.centerY() + cellTextPaint.textSize * 0.35f,
                        cellTextPaint,
                    )
                }
            }
        }

        axisLabelPaint.textAlign = Paint.Align.CENTER
        layout.xAxisLabels.forEach { axisLabel ->
            val text = fitTextToWidth(
                text = axisLabel.label,
                paint = axisLabelPaint,
                maxWidth = axisLabel.rect.width - 4f * density,
            )
            if (text.isNotEmpty()) {
                canvas.drawText(
                    text,
                    axisLabel.rect.left + axisLabel.rect.width * 0.5f,
                    axisLabel.rect.top + axisLabel.rect.height * 0.5f + axisLabelPaint.textSize * 0.35f,
                    axisLabelPaint,
                )
            }
        }

        axisLabelPaint.textAlign = Paint.Align.RIGHT
        layout.yAxisLabels.forEach { axisLabel ->
            val text = fitTextToWidth(
                text = axisLabel.label,
                paint = axisLabelPaint,
                maxWidth = axisLabel.rect.width - 4f * density,
            )
            if (text.isNotEmpty()) {
                canvas.drawText(
                    text,
                    axisLabel.rect.right,
                    axisLabel.rect.top + axisLabel.rect.height * 0.5f + axisLabelPaint.textSize * 0.35f,
                    axisLabelPaint,
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val hit = findMatrixHeatmapHit(layout, event.x, event.y)
                val cell = hit?.cell
                if (cell != null) {
                    onCellClickListener?.invoke(cell)
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
        axisLinePaint.color = styleOptions.axisLineColor
        axisLinePaint.strokeWidth = 1f * density
        cellBorderPaint.color = styleOptions.cellBorderColor
        cellBorderPaint.strokeWidth = 1f * density
        axisLabelPaint.color = styleOptions.axisLabelColor
        axisLabelPaint.textSize = presentationOptions.axisLabelTextSizeSp * scaledDensity
        cellTextPaint.color = styleOptions.cellTextColor
        cellTextPaint.textSize = presentationOptions.cellTextSizeSp * scaledDensity
        emptyTextPaint.color = styleOptions.axisLabelColor
        emptyTextPaint.textSize = presentationOptions.axisLabelTextSizeSp * scaledDensity
    }

    private fun recomputeLayout(w: Int, h: Int) {
        layout = resolveMatrixHeatmapChartLayout(
            data = data,
            config = MatrixHeatmapChartLayoutConfig(
                widthPx = w.toFloat(),
                heightPx = h.toFloat(),
                contentPaddingPx = styleOptions.contentPaddingDp * density,
                xAxisLabelHeightPx = if (presentationOptions.showXAxisLabels && data.xLabels.isNotEmpty()) {
                    axisLabelPaint.fontSpacing
                } else {
                    0f
                },
                yAxisLabelWidthPx = if (presentationOptions.showYAxisLabels && data.yLabels.isNotEmpty()) {
                    maxLabelWidth(data.yLabels)
                } else {
                    0f
                },
                axisLabelGapPx = styleOptions.axisLabelGapDp * density,
                cellGapPx = styleOptions.cellGapDp * density,
            ),
            style = styleOptions,
            presentation = presentationOptions,
        )
    }

    private fun maxLabelWidth(labels: List<String>): Float {
        return labels.maxOfOrNull { label -> axisLabelPaint.measureText(label.trim()) } ?: 0f
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

    private fun emptyLayoutConfig(): MatrixHeatmapChartLayoutConfig {
        return MatrixHeatmapChartLayoutConfig(
            widthPx = 0f,
            heightPx = 0f,
            contentPaddingPx = 0f,
            xAxisLabelHeightPx = 0f,
            yAxisLabelWidthPx = 0f,
            axisLabelGapPx = 0f,
            cellGapPx = 0f,
        )
    }
}
