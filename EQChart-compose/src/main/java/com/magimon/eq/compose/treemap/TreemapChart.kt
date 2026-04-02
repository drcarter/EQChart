package com.magimon.eq.compose.treemap

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.treemap.TreemapChartLayoutConfig
import com.magimon.eq.treemap.TreemapChartLayoutResult
import com.magimon.eq.treemap.TreemapChartPresentationOptions
import com.magimon.eq.treemap.TreemapChartStyleOptions
import com.magimon.eq.treemap.TreemapGroup
import com.magimon.eq.treemap.TreemapItem
import com.magimon.eq.treemap.TreemapItemLayout
import com.magimon.eq.treemap.resolveTreemapChartLayout
import kotlin.math.min

private const val MIN_LABEL_WIDTH_DP = 42f
private const val MIN_LABEL_HEIGHT_DP = 22f
private const val MIN_SUPPORTING_TEXT_HEIGHT_DP = 36f
private const val TEXT_SCALE_BASE_DP = 58f
private const val TILE_TEXT_PADDING_DP = 4f

/**
 * Compose Treemap chart for grouped part-to-whole data.
 *
 * @param groups Two-level group/item input rendered as tiled rectangles.
 * @param modifier Compose modifier applied to the chart container.
 * @param styleOptions Shared visual styling for tiles, headers, and spacing.
 * @param presentationOptions Shared label and empty-state behavior.
 * @param onItemClick Optional callback invoked when a rendered tile is tapped.
 */
@Composable
fun TreemapChart(
    groups: List<TreemapGroup>,
    modifier: Modifier = Modifier,
    styleOptions: TreemapChartStyleOptions = TreemapChartStyleOptions(),
    presentationOptions: TreemapChartPresentationOptions = TreemapChartPresentationOptions(),
    onItemClick: ((TreemapItem) -> Unit)? = null,
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val computed = remember(groups, widthPx, heightPx, styleOptions, presentationOptions, density.density) {
            computeTreemapLayout(
                groups = groups,
                widthPx = widthPx,
                heightPx = heightPx,
                density = density.density,
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(computed.itemLayouts, onItemClick) {
                    detectTapGestures { tap ->
                        val hit = computed.itemLayouts.firstOrNull { it.rect.contains(tap.x, tap.y) }
                        hit?.let { onItemClick?.invoke(it.item) }
                    }
                },
        ) {
            drawRect(color = styleOptions.backgroundColor.toComposeColor())

            if (!computed.isRenderable) {
                if (presentationOptions.emptyText.isNotBlank()) {
                    val emptyPaint = newTextPaint(
                        color = styleOptions.headerTextColor,
                        textSizePx = with(density) { 14.sp.toPx() },
                        align = Paint.Align.CENTER,
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        presentationOptions.emptyText,
                        size.width * 0.5f,
                        size.height * 0.5f,
                        emptyPaint,
                    )
                }
                return@Canvas
            }

            computed.itemLayouts.forEach { block ->
                drawRect(
                    color = block.fillColor.toComposeColor(),
                    topLeft = Offset(block.rect.left, block.rect.top),
                    size = Size(block.rect.width, block.rect.height),
                )
                drawRect(
                    color = styleOptions.tileBorderColor.toComposeColor(),
                    topLeft = Offset(block.rect.left, block.rect.top),
                    size = Size(block.rect.width, block.rect.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                )
                drawTreemapBlockText(
                    block = block,
                    density = density.density,
                    scaledDensity = density.fontScale * density.density,
                    styleOptions = styleOptions,
                    presentationOptions = presentationOptions,
                )
            }

            val titlePaint = newTextPaint(
                color = styleOptions.headerTextColor,
                textSizePx = with(density) { presentationOptions.headerTextSizeSp.sp.toPx() },
                align = Paint.Align.LEFT,
                bold = true,
            )
            computed.groupHeaders.forEach { header ->
                drawRect(
                    color = header.fillColor.toComposeColor(),
                    topLeft = Offset(header.rect.left, header.rect.top),
                    size = Size(header.rect.width, header.rect.height),
                )
                drawRect(
                    color = styleOptions.tileBorderColor.toComposeColor(),
                    topLeft = Offset(header.rect.left, header.rect.top),
                    size = Size(header.rect.width, header.rect.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    header.group.label,
                    header.rect.left + density.density * TILE_TEXT_PADDING_DP,
                    header.rect.top + titlePaint.textSize + (2f * density.density),
                    titlePaint,
                )
            }
        }
    }
}

internal fun computeTreemapLayout(
    groups: List<TreemapGroup>,
    widthPx: Float,
    heightPx: Float,
    density: Float,
    styleOptions: TreemapChartStyleOptions,
    presentationOptions: TreemapChartPresentationOptions,
): TreemapChartLayoutResult {
    return resolveTreemapChartLayout(
        groups = groups,
        config = TreemapChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = styleOptions.contentPaddingDp * density,
            groupGapPx = styleOptions.groupGapDp * density,
            tileGapPx = styleOptions.tileGapDp * density,
            headerHeightPx = styleOptions.headerHeightDp * density,
        ),
        style = styleOptions,
        presentation = presentationOptions,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTreemapBlockText(
    block: TreemapItemLayout,
    density: Float,
    scaledDensity: Float,
    styleOptions: TreemapChartStyleOptions,
    presentationOptions: TreemapChartPresentationOptions,
) {
    val width = block.rect.width
    val height = block.rect.height
    if (width < MIN_LABEL_WIDTH_DP * density || height < MIN_LABEL_HEIGHT_DP * density) return

    val textScale = (min(width, height) / (TEXT_SCALE_BASE_DP * density)).coerceIn(0.85f, 1.45f)
    val symbolPaint = newTextPaint(
        color = styleOptions.itemLabelTextColor,
        textSizePx = presentationOptions.itemLabelTextSizeSp * scaledDensity * textScale,
        align = Paint.Align.LEFT,
        bold = true,
    )
    val supportingPaint = newTextPaint(
        color = styleOptions.itemSupportingTextColor,
        textSizePx = presentationOptions.itemSupportingTextSizeSp * scaledDensity * textScale,
        align = Paint.Align.LEFT,
    )

    val padding = TILE_TEXT_PADDING_DP * density
    val labelX = block.rect.left + padding
    val labelY = block.rect.top + padding + symbolPaint.textSize
    drawContext.canvas.nativeCanvas.drawText(block.item.label, labelX, labelY, symbolPaint)

    val supportingText = block.item.supportingText
    if (
        presentationOptions.showSupportingText &&
        !supportingText.isNullOrBlank() &&
        height >= MIN_SUPPORTING_TEXT_HEIGHT_DP * density
    ) {
        val supportingY = labelY + supportingPaint.textSize + (2f * density)
        drawContext.canvas.nativeCanvas.drawText(supportingText, labelX, supportingY, supportingPaint)
    }
}
