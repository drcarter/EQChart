package com.magimon.eq.compose.cycle

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleLink
import com.magimon.eq.cycle.CycleNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CycleChartLogicTest {

    private val nodes = listOf(
        CycleNode("plan", "Plan", 0xFF2B80FF.toInt(), payload = "plan"),
        CycleNode("build", "Build", 0xFF13C3A3.toInt(), payload = "build"),
        CycleNode("measure", "Measure", 0xFFFF9F1C.toInt(), payload = "measure"),
        CycleNode("learn", "Learn", 0xFF8A79FF.toInt(), payload = "learn"),
    )

    private val links = listOf(
        CycleLink("plan", "build", 18.0, label = "18", payload = "plan-build"),
        CycleLink("build", "measure", 12.0, payload = "build-measure"),
        CycleLink("measure", "learn", 9.0, payload = "measure-learn"),
        CycleLink("learn", "plan", 14.0, payload = "learn-plan"),
    )

    @Test
    fun computeCycleLayout_buildsRenderableLayout_withDpConfig() {
        val layout = computeCycleLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 280f,
            density = Density(1f, 1f),
            styleOptions = CycleChartStyleOptions(),
            presentationOptions = CycleChartPresentationOptions(),
        )

        assertTrue(layout.isRenderable)
        assertEquals(4, layout.nodeLayouts.size)
        assertEquals(4, layout.linkLayouts.size)
        assertTrue(layout.orbitRadius > 0f)
    }

    @Test
    fun resolveCycleTapSelection_prefersNode_thenLink_thenEmpty() {
        val layout = computeCycleLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 280f,
            density = Density(1f, 1f),
            styleOptions = CycleChartStyleOptions(),
            presentationOptions = CycleChartPresentationOptions(),
        )
        val firstNode = layout.nodeLayouts.first()
        val firstLink = layout.linkLayouts.first()

        val nodeSelection = resolveCycleTapSelection(
            layout = layout,
            tap = Offset(firstNode.centerX, firstNode.centerY),
            linkHitTolerancePx = 8f,
        )
        val linkSelection = resolveCycleTapSelection(
            layout = layout,
            tap = Offset(firstLink.midX, firstLink.midY),
            linkHitTolerancePx = 8f,
        )
        val emptySelection = resolveCycleTapSelection(
            layout = layout,
            tap = Offset(0f, 0f),
            linkHitTolerancePx = 8f,
        )

        assertEquals(firstNode.originalIndex, nodeSelection.selectedNodeIndex)
        assertNull(nodeSelection.selectedLinkIndex)
        assertEquals(firstLink.originalIndex, linkSelection.selectedLinkIndex)
        assertNull(linkSelection.selectedNodeIndex)
        assertNull(emptySelection.selectedNodeIndex)
        assertNull(emptySelection.selectedLinkIndex)
    }

    @Test
    fun renderHelpers_followSelectionState() {
        val layout = computeCycleLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 280f,
            density = Density(1f, 1f),
            styleOptions = CycleChartStyleOptions(),
            presentationOptions = CycleChartPresentationOptions(linkAlpha = 0.5f),
        )
        val node = layout.nodeLayouts.first()
        val link = layout.linkLayouts.first()

        assertTrue(cycleNodeHighlighted(node.originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertTrue(cycleLinkHighlighted(link.originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertFalse(cycleNodeHighlighted(layout.nodeLayouts.last().originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null))
        assertEquals(1f, cycleNodeFillAlpha(node.originalIndex, layout, selectedNodeIndex = node.originalIndex, selectedLinkIndex = null, renderProgress = 1f), 0f)
        assertEquals(0.25f, cycleLinkStrokeAlpha(link.originalIndex, layout, CycleChartPresentationOptions(linkAlpha = 0.5f), selectedNodeIndex = null, selectedLinkIndex = null, renderProgress = 0.5f), 0.001f)
        assertEquals(0.32f, cycleLinkLabelAlpha(link.originalIndex, layout, selectedNodeIndex = null, selectedLinkIndex = 999), 0.001f)
    }

    @Test
    fun arrowHeadAndFormatting_helpers_work() {
        val layout = computeCycleLayout(
            nodes = nodes,
            links = links,
            widthPx = 360f,
            heightPx = 280f,
            density = Density(1f, 1f),
            styleOptions = CycleChartStyleOptions(),
            presentationOptions = CycleChartPresentationOptions(),
        )
        val link = layout.linkLayouts.first()
        val arrowHead = cycleArrowHead(link, arrowSizePx = 8f)
        val midpoint = cycleQuadraticPoint(link, t = 0.5f)
        val tinted = cycleApplyAlpha(0x80FF0000.toInt(), 2f)
        val transparent = cycleApplyAlpha(0x80FF0000.toInt(), -1f)

        assertNotNull(arrowHead)
        assertEquals(link.midX, midpoint.x, 0.01f)
        assertEquals(link.midY, midpoint.y, 0.01f)
        assertEquals("18", cycleLinkLabel(link))
        assertEquals(0x80, Color.alpha(tinted))
        assertEquals(0, Color.alpha(transparent))
    }
}
