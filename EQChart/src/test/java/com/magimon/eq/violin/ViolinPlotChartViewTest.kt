package com.magimon.eq.violin

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
class ViolinPlotChartViewTest {

    @Test
    fun violinPlotChart_drawsSeriesAndDispatchesCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = ViolinPlotChartView(context)
        var callbackIndex: Int? = null
        var callbackSeries: ViolinPlotSeries? = null

        view.setStyleOptions(
            ViolinPlotChartStyleOptions(
                backgroundColor = Color.WHITE,
                defaultViolinColor = Color.parseColor("#2B80FF"),
            ),
        )
        view.setPresentationOptions(
            ViolinPlotChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showValueLabels = true,
            ),
        )
        view.setOnSeriesClickListener { index, series ->
            callbackIndex = index
            callbackSeries = series
        }
        view.setSeries(
            listOf(
                ViolinPlotSeries("API", listOf(10.0, 12.0, 16.0, 22.0)),
                ViolinPlotSeries("Worker", listOf(8.0, 9.0, 11.0, 14.0)),
            ),
        )

        layoutAndDraw(view, width = 520, height = 320)

        val entries = view.readPrivate<List<Any>>("renderEntries")
        assertEquals(2, entries.size)

        val touchRect = entries[1].readPrivate<RectF>("touchRect")
        touchUp(view, touchRect.centerX(), touchRect.centerY())

        assertEquals(1, callbackIndex)
        assertEquals("Worker", callbackSeries?.label)
        assertTrue(view.performClick())
    }

    @Test
    fun violinPlotChart_filtersInvalidSeriesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = ViolinPlotChartView(context)

        view.setPresentationOptions(
            ViolinPlotChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setSeries(
            listOf(
                ViolinPlotSeries("", listOf(1.0, 2.0)),
                ViolinPlotSeries("Bad", listOf(Double.NaN)),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("renderEntries").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedEntry"))
    }
}
