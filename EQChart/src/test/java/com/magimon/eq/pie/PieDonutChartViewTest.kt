package com.magimon.eq.pie

import android.graphics.Color
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
class PieDonutChartViewTest {

    @Test
    fun pieChart_drawsSingleSlice_andDispatchesClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = PieChartView(context)
        var clickedIndex: Int? = null
        var clickedPayload: Any? = null

        view.setStyleOptions(PieDonutStyleOptions(backgroundColor = Color.LTGRAY))
        view.setPresentationOptions(
            PieDonutPresentationOptions(
                showLegend = true,
                showLabels = true,
                animateOnDataChange = false,
            ),
        )
        view.setOnSliceClickListener { index, _, payload ->
            clickedIndex = index
            clickedPayload = payload
        }
        view.setData(listOf(PieSlice(label = "Revenue", value = 42.0, color = Color.RED, payload = "slice-0")))

        layoutAndDraw(view, width = 420, height = 320)

        val centerX = view.readPrivate<Float>("centerX")
        val centerY = view.readPrivate<Float>("centerY")
        val outerRadius = view.readPrivate<Float>("outerRadius")

        touchUp(view, centerX + (outerRadius * 0.5f), centerY)

        assertEquals(0, clickedIndex)
        assertEquals("slice-0", clickedPayload)
        assertTrue(view.performClick())
    }

    @Test
    fun donutChart_clampsRatio_andClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = DonutChartView(context)

        view.setDonutInnerRadiusRatio(5f)
        assertEquals(0.92f, view.getDonutInnerRadiusRatio(), 0f)
        view.setDonutInnerRadiusRatio(-1f)
        assertEquals(0f, view.getDonutInnerRadiusRatio(), 0f)

        view.setPresentationOptions(
            PieDonutPresentationOptions(
                enableSelectionExpand = true,
                animateOnDataChange = false,
                centerText = "Total",
                centerSubText = "2026",
            ),
        )
        view.setData(
            listOf(
                PieSlice(label = "A", value = 60.0, color = Color.BLUE),
                PieSlice(label = "B", value = 40.0, color = Color.GREEN),
            ),
        )

        layoutAndDraw(view, width = 420, height = 320)
        touchUp(view, 2f, 2f)

        assertNull(view.readPrivate<Int?>("selectedSliceIndex"))
    }

    @Test
    fun pieChart_drawsEmptyState_whenAllSlicesAreInvalid() {
        val context = RuntimeEnvironment.getApplication()
        val view = PieChartView(context)

        view.setPresentationOptions(PieDonutPresentationOptions(animateOnDataChange = false, emptyText = "Empty"))
        view.setData(
            listOf(
                PieSlice(label = "bad-1", value = 0.0, color = Color.RED),
                PieSlice(label = "bad-2", value = Double.NaN, color = Color.BLUE),
            ),
        )

        layoutAndDraw(view, width = 360, height = 260)

        val segments = view.readPrivate<List<*>>("segments")
        assertTrue(segments.isEmpty())
    }
}
