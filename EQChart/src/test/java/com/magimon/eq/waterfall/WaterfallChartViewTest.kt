package com.magimon.eq.waterfall

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
class WaterfallChartViewTest {

    @Test
    fun waterfallChart_drawsBarsAndDispatchesEntryCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = WaterfallChartView(context)
        var callbackIndex: Int? = null
        var callbackEntry: WaterfallEntry? = null
        var callbackTotal: Double? = null

        view.setStyleOptions(
            WaterfallChartStyleOptions(
                backgroundColor = Color.WHITE,
                positiveBarColor = Color.parseColor("#13C3A3"),
                negativeBarColor = Color.parseColor("#EF476F"),
                totalBarColor = Color.parseColor("#2B80FF"),
            ),
        )
        view.setPresentationOptions(
            WaterfallChartPresentationOptions(
                animateOnDataChange = false,
                showGrid = true,
                showAxes = true,
                showBarLabels = true,
                showConnectorLines = true,
            ),
        )
        view.setOnEntryClickListener { index, entry, cumulativeTotal ->
            callbackIndex = index
            callbackEntry = entry
            callbackTotal = cumulativeTotal
        }
        view.setEntries(
            listOf(
                WaterfallEntry("Revenue", 120.0, payload = "revenue"),
                WaterfallEntry("Cost", -35.0, payload = "cost"),
                WaterfallEntry("Subtotal", Double.NaN, kind = WaterfallEntryKind.SUBTOTAL, payload = "subtotal"),
                WaterfallEntry("Tax", -15.0, payload = "tax"),
                WaterfallEntry("Total", 0.0, kind = WaterfallEntryKind.TOTAL, payload = "total"),
            ),
        )

        layoutAndDraw(view, width = 480, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        assertEquals(5, bars.size)

        val thirdRect = bars[2].readPrivate<RectF>("rect")
        touchUp(view, thirdRect.centerX(), thirdRect.centerY())

        assertEquals(2, callbackIndex)
        assertEquals("Subtotal", callbackEntry?.label)
        assertEquals(WaterfallEntryKind.SUBTOTAL, callbackEntry?.kind)
        assertEquals(85.0, callbackTotal ?: Double.NaN, 0.0)
        assertTrue(view.performClick())
    }

    @Test
    fun waterfallChart_filtersInvalidEntriesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = WaterfallChartView(context)

        view.setPresentationOptions(
            WaterfallChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setEntries(
            listOf(
                WaterfallEntry("", 10.0),
                WaterfallEntry("Bad", Double.NaN),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("bars").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedBar"))
    }

    @Test
    fun waterfallChart_rendersNegativeAndSummaryBars() {
        val context = RuntimeEnvironment.getApplication()
        val view = WaterfallChartView(context)

        view.setPresentationOptions(
            WaterfallChartPresentationOptions(
                animateOnDataChange = false,
                showBarLabels = true,
                showConnectorLines = true,
            ),
        )
        view.setEntries(
            listOf(
                WaterfallEntry("Start", 80.0),
                WaterfallEntry("Loss", -120.0),
                WaterfallEntry("Subtotal", 0.0, kind = WaterfallEntryKind.SUBTOTAL),
                WaterfallEntry("Recovery", 40.0),
            ),
        )

        layoutAndDraw(view, width = 500, height = 320)

        val bars = view.readPrivate<List<Any>>("bars")
        val connectors = view.readPrivate<List<Any>>("connectors")
        val negativeRect = bars[1].readPrivate<RectF>("rect")
        val subtotalRect = bars[2].readPrivate<RectF>("rect")

        assertEquals(4, bars.size)
        assertEquals(3, connectors.size)
        assertTrue(negativeRect.height() > 0f)
        assertTrue(subtotalRect.height() > 0f)
        assertTrue(subtotalRect.bottom > subtotalRect.top)
    }
}
