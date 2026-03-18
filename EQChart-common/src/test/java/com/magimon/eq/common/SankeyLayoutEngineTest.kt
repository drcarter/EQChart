package com.magimon.eq.common

import com.magimon.eq.sankey.SankeyChartLayoutConfig
import com.magimon.eq.sankey.SankeyChartLayoutEngine
import com.magimon.eq.sankey.SankeyChartStyleOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

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
        assertEquals(node.originalIndex, SankeyChartLayoutEngine.hitTestNode(result, node.left, node.top))
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, node.left - 2f, node.top + 2f))
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, node.left + 2f, node.top - 2f))
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, node.left + 2f, node.bottom + 2f))

        val link = result.linkLayouts.first()
        val hitLink = SankeyChartLayoutEngine.hitTestLink(
            layout = result,
            x = (link.sourceRight + link.targetLeft) * 0.5f,
            y = (link.sourceCenterY + link.targetCenterY) * 0.5f,
            tolerancePx = 6f,
        )
        assertNotNull(hitLink)
    }

    @Test
    fun compute_rejectsInvalidInputShapes() {
        val nodes = listOf(
            SankeyNode("source", "Source", 1),
            SankeyNode("target", "Target", 2),
        )
        val links = listOf(SankeyLink("source", "target", 10.0))
        val style = SankeyChartStyleOptions()

        assertEquals(
            "non-positive size",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "node width",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(nodeWidthPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "non-positive size",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 100f, heightPx = 0f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "blank node id",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(SankeyNode(" ", "Source", 1), SankeyNode("target", "Target", 2)),
                links = listOf(SankeyLink("target", "target", 1.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "duplicate node id",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(SankeyNode("dup", "A", 1), SankeyNode("dup", "B", 2)),
                links = listOf(SankeyLink("dup", "dup", 1.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "negative stage",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(SankeyNode("source", "Source", 1, stage = -1), SankeyNode("target", "Target", 2)),
                links = links,
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(SankeyLink("source", "missing", 10.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(SankeyLink("missing", "target", 10.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(SankeyLink("source", "target", Double.NaN)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "no valid links",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = listOf(SankeyLink("source", "target", 0.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "self cycle",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(SankeyNode("source", "Source", 1)),
                links = listOf(SankeyLink("source", "source", 10.0)),
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "backward explicit stage",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(
                    SankeyNode("source", "Source", 1, stage = 0),
                    SankeyNode("target", "Target", 2, stage = 0),
                ),
                links = links,
                config = config,
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "insufficient size",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 10f, heightPx = 10f, contentPaddingPx = 8f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "insufficient size",
            SankeyChartLayoutEngine.compute(
                nodes = nodes,
                links = links,
                config = config.copy(widthPx = 20f, heightPx = 100f, contentPaddingPx = 15f),
                styleOptions = style,
            ).emptyReason,
        )
        assertEquals(
            "invalid scale",
            SankeyChartLayoutEngine.compute(
                nodes = listOf(
                    SankeyNode("left-1", "Left 1", 1, stage = 0),
                    SankeyNode("left-2", "Left 2", 2, stage = 0),
                    SankeyNode("right", "Right", 3, stage = 1),
                ),
                links = listOf(
                    SankeyLink("left-1", "right", 10.0),
                    SankeyLink("left-2", "right", 10.0),
                ),
                config = config.copy(heightPx = 40f, nodeGapPx = 20f),
                styleOptions = style,
            ).emptyReason,
        )
    }

    @Test
    fun compute_relaxesDenseStages_scalesHeights_andUsesFallbackColors() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("s1", "S1", 0xFF112233.toInt()),
                SankeyNode("s2", "S2", 0xFF445566.toInt()),
                SankeyNode("t1", "T1", 0xFF778899.toInt()),
                SankeyNode("t2", "T2", 0xFF99AABB.toInt()),
            ),
            links = listOf(
                SankeyLink("s1", "t1", 8.0, color = 0),
                SankeyLink("s2", "t1", 6.0),
                SankeyLink("s2", "t2", 4.0),
            ),
            config = config.copy(
                widthPx = 120f,
                heightPx = 36f,
                contentPaddingPx = 0f,
                nodeMinHeightPx = 10f,
                nodeGapPx = 2f,
                columnGapPx = 24f,
            ),
            styleOptions = SankeyChartStyleOptions(defaultLinkColor = 0xFF00AAFF.toInt()),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.stageCount)
        assertEquals(4, result.nodeLayouts.size)
        assertEquals(3, result.linkLayouts.size)
        assertTrue(result.nodeLayouts.all { node -> node.centerY == (node.top + node.bottom) * 0.5f })
        assertTrue(result.linkLayouts.all { it.thickness >= 2f })
        assertEquals(0xFF00AAFF.toInt(), result.linkLayouts.first { it.originalIndex == 0 }.color)
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, -1f, -1f))
        val firstNode = result.nodeLayouts.first()
        val maxBottom = result.nodeLayouts.maxOf { it.bottom }
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, firstNode.left + 1f, maxBottom + 10f))
        assertNull(SankeyChartLayoutEngine.hitTestNode(result, firstNode.right + 10f, firstNode.top + 1f))
        assertNull(SankeyChartLayoutEngine.hitTestLink(result, x = -100f, y = -100f, tolerancePx = 1f))
    }

    @Test
    fun hitTestLink_supportsZeroLengthSegmentsWhenColumnsTouch() {
        val result = SankeyChartLayoutEngine.compute(
            nodes = listOf(
                SankeyNode("source", "Source", 1),
                SankeyNode("target", "Target", 2),
            ),
            links = listOf(SankeyLink("source", "target", 10.0)),
            config = config.copy(
                widthPx = 36f,
                heightPx = 40f,
                contentPaddingPx = 0f,
                nodeWidthPx = 18f,
                nodeGapPx = 0f,
                columnGapPx = 100f,
            ),
            styleOptions = SankeyChartStyleOptions(),
        )

        assertTrue(result.isRenderable)
        assertEquals(1, result.linkLayouts.size)

        val link = result.linkLayouts.single()
        val hit = SankeyChartLayoutEngine.hitTestLink(
            layout = result,
            x = link.sourceRight,
            y = link.sourceCenterY,
            tolerancePx = 2f,
        )

        assertEquals(link.originalIndex, hit)
        assertEquals(link.sourceCenterY, link.targetCenterY, 0.0001f)
    }

    @Test
    fun privateHelpers_coverOverflowAndExtraClampBranches() {
        invokeRelaxStage(
            stage = 9,
            stageGroups = emptyMap(),
            compactNodes = emptyList(),
            links = emptyList(),
            nodeGapPx = 0f,
            availableHeightPx = 10f,
            contentPaddingPx = 0f,
            useIncoming = false,
        )

        val nodes = mutableListOf(
            newMutableNode(
                originalIndex = 0,
                node = SankeyNode("a", "A", 1),
                stage = 1,
                totalValue = 10.0,
                top = 0f,
                bottom = 50f,
            ),
            newMutableNode(
                originalIndex = 1,
                node = SankeyNode("b", "B", 2),
                stage = 1,
                totalValue = 10.0,
                top = -20f,
                bottom = 20f,
            ),
        )

        invokeRelaxStage(
            stage = 1,
            stageGroups = mapOf(1 to listOf(0, 1)),
            compactNodes = nodes,
            links = emptyList(),
            nodeGapPx = 2f,
            availableHeightPx = 20f,
            contentPaddingPx = 10f,
            useIncoming = false,
        )

        val firstTop = readFloat(nodes[0], "top")
        val firstBottom = readFloat(nodes[0], "bottom")
        val secondTop = readFloat(nodes[1], "top")
        val secondBottom = readFloat(nodes[1], "bottom")

        val tops = listOf(firstTop, secondTop).sorted()
        val bottoms = listOf(firstBottom, secondBottom).sorted()
        assertEquals(2, tops.distinct().size)
        assertEquals(2, bottoms.distinct().size)
        assertTrue(tops[1] >= tops[0])
        assertTrue(bottoms[1] >= bottoms[0])

        val heights = invokeFitHeightsToStage(
            values = listOf(1.0, 1.0),
            valueScale = 10f,
            minHeightPx = 10f,
            gapPx = 0f,
            availableHeightPx = 3f,
        )

        assertEquals(2, heights.size)
        assertEquals(2f, heights[0], 0.0001f)
        assertEquals(2f, heights[1], 0.0001f)
        assertTrue(invokeFitHeightsToStage(emptyList(), 1f, 1f, 0f, 1f).isEmpty())
    }

    private fun invokeRelaxStage(
        stage: Int,
        stageGroups: Map<Int, List<Int>>,
        compactNodes: List<Any>,
        links: List<Any>,
        nodeGapPx: Float,
        availableHeightPx: Float,
        contentPaddingPx: Float,
        useIncoming: Boolean,
    ) {
        val method = privateMethod(
            "relaxStage",
            Int::class.javaPrimitiveType!!,
            Map::class.java,
            List::class.java,
            List::class.java,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
        )
        method.invoke(
            SankeyChartLayoutEngine,
            stage,
            stageGroups,
            compactNodes,
            links,
            nodeGapPx,
            availableHeightPx,
            contentPaddingPx,
            useIncoming,
        )
    }

    private fun invokeFitHeightsToStage(
        values: List<Double>,
        valueScale: Float,
        minHeightPx: Float,
        gapPx: Float,
        availableHeightPx: Float,
    ): FloatArray {
        val method = privateMethod(
            "fitHeightsToStage",
            List::class.java,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
        )
        @Suppress("UNCHECKED_CAST")
        return method.invoke(
            SankeyChartLayoutEngine,
            values,
            valueScale,
            minHeightPx,
            gapPx,
            availableHeightPx,
        ) as FloatArray
    }

    private fun privateMethod(name: String, vararg parameterTypes: Class<*>): Method {
        return SankeyChartLayoutEngine::class.java.getDeclaredMethod(name, *parameterTypes).apply {
            isAccessible = true
        }
    }

    private fun newMutableNode(
        originalIndex: Int,
        node: SankeyNode,
        stage: Int,
        totalValue: Double,
        top: Float,
        bottom: Float,
    ): Any {
        val clazz = Class.forName("com.magimon.eq.sankey.SankeyChartLayoutEngine\$MutableNode")
        val constructor = clazz.getDeclaredConstructor(
            Int::class.javaPrimitiveType!!,
            SankeyNode::class.java,
            Int::class.javaPrimitiveType!!,
            Double::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
        ).apply {
            isAccessible = true
        }
        return constructor.newInstance(originalIndex, node, stage, totalValue, top, bottom, 0f, 0f)
    }

    private fun newMutableLink(
        originalIndex: Int,
        link: SankeyLink,
        sourceIndex: Int,
        targetIndex: Int,
        value: Double,
        color: Int,
    ): Any {
        val clazz = Class.forName("com.magimon.eq.sankey.SankeyChartLayoutEngine\$MutableLink")
        val constructor = clazz.getDeclaredConstructor(
            Int::class.javaPrimitiveType!!,
            SankeyLink::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Double::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
        ).apply {
            isAccessible = true
        }
        return constructor.newInstance(originalIndex, link, sourceIndex, targetIndex, value, color, 0f, 0f, 0f, 0f)
    }

    private fun readFloat(instance: Any, fieldName: String): Float {
        return instance.javaClass.getDeclaredField(fieldName).apply {
            isAccessible = true
        }.getFloat(instance)
    }
}
