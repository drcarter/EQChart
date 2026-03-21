package com.magimon.eq.histogram

import android.graphics.Color
import android.graphics.RectF
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HistogramChartViewTest {

    @Test
    fun histogramChart_drawsBarsAndDispatchesBinCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = HistogramChartView(context)
        var callbackIndex: Int? = null
        var callbackBin: HistogramBin? = null
        var callbackValue: Double? = null

        view.setStyleOptions(
            HistogramChartStyleOptions(
                backgroundColor = Color.WHITE,
                barColor = Color.parseColor("#2B80FF"),
            ),
        )
        view.setPresentationOptions(
            HistogramChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
            ),
        )
        view.setOnBinClickListener { index, bin, value ->
            callbackIndex = index
            callbackBin = bin
            callbackValue = value
        }
        view.setBins(
            listOf(
                HistogramBin(0.0, 10.0, 4.0, payload = "a"),
                HistogramBin(10.0, 20.0, 9.0, payload = "b"),
                HistogramBin(20.0, 30.0, 3.0, payload = "c"),
            ),
        )

        layoutAndDraw(view, width = 480, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        assertEquals(3, bars.size)

        val secondRect = bars[1].readPrivate<RectF>("rect")
        touchUp(view, secondRect.centerX(), secondRect.centerY())

        assertEquals(1, callbackIndex)
        assertEquals(10.0, callbackBin?.start ?: Double.NaN, 0.0)
        assertEquals(9.0, callbackValue ?: Double.NaN, 0.0)
        assertTrue(view.performClick())
    }

    @Test
    fun histogramChart_filtersInvalidBinsAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = HistogramChartView(context)

        view.setPresentationOptions(
            HistogramChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setBins(
            listOf(
                HistogramBin(10.0, 10.0, 4.0),
                HistogramBin(20.0, 30.0, Double.NaN),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("bars").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedBar"))
    }

    @Test
    fun histogramChart_rendersNegativeBarsWhenProvided() {
        val context = RuntimeEnvironment.getApplication()
        val view = HistogramChartView(context)

        view.setPresentationOptions(
            HistogramChartPresentationOptions(
                animateOnDataChange = false,
                showBarLabels = true,
            ),
        )
        view.setBins(
            listOf(
                HistogramBin(0.0, 10.0, 8.0),
                HistogramBin(10.0, 20.0, -3.0),
                HistogramBin(20.0, 30.0, 6.0),
            ),
        )

        layoutAndDraw(view, width = 500, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        val negativeRect = bars[1].readPrivate<RectF>("rect")

        assertEquals(3, bars.size)
        assertTrue(negativeRect.height() > 0f)
        assertTrue(negativeRect.bottom > negativeRect.top)
    }
}
