package com.magimon.eq.compose.sankey

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SankeyChartLogicTest {

    private val nodes = listOf(
        SankeyNode("a", "A", 0xFF1E88E5.toInt(), payload = "node-a"),
        SankeyNode("b", "B", 0xFF43A047.toInt(), payload = "node-b"),
        SankeyNode("c", "C", 0xFFFB8C00.toInt(), payload = "node-c"),
    )

    private val links = listOf(
        SankeyLink("a", "b", 10.0, label = "10", payload = "link-ab"),
        SankeyLink("b", "c", 6.5, payload = "link-bc"),
    )

    @Test
    fun computeSankeyLayout_buildsRenderableLayout_withDpConfig() {
        val layout = computeSankeyLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 260f,
            density = Density(1f, 1f),
            styleOptions = SankeyChartStyleOptions(),
            presentationOptions = SankeyChartPresentationOptions(),
        )

        assertTrue(layout.isRenderable)
        assertEquals(3, layout.nodeLayouts.size)
        assertEquals(2, layout.linkLayouts.size)
        assertEquals(3, layout.stageCount)
    }

    @Test
    fun resolveSankeyTapSelection_prefersNode_thenLink_thenEmpty() {
        val layout = computeSankeyLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 260f,
            density = Density(1f, 1f),
            styleOptions = SankeyChartStyleOptions(),
            presentationOptions = SankeyChartPresentationOptions(showNodeLabels = false, showLinkValues = false),
        )
        val firstNode = layout.nodeLayouts.first()
        val targetLink = layout.linkLayouts.last()

        val nodeSelection = resolveSankeyTapSelection(
            layout = layout,
            tap = Offset((firstNode.left + firstNode.right) * 0.5f, (firstNode.top + firstNode.bottom) * 0.5f),
            linkHitTolerancePx = 8f,
        )
        val linkSelection = resolveSankeyTapSelection(
            layout = layout,
            tap = sankeyLinkCenterPoint(targetLink, t = 0.1f),
            linkHitTolerancePx = 8f,
        )
        val emptySelection = resolveSankeyTapSelection(
            layout = layout,
            tap = Offset(0f, 0f),
            linkHitTolerancePx = 8f,
        )

        assertEquals(firstNode.originalIndex, nodeSelection.selectedNodeIndex)
        assertNull(nodeSelection.selectedLinkIndex)
        assertEquals(targetLink.originalIndex, linkSelection.selectedLinkIndex)
        assertNull(linkSelection.selectedNodeIndex)
        assertNull(emptySelection.selectedNodeIndex)
        assertNull(emptySelection.selectedLinkIndex)
    }

    @Test
    fun renderHelpers_followSelectionState() {
        val layout = computeSankeyLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 260f,
            density = Density(1f, 1f),
            styleOptions = SankeyChartStyleOptions(),
            presentationOptions = SankeyChartPresentationOptions(linkAlpha = 0.5f),
        )
        val node = layout.nodeLayouts.first()
        val link = layout.linkLayouts.first()

        assertTrue(sankeyNodeHighlighted(node.originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertTrue(sankeyLinkHighlighted(link.originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertFalse(sankeyNodeHighlighted(layout.nodeLayouts.last().originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertEquals(1f, sankeyNodeFillAlpha(node, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null, renderProgress = 1f), 0f)
        assertEquals(0.24f, sankeyLinkValueAlpha(link, selectedNodeIndex = null, selectedLinkIndex = 999, layout = layout, renderProgress = 1f), 0.001f)
        assertEquals("6.5", sankeyLinkLabel(layout.linkLayouts.last()))

        val color = sankeyLinkColor(
            link = link,
            layout = layout,
            presentationOptions = SankeyChartPresentationOptions(linkAlpha = 0.5f),
            selectedNodeIndex = null,
            selectedLinkIndex = null,
            renderProgress = 1f,
        )

        assertEquals(128f / 255f, color.alpha, 0.001f)
    }

    @Test
    fun alphaAndValueFormatting_helpers_clampAndFormat() {
        val tinted = withAlpha(0x80FF0000.toInt(), 2f)
        val transparent = withAlpha(0x80FF0000.toInt(), -1f)

        assertEquals(0x80, Color.alpha(tinted))
        assertEquals(0, Color.alpha(transparent))
        assertEquals("7", sankeyFormatValue(7.0))
        assertEquals("7.3", sankeyFormatValue(7.25))
    }
}
