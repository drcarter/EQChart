package com.magimon.eq.common

import com.magimon.eq.sunburst.SunburstChartLayoutEngine
import com.magimon.eq.sunburst.SunburstChartLayoutResult
import com.magimon.eq.sunburst.SunburstChartPresentationOptions
import com.magimon.eq.sunburst.SunburstSegmentLayout
import com.magimon.eq.sunburst.SunburstChartStyleOptions
import com.magimon.eq.sunburst.SunburstNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SunburstLayoutEngineTest {

    @Test
    fun compute_buildsHierarchicalNormalizedLayout() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(
                SunburstNode(
                    label = "Revenue",
                    children = listOf(
                        SunburstNode("Enterprise", 60.0, payload = "enterprise"),
                        SunburstNode("SMB", 40.0),
                    ),
                ),
                SunburstNode(
                    label = "Services",
                    children = listOf(
                        SunburstNode("Support", 25.0),
                        SunburstNode("Training", 15.0),
                    ),
                ),
            ),
            styleOptions = SunburstChartStyleOptions(),
            presentationOptions = SunburstChartPresentationOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.depthCount)
        assertEquals(6, result.segments.size)
        assertEquals(140.0, result.totalValue, 0.0)

        val root = result.segments.first { it.label == "Revenue" }
        val child = result.segments.first { it.label == "Enterprise" }
        assertEquals(0, root.depth)
        assertEquals(1, child.depth)
        assertEquals(root.outerRadiusRatio + 0.025f, child.innerRadiusRatio, 0.0001f)
        assertEquals("enterprise", child.payload)
        assertTrue(root.hasChildren)
        assertFalse(child.hasChildren)
        assertTrue(root.sweepAngleDeg > child.sweepAngleDeg)
    }

    @Test
    fun compute_usesExplicitLeafValuesAndInheritedColors() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(
                SunburstNode(
                    label = "Root",
                    color = 0xFF224466.toInt(),
                    children = listOf(
                        SunburstNode("Child A", 30.0),
                        SunburstNode("Child B", 10.0, color = 0xFF884422.toInt()),
                    ),
                ),
            ),
        )

        assertTrue(result.isRenderable)
        val root = result.segments.first { it.label == "Root" }
        val childA = result.segments.first { it.label == "Child A" }
        val childB = result.segments.first { it.label == "Child B" }

        assertEquals(40.0, root.value, 0.0)
        assertNotEquals(root.color, childA.color)
        assertEquals(0xFF884422.toInt(), childB.color)
    }

    @Test
    fun compute_handlesSingleLeafRoot() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(SunburstNode(label = "Leaf", value = 12.0)),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.depthCount)
        assertEquals(1, result.segments.size)
        assertEquals("Leaf", result.segments.first().label)
        assertFalse(result.segments.first().hasChildren)
    }

    @Test
    fun compute_filtersInvalidNodes_andSupportsCounterClockwiseSweeps() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(
                SunburstNode(
                    label = "Platform",
                    children = listOf(
                        SunburstNode("", 10.0),
                        SunburstNode("Valid", 50.0),
                        SunburstNode("NaN", Double.NaN),
                    ),
                ),
                SunburstNode("Zero", 0.0),
            ),
            presentationOptions = SunburstChartPresentationOptions(clockwise = false, startAngleDeg = 0f),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.segments.size)
        assertTrue(result.segments.all { it.sweepAngleDeg < 0f })
        assertEquals("Platform", result.segments.first().label)
        assertEquals("Valid", result.segments.last().label)
    }

    @Test
    fun compute_rejectsInvalidConfigurationAndEmptyTrees() {
        val nodes = listOf(SunburstNode("One", 10.0))
        val style = SunburstChartStyleOptions()

        assertEquals(
            "inner hole ratio",
            SunburstChartLayoutEngine.compute(
                nodes = nodes,
                styleOptions = style,
                presentationOptions = SunburstChartPresentationOptions(innerHoleRatio = -0.1f),
            ).emptyReason,
        )
        assertEquals(
            "inner hole ratio",
            SunburstChartLayoutEngine.compute(
                nodes = nodes,
                styleOptions = style,
                presentationOptions = SunburstChartPresentationOptions(innerHoleRatio = 1f),
            ).emptyReason,
        )
        assertEquals(
            "ring gap ratio",
            SunburstChartLayoutEngine.compute(
                nodes = nodes,
                styleOptions = style,
                presentationOptions = SunburstChartPresentationOptions(ringGapRatio = -0.1f),
            ).emptyReason,
        )
        assertEquals(
            "ring gap ratio",
            SunburstChartLayoutEngine.compute(
                nodes = nodes,
                styleOptions = style,
                presentationOptions = SunburstChartPresentationOptions(ringGapRatio = 1f),
            ).emptyReason,
        )
        assertEquals(
            "segment colors",
            SunburstChartLayoutEngine.compute(
                nodes = nodes,
                styleOptions = style.copy(segmentColors = emptyList()),
            ).emptyReason,
        )
        assertEquals(
            "insufficient ring width",
            SunburstChartLayoutEngine.compute(
                nodes = listOf(
                    SunburstNode(
                        label = "Root",
                        children = listOf(
                            SunburstNode(
                                label = "Level1",
                                children = listOf(
                                    SunburstNode(
                                        label = "Level2",
                                        children = listOf(SunburstNode("Leaf", 1.0)),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                presentationOptions = SunburstChartPresentationOptions(innerHoleRatio = 0.8f, ringGapRatio = 0.1f),
            ).emptyReason,
        )
        assertEquals(
            "no valid nodes",
            SunburstChartLayoutEngine.compute(
                nodes = listOf(SunburstNode("", 10.0), SunburstNode("Bad", Double.NaN)),
                styleOptions = style,
            ).emptyReason,
        )
    }

    @Test
    fun hitTestSegment_matchesExpectedRingAndAngle() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(
                SunburstNode(
                    label = "Root",
                    children = listOf(
                        SunburstNode("A", 30.0),
                        SunburstNode("B", 70.0),
                    ),
                ),
            ),
        )

        val root = result.segments.first { it.label == "Root" }
        val childB = result.segments.first { it.label == "B" }
        val rootHit = SunburstChartLayoutEngine.hitTestSegment(
            layout = result,
            normalizedRadiusRatio = (root.innerRadiusRatio + root.outerRadiusRatio) * 0.5f,
            angleDeg = -10f,
        )
        val childHit = SunburstChartLayoutEngine.hitTestSegment(
            layout = result,
            normalizedRadiusRatio = (childB.innerRadiusRatio + childB.outerRadiusRatio) * 0.5f,
            angleDeg = 120f,
        )

        assertNotNull(rootHit)
        assertEquals("Root", rootHit?.label)
        assertNotNull(childHit)
        assertEquals("B", childHit?.label)
        assertNull(
            SunburstChartLayoutEngine.hitTestSegment(
                layout = result,
                normalizedRadiusRatio = 0.1f,
                angleDeg = 180f,
            ),
        )
        assertNull(
            SunburstChartLayoutEngine.hitTestSegment(
                layout = result.copy(isRenderable = false),
                normalizedRadiusRatio = 0.5f,
                angleDeg = 90f,
            ),
        )
    }

    @Test
    fun hitTestSegment_supportsCounterClockwiseAndMissesOutsideSweep() {
        val result = SunburstChartLayoutEngine.compute(
            nodes = listOf(
                SunburstNode(
                    label = "Root",
                    children = listOf(
                        SunburstNode("Left", 20.0),
                        SunburstNode("Right", 80.0),
                    ),
                ),
            ),
            presentationOptions = SunburstChartPresentationOptions(clockwise = false, startAngleDeg = 0f),
        )

        val right = result.segments.first { it.label == "Right" }
        assertEquals(
            "Right",
            SunburstChartLayoutEngine.hitTestSegment(
                layout = result,
                normalizedRadiusRatio = (right.innerRadiusRatio + right.outerRadiusRatio) * 0.5f,
                angleDeg = -200f,
            )?.label,
        )
        assertEquals(
            "Left",
            SunburstChartLayoutEngine.hitTestSegment(
                layout = result,
                normalizedRadiusRatio = (right.innerRadiusRatio + right.outerRadiusRatio) * 0.5f,
                angleDeg = -30f,
            )?.label,
        )
    }

    @Test
    fun hitTestSegment_handlesManualPositiveSweepMisses() {
        val layout = SunburstChartLayoutResult(
            segments = listOf(
                SunburstSegmentLayout(
                    label = "Slice",
                    value = 10.0,
                    depth = 0,
                    startAngleDeg = -90f,
                    sweepAngleDeg = 90f,
                    innerRadiusRatio = 0.2f,
                    outerRadiusRatio = 0.8f,
                    color = 0xFF336699.toInt(),
                    payload = "slice",
                    path = listOf(0),
                    hasChildren = false,
                ),
            ),
            depthCount = 1,
            totalValue = 10.0,
            isRenderable = true,
        )

        assertNull(
            SunburstChartLayoutEngine.hitTestSegment(
                layout = layout,
                normalizedRadiusRatio = -0.1f,
                angleDeg = 0f,
            ),
        )
        assertNull(
            SunburstChartLayoutEngine.hitTestSegment(
                layout = layout,
                normalizedRadiusRatio = 0.95f,
                angleDeg = -45f,
            ),
        )
        assertNull(
            SunburstChartLayoutEngine.hitTestSegment(
                layout = layout,
                normalizedRadiusRatio = 0.5f,
                angleDeg = 180f,
            ),
        )
    }
}
