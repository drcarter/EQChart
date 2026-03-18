package com.magimon.eq.gauge

import android.graphics.Color
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GaugeChartViewTest {

    @Test
    fun gaugeChart_drawsWithValidValueAndRanges() {
        val context = RuntimeEnvironment.getApplication()
        val view = GaugeChartView(context)

        view.setStyleOptions(
            GaugeChartStyleOptions(
                backgroundColor = Color.WHITE,
                progressColor = Color.BLUE,
            ),
        )
        view.setPresentationOptions(GaugeChartPresentationOptions(animateOnValueChange = false))
        view.setRanges(
            listOf(
                GaugeRange(0.0, 50.0, Color.GREEN),
                GaugeRange(50.0, 80.0, Color.YELLOW),
                GaugeRange(80.0, 100.0, Color.RED),
            ),
        )
        view.setValue(GaugeValue(value = 72.0, minValue = 0.0, maxValue = 100.0, label = "CPU"))

        layoutAndDraw(view, width = 420, height = 300)

        assertEquals(0.72f, view.readPrivate<Float>("renderedProgress"), 0.0001f)
        assertTrue(view.readPrivate<Float>("arcRadius") > 0f)
        assertEquals(3, view.readPrivate<List<*>>("resolvedRanges").size)
    }

    @Test
    fun gaugeChart_showsEmptyStateForInvalidDomain() {
        val context = RuntimeEnvironment.getApplication()
        val view = GaugeChartView(context)

        view.setPresentationOptions(
            GaugeChartPresentationOptions(
                animateOnValueChange = false,
                emptyText = "Empty",
            ),
        )
        view.setValue(GaugeValue(value = 10.0, minValue = 5.0, maxValue = 5.0))

        layoutAndDraw(view, width = 360, height = 260)

        val resolved = view.readPrivate<Any?>("resolvedValue")
        assertEquals(0f, view.readPrivate<Float>("renderedProgress"), 0.0f)
        assertEquals(null, resolved)
    }
}
