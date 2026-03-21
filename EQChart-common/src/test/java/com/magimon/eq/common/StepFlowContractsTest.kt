package com.magimon.eq.common

import com.magimon.eq.stepflow.StepFlowChartPresentationOptions
import com.magimon.eq.stepflow.StepFlowChartStyleOptions
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowHubLayout
import com.magimon.eq.stepflow.StepFlowHubRingSegment
import com.magimon.eq.stepflow.StepFlowSpineSegment
import com.magimon.eq.stepflow.StepFlowStep
import com.magimon.eq.stepflow.StepFlowStepLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepFlowContractsTest {

    @Test
    fun `step contract stores all fields`() {
        val payload = mapOf("id" to 1)
        val step = StepFlowStep(
            id = "discover",
            badgeLabel = "STEP 01",
            title = "Discover",
            description = "Collect requirements",
            accentColor = 0xFF00FF,
            iconText = "!",
            payload = payload,
        )

        assertEquals("discover", step.id)
        assertEquals("STEP 01", step.badgeLabel)
        assertEquals("Discover", step.title)
        assertEquals("Collect requirements", step.description)
        assertEquals(0xFF00FF, step.accentColor)
        assertEquals("!", step.iconText)
        assertEquals(payload, step.payload)
        assertEquals("Refine", step.copy(title = "Refine").title)
        assertTrue(step.toString().contains("STEP 01"))
    }

    @Test
    fun `hub contract and options support copy equality and defaults`() {
        val hub = StepFlowHubContent(
            eyebrow = "INFOGRAPHIC",
            title = "STEPS",
            description = "Five-step process",
            payload = "hub",
        )
        val defaultHub = StepFlowHubContent(title = "TITLE")
        val styleOptions = StepFlowChartStyleOptions()
        val customStyle = StepFlowChartStyleOptions(
            backgroundColor = 1,
            contentPaddingDp = 2f,
            hubRadiusDp = 3f,
            hubRingThicknessDp = 4f,
            hubFillColor = 5,
            hubStrokeColor = 6,
            hubStrokeWidthDp = 7f,
            hubEyebrowTextColor = 8,
            hubEyebrowTextSizeSp = 9f,
            hubTitleTextColor = 10,
            hubTitleTextSizeSp = 11f,
            hubBodyTextColor = 12,
            hubBodyTextSizeSp = 13f,
            spineColor = 14,
            spineWidthDp = 15f,
            spineDotRadiusDp = 16f,
            tailDotRadiusDp = 17f,
            badgeRadiusDp = 18f,
            badgeFillColor = 19,
            badgeStrokeColor = 20,
            badgeStrokeWidthDp = 21f,
            badgeTextColor = 22,
            badgeTextSizeSp = 23f,
            cardWidthDp = 24f,
            cardHeightDp = 25f,
            cardCornerRadiusDp = 26f,
            cardTextPaddingStartDp = 27f,
            cardTextPaddingEndDp = 28f,
            cardTitleTextColor = 29,
            cardTitleTextSizeSp = 30f,
            cardBodyTextColor = 31,
            cardBodyTextSizeSp = 32f,
            connectorWidthDp = 33f,
            connectorColor = 34,
            iconCircleRadiusDp = 35f,
            iconCircleFillColor = 36,
            iconTextColor = 37,
            iconTextSizeSp = 38f,
            selectedStrokeColor = 39,
            selectedStrokeWidthDp = 40f,
        )
        val presentationOptions = StepFlowChartPresentationOptions()
        val customPresentation = StepFlowChartPresentationOptions(
            animateOnDataChange = false,
            animationDurationMs = 321L,
            showStepDescriptions = false,
            showHubDescription = false,
            showTopTailDot = false,
            showBottomTailDot = false,
            hubRingStartAngleDeg = -90f,
            hubRingSweepDeg = 180f,
            spineBendFactor = 0.25f,
            emptyText = "EMPTY",
        )

        assertEquals("FLOW", hub.copy(title = "FLOW").title)
        assertEquals(null, defaultHub.eyebrow)
        assertEquals(null, defaultHub.description)
        assertEquals(
            StepFlowHubContent(
                eyebrow = "INFOGRAPHIC",
                title = "STEPS",
                description = "Five-step process",
                payload = "hub",
            ),
            hub,
        )
        assertTrue(styleOptions.contentPaddingDp > 0f)
        assertTrue(styleOptions.hubRadiusDp > 0f)
        assertTrue(styleOptions.cardWidthDp > 0f)
        assertTrue(presentationOptions.animateOnDataChange)
        assertEquals(650L, presentationOptions.animationDurationMs)
        assertTrue(presentationOptions.showStepDescriptions)
        assertTrue(presentationOptions.showHubDescription)
        assertTrue(presentationOptions.showTopTailDot)
        assertTrue(presentationOptions.showBottomTailDot)
        assertEquals(-118f, presentationOptions.hubRingStartAngleDeg, 0.0f)
        assertTrue(presentationOptions.hubRingSweepDeg > 0f)
        assertTrue(presentationOptions.spineBendFactor > 0f)
        assertEquals("No data", presentationOptions.emptyText)
        assertEquals(200f, styleOptions.copy(cardWidthDp = 200f).cardWidthDp, 0.0f)
        assertFalse(presentationOptions.copy(showBottomTailDot = false).showBottomTailDot)
        val partialStyle = StepFlowChartStyleOptions(
            cardWidthDp = 190f,
            selectedStrokeWidthDp = 3.5f,
        ).copy(cardHeightDp = 60f)
        assertEquals(190f, partialStyle.cardWidthDp, 0.0f)
        assertEquals(60f, partialStyle.cardHeightDp, 0.0f)
        assertEquals(3.5f, partialStyle.selectedStrokeWidthDp, 0.0f)
        assertEquals(styleOptions.backgroundColor, partialStyle.backgroundColor)
        val syntheticCtor = StepFlowChartStyleOptions::class.java.declaredConstructors
            .first { constructor ->
                constructor.parameterTypes.lastOrNull()?.name == "kotlin.jvm.internal.DefaultConstructorMarker"
            }
        val reflected = syntheticCtor.newInstance(
            1,
            2f,
            3f,
            4f,
            5,
            6,
            7f,
            8,
            9f,
            10,
            11f,
            12,
            13f,
            14,
            15f,
            16f,
            17f,
            18f,
            19,
            20,
            21f,
            22,
            23f,
            24f,
            25f,
            26f,
            27f,
            28f,
            29,
            30f,
            31,
            32f,
            33f,
            34,
            35f,
            36,
            37,
            38f,
            39,
            40f,
            0,
            0,
            null,
        ) as StepFlowChartStyleOptions
        assertEquals(1, reflected.backgroundColor)
        assertEquals(40f, reflected.selectedStrokeWidthDp, 0.0f)
        assertEquals(1, customStyle.backgroundColor)
        assertEquals(2f, customStyle.contentPaddingDp, 0.0f)
        assertEquals(3f, customStyle.hubRadiusDp, 0.0f)
        assertEquals(4f, customStyle.hubRingThicknessDp, 0.0f)
        assertEquals(5, customStyle.hubFillColor)
        assertEquals(6, customStyle.hubStrokeColor)
        assertEquals(7f, customStyle.hubStrokeWidthDp, 0.0f)
        assertEquals(8, customStyle.hubEyebrowTextColor)
        assertEquals(9f, customStyle.hubEyebrowTextSizeSp, 0.0f)
        assertEquals(10, customStyle.hubTitleTextColor)
        assertEquals(11f, customStyle.hubTitleTextSizeSp, 0.0f)
        assertEquals(12, customStyle.hubBodyTextColor)
        assertEquals(13f, customStyle.hubBodyTextSizeSp, 0.0f)
        assertEquals(14, customStyle.spineColor)
        assertEquals(15f, customStyle.spineWidthDp, 0.0f)
        assertEquals(16f, customStyle.spineDotRadiusDp, 0.0f)
        assertEquals(17f, customStyle.tailDotRadiusDp, 0.0f)
        assertEquals(18f, customStyle.badgeRadiusDp, 0.0f)
        assertEquals(19, customStyle.badgeFillColor)
        assertEquals(20, customStyle.badgeStrokeColor)
        assertEquals(21f, customStyle.badgeStrokeWidthDp, 0.0f)
        assertEquals(22, customStyle.badgeTextColor)
        assertEquals(23f, customStyle.badgeTextSizeSp, 0.0f)
        assertEquals(24f, customStyle.cardWidthDp, 0.0f)
        assertEquals(25f, customStyle.cardHeightDp, 0.0f)
        assertEquals(26f, customStyle.cardCornerRadiusDp, 0.0f)
        assertEquals(27f, customStyle.cardTextPaddingStartDp, 0.0f)
        assertEquals(28f, customStyle.cardTextPaddingEndDp, 0.0f)
        assertEquals(29, customStyle.cardTitleTextColor)
        assertEquals(30f, customStyle.cardTitleTextSizeSp, 0.0f)
        assertEquals(31, customStyle.cardBodyTextColor)
        assertEquals(32f, customStyle.cardBodyTextSizeSp, 0.0f)
        assertEquals(33f, customStyle.connectorWidthDp, 0.0f)
        assertEquals(34, customStyle.connectorColor)
        assertEquals(35f, customStyle.iconCircleRadiusDp, 0.0f)
        assertEquals(36, customStyle.iconCircleFillColor)
        assertEquals(37, customStyle.iconTextColor)
        assertEquals(38f, customStyle.iconTextSizeSp, 0.0f)
        assertEquals(39, customStyle.selectedStrokeColor)
        assertEquals(40f, customStyle.selectedStrokeWidthDp, 0.0f)
        assertFalse(customPresentation.animateOnDataChange)
        assertEquals(321L, customPresentation.animationDurationMs)
        assertFalse(customPresentation.showStepDescriptions)
        assertFalse(customPresentation.showHubDescription)
        assertFalse(customPresentation.showTopTailDot)
        assertFalse(customPresentation.showBottomTailDot)
        assertEquals(-90f, customPresentation.hubRingStartAngleDeg, 0.0f)
        assertEquals(180f, customPresentation.hubRingSweepDeg, 0.0f)
        assertEquals(0.25f, customPresentation.spineBendFactor, 0.0f)
        assertEquals("EMPTY", customPresentation.emptyText)
    }

    @Test
    fun `layout models expose derived geometry`() {
        val step = StepFlowStep(
            id = "launch",
            badgeLabel = "STEP 03",
            title = "Launch",
            description = "Ship the release",
            accentColor = 0x33AAFF,
            iconText = "$",
        )
        val hubLayout = StepFlowHubLayout(
            content = StepFlowHubContent(title = "STEPS"),
            centerX = 120f,
            centerY = 180f,
            radius = 64f,
            ringThickness = 8f,
            eyebrowCenterX = 120f,
            eyebrowBaselineY = 144f,
            titleCenterX = 120f,
            titleBaselineY = 186f,
            descriptionCenterX = 120f,
            descriptionBaselineY = 218f,
        )
        val ringSegment = StepFlowHubRingSegment(
            originalIndex = 2,
            color = 0x33AAFF,
            startAngleDeg = -90f,
            sweepAngleDeg = 42f,
        )
        val spineSegment = StepFlowSpineSegment(
            startX = 12f,
            startY = 20f,
            controlX = 30f,
            controlY = 40f,
            endX = 48f,
            endY = 60f,
        )
        val stepLayout = StepFlowStepLayout(
            originalIndex = 2,
            step = step,
            spineDotCenterX = 300f,
            spineDotCenterY = 160f,
            spineDotRadius = 9f,
            badgeCenterX = 336f,
            badgeCenterY = 160f,
            badgeRadius = 26f,
            cardLeft = 350f,
            cardTop = 132f,
            cardRight = 544f,
            cardBottom = 188f,
            cardCornerRadius = 28f,
            connectorStartX = 300f,
            connectorStartY = 160f,
            connectorEndX = 310f,
            connectorEndY = 160f,
            iconCenterX = 512f,
            iconCenterY = 160f,
            iconRadius = 14f,
            titleX = 372f,
            titleBaselineY = 150f,
            bodyX = 372f,
            bodyBaselineY = 170f,
            textRight = 484f,
        )

        assertEquals(56f, hubLayout.left, 0.0f)
        assertEquals(116f, hubLayout.top, 0.0f)
        assertEquals(184f, hubLayout.right, 0.0f)
        assertEquals(244f, hubLayout.bottom, 0.0f)
        assertEquals(8f, hubLayout.ringThickness, 0.0f)
        assertEquals(120f, hubLayout.eyebrowCenterX, 0.0f)
        assertEquals(144f, hubLayout.eyebrowBaselineY, 0.0f)
        assertEquals(120f, hubLayout.titleCenterX, 0.0f)
        assertEquals(186f, hubLayout.titleBaselineY, 0.0f)
        assertEquals(120f, hubLayout.descriptionCenterX, 0.0f)
        assertEquals(218f, hubLayout.descriptionBaselineY, 0.0f)
        assertEquals(2, ringSegment.originalIndex)
        assertEquals(0x33AAFF, ringSegment.color)
        assertEquals(-90f, ringSegment.startAngleDeg, 0.0f)
        assertEquals(42f, ringSegment.sweepAngleDeg, 0.0f)
        assertEquals(12f, spineSegment.startX, 0.0f)
        assertEquals(20f, spineSegment.startY, 0.0f)
        assertEquals(30f, spineSegment.controlX, 0.0f)
        assertEquals(40f, spineSegment.controlY, 0.0f)
        assertEquals(48f, spineSegment.endX, 0.0f)
        assertEquals(60f, spineSegment.endY, 0.0f)
        assertEquals(194f, stepLayout.width, 0.0f)
        assertEquals(56f, stepLayout.height, 0.0f)
        assertEquals(28f, stepLayout.cardCornerRadius, 0.0f)
        assertEquals(512f, stepLayout.iconCenterX, 0.0f)
        assertEquals(160f, stepLayout.iconCenterY, 0.0f)
        assertEquals(14f, stepLayout.iconRadius, 0.0f)
        assertEquals(372f, stepLayout.titleX, 0.0f)
        assertEquals(150f, stepLayout.titleBaselineY, 0.0f)
        assertEquals(372f, stepLayout.bodyX, 0.0f)
        assertEquals(170f, stepLayout.bodyBaselineY, 0.0f)
        assertEquals(484f, stepLayout.textRight, 0.0f)
        assertEquals(560f, stepLayout.copy(cardRight = 560f).cardRight, 0.0f)
        assertTrue(stepLayout.toString().contains("Launch"))

        val tailDot = com.magimon.eq.stepflow.StepFlowTailDotLayout(
            isTop = true,
            centerX = 20f,
            centerY = 22f,
            radius = 6f,
        )
        val result = com.magimon.eq.stepflow.StepFlowChartLayoutResult(
            hubLayout = hubLayout,
            hubRingSegments = listOf(ringSegment),
            spineSegments = listOf(spineSegment),
            tailDots = listOf(tailDot),
            stepLayouts = listOf(stepLayout),
            isRenderable = true,
        )
        assertTrue(tailDot.isTop)
        assertEquals(20f, tailDot.centerX, 0.0f)
        assertEquals(22f, tailDot.centerY, 0.0f)
        assertEquals(6f, tailDot.radius, 0.0f)
        assertEquals(true, result.isRenderable)
        assertEquals(1, result.hubRingSegments.size)
        assertEquals(1, result.spineSegments.size)
        assertEquals(1, result.tailDots.size)
        assertEquals(1, result.stepLayouts.size)
        assertEquals(null, result.emptyReason)
        assertEquals("ignored", result.copy(emptyReason = "ignored").emptyReason)
    }
}
