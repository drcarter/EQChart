package com.magimon.eq.compose.sunburst

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magimon.eq.compose.internal.degreeToOffset
import com.magimon.eq.compose.internal.newTextPaint
import com.magimon.eq.compose.internal.toComposeColor
import com.magimon.eq.sunburst.SunburstChartLayoutEngine
import com.magimon.eq.sunburst.SunburstChartPresentationOptions
import com.magimon.eq.sunburst.SunburstChartStyleOptions
import com.magimon.eq.sunburst.SunburstNode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * Compose Sunburst chart for hierarchical part-to-whole data.
 *
 * The composable consumes shared [SunburstNode] contracts and delegates normalized geometry and
 * hit testing to [SunburstChartLayoutEngine].
 *
 * @param nodes Root nodes that define the visible hierarchy.
 * @param modifier Compose modifier applied to the chart container.
 * @param styleOptions Shared visual styling for rings, labels, and the center text.
 * @param presentationOptions Shared presentation and animation behavior.
 * @param onNodeClick Optional callback invoked when a rendered segment is tapped.
 * @see SunburstNode
 * @see SunburstChartStyleOptions
 * @see SunburstChartPresentationOptions
 * @see SunburstChartLayoutEngine
 */
@Composable
fun SunburstChart(
    nodes: List<SunburstNode>,
    modifier: Modifier = Modifier,
    styleOptions: SunburstChartStyleOptions = SunburstChartStyleOptions(),
    presentationOptions: SunburstChartPresentationOptions = SunburstChartPresentationOptions(),
    onNodeClick: ((segmentIndex: Int, node: SunburstNode, payload: Any?) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val layout = remember(nodes, styleOptions, presentationOptions) {
        SunburstChartLayoutEngine.compute(
            nodes = nodes,
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )
    }
    var selectedPath by remember { mutableStateOf<List<Int>?>(null) }
    val drawProgress = remember { Animatable(1f) }

    LaunchedEffect(
        layout,
        presentationOptions.animateOnDataChange,
        presentationOptions.enterAnimationDurationMs,
        presentationOptions.enterAnimationDelayMs,
    ) {
        selectedPath = null
        if (presentationOptions.animateOnDataChange && layout.isRenderable) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = presentationOptions.enterAnimationDurationMs.toInt().coerceAtLeast(0),
                    delayMillis = presentationOptions.enterAnimationDelayMs.toInt().coerceAtLeast(0),
                ),
            )
        } else {
            drawProgress.snapTo(1f)
        }
    }

    Box(
        modifier = modifier.pointerInput(layout) {
            detectTapGestures { tap ->
                if (!layout.isRenderable) return@detectTapGestures
                val contentPadding = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
                val radius = (min(size.width, size.height) * 0.5f - contentPadding).coerceAtLeast(0f)
                if (radius <= 0f) return@detectTapGestures
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                val dx = tap.x - center.x
                val dy = tap.y - center.y
                val normalizedRadius = hypot(dx, dy) / radius
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val hit = SunburstChartLayoutEngine.hitTestSegment(layout, normalizedRadius, angle)
                selectedPath = hit?.path
                if (hit != null) {
                    findNode(nodes, hit.path)?.let { node ->
                        val index = layout.segments.indexOfFirst { it.path == hit.path }
                        onNodeClick?.invoke(index, node, node.payload)
                    }
                }
            }
        },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(styleOptions.backgroundColor.toComposeColor())

            val contentPadding = with(density) { styleOptions.contentPaddingDp.dp.toPx() }
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val radius = (min(size.width, size.height) * 0.5f - contentPadding).coerceAtLeast(0f)
            if (!layout.isRenderable || radius <= 0f) {
                val message = presentationOptions.emptyText?.trim().orEmpty()
                if (message.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        message,
                        center.x,
                        center.y,
                        newTextPaint(
                            color = styleOptions.centerSubTextColor,
                            textSizePx = with(density) { 13f.sp.toPx() },
                            align = Paint.Align.CENTER,
                        ),
                    )
                }
                return@Canvas
            }

            val strokeWidth = with(density) { styleOptions.segmentStrokeWidthDp.dp.toPx() }.coerceAtLeast(1f)
            val segmentPath = Path()
            val progress = drawProgress.value.coerceIn(0f, 1f)

            layout.segments.forEach { segment ->
                val innerRadius = radius * segment.innerRadiusRatio
                val outerRadius = radius * segment.outerRadiusRatio
                val sweep = segment.sweepAngleDeg * progress
                if (abs(sweep) < 0.01f) return@forEach

                buildSegmentPath(
                    path = segmentPath,
                    center = center,
                    innerRadius = innerRadius,
                    outerRadius = outerRadius,
                    startAngleDeg = segment.startAngleDeg,
                    sweepAngleDeg = sweep,
                )

                drawPath(segmentPath, color = segment.color.toComposeColor())
                drawPath(
                    path = segmentPath,
                    color = if (selectedPath == segment.path) {
                        styleOptions.selectedSegmentStrokeColor.toComposeColor()
                    } else {
                        styleOptions.segmentStrokeColor.toComposeColor()
                    },
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )

                if (presentationOptions.showLabels && abs(sweep) >= presentationOptions.minLabelSweepDeg) {
                    val midAngle = segment.startAngleDeg + (sweep * 0.5f)
                    val labelRadius = (innerRadius + outerRadius) * 0.5f
                    val labelOffset = degreeToOffset(midAngle, labelRadius)
                    val labelY = center.y + labelOffset.y
                    val labelPaint = newTextPaint(
                        color = styleOptions.labelTextColor,
                        textSizePx = with(density) { styleOptions.labelTextSizeSp.sp.toPx() },
                        align = Paint.Align.CENTER,
                    )
                    val baseline = labelY - ((labelPaint.descent() + labelPaint.ascent()) * 0.5f)
                    drawContext.canvas.nativeCanvas.drawText(
                        segment.label,
                        center.x + labelOffset.x,
                        baseline,
                        labelPaint,
                    )
                }
            }

            val centerText = presentationOptions.centerText?.trim().orEmpty()
            val centerSubText = presentationOptions.centerSubText?.trim().orEmpty()
            if (radius * presentationOptions.innerHoleRatio > 0f && (centerText.isNotEmpty() || centerSubText.isNotEmpty())) {
                val titlePaint = newTextPaint(
                    color = styleOptions.centerTextColor,
                    textSizePx = with(density) { styleOptions.centerTextSizeSp.sp.toPx() },
                    align = Paint.Align.CENTER,
                    bold = true,
                )
                val subtitlePaint = newTextPaint(
                    color = styleOptions.centerSubTextColor,
                    textSizePx = with(density) { styleOptions.centerSubTextSizeSp.sp.toPx() },
                    align = Paint.Align.CENTER,
                )
                if (centerText.isNotEmpty() && centerSubText.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(centerText, center.x, center.y - with(density) { 6f.dp.toPx() }, titlePaint)
                    drawContext.canvas.nativeCanvas.drawText(centerSubText, center.x, center.y + with(density) { 12f.dp.toPx() }, subtitlePaint)
                } else if (centerText.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        centerText,
                        center.x,
                        center.y - ((titlePaint.descent() + titlePaint.ascent()) * 0.5f),
                        titlePaint,
                    )
                } else {
                    drawContext.canvas.nativeCanvas.drawText(
                        centerSubText,
                        center.x,
                        center.y - ((subtitlePaint.descent() + subtitlePaint.ascent()) * 0.5f),
                        subtitlePaint,
                    )
                }
            }
        }
    }
}

