package com.magimon.eq.funnel

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
class FunnelChartViewTest {

    @Test
    fun funnelChart_drawsStagesAndDispatchesClickCallback() {
        val context = RuntimeEnvironment.getApplication()
        val view = FunnelChartView(context)
        var clickedLabel: String? = null
        var clickedValue: Double? = null

        view.setStyleOptions(FunnelChartStyleOptions(backgroundColor = Color.WHITE))
        view.setPresentationOptions(FunnelChartPresentationOptions(animateOnDataChange = false))
        view.setOnStageClickListener { _, stage, value ->
            clickedLabel = stage.label
            clickedValue = value
        }
        view.setStages(
            listOf(
                FunnelStage("Visit", 120.0, payload = "visit"),
                FunnelStage("Qualified", 80.0, payload = "qualified"),
                FunnelStage("Won", 32.0, payload = "won"),
            ),
        )

        layoutAndDraw(view, width = 480, height = 360)

        val stages = view.readPrivate<List<Any>>("renderStages")
        assertEquals(3, stages.size)
        val first = stages.first()
        val tapX = first.readPrivate<Float>("centerX")
        val tapY = first.readPrivate<Float>("centerY")
        touchUp(view, tapX, tapY)

        assertEquals("Visit", clickedLabel)
        assertEquals(120.0, clickedValue ?: Double.NaN, 0.0)
        assertTrue(view.performClick())
    }

    @Test
    fun funnelChart_filtersInvalidStagesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = FunnelChartView(context)

        view.setPresentationOptions(
            FunnelChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setStages(
            listOf(
                FunnelStage("", 10.0),
                FunnelStage("Bad", Double.NaN),
                FunnelStage("Zero", 0.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 240)
        touchUp(view, 8f, 8f)

        assertTrue(view.readPrivate<List<*>>("renderStages").isEmpty())
        assertNull(view.readPrivate<Any?>("selectedStage"))
    }
}
