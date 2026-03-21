package com.magimon.eq.compose.funnel

import com.magimon.eq.funnel.FunnelChartPresentationOptions
import com.magimon.eq.funnel.FunnelChartStyleOptions
import com.magimon.eq.funnel.FunnelStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunnelChartTest {

    @Test
    fun computeFunnelLayout_buildsStagesForValidInput() {
        val computed = computeFunnelLayout(
            widthPx = 320f,
            heightPx = 280f,
            stages = listOf(
                FunnelStage("Visit", 120.0, payload = "visit"),
                FunnelStage("Qualified", 80.0, payload = "qualified"),
                FunnelStage("Won", 40.0, payload = "won"),
            ),
            style = FunnelChartStyleOptions(),
            presentation = FunnelChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
        )

        assertEquals(3, computed.stages.size)
        assertTrue(computed.stages[1].topLeftX > computed.stages[0].topLeftX)
        assertTrue(computed.stages[2].bottomRightX > computed.stages[2].bottomLeftX)
    }

    @Test
    fun computeFunnelLayout_filtersInvalidStagesAndKeepsEmptyState() {
        val computed = computeFunnelLayout(
            widthPx = 320f,
            heightPx = 280f,
            stages = listOf(
                FunnelStage("", 12.0),
                FunnelStage("Bad", Double.NaN),
                FunnelStage("Zero", 0.0),
            ),
            style = FunnelChartStyleOptions(),
            presentation = FunnelChartPresentationOptions(animateOnDataChange = false),
            progress = 1f,
            density = 1f,
        )

        assertTrue(computed.stages.isEmpty())
    }
}
