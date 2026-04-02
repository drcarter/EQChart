package com.magimon.eq.compose.treemap

import com.magimon.eq.treemap.TreemapChartPresentationOptions
import com.magimon.eq.treemap.TreemapChartStyleOptions
import com.magimon.eq.treemap.TreemapGroup
import com.magimon.eq.treemap.TreemapItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TreemapChartTest {

    @Test
    fun computeTreemapLayout_buildsHeadersAndFlatLayouts() {
        val grouped = computeTreemapLayout(
            groups = listOf(
                TreemapGroup(
                    label = "Growth",
                    color = 0xFF2563EB.toInt(),
                    items = listOf(
                        TreemapItem("Paid", 18.0, supportingText = "Q2"),
                        TreemapItem("Organic", 12.0),
                    ),
                ),
                TreemapGroup(
                    label = "Ops",
                    items = listOf(TreemapItem("Support", 9.0)),
                ),
            ),
            widthPx = 720f,
            heightPx = 520f,
            density = 1f,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )
        val flat = computeTreemapLayout(
            groups = listOf(
                TreemapGroup(
                    label = "Revenue",
                    items = listOf(TreemapItem("Subscriptions", 30.0)),
                ),
            ),
            widthPx = 240f,
            heightPx = 160f,
            density = 1f,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )

        assertTrue(grouped.isRenderable)
        assertFalse(grouped.groupHeaders.isEmpty())
        assertEquals(3, grouped.itemLayouts.size)
        assertTrue(flat.groupHeaders.isEmpty())
        assertEquals(1, flat.itemLayouts.size)
    }

    @Test
    fun computeTreemapLayout_returnsNonRenderableForInvalidBounds() {
        val computed = computeTreemapLayout(
            groups = listOf(TreemapGroup("Bad", items = listOf(TreemapItem("Zero", 0.0)))),
            widthPx = 0f,
            heightPx = 100f,
            density = 1f,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )

        assertFalse(computed.isRenderable)
        assertTrue(computed.itemLayouts.isEmpty())
    }
}
