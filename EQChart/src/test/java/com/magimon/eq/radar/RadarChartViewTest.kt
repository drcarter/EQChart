package com.magimon.eq.radar

import android.graphics.Color
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RadarChartViewTest {

    @Test
    fun radarChart_drawsRenderableSeries_andDispatchesPointClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = RadarChartView(context)
        var callbackSeriesIndex: Int? = null
        var callbackAxisIndex: Int? = null
        var callbackPayload: Any? = null

        view.setStyleOptions(RadarChartStyleOptions(backgroundColor = Color.WHITE))
        view.setPresentationOptions(
            RadarChartPresentationOptions(
                animateOnDataChange = false,
                showLegend = true,
                showAxisLabels = true,
                showPoints = true,
            ),
        )
        view.setOnPointClickListener { seriesIndex, axisIndex, _, payload ->
            callbackSeriesIndex = seriesIndex
            callbackAxisIndex = axisIndex
            callbackPayload = payload
        }
        view.setAxes(
            listOf(
                RadarAxis("Speed"),
                RadarAxis("Power"),
                RadarAxis("Control"),
                RadarAxis("Range"),
            ),
        )
        view.setSeries(
            listOf(
                RadarSeries(name = "invalid", color = Color.GRAY, values = listOf(1.0, 2.0)),
                RadarSeries(name = "alpha", color = Color.RED, values = listOf(70.0, 80.0, 90.0, 60.0), payload = "alpha-payload"),
            ),
        )

        layoutAndDraw(view, width = 460, height = 420)

        val pointsBySeries = view.readPrivate<List<List<*>>>("cachedPointsBySeries")
        val firstSeriesPoints = pointsBySeries.firstOrNull() ?: error("Expected rendered series points")
        val firstPoint = firstSeriesPoints.firstOrNull() ?: error("Expected at least one point")
        val pointX = firstPoint.readPrivate<Float>("x")
        val pointY = firstPoint.readPrivate<Float>("y")

        touchUp(view, pointX, pointY)

        assertEquals(0, callbackSeriesIndex)
        assertEquals(0, callbackAxisIndex)
        assertEquals("alpha-payload", callbackPayload)
        assertTrue(view.performClick())
    }

    @Test
    fun radarChart_handlesInvalidValueMax_andEmptyGeometry() {
        val context = RuntimeEnvironment.getApplication()
        val view = RadarChartView(context)

        view.setValueMax(-10.0)
        view.setPresentationOptions(RadarChartPresentationOptions(animateOnDataChange = true))
        view.playEnterAnimation()
        view.setAxes(listOf(RadarAxis("Only"), RadarAxis("Two")))
        view.setSeries(listOf(RadarSeries(name = "short", color = Color.BLUE, values = listOf(1.0, Double.NaN))))

        layoutAndDraw(view, width = 360, height = 280)

        assertFalse(view.readPrivate<List<*>>("cachedPointsBySeries").isNotEmpty())
    }
}
