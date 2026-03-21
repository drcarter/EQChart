package com.magimon.eq.stepflow

import android.os.Looper
import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class StepFlowChartViewTest {

    private fun sampleSteps(): List<StepFlowStep> {
        return listOf(
            StepFlowStep("discover", "STEP 01", "Discover", "Collect inputs", 0xFF8A3FFC.toInt(), "!"),
            StepFlowStep("design", "STEP 02", "Design", "Shape the plan", 0xFF7C4DFF.toInt(), "#"),
            StepFlowStep("build", "STEP 03", "Build", "Implement the work", 0xFF58A8FF.toInt(), "*"),
            StepFlowStep("launch", "STEP 04", "Launch", "Release to users", 0xFFF0B429.toInt(), "$"),
            StepFlowStep("measure", "STEP 05", "Measure", "Track outcomes", 0xFFA7D129.toInt(), "+"),
        )
    }

    @Test
    fun stepFlowChart_dispatchesStepAndHubClicks() {
        val context = RuntimeEnvironment.getApplication()
        val view = StepFlowChartView(context)
        var clickedStepIndex: Int? = null
        var clickedHubTitle: String? = null

        view.setPresentationOptions(
            StepFlowChartPresentationOptions(
                animateOnDataChange = false,
                showStepDescriptions = true,
                showHubDescription = true,
            ),
        )
        view.setHubContent(
            StepFlowHubContent(
                eyebrow = "INFOGRAPHIC",
                title = "STEPS",
                description = "Shared hub",
                payload = "hub",
            ),
        )
        view.setSteps(sampleSteps())
        view.setOnStepClickListener { index, _, _ -> clickedStepIndex = index }
        view.setOnHubClickListener { content, _ -> clickedHubTitle = content.title }

        layoutAndDraw(view, width = 900, height = 520)

        val layout = view.readPrivate<StepFlowChartLayoutResult>("layoutResult")
        val thirdStep = layout.stepLayouts[2]
        touchUp(view, thirdStep.cardLeft + 12f, thirdStep.cardTop + 12f)
        assertEquals(2, clickedStepIndex)
        assertEquals(2, view.readPrivate<Int?>("selectedStepIndex"))

        val hub = layout.hubLayout!!
        touchUp(view, hub.centerX, hub.centerY)
        assertEquals("STEPS", clickedHubTitle)
        assertTrue(view.readPrivate<Boolean>("hubSelected"))
        assertNull(view.readPrivate<Int?>("selectedStepIndex"))
    }

    @Test
    fun stepFlowChart_marksInvalidInputAsEmpty() {
        val context = RuntimeEnvironment.getApplication()
        val view = StepFlowChartView(context)

        view.setPresentationOptions(
            StepFlowChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setHubContent(StepFlowHubContent(title = "STEPS"))
        view.setSteps(emptyList())

        layoutAndDraw(view, width = 360, height = 280)

        val layout = view.readPrivate<StepFlowChartLayoutResult>("layoutResult")
        assertFalse(layout.isRenderable)
        assertTrue(layout.stepLayouts.isEmpty())
        assertTrue(layout.spineSegments.isEmpty())
    }

    @Test
    fun stepFlowChart_scalesDownAndRenders_onCompactWidth() {
        val context = RuntimeEnvironment.getApplication()
        val view = StepFlowChartView(context)

        view.setPresentationOptions(
            StepFlowChartPresentationOptions(
                animateOnDataChange = false,
                showStepDescriptions = true,
                showHubDescription = true,
            ),
        )
        view.setHubContent(
            StepFlowHubContent(
                eyebrow = "INFOGRAPHIC",
                title = "STEPS",
                description = "Shared hub",
            ),
        )
        view.setSteps(sampleSteps())

        layoutAndDraw(view, width = 360, height = 320)

        val layout = view.readPrivate<StepFlowChartLayoutResult>("layoutResult")
        assertTrue(layout.isRenderable)
        assertEquals(5, layout.stepLayouts.size)
        assertTrue(layout.stepLayouts.first().cardLeft > layout.hubLayout!!.right)
    }

    @Test
    fun stepFlowChart_animatesAndClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = StepFlowChartView(context)

        view.setPresentationOptions(
            StepFlowChartPresentationOptions(
                animateOnDataChange = true,
                animationDurationMs = 16L,
            ),
        )
        view.setHubContent(
            StepFlowHubContent(
                eyebrow = "INFOGRAPHIC",
                title = "STEPS",
                description = "Shared hub",
            ),
        )
        view.setSteps(sampleSteps())

        layoutAndDraw(view, width = 900, height = 520)
        shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)

        val layout = view.readPrivate<StepFlowChartLayoutResult>("layoutResult")
        val firstStep = layout.stepLayouts.first()
        touchUp(view, firstStep.badgeCenterX, firstStep.badgeCenterY)
        assertEquals(0, view.readPrivate<Int?>("selectedStepIndex"))

        touchUp(view, 5f, 5f)
        assertEquals(1f, view.readPrivate<Float>("renderProgress"), 0.0001f)
        assertNull(view.readPrivate<Int?>("selectedStepIndex"))
        assertFalse(view.readPrivate<Boolean>("hubSelected"))
    }
}
