package com.magimon.eq.common

import com.magimon.eq.cycle.CycleChartPresentationOptions
import com.magimon.eq.cycle.CycleChartStyleOptions
import com.magimon.eq.cycle.CycleLink
import com.magimon.eq.cycle.CycleLinkLayout
import com.magimon.eq.cycle.CycleNode
import com.magimon.eq.cycle.CycleNodeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleContractsTest {

    @Test
    fun cycleNode_preservesPropertiesAndCopy() {
        val payload = mutableMapOf("kind" to "node")
        val defaultNode = CycleNode(id = "a", label = "A", color = 1)
        val node = CycleNode(id = "b", label = "B", color = 2, payload = payload)
        val copied = node.copy(label = "B2")

        assertEquals("a", defaultNode.id)
        assertNull(defaultNode.payload)
        assertEquals("b", node.id)
        assertSame(payload, node.payload)
        assertEquals("B2", copied.label)
    }

    @Test
    fun cycleLink_preservesProperties() {
        val payload = mutableListOf("flow")
        val link = CycleLink("a", "b", 42.0, color = 3, label = "AB", payload = payload)

        assertEquals("a", link.sourceId)
        assertEquals("b", link.targetId)
        assertEquals(42.0, link.value, 0.0)
        assertEquals(3, link.color)
        assertEquals("AB", link.label)
        assertSame(payload, link.payload)
    }

    @Test
    fun cyclePresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = CycleChartPresentationOptions()
        val custom = CycleChartPresentationOptions(
            showNodeLabels = false,
            showLinkLabels = true,
            animateOnDataChange = false,
            animationDurationMs = 480L,
            startAngleDeg = 15f,
            clockwise = false,
            linkInnerRadiusFactor = 0.5f,
            linkAlpha = 0.6f,
            emptyText = "EMPTY",
        )

        assertTrue(defaults.showNodeLabels)
        assertEquals(false, defaults.showLinkLabels)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(650L, defaults.animationDurationMs)
        assertEquals(-90f, defaults.startAngleDeg, 0.0f)
        assertTrue(defaults.clockwise)
        assertEquals(0.34f, defaults.linkInnerRadiusFactor, 0.0f)
        assertEquals(0.42f, defaults.linkAlpha, 0.0f)
        assertEquals("No data", defaults.emptyText)

        assertEquals(false, custom.showNodeLabels)
        assertEquals(true, custom.showLinkLabels)
        assertEquals(false, custom.animateOnDataChange)
        assertEquals(480L, custom.animationDurationMs)
        assertEquals(15f, custom.startAngleDeg, 0.0f)
        assertEquals(false, custom.clockwise)
        assertEquals(0.5f, custom.linkInnerRadiusFactor, 0.0f)
        assertEquals(0.6f, custom.linkAlpha, 0.0f)
        assertEquals("EMPTY", custom.emptyText)
    }

    @Test
    fun cycleStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = CycleChartStyleOptions()
        val custom = CycleChartStyleOptions(
            backgroundColor = 1,
            nodeRadiusDp = 20f,
            nodeStrokeColor = 2,
            nodeStrokeWidthDp = 3f,
            defaultLinkColor = 4,
            linkMinThicknessDp = 5f,
            linkMaxThicknessDp = 12f,
            linkArrowSizeDp = 9f,
            linkInsetDp = 4f,
            nodeLabelTextColor = 6,
            nodeLabelTextSizeSp = 13f,
            linkLabelTextColor = 7,
            linkLabelTextSizeSp = 12f,
            selectedStrokeColor = 8,
            selectedStrokeWidthDp = 3f,
            contentPaddingDp = 16f,
        )

        assertEquals(24f, defaults.nodeRadiusDp, 0.0f)
        assertEquals(2f, defaults.nodeStrokeWidthDp, 0.0f)
        assertEquals(3f, defaults.linkMinThicknessDp, 0.0f)
        assertEquals(10f, defaults.linkMaxThicknessDp, 0.0f)
        assertEquals(8f, defaults.linkArrowSizeDp, 0.0f)
        assertEquals(3f, defaults.linkInsetDp, 0.0f)
        assertEquals(12f, defaults.nodeLabelTextSizeSp, 0.0f)
        assertEquals(11f, defaults.linkLabelTextSizeSp, 0.0f)
        assertEquals(2.5f, defaults.selectedStrokeWidthDp, 0.0f)
        assertEquals(18f, defaults.contentPaddingDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(20f, custom.nodeRadiusDp, 0.0f)
        assertEquals(2, custom.nodeStrokeColor)
        assertEquals(3f, custom.nodeStrokeWidthDp, 0.0f)
        assertEquals(4, custom.defaultLinkColor)
        assertEquals(5f, custom.linkMinThicknessDp, 0.0f)
        assertEquals(12f, custom.linkMaxThicknessDp, 0.0f)
        assertEquals(9f, custom.linkArrowSizeDp, 0.0f)
        assertEquals(4f, custom.linkInsetDp, 0.0f)
        assertEquals(6, custom.nodeLabelTextColor)
        assertEquals(13f, custom.nodeLabelTextSizeSp, 0.0f)
        assertEquals(7, custom.linkLabelTextColor)
        assertEquals(12f, custom.linkLabelTextSizeSp, 0.0f)
        assertEquals(8, custom.selectedStrokeColor)
        assertEquals(3f, custom.selectedStrokeWidthDp, 0.0f)
        assertEquals(16f, custom.contentPaddingDp, 0.0f)
    }

    @Test
    fun cycleLayoutModels_exposeAllProperties() {
        val node = CycleNode("plan", "Plan", 0xFF123456.toInt())
        val link = CycleLink("plan", "build", 12.5, color = 0xFF654321.toInt(), label = "12.5")
        val nodeLayout = CycleNodeLayout(
            originalIndex = 3,
            node = node,
            centerX = 100f,
            centerY = 120f,
            radius = 24f,
            angleDeg = -90f,
        )
        val linkLayout = CycleLinkLayout(
            originalIndex = 4,
            link = link,
            sourceNodeOriginalIndex = 1,
            targetNodeOriginalIndex = 2,
            startX = 10f,
            startY = 20f,
            controlX = 30f,
            controlY = 40f,
            endX = 50f,
            endY = 60f,
            midX = 35f,
            midY = 45f,
            thickness = 8f,
            color = 0xFFAA5500.toInt(),
        )
        val (
            nodeIndex,
            destructuredNode,
            centerX,
            centerY,
            radius,
            angleDeg,
        ) = nodeLayout
        val nodeLayoutCopy = nodeLayout.copy(radius = 26f)
        val (
            linkIndex,
            destructuredLink,
            sourceNodeIndex,
            targetNodeIndex,
            startX,
            startY,
            controlX,
            controlY,
            endX,
            endY,
            midX,
            midY,
            thickness,
            color,
        ) = linkLayout
        val linkLayoutCopy = linkLayout.copy(thickness = 9f)

        assertEquals(3, nodeLayout.originalIndex)
        assertSame(node, nodeLayout.node)
        assertEquals(100f, nodeLayout.centerX, 0.0f)
        assertEquals(120f, nodeLayout.centerY, 0.0f)
        assertEquals(24f, nodeLayout.radius, 0.0f)
        assertEquals(-90f, nodeLayout.angleDeg, 0.0f)
        assertEquals(76f, nodeLayout.left, 0.0f)
        assertEquals(96f, nodeLayout.top, 0.0f)
        assertEquals(124f, nodeLayout.right, 0.0f)
        assertEquals(144f, nodeLayout.bottom, 0.0f)
        assertEquals(3, nodeIndex)
        assertSame(node, destructuredNode)
        assertEquals(100f, centerX, 0.0f)
        assertEquals(120f, centerY, 0.0f)
        assertEquals(24f, radius, 0.0f)
        assertEquals(-90f, angleDeg, 0.0f)
        assertEquals(nodeLayout, nodeLayout.copy())
        assertEquals(nodeLayout.hashCode(), nodeLayout.copy().hashCode())
        assertNotEquals(nodeLayout, nodeLayoutCopy)
        assertEquals(26f, nodeLayoutCopy.radius, 0.0f)
        assertTrue(nodeLayout.toString().contains("centerX=100.0"))

        assertEquals(4, linkLayout.originalIndex)
        assertSame(link, linkLayout.link)
        assertEquals(1, linkLayout.sourceNodeOriginalIndex)
        assertEquals(2, linkLayout.targetNodeOriginalIndex)
        assertEquals(10f, linkLayout.startX, 0.0f)
        assertEquals(20f, linkLayout.startY, 0.0f)
        assertEquals(30f, linkLayout.controlX, 0.0f)
        assertEquals(40f, linkLayout.controlY, 0.0f)
        assertEquals(50f, linkLayout.endX, 0.0f)
        assertEquals(60f, linkLayout.endY, 0.0f)
        assertEquals(35f, linkLayout.midX, 0.0f)
        assertEquals(45f, linkLayout.midY, 0.0f)
        assertEquals(8f, linkLayout.thickness, 0.0f)
        assertEquals(0xFFAA5500.toInt(), linkLayout.color)
        assertEquals(4, linkIndex)
        assertSame(link, destructuredLink)
        assertEquals(1, sourceNodeIndex)
        assertEquals(2, targetNodeIndex)
        assertEquals(10f, startX, 0.0f)
        assertEquals(20f, startY, 0.0f)
        assertEquals(30f, controlX, 0.0f)
        assertEquals(40f, controlY, 0.0f)
        assertEquals(50f, endX, 0.0f)
        assertEquals(60f, endY, 0.0f)
        assertEquals(35f, midX, 0.0f)
        assertEquals(45f, midY, 0.0f)
        assertEquals(8f, thickness, 0.0f)
        assertEquals(0xFFAA5500.toInt(), color)
        assertEquals(linkLayout, linkLayout.copy())
        assertEquals(linkLayout.hashCode(), linkLayout.copy().hashCode())
        assertNotEquals(linkLayout, linkLayoutCopy)
        assertEquals(9f, linkLayoutCopy.thickness, 0.0f)
        assertTrue(linkLayout.toString().contains("sourceNodeOriginalIndex=1"))
    }
}
