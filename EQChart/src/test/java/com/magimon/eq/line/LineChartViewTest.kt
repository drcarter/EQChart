package com.magimon.eq.line

import android.graphics.Color
import android.graphics.RectF
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LineChartViewTest {

    @Test
    fun lineChart_sanitizesAndSortsPoints_thenDispatchesSeriesPayloadFallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = LineChartView(context)
        var callbackSeriesIndex: Int? = null
        var callbackPointIndex: Int? = null
        var callbackPoint: LineDatum? = null
        var callbackPayload: Any? = null

        view.setStyleOptions(LineChartStyleOptions(backgroundColor = Color.WHITE))
        view.setPresentationOptions(
            LineChartPresentationOptions(
                animateOnDataChange = false,
                showLegend = true,
                showGrid = true,
                showAxes = true,
                showPoints = true,
            ),
        )
        view.setOnPointClickListener { seriesIndex, pointIndex, point, payload ->
            callbackSeriesIndex = seriesIndex
            callbackPointIndex = pointIndex
            callbackPoint = point
            callbackPayload = payload
        }
        view.setSeries(
            listOf(
                LineSeries(
                    name = "alpha",
                    color = Color.RED,
                    payload = "series-alpha",
                    points = listOf(
                        LineDatum(x = 2.0, y = 20.0),
                        LineDatum(x = Double.NaN, y = 5.0),
                        LineDatum(x = 0.0, y = 10.0),
                        LineDatum(x = 1.0, y = 15.0, payload = "point-1"),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 460, height = 320)

        val cachedPoints = view.readPrivate<List<List<Any>>>("cachedPoints")
        assertEquals(1, cachedPoints.size)
        assertEquals(3, cachedPoints.first().size)

        val firstPoint = cachedPoints.first().first()
        val secondPoint = cachedPoints.first()[1]
        assertTrue(firstPoint.readPrivate<Float>("x") < secondPoint.readPrivate<Float>("x"))

        val thirdPoint = cachedPoints.first()[2]
        touchUp(view, thirdPoint.readPrivate("x"), thirdPoint.readPrivate("y"))

        assertEquals(0, callbackSeriesIndex)
        assertEquals(2, callbackPointIndex)
        assertEquals(LineDatum(x = 2.0, y = 20.0), callbackPoint)
        assertEquals("series-alpha", callbackPayload)
        assertTrue(view.performClick())
    }

    @Test
    fun lineChart_clearsSelectionOnMiss_andHandlesOnlyInvalidSeries() {
        val context = RuntimeEnvironment.getApplication()
        val view = LineChartView(context)

        view.setPresentationOptions(
            LineChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setSeries(
            listOf(
                LineSeries(
                    name = "invalid",
                    color = Color.GRAY,
                    points = listOf(
                        LineDatum(x = Double.NaN, y = 1.0),
                        LineDatum(x = 2.0, y = Double.NaN),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 6f, 6f)

        assertTrue(view.readPrivate<List<*>>("seriesList").isEmpty())
        assertTrue(view.readPrivate<List<*>>("cachedPoints").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedPoint"))
    }

    @Test
    fun areaChart_enablesAreaFill_andBuildsChartGeometry() {
        val context = RuntimeEnvironment.getApplication()
        val view = AreaChartView(context)

        view.setPresentationOptions(
            LineChartPresentationOptions(
                animateOnDataChange = false,
                showLegend = true,
                showAreaFill = true,
                showPoints = true,
            ),
        )
        view.setSeries(
            listOf(
                LineSeries(
                    name = "throughput",
                    color = Color.BLUE,
                    areaFillColor = Color.CYAN,
                    points = listOf(
                        LineDatum(x = -1.0, y = -2.0),
                        LineDatum(x = 0.0, y = 3.0),
                        LineDatum(x = 2.0, y = 1.5),
                    ),
                ),
            ),
        )

        layoutAndDraw(view, width = 480, height = 340)

        val chartArea = view.readPrivate<RectF>("chartArea")
        val cachedPoints = view.readPrivate<List<List<*>>>("cachedPoints")
        val presentation = view.readPrivate<LineChartPresentationOptions>("presentationOptions")

        assertTrue(chartArea.width() > 0f)
        assertTrue(chartArea.height() > 0f)
        assertFalse(cachedPoints.first().isEmpty())
        assertTrue(presentation.showAreaFill)
    }
}
