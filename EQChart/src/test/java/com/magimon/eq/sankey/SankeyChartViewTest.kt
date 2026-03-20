package com.magimon.eq.sankey

import android.os.Looper
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
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

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

    @Test
    fun sankeyChart_animatesRenderableGraph_andClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = SankeyChartView(context)

        view.setPresentationOptions(
            SankeyChartPresentationOptions(
                animateOnDataChange = true,
                animationDurationMs = 16L,
                showNodeLabels = true,
                showLinkValues = true,
            ),
        )
        view.setNodes(
            listOf(
                SankeyNode("a", "A", 0xFF2B80FF.toInt()),
                SankeyNode("b", "B", 0xFF13C3A3.toInt()),
                SankeyNode("c", "C", 0xFFFF7043.toInt()),
            ),
        )
        view.setLinks(
            listOf(
                SankeyLink("a", "b", 12.0, label = "12"),
                SankeyLink("b", "c", 7.0, label = "7"),
            ),
        )

        layoutAndDraw(view, width = 440, height = 320)
        shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)

        val layout = view.readPrivate<SankeyChartLayoutResult>("layoutResult")
        val node = layout.nodeLayouts.first()
        touchUp(view, node.left + 4f, node.top + 4f)
        assertEquals(node.originalIndex, view.readPrivate<Int?>("selectedNodeIndex"))

        touchUp(view, 2f, 2f)
        assertEquals(1f, view.readPrivate<Float>("renderProgress"), 0.0001f)
        assertEquals(null, view.readPrivate<Int?>("selectedNodeIndex"))
        assertEquals(null, view.readPrivate<Int?>("selectedLinkIndex"))
    }
}
