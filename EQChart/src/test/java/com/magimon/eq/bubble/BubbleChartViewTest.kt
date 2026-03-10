package com.magimon.eq.bubble

import android.graphics.Color
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BubbleChartViewTest {

    @Test
    fun scatterMode_drawsLegendAndDispatchesBubbleClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = BubbleChartView(context)
        var clickedLabel: String? = null

        view.setAxisOptions(BubbleAxisOptions())
        view.setPresentationOptions(
            BubblePresentationOptions(
                title = "Market Map",
                showLegend = true,
                legendMode = BubbleLegendMode.AUTO,
            ),
        )
        view.setLegendItems(listOf(BubbleLegendItem(label = "Manual", color = Color.BLACK)))
        view.setScaleOverride(BubbleScaleOverride(xMin = 0.0, xMax = 100.0, yMin = 0.0, yMax = 100.0, sizeMin = 0.0, sizeMax = 100.0))
        view.setOnBubbleClickListener { clickedLabel = it.label }
        view.setData(
            listOf(
                BubbleDatum(x = 20.0, y = 80.0, size = 40.0, color = Color.RED, label = "A", legendGroup = "Growth"),
                BubbleDatum(x = 80.0, y = 20.0, size = 60.0, color = Color.BLUE, label = "B", legendGroup = "Value"),
                BubbleDatum(x = Double.NaN, y = 10.0, size = 20.0, color = Color.GREEN, label = "Invalid"),
            ),
        )

        layoutAndDraw(view, width = 520, height = 360)

        val layouts = view.readPrivate<List<*>>("bubbleLayouts")
        val firstBubble = layouts.firstOrNull() ?: error("Expected at least one bubble")
        val centerX = firstBubble.readPrivate<Float>("centerX")
        val centerY = firstBubble.readPrivate<Float>("centerY")

        touchUp(view, centerX, centerY)

        assertNotNull(view.readPrivate<Any?>("selectedBubble"))
        assertTrue(clickedLabel == "A" || clickedLabel == "B")
    }

    @Test
    fun packedMode_recomputesLayout_withoutAxes() {
        val context = RuntimeEnvironment.getApplication()
        val view = BubbleChartView(context)

        view.setChartBackgroundColor(Color.WHITE)
        view.setAxisOptions(
            BubbleAxisOptions(
                showAxes = false,
                showGrid = false,
                showTicks = false,
            ),
        )
        view.setPresentationOptions(BubblePresentationOptions(showLegend = false))
        view.setLayoutMode(BubbleLayoutMode.PACKED)
        view.setData(
            listOf(
                BubbleDatum(x = 0.0, y = 0.0, size = 80.0, color = Color.MAGENTA, label = "One"),
                BubbleDatum(x = 0.0, y = 0.0, size = 50.0, color = Color.CYAN, label = "Two"),
                BubbleDatum(x = 0.0, y = 0.0, size = 30.0, color = Color.YELLOW, label = "Three"),
            ),
        )

        layoutAndDraw(view, width = 420, height = 320)

        val layouts = view.readPrivate<List<*>>("bubbleLayouts")
        val xTicks = view.readPrivate<List<*>>("xTicks")
        val yTicks = view.readPrivate<List<*>>("yTicks")

        assertEquals(3, layouts.size)
        assertFalse(xTicks.isNotEmpty())
        assertFalse(yTicks.isNotEmpty())
    }
}