private fun findNode(nodes: List<SunburstNode>, path: List<Int>): SunburstNode? {
    var current: SunburstNode? = null
    var level = nodes
    path.forEach { index ->
        current = level.getOrNull(index) ?: return null
        level = current?.children ?: emptyList()
    }
    return current
}

private fun buildSegmentPath(
    path: Path,
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    startAngleDeg: Float,
    sweepAngleDeg: Float,
) {
    path.reset()
    val outerRect = Rect(center - Offset(outerRadius, outerRadius), Size(outerRadius * 2f, outerRadius * 2f))
    val innerRect = Rect(center - Offset(innerRadius, innerRadius), Size(innerRadius * 2f, innerRadius * 2f))
    val outerStart = center + degreeToOffset(startAngleDeg, outerRadius)
    path.moveTo(outerStart.x, outerStart.y)
    path.arcTo(outerRect, startAngleDeg, sweepAngleDeg, false)
    if (innerRadius > 0f) {
        val innerEnd = center + degreeToOffset(startAngleDeg + sweepAngleDeg, innerRadius)
        path.lineTo(innerEnd.x, innerEnd.y)
        path.arcTo(innerRect, startAngleDeg + sweepAngleDeg, -sweepAngleDeg, false)
    } else {
        path.lineTo(center.x, center.y)
    }
    path.close()
}
