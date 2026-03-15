package com.magimon.eq.common

import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SankeyContractsTest {

    @Test
    fun sankeyNode_preservesPropertiesAndCopy() {
        val payload = mutableMapOf("kind" to "node")
        val defaultNode = SankeyNode(id = "a", label = "A", color = 1)
        val node = SankeyNode(id = "b", label = "B", color = 2, stage = 3, payload = payload)
        val copied = node.copy(label = "B2")

        assertEquals("a", defaultNode.id)
        assertNull(defaultNode.stage)
        assertNull(defaultNode.payload)
        assertEquals("b", node.id)
        assertEquals(3, node.stage)
        assertSame(payload, node.payload)
        assertEquals("B2", copied.label)
    }

    @Test
    fun sankeyLink_preservesProperties() {
        val payload = mutableListOf("flow")
        val link = SankeyLink("a", "b", 42.0, color = 3, label = "AB", payload = payload)

        assertEquals("a", link.sourceId)
        assertEquals("b", link.targetId)
        assertEquals(42.0, link.value, 0.0)
        assertEquals(3, link.color)
        assertEquals("AB", link.label)
        assertSame(payload, link.payload)
    }

    @Test
    fun sankeyPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = SankeyChartPresentationOptions()
        val custom = SankeyChartPresentationOptions(
            showNodeLabels = false,
            showLinkValues = true,
            animateOnDataChange = false,
            animationDurationMs = 420L,
            nodeGapDp = 8f,
            columnGapDp = 40f,
            linkAlpha = 0.55f,
            emptyText = "EMPTY",
        )

        assertTrue(defaults.showNodeLabels)
        assertEquals(false, defaults.showLinkValues)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(650L, defaults.animationDurationMs)
        assertEquals(12f, defaults.nodeGapDp, 0.0f)
        assertEquals(48f, defaults.columnGapDp, 0.0f)
        assertEquals(0.38f, defaults.linkAlpha, 0.0f)
        assertEquals("No data", defaults.emptyText)

        assertEquals(false, custom.showNodeLabels)
        assertEquals(true, custom.showLinkValues)
        assertEquals(false, custom.animateOnDataChange)
        assertEquals(420L, custom.animationDurationMs)
        assertEquals(8f, custom.nodeGapDp, 0.0f)
        assertEquals(40f, custom.columnGapDp, 0.0f)
        assertEquals(0.55f, custom.linkAlpha, 0.0f)
        assertEquals("EMPTY", custom.emptyText)
    }

    @Test
    fun sankeyStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = SankeyChartStyleOptions()
        val custom = SankeyChartStyleOptions(
            backgroundColor = 1,
            nodeWidthDp = 20f,
            nodeMinHeightDp = 14f,
            nodeCornerRadiusDp = 8f,
            nodeStrokeColor = 2,
            nodeStrokeWidthDp = 2f,
            defaultLinkColor = 3,
            nodeLabelTextColor = 4,
            nodeLabelTextSizeSp = 13f,
            linkValueTextColor = 5,
            linkValueTextSizeSp = 12f,
            selectedStrokeColor = 6,
            selectedStrokeWidthDp = 3f,
            contentPaddingDp = 18f,
        )

        assertEquals(18f, defaults.nodeWidthDp, 0.0f)
        assertEquals(12f, defaults.nodeMinHeightDp, 0.0f)
        assertEquals(6f, defaults.nodeCornerRadiusDp, 0.0f)
        assertEquals(1f, defaults.nodeStrokeWidthDp, 0.0f)
        assertEquals(12f, defaults.nodeLabelTextSizeSp, 0.0f)
        assertEquals(11f, defaults.linkValueTextSizeSp, 0.0f)
        assertEquals(2f, defaults.selectedStrokeWidthDp, 0.0f)
        assertEquals(16f, defaults.contentPaddingDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(20f, custom.nodeWidthDp, 0.0f)
        assertEquals(14f, custom.nodeMinHeightDp, 0.0f)
        assertEquals(8f, custom.nodeCornerRadiusDp, 0.0f)
        assertEquals(2, custom.nodeStrokeColor)
        assertEquals(2f, custom.nodeStrokeWidthDp, 0.0f)
        assertEquals(3, custom.defaultLinkColor)
        assertEquals(4, custom.nodeLabelTextColor)
        assertEquals(13f, custom.nodeLabelTextSizeSp, 0.0f)
        assertEquals(5, custom.linkValueTextColor)
        assertEquals(12f, custom.linkValueTextSizeSp, 0.0f)
        assertEquals(6, custom.selectedStrokeColor)
        assertEquals(3f, custom.selectedStrokeWidthDp, 0.0f)
        assertEquals(18f, custom.contentPaddingDp, 0.0f)
    }
}
