package com.magimon.eq.rangebar

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
class RangeBarChartViewTest {

    @Test
    fun rangeBarChart_drawsBarsAndDispatchesEntryCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = RangeBarChartView(context)
        var callbackIndex: Int? = null
        var callbackEntry: RangeBarEntry? = null

        view.setStyleOptions(
            RangeBarChartStyleOptions(
                backgroundColor = Color.WHITE,
                defaultBarColor = Color.parseColor("#2563EB"),
                selectedBarStrokeColor = Color.BLACK,
            ),
        )
        view.setPresentationOptions(
            RangeBarChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
            ),
        )
        view.setOnEntryClickListener { index, entry ->
            callbackIndex = index
            callbackEntry = entry
        }
        view.setEntries(
            listOf(
                RangeBarEntry("Discovery", 0.0, 2.0, payload = "discovery"),
                RangeBarEntry("Build", 2.0, 6.0, payload = "build"),
                RangeBarEntry("Launch", 6.0, 8.0, color = 0xFFEF4444.toInt(), payload = "launch"),
            ),
        )

        layoutAndDraw(view, width = 520, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        assertEquals(3, bars.size)

        val secondRect = bars[1].readPrivate<RectF>("rect")
        touchUp(view, secondRect.centerX(), secondRect.centerY())

        assertEquals(1, callbackIndex)
        assertEquals("Build", callbackEntry?.label)
        assertEquals(2.0, callbackEntry?.start ?: Double.NaN, 0.0)
        assertEquals(6.0, callbackEntry?.end ?: Double.NaN, 0.0)
        assertEquals("build", callbackEntry?.payload)
        assertTrue(view.performClick())
    }

    @Test
    fun rangeBarChart_reflectsNormalizedWidthsForReversedIntervals() {
        val context = RuntimeEnvironment.getApplication()
        val view = RangeBarChartView(context)

        view.setPresentationOptions(
            RangeBarChartPresentationOptions(
                animateOnDataChange = false,
                showBarLabels = false,
            ),
        )
        view.setEntries(
            listOf(
                RangeBarEntry("Short", 1.0, 2.0),
                RangeBarEntry("Long", 7.0, 3.0),
            ),
        )

        layoutAndDraw(view, width = 520, height = 300)

        val bars = view.readPrivate<List<Any>>("bars")
        val shortRect = bars[0].readPrivate<RectF>("rect")
        val longRect = bars[1].readPrivate<RectF>("rect")

        assertTrue(longRect.width() > shortRect.width())
        assertTrue(shortRect.height() > 0f)
        assertTrue(longRect.height() > 0f)
    }

    @Test
    fun rangeBarChart_filtersInvalidEntriesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = RangeBarChartView(context)

        view.setPresentationOptions(
            RangeBarChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setEntries(
            listOf(
                RangeBarEntry("", 0.0, 1.0),
                RangeBarEntry("Bad", Double.NaN, 2.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("bars").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedBar"))
    }
}
