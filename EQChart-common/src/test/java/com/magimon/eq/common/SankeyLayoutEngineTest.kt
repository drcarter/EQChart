package com.magimon.eq.common

import com.magimon.eq.sankey.SankeyChartLayoutConfig
import com.magimon.eq.sankey.SankeyChartLayoutEngine
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SankeyLayoutEngineTest {

    private val config = SankeyChartLayoutConfig(
        widthPx = 640f,
        heightPx = 360f,
        contentPaddingPx = 16f,
        nodeWidthPx = 18f,
        nodeMinHeightPx = 12f,
        nodeGapPx = 12f,
        columnGapPx = 48f,
    )

    @Test
    fun compute_infersStagesAndBuildsRenderableLayout() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("source", "Source", 1),
                SankeyNode("mid", "Mid", 2),
                SankeyNode("sink", "Sink", 3),
            ),
            links = listOf(
                SankeyLink("source", "mid", 10.0),
                SankeyLink("mid", "sink", 10.0),
            ),
            config = config,
            styleOptions = SankeyChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(3, result.stageCount)
        assertEquals(3, result.nodeLayouts.size)
        assertEquals(2, result.linkLayouts.size)
        assertEquals(0, result.nodeLayouts.first { it.node.id == "source" }.stage)
        assertEquals(1, result.nodeLayouts.first { it.node.id == "mid" }.stage)
        assertEquals(2, result.nodeLayouts.first { it.node.id == "sink" }.stage)
    }

    @Test
    fun compute_respectsExplicitStages() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("a", "A", 1, stage = 0),
                SankeyNode("b", "B", 2, stage = 2),
                SankeyNode("c", "C", 3),
            ),
            links = listOf(
                SankeyLink("a", "c", 5.0),
                SankeyLink("c", "b", 5.0),
            ),
            config = config,
            styleOptions = SankeyChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.nodeLayouts.first { it.node.id == "b" }.stage)
    }

    @Test
    fun compute_rejectsCycles() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("a", "A", 1),
                SankeyNode("b", "B", 2),
            ),
            links = listOf(
                SankeyLink("a", "b", 1.0),
                SankeyLink("b", "a", 1.0),
            ),
            config = config,
            styleOptions = SankeyChartStyleOptions(),
        )

        assertFalse(result.isRenderable)
        assertEquals(0, result.nodeLayouts.size)
    }

    @Test
    fun compute_filtersInvalidLinks_andHitTestingWorks() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("left", "Left", 1),
                SankeyNode("right", "Right", 2),
            ),
            links = listOf(
                SankeyLink("left", "right", 12.0),
                SankeyLink("left", "missing", 8.0),
            ),
            config = config,
            styleOptions = SankeyChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.linkLayouts.size)

        val node = result.nodeLayouts.first()
        val hitNode = SankeyChartLayoutEngine.hitTestNode(result, node.left + 2f, node.top + 2f)
        assertEquals(node.originalIndex, hitNode)

        val link = result.linkLayouts.first()
        val hitLink = SankeyChartLayoutEngine.hitTestLink(
            layout = result,
            x = (link.sourceRight + link.targetLeft) * 0.5f,
            y = (link.sourceCenterY + link.targetCenterY) * 0.5f,
            tolerancePx = 6f,
        )
        assertNotNull(hitLink)
    }
}
