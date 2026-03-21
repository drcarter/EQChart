package com.magimon.eq.common

import android.graphics.Color
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
        assertEquals(680L, defaults.enterAnimationDurationMs)
        assertEquals(30L, defaults.enterAnimationDelayMs)
        assertEquals("No data", defaults.emptyText)
        assertEquals(12f, defaults.labelTextSizeSp, 0.0f)
        assertEquals(11.5f, defaults.valueTextSizeSp, 0.0f)
        assertEquals("Visit", defaults.stageLabelFormatter(FunnelStage("Visit", 1.0)))
        assertEquals("12", defaults.valueLabelFormatter(12.9))

        assertFalse(custom.showLabels)
        assertFalse(custom.showValues)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(15L, custom.enterAnimationDelayMs)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals(13f, custom.labelTextSizeSp, 0.0f)
        assertEquals(14f, custom.valueTextSizeSp, 0.0f)
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
        assertEquals(Color.WHITE, defaults.backgroundColor)
        assertEquals(Color.parseColor("#FFFFFF"), defaults.stageBorderColor)
        assertEquals(Color.parseColor("#1F2937"), defaults.selectedStageBorderColor)
        assertEquals(Color.WHITE, defaults.labelTextColor)
        assertEquals(Color.parseColor("#E5E7EB"), defaults.valueTextColor)
        assertEquals(Color.parseColor("#2563EB"), defaults.stageColors[0])
        assertEquals(Color.parseColor("#EF4444"), defaults.stageColors[4])

        assertEquals(1, custom.backgroundColor)
        assertEquals(2, custom.stageBorderColor)
        assertEquals(3, custom.selectedStageBorderColor)
        assertEquals(4, custom.labelTextColor)
        assertEquals(5, custom.valueTextColor)
        assertEquals(2, custom.stageColors.size)
        assertEquals(8f, custom.contentPaddingDp, 0.0f)
        assertEquals(9f, custom.stageGapDp, 0.0f)
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
        assertEquals(0, layout.stages[0].index)
        assertEquals("Visit", layout.stages[0].label)
        assertEquals(120.0, layout.stages[0].value, 0.0)
        assertEquals(FunnelChartStyleOptions().stageColors[0], layout.stages[0].color)
        assertEquals(layout.stages[1].topWidthRatio, layout.stages[0].bottomWidthRatio, 0.0f)
        assertTrue(layout.stages[1].topWidthRatio < layout.stages[0].topWidthRatio)
        assertEquals(FunnelChartStyleOptions().stageColors[1], layout.stages[1].color)
        assertTrue(layout.stages[2].bottomWidthRatio <= layout.stages[2].topWidthRatio)
        assertEquals(maxOf(0.16f, 0.28f * 0.7f), layout.stages[2].bottomWidthRatio, 0.0f)
        assertEquals("visit", layout.stages[0].payload)
    }

    @Test
    fun funnelLayoutEngine_handlesEmptyAndSingleStageLayouts() {
        val emptyLayout = resolveFunnelChartLayout(
            stages = listOf(
                FunnelStage("", 12.0),
                FunnelStage("Bad", Double.NaN),
                FunnelStage("Zero", 0.0),
            ),
        )
        val customStyle = FunnelChartStyleOptions(
            stageColors = listOf(11, 12),
            minStageWidthRatio = 0.3f,
            tipWidthRatio = 0.2f,
        )
        val singleStage = FunnelStage(
            label = "Only",
            value = 50.0,
            color = 99,
            payload = "only",
        )
        val singleLayout = resolveFunnelChartLayout(
            stages = listOf(singleStage),
            style = customStyle,
        )

        assertTrue(emptyLayout.stages.isEmpty())
        assertEquals(1.0, emptyLayout.maxValue, 0.0)

        assertEquals(1, singleLayout.stages.size)
        assertEquals(50.0, singleLayout.maxValue, 0.0)
        assertEquals(0, singleLayout.stages[0].index)
        assertEquals("Only", singleLayout.stages[0].label)
        assertEquals(50.0, singleLayout.stages[0].value, 0.0)
        assertEquals(1.0f, singleLayout.stages[0].topWidthRatio, 0.0f)
        assertEquals(1.0f, singleLayout.stages[0].bottomWidthRatio, 0.0f)
        assertEquals(99, singleLayout.stages[0].color)
        assertEquals("only", singleLayout.stages[0].payload)
    }

    @Test
    fun funnelLayoutEngine_appliesLastStageFallbackAndDefaultColors() {
        val tipDominantStyle = FunnelChartStyleOptions(
            stageColors = listOf(11),
            minStageWidthRatio = 0.2f,
            tipWidthRatio = 0.5f,
        )
        val tipDominantLayout = resolveFunnelChartLayout(
            stages = listOf(
                FunnelStage("Visit", 100.0),
                FunnelStage("Won", 40.0),
            ),
            style = tipDominantStyle,
        )

        assertEquals(2, tipDominantLayout.stages.size)
        assertEquals(11, tipDominantLayout.stages[1].color)
        assertEquals(0.5f, tipDominantLayout.stages[1].bottomWidthRatio, 0.0f)

        val minWidthDominantStyle = FunnelChartStyleOptions(
            stageColors = listOf(21, 22),
            minStageWidthRatio = 0.4f,
            tipWidthRatio = 0.1f,
        )
        val minWidthDominantLayout = resolveFunnelChartLayout(
            stages = listOf(
                FunnelStage("Visit", 100.0),
                FunnelStage("Won", 40.0),
            ),
            style = minWidthDominantStyle,
        )

        assertEquals(22, minWidthDominantLayout.stages[1].color)
        assertEquals(0.28f, minWidthDominantLayout.stages[1].bottomWidthRatio, 0.0f)
    }

    @Test
    fun funnelLayoutEngine_updatesMaxValueWhenLaterStageIsLarger() {
        val layout = resolveFunnelChartLayout(
            stages = listOf(
                FunnelStage("Lead", 20.0),
                FunnelStage("Visit", 50.0),
            ),
        )

        assertEquals(50.0, layout.maxValue, 0.0)
        assertTrue(layout.stages[0].topWidthRatio < layout.stages[1].topWidthRatio)
        assertEquals(1.0f, layout.stages[1].topWidthRatio, 0.0f)
    }
}
