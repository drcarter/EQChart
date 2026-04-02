package com.magimon.eq.treemap

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import kotlin.math.min

private const val MIN_LABEL_WIDTH_DP = 42f
private const val MIN_LABEL_HEIGHT_DP = 22f
private const val MIN_SUPPORTING_TEXT_HEIGHT_DP = 36f
private const val TEXT_SCALE_BASE_DP = 58f
private const val TILE_TEXT_PADDING_DP = 4f

internal fun buildTreemapViewLayout(
    groups: List<TreemapGroup>,
    widthPx: Int,
    heightPx: Int,
    density: Float,
    styleOptions: TreemapChartStyleOptions,
    presentationOptions: TreemapChartPresentationOptions,
): TreemapChartLayoutResult {
    return resolveTreemapChartLayout(
        groups = groups,
        config = TreemapChartLayoutConfig(
            widthPx = widthPx.toFloat(),
            heightPx = heightPx.toFloat(),
            contentPaddingPx = styleOptions.contentPaddingDp * density,
            groupGapPx = styleOptions.groupGapDp * density,
            tileGapPx = styleOptions.tileGapDp * density,
            headerHeightPx = styleOptions.headerHeightDp * density,
        ),
        style = styleOptions,
        presentation = presentationOptions,
    )
}

internal fun drawTreemapScene(
    canvas: Canvas,
    widthPx: Int,
    heightPx: Int,
    layout: TreemapChartLayoutResult,
    density: Float,
    scaledDensity: Float,
    styleOptions: TreemapChartStyleOptions,
    presentationOptions: TreemapChartPresentationOptions,
) {
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = styleOptions.tileBorderColor
        strokeWidth = 1f
    }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = styleOptions.headerTextColor
        textAlign = Paint.Align.LEFT
        textSize = presentationOptions.headerTextSizeSp * scaledDensity
        isFakeBoldText = true
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = styleOptions.itemLabelTextColor
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    val supportingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = styleOptions.itemSupportingTextColor
        textAlign = Paint.Align.LEFT
    }
    val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = styleOptions.headerTextColor
        textAlign = Paint.Align.CENTER
        textSize = 14f * scaledDensity
    }

    tilePaint.color = styleOptions.backgroundColor
    canvas.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), tilePaint)

    if (!layout.isRenderable) {
        if (presentationOptions.emptyText.isNotBlank()) {
            canvas.drawText(
                presentationOptions.emptyText,
                widthPx * 0.5f,
                heightPx * 0.5f,
                emptyTextPaint,
            )
        }
        return
    }

    layout.itemLayouts.forEach { block ->
        tilePaint.color = block.fillColor
        canvas.drawRect(block.rect.left, block.rect.top, block.rect.right, block.rect.bottom, tilePaint)
        canvas.drawRect(block.rect.left, block.rect.top, block.rect.right, block.rect.bottom, borderPaint)
        drawTreemapBlockText(
            canvas = canvas,
            block = block,
            density = density,
            scaledDensity = scaledDensity,
            presentationOptions = presentationOptions,
            labelPaint = labelPaint,
            supportingPaint = supportingPaint,
        )
    }

    layout.groupHeaders.forEach { header ->
        headerPaint.color = header.fillColor
        canvas.drawRect(header.rect.left, header.rect.top, header.rect.right, header.rect.bottom, headerPaint)
        canvas.drawRect(header.rect.left, header.rect.top, header.rect.right, header.rect.bottom, borderPaint)
        canvas.drawText(
            header.group.label,
            header.rect.left + density * TILE_TEXT_PADDING_DP,
            header.rect.top + headerTextPaint.textSize + (2f * density),
            headerTextPaint,
        )
    }
}

private fun drawTreemapBlockText(
    canvas: Canvas,
    block: TreemapItemLayout,
    density: Float,
    scaledDensity: Float,
    presentationOptions: TreemapChartPresentationOptions,
    labelPaint: Paint,
    supportingPaint: Paint,
) {
    val width = block.rect.width
    val height = block.rect.height
    if (width < MIN_LABEL_WIDTH_DP * density || height < MIN_LABEL_HEIGHT_DP * density) return

    val textScale = (min(width, height) / (TEXT_SCALE_BASE_DP * density)).coerceIn(0.85f, 1.45f)
    labelPaint.textSize = presentationOptions.itemLabelTextSizeSp * scaledDensity * textScale
    supportingPaint.textSize = presentationOptions.itemSupportingTextSizeSp * scaledDensity * textScale

    val padding = TILE_TEXT_PADDING_DP * density
    val labelX = block.rect.left + padding
    val labelY = block.rect.top + padding + labelPaint.textSize
    canvas.drawText(block.item.label, labelX, labelY, labelPaint)

    if (
        presentationOptions.showSupportingText &&
        !block.item.supportingText.isNullOrBlank() &&
        height >= MIN_SUPPORTING_TEXT_HEIGHT_DP * density
    ) {
        val supportingY = labelY + supportingPaint.textSize + (2f * density)
        canvas.drawText(block.item.supportingText!!, labelX, supportingY, supportingPaint)
    }
}

internal fun findTreemapHit(
    layout: TreemapChartLayoutResult,
    event: MotionEvent,
): TreemapItemLayout? {
    return layout.itemLayouts.firstOrNull { it.rect.contains(event.x, event.y) }
}
