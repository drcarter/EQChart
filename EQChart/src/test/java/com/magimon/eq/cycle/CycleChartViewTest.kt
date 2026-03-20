package com.magimon.eq.cycle

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
class CycleChartViewTest {

    @Test
    fun cycleChart_dispatchesNodeAndLinkClicks() {
        val context = RuntimeEnvironment.getApplication()
        val view = CycleChartView(context)
        var clickedNode: Int? = null
        var clickedLink: Int? = null

        view.setPresentationOptions(
            CycleChartPresentationOptions(
                animateOnDataChange = false,
                showNodeLabels = true,
                showLinkLabels = true,
            ),
        )
        view.setOnNodeClickListener { nodeIndex, _, _ -> clickedNode = nodeIndex }
        view.setOnLinkClickListener { linkIndex, _, _ -> clickedLink = linkIndex }
        view.setNodes(
            listOf(
                CycleNode("plan", "Plan", 0xFF2B80FF.toInt(), payload = "plan"),
                CycleNode("build", "Build", 0xFF13C3A3.toInt(), payload = "build"),
                CycleNode("measure", "Measure", 0xFFFF9F1C.toInt(), payload = "measure"),
                CycleNode("learn", "Learn", 0xFF8A79FF.toInt(), payload = "learn"),
            ),
        )
        view.setLinks(
            listOf(
                CycleLink("plan", "build", 18.0, label = "18", payload = "plan-build"),
                CycleLink("build", "measure", 12.0, payload = "build-measure"),
                CycleLink("measure", "learn", 9.0, payload = "measure-learn"),
                CycleLink("learn", "plan", 14.0, payload = "learn-plan"),
            ),
        )

        layoutAndDraw(view, width = 440, height = 360)

        val layout = view.readPrivate<CycleChartLayoutResult>("layoutResult")
        val node = layout.nodeLayouts.first()
        touchUp(view, node.centerX, node.centerY)
        assertEquals(node.originalIndex, clickedNode)

        val link = layout.linkLayouts.first()
        touchUp(view, link.midX, link.midY)
        assertEquals(link.originalIndex, clickedLink)
        assertEquals(link.originalIndex, view.readPrivate<Int?>("selectedLinkIndex"))
    }

    @Test
    fun cycleChart_marksInvalidInputAsEmpty() {
        val context = RuntimeEnvironment.getApplication()
        val view = CycleChartView(context)

        view.setPresentationOptions(
            CycleChartPresentationOptions(
                animateOnDataChange = false,
                emptyText = "Empty",
            ),
        )
        view.setNodes(
            listOf(
                CycleNode("plan", "Plan", 1),
                CycleNode("build", "Build", 2),
            ),
        )
        view.setLinks(
            listOf(
                CycleLink("plan", "plan", 10.0),
            ),
        )

        layoutAndDraw(view, width = 360, height = 280)

        val layout = view.readPrivate<CycleChartLayoutResult>("layoutResult")
        assertFalse(layout.isRenderable)
        assertTrue(layout.nodeLayouts.isEmpty())
        assertTrue(layout.linkLayouts.isEmpty())
    }

    @Test
    fun cycleChart_animatesRenderableGraph_andClearsSelectionOnMiss() {
        val context = RuntimeEnvironment.getApplication()
        val view = CycleChartView(context)

        view.setPresentationOptions(
            CycleChartPresentationOptions(
                animateOnDataChange = true,
                animationDurationMs = 16L,
                showNodeLabels = true,
                showLinkLabels = true,
            ),
        )
        view.setNodes(
            listOf(
                CycleNode("plan", "Plan", 0xFF2B80FF.toInt()),
                CycleNode("build", "Build", 0xFF13C3A3.toInt()),
                CycleNode("measure", "Measure", 0xFFFF9F1C.toInt()),
                CycleNode("learn", "Learn", 0xFF8A79FF.toInt()),
            ),
        )
        view.setLinks(
            listOf(
                CycleLink("plan", "build", 18.0, label = "18"),
                CycleLink("build", "measure", 12.0, label = "12"),
                CycleLink("measure", "learn", 9.0, label = "9"),
                CycleLink("learn", "plan", 14.0, label = "14"),
            ),
        )

        layoutAndDraw(view, width = 440, height = 360)
        shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)

        val layout = view.readPrivate<CycleChartLayoutResult>("layoutResult")
        val node = layout.nodeLayouts.first()
        touchUp(view, node.centerX, node.centerY)
        assertEquals(node.originalIndex, view.readPrivate<Int?>("selectedNodeIndex"))

        touchUp(view, 4f, 4f)
        assertEquals(1f, view.readPrivate<Float>("renderProgress"), 0.0001f)
        assertNull(view.readPrivate<Int?>("selectedNodeIndex"))
        assertNull(view.readPrivate<Int?>("selectedLinkIndex"))
    }
}
