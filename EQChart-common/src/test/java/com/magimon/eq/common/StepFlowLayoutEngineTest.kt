package com.magimon.eq.common

import com.magimon.eq.stepflow.StepFlowChartLayoutConfig
import com.magimon.eq.stepflow.StepFlowChartLayoutEngine
import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartStyleOptions
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowChartLayoutResult
import com.magimon.eq.stepflow.StepFlowStepLayout
import com.magimon.eq.stepflow.StepFlowStep
import com.magimon.eq.stepflow.StepFlowTailDotLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepFlowLayoutEngineTest {

    private val styleOptions = StepFlowChartStyleOptions()

    private val presentationOptions = StepFlowChartPresentationOptions()

    private val hubContent = StepFlowHubContent(
        eyebrow = "INFOGRAPHIC",
        title = "STEPS",
        description = "Show the process clearly",
        payload = "hub",
    )

    private val steps = listOf(
        StepFlowStep("discover", "STEP 01", "Discover", "Collect inputs", 0xFF9333.toInt(), "!"),
        StepFlowStep("design", "STEP 02", "Design", "Shape the plan", 0xAA55FF, "#"),
        StepFlowStep("build", "STEP 03", "Build", "Implement the work", 0x55AAFF, "*"),
        StepFlowStep("launch", "STEP 04", "Launch", "Release to users", 0xF6C026.toInt(), "$"),
        StepFlowStep("measure", "STEP 05", "Measure", "Track outcomes", 0xA7D129.toInt(), "+"),
    )

    private fun validConfig(
        widthPx: Float = 900f,
        heightPx: Float = 520f,
    ): StepFlowChartLayoutConfig {
        return StepFlowChartLayoutConfig(
            widthPx = widthPx,
            heightPx = heightPx,
            contentPaddingPx = 24f,
            hubRadiusPx = 100f,
            hubRingThicknessPx = 10f,
            spineWidthPx = 4f,
            spineDotRadiusPx = 8f,
            tailDotRadiusPx = 6f,
            badgeRadiusPx = 28f,
            cardWidthPx = 220f,
            cardHeightPx = 64f,
            cardCornerRadiusPx = 32f,
            connectorWidthPx = 2f,
            iconCircleRadiusPx = 16f,
            cardGapPx = 20f,
            badgeOverlapPx = 12f,
            topBottomInsetPx = 40f,
        )
    }

    @Test
    fun `compute returns renderable layout for valid data`() {
        val result = StepFlowChartLayoutEngine.compute(
            hubContent = hubContent,
            steps = steps,
            config = validConfig(),
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )

        assertTrue(result.isRenderable)
        assertNull(result.emptyReason)
        assertNotNull(result.hubLayout)
        assertEquals(5, result.hubRingSegments.size)
        assertEquals(6, result.spineSegments.size)
        assertEquals(2, result.tailDots.size)
        assertEquals(5, result.stepLayouts.size)
        assertEquals("hub", result.hubLayout!!.content.payload)
        assertEquals(-118f, result.hubRingSegments.first().startAngleDeg, 0.0f)
        assertEquals(50.4f, result.hubRingSegments.last().sweepAngleDeg, 0.001f)
        assertEquals("Discover", result.stepLayouts.first().step.title)
        assertEquals("+", result.stepLayouts.last().step.iconText)
        assertTrue(result.stepLayouts.first().spineDotCenterY < result.stepLayouts.last().spineDotCenterY)
        assertTrue(result.stepLayouts.first().cardLeft > result.hubLayout!!.right)
    }

    @Test
    fun `scaleToFit shrinks configuration for narrow viewports`() {
        val scaled = validConfig(widthPx = 360f, heightPx = 320f).scaleToFit(stepCount = steps.size)

        assertTrue(scaled.cardWidthPx < validConfig().cardWidthPx)
        assertTrue(scaled.hubRadiusPx < validConfig().hubRadiusPx)
        assertTrue(scaled.topBottomInsetPx < validConfig().topBottomInsetPx)
        assertEquals(360f, scaled.widthPx, 0f)
        assertEquals(320f, scaled.heightPx, 0f)
    }

    @Test
    fun `scaleToFit keeps original configuration when space is sufficient or no steps exist`() {
        val original = validConfig()

        assertEquals(original, original.scaleToFit(stepCount = steps.size))
        assertEquals(original, original.scaleToFit(stepCount = 0))
    }

    @Test
    fun `scaleToFit handles zero preferred size and single-step height path`() {
        val degenerate = validConfig(widthPx = 320f, heightPx = 240f).copy(
            contentPaddingPx = 0f,
            hubRadiusPx = 0f,
            hubRingThicknessPx = 0f,
            spineDotRadiusPx = 0f,
            badgeRadiusPx = 0f,
            cardWidthPx = 0f,
            topBottomInsetPx = 0f,
            cardGapPx = 0f,
        )

        assertEquals(degenerate, degenerate.scaleToFit(stepCount = 1))
    }

    @Test
    fun `compute supports hidden tail dots and missing hub`() {
        val result = StepFlowChartLayoutEngine.compute(
            hubContent = null,
            steps = steps.take(3),
            config = validConfig(),
            styleOptions = styleOptions,
            presentationOptions = StepFlowChartPresentationOptions(
                showTopTailDot = false,
                showBottomTailDot = false,
            ),
        )

        assertTrue(result.isRenderable)
        assertNull(result.hubLayout)
        assertTrue(result.tailDots.isEmpty())
        assertEquals(2, result.spineSegments.size)
        assertEquals(3, result.stepLayouts.size)
    }

    @Test
    fun `hit testing resolves step card badge connector and hub`() {
        val result = StepFlowChartLayoutEngine.compute(
            hubContent = hubContent,
            steps = steps,
            config = validConfig(),
            styleOptions = styleOptions,
            presentationOptions = presentationOptions,
        )
        val second = result.stepLayouts[1]
        val first = result.stepLayouts.first()
        val connectorProbeX = (second.connectorStartX + second.spineDotRadius + second.connectorEndX) * 0.5f

        assertEquals(
            0,
            StepFlowChartLayoutEngine.hitTestStep(
                layout = result,
                x = first.spineDotCenterX,
                y = first.spineDotCenterY,
            ),
        )
        assertEquals(
            1,
            StepFlowChartLayoutEngine.hitTestStep(
                layout = result,
                x = second.cardLeft + 8f,
                y = second.cardTop + 8f,
            ),
        )
        assertEquals(
            1,
            StepFlowChartLayoutEngine.hitTestStep(
                layout = result,
                x = second.badgeCenterX,
                y = second.badgeCenterY,
            ),
        )
        assertEquals(
            1,
            StepFlowChartLayoutEngine.hitTestStep(
                layout = result,
                x = connectorProbeX,
                y = second.connectorStartY,
                tolerancePx = 0f,
            ),
        )
        assertNull(
            StepFlowChartLayoutEngine.hitTestStep(
                layout = result,
                x = 5f,
                y = 5f,
            ),
        )
        assertTrue(
            StepFlowChartLayoutEngine.hitTestHub(
                layout = result,
                x = result.hubLayout!!.centerX,
                y = result.hubLayout!!.centerY,
            ),
        )
        assertFalse(
            StepFlowChartLayoutEngine.hitTestHub(
                layout = result,
                x = result.hubLayout!!.right + 24f,
                y = result.hubLayout!!.centerY,
            ),
        )
    }

    @Test
    fun `hit testing resolves connector branch on manual layout`() {
        val step = StepFlowStep("manual", "STEP 01", "Manual", accentColor = 0xFFAA33)
        val manualLayout = StepFlowChartLayoutResult(
            hubLayout = null,
            hubRingSegments = emptyList(),
            spineSegments = emptyList(),
            tailDots = emptyList(),
            stepLayouts = listOf(
                StepFlowStepLayout(
                    originalIndex = 0,
                    step = step,
                    spineDotCenterX = 20f,
                    spineDotCenterY = 40f,
                    spineDotRadius = 4f,
                    badgeCenterX = 60f,
                    badgeCenterY = 40f,
                    badgeRadius = 6f,
                    cardLeft = 100f,
                    cardTop = 20f,
                    cardRight = 180f,
                    cardBottom = 60f,
                    cardCornerRadius = 20f,
                    connectorStartX = 25f,
                    connectorStartY = 40f,
                    connectorEndX = 53f,
                    connectorEndY = 40f,
                    iconCenterX = 160f,
                    iconCenterY = 40f,
                    iconRadius = 8f,
                    titleX = 112f,
                    titleBaselineY = 34f,
                    bodyX = 112f,
                    bodyBaselineY = 48f,
                    textRight = 148f,
                ),
            ),
            isRenderable = true,
            emptyReason = null,
        )

        assertEquals(
            0,
            StepFlowChartLayoutEngine.hitTestStep(
                layout = manualLayout,
                x = 39f,
                y = 40f,
                tolerancePx = 0f,
            ),
        )
    }

    @Test
    fun `invalid inputs return non renderable layout with reason`() {
        val cases = listOf(
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig(widthPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig(heightPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = emptyList(),
                config = validConfig(),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = listOf(
                    StepFlowStep(" ", "STEP 01", "Discover", accentColor = 0xFFFFFF),
                ),
                config = validConfig(),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = listOf(
                    StepFlowStep("dup", "STEP 01", "Discover", accentColor = 0xFFFFFF),
                    StepFlowStep("dup", "STEP 02", "Design", accentColor = 0xAAAAAA),
                ),
                config = validConfig(),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = listOf(
                    StepFlowStep("valid", " ", "Discover", accentColor = 0xFFFFFF),
                ),
                config = validConfig(),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = listOf(
                    StepFlowStep("valid", "STEP 01", " ", accentColor = 0xFFFFFF),
                ),
                config = validConfig(),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig(widthPx = 300f, heightPx = 220f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(cardGapPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(badgeOverlapPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(contentPaddingPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(hubRadiusPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(hubRingThicknessPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(spineWidthPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(spineDotRadiusPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(tailDotRadiusPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(badgeRadiusPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(cardWidthPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(cardHeightPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(cardCornerRadiusPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(connectorWidthPx = 0f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(iconCircleRadiusPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig().copy(topBottomInsetPx = -1f),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps.take(1),
                config = validConfig(heightPx = 220f).copy(
                    contentPaddingPx = 8f,
                    topBottomInsetPx = 8f,
                    cardHeightPx = 210f,
                ),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps.take(1),
                config = validConfig(heightPx = 200f).copy(
                    contentPaddingPx = 120f,
                    topBottomInsetPx = 40f,
                ),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
            StepFlowChartLayoutEngine.compute(
                hubContent = hubContent,
                steps = steps,
                config = validConfig(heightPx = 260f).copy(
                    contentPaddingPx = 16f,
                    topBottomInsetPx = 24f,
                ),
                styleOptions = styleOptions,
                presentationOptions = presentationOptions,
            ),
        )

        val reasons = cases.mapNotNull { it.emptyReason }

        assertTrue(cases.all { !it.isRenderable })
        assertTrue(reasons.contains("non-positive size"))
        assertTrue(reasons.contains("no steps"))
        assertTrue(reasons.contains("blank step id"))
        assertTrue(reasons.contains("duplicate step id"))
        assertTrue(reasons.contains("blank badge label"))
        assertTrue(reasons.contains("blank step title"))
        assertTrue(reasons.contains("card gap"))
        assertTrue(reasons.contains("badge overlap"))
        assertTrue(reasons.contains("content padding"))
        assertTrue(reasons.contains("hub radius"))
        assertTrue(reasons.contains("hub ring thickness"))
        assertTrue(reasons.contains("spine width"))
        assertTrue(reasons.contains("spine dot radius"))
        assertTrue(reasons.contains("tail dot radius"))
        assertTrue(reasons.contains("badge radius"))
        assertTrue(reasons.contains("card size"))
        assertTrue(reasons.contains("card corner radius"))
        assertTrue(reasons.contains("connector width"))
        assertTrue(reasons.contains("icon radius"))
        assertTrue(reasons.contains("top bottom inset"))
        assertTrue(reasons.contains("insufficient height"))
        assertTrue(reasons.contains("insufficient vertical spacing"))
        assertTrue(reasons.contains("card vertical overflow"))
    }

    @Test
    fun `presentation options change ring sweep and spine bend`() {
        val custom = StepFlowChartLayoutEngine.compute(
            hubContent = hubContent,
            steps = steps.take(4),
            config = validConfig(),
            styleOptions = styleOptions,
            presentationOptions = StepFlowChartPresentationOptions(
                hubRingStartAngleDeg = -90f,
                hubRingSweepDeg = 180f,
                spineBendFactor = 0f,
                showTopTailDot = true,
                showBottomTailDot = false,
            ),
        )

        assertTrue(custom.isRenderable)
        assertEquals(4, custom.hubRingSegments.size)
        assertEquals(-90f, custom.hubRingSegments.first().startAngleDeg, 0.0f)
        assertEquals(45f, custom.hubRingSegments.first().sweepAngleDeg, 0.0f)
        assertEquals(1, custom.tailDots.size)
        assertEquals(
            custom.stepLayouts.first().spineDotCenterX,
            custom.stepLayouts.last().spineDotCenterX,
            0.001f,
        )
    }

    @Test
    fun `hit testing and helpers cover non renderable and private branches`() {
        val emptyLayout = StepFlowChartLayoutResult(
            hubLayout = null,
            hubRingSegments = emptyList(),
            spineSegments = emptyList(),
            tailDots = listOf(StepFlowTailDotLayout(true, 1f, 2f, 3f)),
            stepLayouts = emptyList(),
            isRenderable = false,
            emptyReason = "empty",
        )

        assertNull(StepFlowChartLayoutEngine.hitTestStep(emptyLayout, 0f, 0f))
        assertFalse(StepFlowChartLayoutEngine.hitTestHub(emptyLayout, 0f, 0f))
        assertFalse(
            StepFlowChartLayoutEngine.hitTestHub(
                emptyLayout.copy(isRenderable = true, emptyReason = null),
                0f,
                0f,
            ),
        )

        val ringSegments: List<*> = invokePrivate("buildRingSegments", emptyList<StepFlowStep>(), 0f, 45f)
        val zeroSweep: List<*> = invokePrivate("buildRingSegments", steps.take(1), 0f, 0f)
        val noSegments: List<*> = invokePrivate("buildSpineSegments", emptyList<Pair<Float, Float>>())
        val onePoint: List<*> = invokePrivate("buildSpineSegments", listOf(1f to 2f))
        val rectHit: Boolean = invokePrivate("pointInRect", 5f, 5f, 0f, 0f, 10f, 10f)
        val rectMiss: Boolean = invokePrivate("pointInRect", 15f, 5f, 0f, 0f, 10f, 10f)
        val rectMissY: Boolean = invokePrivate("pointInRect", 5f, 15f, 0f, 0f, 10f, 10f)
        val zeroSegmentDistance: Float = invokePrivate("distanceToSegment", 3f, 4f, 0f, 0f, 0f, 0f)
        val clampedDistance: Float = invokePrivate("distanceToSegment", 20f, 0f, 0f, 0f, 10f, 0f)

        assertTrue(ringSegments.isEmpty())
        assertTrue(zeroSweep.isEmpty())
        assertTrue(noSegments.isEmpty())
        assertTrue(onePoint.isEmpty())
        assertTrue(rectHit)
        assertFalse(rectMiss)
        assertFalse(rectMissY)
        assertEquals(5f, zeroSegmentDistance, 0.001f)
        assertEquals(10f, clampedDistance, 0.001f)
    }

    private fun <T> invokePrivate(
        name: String,
        vararg args: Any,
    ): T {
        val parameterTypes = args.map { arg ->
            when (arg) {
                is Float -> Float::class.javaPrimitiveType
                is Int -> Int::class.javaPrimitiveType
                is Boolean -> Boolean::class.javaPrimitiveType
                is String -> String::class.java
                is List<*> -> List::class.java
                else -> arg::class.java
            }
        }.toTypedArray()
        val method = StepFlowChartLayoutEngine::class.java.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(StepFlowChartLayoutEngine, *args) as T
    }
}
