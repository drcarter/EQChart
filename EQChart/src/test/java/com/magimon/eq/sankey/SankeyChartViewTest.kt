package com.magimon.eq.sankey

import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SankeyChartViewTest {

    @Test
    fun sankeyChart_dispatchesNodeAndLinkClicks() {
        val context = RuntimeEnvironment.getApplication()
        val view = SankeyChartView(context)
        var clickedNode: Int? = null
        var clickedLink: Int? = null

        view.setPresentationOptions(
            SankeyChartPresentationOptions(
                animateOnDataChange = false,
                showNodeLabels = true,
                showLinkValues = true,
            ),
        )
        view.setOnNodeClickListener { nodeIndex, _, _ -> clickedNode = nodeIndex }
        view.setOnLinkClickListener { linkIndex, _, _ -> clickedLink = linkIndex }
        view.setNodes(
            listOf(
                SankeyNode("a", "A", 0xFF2B80FF.toInt()),
                SankeyNode("b", "B", 0xFF13C3A3.toInt()),
            ),
        )
        view.setLinks(listOf(SankeyLink("a", "b", 12.0, payload = "flow")))

        layoutAndDraw(view, width = 420, height = 300)

        val layout = view.readPrivate<SankeyChartLayoutResult>("layoutResult")
        val node = layout.nodeLayouts.first()
        touchUp(view, node.left + 4f, node.top + 4f)

        assertEquals(node.originalIndex, clickedNode)

        val link = layout.linkLayouts.first()
        touchUp(view, (link.sourceRight + link.targetLeft) * 0.5f, (link.sourceCenterY + link.targetCenterY) * 0.5f)

        assertEquals(link.originalIndex, clickedLink)
        assertNotNull(view.readPrivate<Int?>("selectedLinkIndex"))
    }

    @Test
    fun sankeyChart_marksInvalidCyclesAsEmpty() {
        val context = RuntimeEnvironment.getApplication()
        val view = SankeyChartView(context)

        view.setPresentationOptions(SankeyChartPresentationOptions(animateOnDataChange = false, emptyText = "Empty"))
        view.setNodes(
            listOf(
                SankeyNode("a", "A", 1),
                SankeyNode("b", "B", 2),
            ),
        )
        view.setLinks(
            listOf(
                SankeyLink("a", "b", 1.0),
                SankeyLink("b", "a", 1.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 260)

        val layout = view.readPrivate<SankeyChartLayoutResult>("layoutResult")
        assertFalse(layout.isRenderable)
        assertTrue(layout.nodeLayouts.isEmpty())
    }
}
