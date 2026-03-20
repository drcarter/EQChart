package com.magimon.eq.compose.bubble

import androidx.compose.ui.geometry.Rect
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubbleLegendItem
import com.magimon.eq.bubble.BubbleLegendMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.bubble.BubbleScaleOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleChartTest {

    private val data = listOf(
        BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech"),
        BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance"),
        BubbleDatum(3.0, 3.5, 10.0, 0xFFFB8C00.toInt(), "C", "Tech"),
    )

    @Test
    fun computeBubbleChart_buildsScatterAndPackedLayouts() {
        val scatter = computeBubbleChart(
            width = 360f,
            height = 260f,
            data = data,
            axisOptions = BubbleAxisOptions(),
            presentationOptions = BubblePresentationOptions(showLegend = true, title = "Bubbles"),
            scaleOverride = null,
            layoutMode = BubbleLayoutMode.SCATTER,
            explicitLegendItems = emptyList(),
            density = 1f,
            scaledDensity = 1f,
        )
        val packed = computeBubbleChart(
            width = 360f,
            height = 260f,
            data = data,
            axisOptions = BubbleAxisOptions(),
            presentationOptions = BubblePresentationOptions(),
            scaleOverride = BubbleScaleOverride(xMin = 0.0, xMax = 5.0),
            layoutMode = BubbleLayoutMode.PACKED,
            explicitLegendItems = listOf(BubbleLegendItem("Manual", 1)),
            density = 1f,
            scaledDensity = 1f,
        )

        assertEquals(3, scatter.layouts.size)
        assertFalse(scatter.xTicks.isEmpty())
        assertTrue(scatter.title.isNotBlank())
        assertEquals(3, packed.layouts.size)
        assertTrue(packed.xTicks.isEmpty())
    }

    @Test
    fun bubbleHelpers_coverLegendRangeAndMappingBranches() {
        val autoLegend = resolveBubbleLegend(data, BubbleLegendMode.AUTO, emptyList())
        val overrideLegend = resolveBubbleLegend(data, BubbleLegendMode.AUTO_WITH_OVERRIDE, listOf(BubbleLegendItem("Override", 7)))
        val range = bubbleResolveRange(listOf(4.0, 4.0), 10.0, 2.0)
        val rect = Rect(0f, 0f, 100f, 100f)

        assertEquals(2, autoLegend.size)
        assertEquals("Override", overrideLegend.single().label)
        assertTrue(range.min < range.max)
        assertTrue(bubbleNormalize(3.0, BubbleNumericRange(0.0, 6.0)) > 0.0)
        assertEquals(50f, bubbleMapX(3.0, BubbleNumericRange(0.0, 6.0), rect), 0.001f)
        assertEquals(50f, bubbleMapY(3.0, BubbleNumericRange(0.0, 6.0), rect), 0.001f)
        assertEquals(5, bubbleTickValues(BubbleNumericRange(0.0, 4.0), 5).size)
    }

    @Test
    fun buildPackedBubbleLayouts_keepsBubblesInsidePlotRect() {
        val plotRect = Rect(0f, 0f, 200f, 120f)
        val layouts = buildPackedBubbleLayouts(
            data = data,
            plotRect = plotRect,
            sizeRange = BubbleNumericRange(10.0, 20.0),
            minRadius = 6f,
            maxRadius = 18f,
            density = 1f,
        )

        assertEquals(3, layouts.size)
        assertTrue(layouts.all { it.centerX in plotRect.left..plotRect.right })
        assertTrue(layouts.all { it.centerY in plotRect.top..plotRect.bottom })
    }

    @Test
    fun computeBubbleChart_returnsEmptyLayoutForInvalidDataOrTinyPlot() {
        val invalid = computeBubbleChart(
            width = 40f,
            height = 30f,
            data = listOf(BubbleDatum(Double.NaN, 1.0, 2.0, 1, "bad", null)),
            axisOptions = BubbleAxisOptions(showAxes = false, showTicks = false),
            presentationOptions = BubblePresentationOptions(showLegend = false),
            scaleOverride = null,
            layoutMode = BubbleLayoutMode.SCATTER,
            explicitLegendItems = emptyList(),
            density = 1f,
            scaledDensity = 1f,
        )
        val swapped = bubbleResolveRange(listOf(5.0, 2.0), 8.0, 3.0)

        assertTrue(invalid.layouts.isEmpty())
        assertTrue(invalid.legendItems.isEmpty())
        assertTrue(swapped.min <= swapped.max)
    }
}
