package com.magimon.eq.compose.stepflow

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartStyleOptions
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StepFlowChartLogicTest {

    private val hubContent = StepFlowHubContent(
        eyebrow = "INFOGRAPHIC",
        title = "STEPS",
        description = "Five-step process",
        payload = "hub",
    )

    private val steps = listOf(
        StepFlowStep("one", "STEP 01", "Discover", "Collect inputs", 0xFF8C4BFF.toInt(), "!"),
        StepFlowStep("two", "STEP 02", "Design", "Shape the plan", 0xFF7C58FF.toInt(), "#"),
        StepFlowStep("three", "STEP 03", "Build", "Implement work", 0xFF5AA8FF.toInt(), "*"),
        StepFlowStep("four", "STEP 04", "Launch", "Ship release", 0xFFF2B323.toInt(), "$"),
        StepFlowStep("five", "STEP 05", "Measure", "Track outcomes", 0xFF9ACA3C.toInt(), "+"),
    )

    @Test
    fun computeStepFlowLayout_buildsRenderableLayout_withDpConfig() {
        val layout = computeStepFlowLayout(
            hubContent = hubContent,
            steps = steps,
            widthPx = 900f,
            heightPx = 520f,
            density = Density(1f, 1f),
            styleOptions = StepFlowChartStyleOptions(),
            presentationOptions = StepFlowChartPresentationOptions(),
        )

        assertTrue(layout.isRenderable)
        assertEquals(5, layout.stepLayouts.size)
        assertEquals(5, layout.hubRingSegments.size)
        assertEquals(2, layout.tailDots.size)
        assertTrue(layout.stepLayouts.first().cardLeft > layout.hubLayout!!.right)
    }

    @Test
    fun computeStepFlowLayout_scalesDownToStayRenderable_onCompactWidth() {
        val layout = computeStepFlowLayout(
            hubContent = hubContent,
            steps = steps,
            widthPx = 328f,
            heightPx = 320f,
            density = Density(1f, 1f),
            styleOptions = StepFlowChartStyleOptions(),
            presentationOptions = StepFlowChartPresentationOptions(),
        )

        assertTrue(layout.isRenderable)
        assertEquals(5, layout.stepLayouts.size)
        assertTrue(layout.stepLayouts.first().cardLeft > layout.hubLayout!!.right)
    }

    @Test
    fun resolveStepFlowTapSelection_prefersHub_thenStep_thenEmpty() {
        val layout = computeStepFlowLayout(
            hubContent = hubContent,
            steps = steps,
            widthPx = 900f,
            heightPx = 520f,
            density = Density(1f, 1f),
            styleOptions = StepFlowChartStyleOptions(),
            presentationOptions = StepFlowChartPresentationOptions(),
        )
        val second = layout.stepLayouts[1]

        val hubSelection = resolveStepFlowTapSelection(
            layout = layout,
            tap = Offset(layout.hubLayout!!.centerX, layout.hubLayout!!.centerY),
            stepHitTolerancePx = 8f,
        )
        val stepSelection = resolveStepFlowTapSelection(
            layout = layout,
            tap = Offset(second.cardLeft + 4f, second.cardTop + 4f),
            stepHitTolerancePx = 8f,
        )
        val emptySelection = resolveStepFlowTapSelection(
            layout = layout,
            tap = Offset(4f, 4f),
            stepHitTolerancePx = 8f,
        )

        assertTrue(hubSelection.hubSelected)
        assertNull(hubSelection.selectedStepIndex)
        assertEquals(1, stepSelection.selectedStepIndex)
        assertFalse(stepSelection.hubSelected)
        assertNull(emptySelection.selectedStepIndex)
        assertFalse(emptySelection.hubSelected)
    }

    @Test
    fun alpha_and_selection_helpers_follow_selection_state() {
        assertTrue(stepFlowStepHighlighted(2, selectedStepIndex = 2, hubSelected = false))
        assertFalse(stepFlowStepHighlighted(1, selectedStepIndex = 2, hubSelected = false))
        assertEquals(1f, stepFlowStepAlpha(2, selectedStepIndex = 2, hubSelected = false, renderProgress = 1f), 0f)
        assertEquals(0.28f, stepFlowStepAlpha(1, selectedStepIndex = 2, hubSelected = false, renderProgress = 1f), 0.001f)
        assertEquals(0.42f, stepFlowStepAlpha(1, selectedStepIndex = null, hubSelected = true, renderProgress = 1f), 0.001f)
        assertEquals(0.35f, stepFlowHubAlpha(selectedStepIndex = 1, hubSelected = false, renderProgress = 1f), 0.001f)
        assertEquals(0.3f, stepFlowSpineAlpha(selectedStepIndex = 1, hubSelected = false, renderProgress = 1f), 0.001f)
    }

    @Test
    fun formatting_and_alpha_helpers_work() {
        val alphaApplied = stepFlowApplyAlpha(0x80FF0000.toInt(), 2f)
        val transparent = stepFlowApplyAlpha(0x80FF0000.toInt(), -1f)
        val step = steps.first()

        assertEquals(0x80, Color.alpha(alphaApplied))
        assertEquals(0, Color.alpha(transparent))
        assertEquals(step.iconText, stepFlowIconText(step))
        assertEquals("", stepFlowDisplayBody(step.description, showBody = false))
        assertEquals("•", stepFlowIconText(step.copy(iconText = " ")))
    }
}
