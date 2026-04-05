package com.magimon.eq.boxplot

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
class BoxPlotChartViewTest {

    @Test
    fun boxPlotChart_drawsEntriesAndDispatchesCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoxPlotChartView(context)
        var callbackIndex: Int? = null
        var callbackEntry: BoxPlotEntry? = null

        view.setStyleOptions(
            BoxPlotChartStyleOptions(
                backgroundColor = Color.WHITE,
                defaultBoxColor = Color.parseColor("#2B80FF"),
            ),
        )
        view.setPresentationOptions(
            BoxPlotChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showValueLabels = true,
            ),
        )
        view.setOnEntryClickListener { index, entry ->
            callbackIndex = index
            callbackEntry = entry
        }
        view.setEntries(
            listOf(
                BoxPlotEntry("API", 120.0, 140.0, 160.0, 190.0, 240.0),
                BoxPlotEntry("Worker", 80.0, 110.0, 135.0, 170.0, 210.0, outliers = listOf(62.0, 248.0)),
            ),
        )

        layoutAndDraw(view, width = 520, height = 320)

        val entries = view.readPrivate<List<Any>>("renderEntries")
        assertEquals(2, entries.size)

        val touchRect = entries[1].readPrivate<RectF>("touchRect")
        touchUp(view, touchRect.centerX(), touchRect.centerY())

        assertEquals(1, callbackIndex)
        assertEquals("Worker", callbackEntry?.label)
        assertEquals(2, callbackEntry?.outliers?.size)
        assertTrue(view.performClick())
    }

    @Test
    fun boxPlotChart_filtersInvalidEntriesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoxPlotChartView(context)

        view.setPresentationOptions(
            BoxPlotChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setEntries(
            listOf(
                BoxPlotEntry("", 1.0, 2.0, 3.0, 4.0, 5.0),
                BoxPlotEntry("Bad", 1.0, Double.NaN, 3.0, 4.0, 5.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("renderEntries").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedEntry"))
    }

    @Test
    fun boxPlotChart_rendersSortedQuartilesEvenWhenInputIsUnordered() {
        val context = RuntimeEnvironment.getApplication()
        val view = BoxPlotChartView(context)

        view.setPresentationOptions(
            BoxPlotChartPresentationOptions(
                animateOnDataChange = false,
                showValueLabels = false,
            ),
        )
        view.setEntries(
            listOf(
                BoxPlotEntry("A", 20.0, 12.0, 16.0, 14.0, 24.0),
            ),
        )

        layoutAndDraw(view, width = 420, height = 280)

        val entries = view.readPrivate<List<Any>>("renderEntries")
        val boxRect = entries[0].readPrivate<RectF>("boxRect")
        val minY = entries[0].readPrivate<Float>("minY")
        val maxY = entries[0].readPrivate<Float>("maxY")

        assertTrue(boxRect.height() > 0f)
        assertTrue(maxY < boxRect.top)
        assertTrue(minY > boxRect.bottom)
    }
}
