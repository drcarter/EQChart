package com.magimon.eq.treemap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreemapChartLayoutEngineTest {

    private fun config() = TreemapChartLayoutConfig(
        widthPx = 480f,
        heightPx = 320f,
        contentPaddingPx = 6f,
        groupGapPx = 3f,
        tileGapPx = 2f,
        headerHeightPx = 20f,
    )

    @Test
    fun resolveTreemapChartLayout_singleGroupCreatesFlatLayout() {
        val result = resolveTreemapChartLayout(
            groups = listOf(
                TreemapGroup(
                    label = "Revenue",
                    color = 0xFF2563EB.toInt(),
                    items = listOf(
                        TreemapItem("Subscriptions", 40.0),
                        TreemapItem("Services", 25.0),
                    ),
                ),
            ),
            config = config(),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.itemLayouts.size)
        assertTrue(result.groupHeaders.isEmpty())
    }

    @Test
    fun resolveTreemapChartLayout_multiGroupBuildsHeadersAndFiltersInvalidItems() {
        val result = resolveTreemapChartLayout(
            groups = listOf(
                TreemapGroup(
                    label = "Growth",
                    color = 0xFF2563EB.toInt(),
                    items = listOf(
                        TreemapItem("Paid", 18.0),
                        TreemapItem("Organic", 0.0),
                        TreemapItem("", 5.0),
                    ),
                ),
                TreemapGroup(
                    label = "Ops",
                    color = null,
                    items = listOf(
                        TreemapItem("Support", 12.0, supportingText = "Q2"),
                    ),
                ),
            ),
            config = config(),
        )

        assertTrue(result.isRenderable)
        assertEquals(2, result.itemLayouts.size)
        assertEquals(2, result.groupHeaders.size)
        assertTrue(result.itemLayouts.all { it.rect.width > 0f && it.rect.height > 0f })
    }

    @Test
    fun resolveTreemapChartLayout_skipsHeadersWhenGroupHeightIsTooSmall() {
        val result = resolveTreemapChartLayout(
            groups = listOf(
                TreemapGroup("A", items = listOf(TreemapItem("one", 10.0))),
                TreemapGroup("B", items = listOf(TreemapItem("two", 10.0))),
                TreemapGroup("C", items = listOf(TreemapItem("three", 10.0))),
            ),
            config = config().copy(heightPx = 48f),
        )

        assertTrue(result.isRenderable)
        assertTrue(result.groupHeaders.size <= 1)
        assertEquals(3, result.itemLayouts.size)
    }

    @Test
    fun resolveTreemapItemColor_usesStablePrecedenceRules() {
        val palette = listOf(0xFF111111.toInt(), 0xFF222222.toInt())
        val explicit = resolveTreemapItemColor(
            groupColor = 0xFF00FF00.toInt(),
            item = TreemapItem("Alpha", 10.0, color = 0xFFABCDEF.toInt()),
            palette = palette,
        )
        val derived = resolveTreemapItemColor(
            groupColor = 0xFF00FF00.toInt(),
            item = TreemapItem("Alpha", 10.0),
            palette = palette,
        )
        val fallback = resolveTreemapItemColor(
            groupColor = null,
            item = TreemapItem("Beta", 10.0),
            palette = palette,
        )

        assertEquals(0xFFABCDEF.toInt(), explicit)
        assertEquals(derived, resolveTreemapItemColor(0xFF00FF00.toInt(), TreemapItem("Alpha", 10.0), palette))
        assertEquals(fallback, resolveTreemapItemColor(null, TreemapItem("Beta", 10.0), palette))
        assertFalse(derived == fallback)
    }

    @Test
    fun resolveTreemapChartLayout_returnsNonRenderableForInvalidInput() {
        val result = resolveTreemapChartLayout(
            groups = listOf(
                TreemapGroup(
                    label = "Bad",
                    items = listOf(
                        TreemapItem("Zero", 0.0),
                        TreemapItem("NaN", Double.NaN),
                    ),
                ),
            ),
            config = config(),
        )

        assertFalse(result.isRenderable)
        assertTrue(result.itemLayouts.isEmpty())
        assertEquals("no valid groups", result.emptyReason)
    }
}
