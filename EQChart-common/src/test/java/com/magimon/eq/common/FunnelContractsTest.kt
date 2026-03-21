package com.magimon.eq.common

import com.magimon.eq.funnel.FunnelChartPresentationOptions
import com.magimon.eq.funnel.FunnelChartStyleOptions
import com.magimon.eq.funnel.FunnelStage
import com.magimon.eq.funnel.resolveFunnelChartLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FunnelContractsTest {

    @Test
    fun funnelStage_preservesValuesAndDefaults() {
        val payload = linkedMapOf("stage" to 1)
        val defaultStage = FunnelStage(label = "Visit", value = 120.0)
        val custom = FunnelStage(
            label = "Signup",
            value = 80.0,
            color = 0xFF224466.toInt(),
            payload = payload,
        )
        val copied = custom.copy(value = 60.0)

        assertEquals("Visit", defaultStage.label)
        assertEquals(120.0, defaultStage.value, 0.0)
        assertNull(defaultStage.color)
        assertNull(defaultStage.payload)

        assertEquals("Signup", custom.label)
        assertEquals(0xFF224466.toInt(), custom.color)
        assertSame(payload, custom.payload)
        assertEquals(60.0, copied.value, 0.0)
    }

    @Test
    fun funnelPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = FunnelChartPresentationOptions()
        val custom = FunnelChartPresentationOptions(
            showLabels = false,
            showValues = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            emptyText = "EMPTY",
            labelTextSizeSp = 13f,
            valueTextSizeSp = 14f,
            stageLabelFormatter = { stage -> "stage:${stage.label}" },
            valueLabelFormatter = { value -> "v:${value.toInt()}" },
        )

        assertTrue(defaults.showLabels)
        assertTrue(defaults.showValues)
        assertTrue(defaults.animateOnDataChange)
        assertTrue(defaults.animationDirection)
        assertEquals("No data", defaults.emptyText)
        assertEquals("Visit", defaults.stageLabelFormatter(FunnelStage("Visit", 1.0)))
        assertEquals("12", defaults.valueLabelFormatter(12.9))

        assertFalse(custom.showLabels)
        assertFalse(custom.showValues)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals("stage:Lead", custom.stageLabelFormatter(FunnelStage("Lead", 1.0)))
        assertEquals("v:18", custom.valueLabelFormatter(18.2))
    }

    @Test
    fun funnelStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = FunnelChartStyleOptions()
        val custom = FunnelChartStyleOptions(
            backgroundColor = 1,
            stageBorderColor = 2,
            selectedStageBorderColor = 3,
            labelTextColor = 4,
            valueTextColor = 5,
            stageColors = listOf(6, 7),
            contentPaddingDp = 8f,
            stageGapDp = 9f,
            minStageWidthRatio = 0.3f,
            tipWidthRatio = 0.2f,
        )

        assertEquals(16f, defaults.contentPaddingDp, 0.0f)
        assertEquals(6f, defaults.stageGapDp, 0.0f)
        assertEquals(0.28f, defaults.minStageWidthRatio, 0.0f)
        assertEquals(0.16f, defaults.tipWidthRatio, 0.0f)
        assertTrue(defaults.stageColors.isNotEmpty())

        assertEquals(1, custom.backgroundColor)
        assertEquals(3, custom.selectedStageBorderColor)
        assertEquals(2, custom.stageColors.size)
        assertEquals(0.3f, custom.minStageWidthRatio, 0.0f)
        assertEquals(0.2f, custom.tipWidthRatio, 0.0f)
    }

    @Test
    fun funnelLayoutEngine_buildsNormalizedWidthsAndFiltersInvalidStages() {
        val layout = resolveFunnelChartLayout(
            stages = listOf(
                FunnelStage("Visit", 120.0, payload = "visit"),
                FunnelStage("Qualified", 80.0),
                FunnelStage("Won", 40.0),
                FunnelStage("", 12.0),
                FunnelStage("Bad", Double.NaN),
            ),
        )

        assertEquals(3, layout.stages.size)
        assertEquals(120.0, layout.maxValue, 0.0)
        assertEquals(1.0f, layout.stages[0].topWidthRatio, 0.0f)
        assertTrue(layout.stages[1].topWidthRatio < layout.stages[0].topWidthRatio)
        assertTrue(layout.stages[2].bottomWidthRatio <= layout.stages[2].topWidthRatio)
        assertEquals("visit", layout.stages[0].payload)
    }
}
