package com.magimon.eq.common

import com.magimon.eq.cycle.CycleChartLayoutConfig
import com.magimon.eq.cycle.CycleChartLayoutEngine
import com.magimon.eq.cycle.CycleChartLayoutResult
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleLink
import com.magimon.eq.cycle.CycleLinkLayout
import com.magimon.eq.cycle.CycleNode
import com.magimon.eq.cycle.CycleNodeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class CycleLayoutEngineTest {

    private val config = CycleChartLayoutConfig(
        widthPx = 640f,
        heightPx = 480f,
        contentPaddingPx = 18f,
        nodeRadiusPx = 24f,
        linkMinThicknessPx = 3f,
        linkMaxThicknessPx = 10f,
        linkInsetPx = 3f,
        startAngleDeg = -90f,
        clockwise = true,
        linkInnerRadiusFactor = 0.34f,
    )

    @Test
    fun compute_buildsCircularNodeLayoutsAndLinks() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("plan", "Plan", 1),
                CycleNode("build", "Build", 2),
                CycleNode("measure", "Measure", 3),
                CycleNode("learn", "Learn", 4),
            ),
            links = listOf(
                CycleLink("plan", "build", 1.0),
                CycleLink("build", "measure", 4.0),
                CycleLink("measure", "learn", 2.0),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(4, result.nodeLayouts.size)
        assertEquals(3, result.linkLayouts.size)
        assertTrue(result.orbitRadius > 0f)

        val first = result.nodeLayouts.first()
        assertEquals(result.centerX, first.centerX, 0.01f)
        assertTrue(first.centerY < result.centerY)

        val thickest = result.linkLayouts.maxBy { it.thickness }
        assertEquals(10f, thickest.thickness, 0.01f)
    }

    @Test
    fun compute_respectsStartAngleAndDirection() {
        val clockwise = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("a", "A", 1),
                CycleNode("b", "B", 2),
                CycleNode("c", "C", 3),
                CycleNode("d", "D", 4),
            ),
            links = listOf(CycleLink("a", "b", 1.0)),
            config = config.copy(startAngleDeg = 0f, clockwise = true),
            styleOptions = CycleChartStyleOptions(),
        )
        val counterClockwise = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("a", "A", 1),
                CycleNode("b", "B", 2),
                CycleNode("c", "C", 3),
                CycleNode("d", "D", 4),
            ),
            links = listOf(CycleLink("a", "b", 1.0)),
            config = config.copy(startAngleDeg = 0f, clockwise = false),
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(clockwise.isRenderable)
        assertTrue(counterClockwise.isRenderable)
        assertTrue(clockwise.nodeLayouts[1].centerY > clockwise.centerY)
        assertTrue(counterClockwise.nodeLayouts[1].centerY < counterClockwise.centerY)
    }

    @Test
    fun compute_filtersInvalidLinks_andHitTestingWorks() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("left", "Left", 1),
                CycleNode("right", "Right", 2),
                CycleNode("bottom", "Bottom", 3),
            ),
            links = listOf(
                CycleLink("left", "right", 12.0),
                CycleLink("missing-source", "right", 6.0),
                CycleLink("left", "missing", 8.0),
                CycleLink("right", "right", 4.0),
                CycleLink("bottom", "left", 0.0),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.linkLayouts.size)

        val node = result.nodeLayouts.first()
        val hitNode = CycleChartLayoutEngine.hitTestNode(result, node.centerX, node.centerY)
        assertEquals(node.originalIndex, hitNode)
        assertNull(CycleChartLayoutEngine.hitTestNode(result, node.centerX + (node.radius * 2f), node.centerY))

        val link = result.linkLayouts.first()
        val hitLink = CycleChartLayoutEngine.hitTestLink(
            layout = result,
            x = link.midX,
            y = link.midY,
            tolerancePx = 6f,
        )
        assertNotNull(hitLink)
    }

    @Test
    fun compute_rejectsInvalidInputShapes() {
        val nodes = listOf(
            CycleNode("a", "A", 1),
            CycleNode("b", "B", 2),
        )
        val links = listOf(CycleLink("a", "b", 1.0))
        val style = CycleChartStyleOptions()

        assertEquals(
            "non-positive size",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "non-positive size",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(heightPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "node radius",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(nodeRadiusPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "link min thickness",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(linkMinThicknessPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "link max thickness",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(linkMaxThicknessPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "link thickness range",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(linkMinThicknessPx = 10f, linkMaxThicknessPx = 5f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "content padding",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(contentPaddingPx = -1f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "not enough nodes",
            CycleChartLayoutEngine.compute(
                nodes = listOf(CycleNode("only", "Only", 1)),
                links = links,
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "blank node id",
            CycleChartLayoutEngine.compute(
                nodes = listOf(CycleNode(" ", "A", 1), CycleNode("b", "B", 2)),
                links = links,
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "duplicate node id",
            CycleChartLayoutEngine.compute(
                nodes = listOf(CycleNode("dup", "A", 1), CycleNode("dup", "B", 2)),
                links = links,
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "insufficient size",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 70f, heightPx = 70f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(CycleLink("a", "missing", 1.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(CycleLink("a", "a", 1.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            CycleChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(CycleLink("a", "b", Double.NaN)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
    }

    @Test
    fun hitTestLink_returnsNullWhenOutsideTolerance() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("a", "A", 1),
                CycleNode("b", "B", 2),
                CycleNode("c", "C", 3),
            ),
            links = listOf(CycleLink("a", "b", 5.0)),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertFalse(
            CycleChartLayoutEngine.hitTestLink(
                layout = result,
                x = result.centerX,
                y = result.centerY + result.orbitRadius,
                tolerancePx = 3f,
            ) != null,
        )
        assertNull(
            CycleChartLayoutEngine.hitTestLink(
                layout = result,
                x = result.centerX,
                y = result.centerY,
                tolerancePx = 0f,
            ),
        )
    }

    @Test
    fun compute_clampsInsetAndInnerRadius_andFallsBackToDefaultLinkColor() {
        val style = CycleChartStyleOptions(defaultLinkColor = 99)
        val nodes = listOf(
            CycleNode("a", "A", 0),
            CycleNode("b", "B", 2),
            CycleNode("c", "C", 3),
        )
        val links = listOf(CycleLink("a", "b", 5.0))

        val centerControl = CycleChartLayoutEngine.compute(
            nodes = nodes,
            links = links,
            config = config.copy(linkInsetPx = -10f, linkInnerRadiusFactor = -1f),
            styleOptions = style,
        )
        val outerControl = CycleChartLayoutEngine.compute(
            nodes = nodes,
            links = links,
            config = config.copy(linkInnerRadiusFactor = 2f),
            styleOptions = style,
        )

        assertTrue(centerControl.isRenderable)
        assertTrue(outerControl.isRenderable)

        val centerLink = centerControl.linkLayouts.first()
        val outerLink = outerControl.linkLayouts.first()
        val sourceNode = centerControl.nodeLayouts.first()

        assertEquals(99, centerLink.color)
        assertEquals(centerControl.centerX, centerLink.controlX, 0.01f)
        assertEquals(centerControl.centerY, centerLink.controlY, 0.01f)
        assertEquals(sourceNode.radius, hypot(centerLink.startX - sourceNode.centerX, centerLink.startY - sourceNode.centerY), 0.02f)
        assertEquals(outerControl.orbitRadius, hypot(outerLink.controlX - outerControl.centerX, outerLink.controlY - outerControl.centerY), 0.02f)
    }

    @Test
    fun compute_trimsNodeIds_andPrefersExplicitLinkColor() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("plan", "Plan", 1),
                CycleNode("build", "Build", 2),
            ),
            links = listOf(
                CycleLink("  plan  ", "  build ", 5.0, color = 1234),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(defaultLinkColor = 99),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.linkLayouts.size)
        assertEquals(1234, result.linkLayouts.first().color)
    }

    @Test
    fun compute_scalesThicknessCorrectly_whenLargestValueAppearsFirst_orTies() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("a", "A", 1),
                CycleNode("b", "B", 2),
                CycleNode("c", "C", 3),
                CycleNode("d", "D", 4),
            ),
            links = listOf(
                CycleLink("a", "b", 10.0),
                CycleLink("b", "c", 10.0),
                CycleLink("c", "d", 5.0),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(3, result.linkLayouts.size)
        assertEquals(config.linkMaxThicknessPx, result.linkLayouts[0].thickness, 0.01f)
        assertEquals(config.linkMaxThicknessPx, result.linkLayouts[1].thickness, 0.01f)
        assertTrue(result.linkLayouts[2].thickness < result.linkLayouts[0].thickness)
    }

    @Test
    fun compute_keepsOriginalLinkIndexAfterFiltering() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("plan", "Plan", 1),
                CycleNode("build", "Build", 2),
                CycleNode("measure", "Measure", 3),
            ),
            links = listOf(
                CycleLink("missing", "build", 5.0),
                CycleLink("plan", "plan", 4.0),
                CycleLink("plan", "measure", Double.NaN),
                CycleLink("plan", "build", 7.0),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.linkLayouts.size)
        assertEquals(3, result.linkLayouts.first().originalIndex)
    }

    @Test
    fun compute_scalesThicknessAcrossFilteredValidLinks() {
        val result = CycleChartLayoutEngine.compute(
            nodes = listOf(
                CycleNode("plan", "Plan", 1),
                CycleNode("build", "Build", 2),
                CycleNode("measure", "Measure", 3),
            ),
            links = listOf(
                CycleLink("missing", "build", 5.0),
                CycleLink("plan", "plan", 4.0),
                CycleLink("plan", "measure", 3.0),
                CycleLink("plan", "build", 7.0),
            ),
            config = config,
            styleOptions = CycleChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.linkLayouts.size)
        assertEquals(2, result.linkLayouts.first().originalIndex)
        assertEquals(3, result.linkLayouts.last().originalIndex)
        assertTrue(result.linkLayouts.first().thickness < result.linkLayouts.last().thickness)
        assertEquals(config.linkMaxThicknessPx, result.linkLayouts.last().thickness, 0.01f)
    }

    @Test
    fun privateHelpers_coverPerpendicularFallbackAndDegenerateDistancePaths() {
        val perpendicular = invokePrivate<Pair<Float, Float>>(
            "resolveControlDirection",
            1f,
            0f,
            -1f,
            0f,
        )
        val normalized = invokePrivate<Pair<Float, Float>>(
            "resolveControlDirection",
            1f,
            0f,
            0f,
            1f,
        )
        val normalizedSecondOnly = invokePrivate<Pair<Float, Float>>(
            "resolveControlDirection",
            0f,
            1f,
            0f,
            1f,
        )
        val hardFallback = invokePrivate<Pair<Float, Float>>(
            "resolveControlDirection",
            0f,
            0f,
            0f,
            0f,
        )
        val normalizedFallback = invokePrivate<Pair<Float, Float>>(
            "normalizeOrFallback",
            0f,
            0f,
            3f,
            4f,
        )
        val zeroNormalized = invokePrivate<Pair<Float, Float>>(
            "normalizeComponent",
            0f,
            0f,
        )
        val midpoint = invokePrivate<Pair<Float, Float>>(
            "quadraticPoint",
            0f,
            0f,
            4f,
            4f,
            8f,
            0f,
            0.5f,
        )
        val degenerateLink = CycleLinkLayout(
            originalIndex = 0,
            link = CycleLink("a", "b", 1.0),
            sourceNodeOriginalIndex = 0,
            targetNodeOriginalIndex = 1,
            startX = 10f,
            startY = 20f,
            controlX = 10f,
            controlY = 20f,
            endX = 10f,
            endY = 20f,
            midX = 10f,
            midY = 20f,
            thickness = 3f,
            color = 1,
        )
        val degenerateDistance = invokePrivate<Float>(
            "distanceToQuadratic",
            13f,
            24f,
            degenerateLink,
        )
        val clampedSegmentDistance = invokePrivate<Float>(
            "distanceToSegment",
            10f,
            0f,
            0f,
            0f,
            4f,
            0f,
        )
        val invalid = invokePrivate<CycleChartLayoutResult>("invalid", "forced")

        assertEquals(0f, perpendicular.first, 0.0001f)
        assertEquals(-1f, perpendicular.second, 0.0001f)
        assertEquals(0.7071f, normalized.first, 0.0002f)
        assertEquals(0.7071f, normalized.second, 0.0002f)
        assertEquals(0f, normalizedSecondOnly.first, 0.0001f)
        assertEquals(1f, normalizedSecondOnly.second, 0.0001f)
        assertEquals(0f, hardFallback.first, 0.0001f)
        assertEquals(-1f, hardFallback.second, 0.0001f)
        assertEquals(0.6f, normalizedFallback.first, 0.0001f)
        assertEquals(0.8f, normalizedFallback.second, 0.0001f)
        assertEquals(0f, zeroNormalized.first, 0.0f)
        assertEquals(0f, zeroNormalized.second, 0.0f)
        assertEquals(4f, midpoint.first, 0.0001f)
        assertEquals(2f, midpoint.second, 0.0001f)
        assertEquals(5f, degenerateDistance, 0.0001f)
        assertEquals(6f, clampedSegmentDistance, 0.0001f)
        assertFalse(invalid.isRenderable)
        assertEquals("forced", invalid.emptyReason)
        assertTrue(invalid.nodeLayouts.isEmpty())
        assertTrue(invalid.linkLayouts.isEmpty())

        val emptyHit = CycleChartLayoutEngine.hitTestLink(
            layout = invalid,
            x = 10f,
            y = 20f,
            tolerancePx = 4f,
        )
        assertNull(emptyHit)
    }

    @Test
    fun hitTestLink_picksNearestLinkFromManualLayout() {
        val linkA = CycleLinkLayout(
            originalIndex = 10,
            link = CycleLink("a", "b", 1.0),
            sourceNodeOriginalIndex = 0,
            targetNodeOriginalIndex = 1,
            startX = 0f,
            startY = 0f,
            controlX = 20f,
            controlY = 0f,
            endX = 40f,
            endY = 0f,
            midX = 20f,
            midY = 0f,
            thickness = 2f,
            color = 1,
        )
        val linkB = CycleLinkLayout(
            originalIndex = 11,
            link = CycleLink("b", "c", 1.0),
            sourceNodeOriginalIndex = 1,
            targetNodeOriginalIndex = 2,
            startX = 0f,
            startY = 20f,
            controlX = 20f,
            controlY = 20f,
            endX = 40f,
            endY = 20f,
            midX = 20f,
            midY = 20f,
            thickness = 2f,
            color = 2,
        )
        val layout = CycleChartLayoutResult(
            nodeLayouts = listOf(
                CycleNodeLayout(0, CycleNode("a", "A", 1), 0f, 0f, 4f, 0f),
                CycleNodeLayout(1, CycleNode("b", "B", 2), 20f, 0f, 4f, 180f),
            ),
            linkLayouts = listOf(linkA, linkB),
            centerX = 20f,
            centerY = 10f,
            orbitRadius = 10f,
            isRenderable = true,
        )

        assertEquals(11, CycleChartLayoutEngine.hitTestLink(layout, x = 19f, y = 20f, tolerancePx = 2f))
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
                else -> arg::class.java
            }
        }.toTypedArray()
        val method = CycleChartLayoutEngine::class.java.getDeclaredMethod(name, *parameterTypes)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(CycleChartLayoutEngine, *args) as T
    }
}
