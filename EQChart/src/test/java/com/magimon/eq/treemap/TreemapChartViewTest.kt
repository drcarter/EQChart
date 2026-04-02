package com.magimon.eq.treemap

import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TreemapChartViewTest {

    @Test
    fun setGroups_buildsLayoutAndDispatchesClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = TreemapChartView(context)
        var clickedLabel: String? = null

        val groups = listOf(
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
        )
        view.setOnItemClickListener { clickedLabel = it.label }
        view.setGroups(groups)

        layoutAndDraw(view, width = 420, height = 320)

        val layout = buildTreemapViewLayout(
            groups = groups,
            widthPx = 420,
            heightPx = 320,
            density = context.resources.displayMetrics.density,
            styleOptions = TreemapChartStyleOptions(),
            presentationOptions = TreemapChartPresentationOptions(),
        )
        val first = layout.itemLayouts.first()
        touchUp(view, first.rect.left + first.rect.width * 0.5f, first.rect.top + first.rect.height * 0.5f)

        assertTrue(layout.isRenderable)
        assertEquals("Paid", clickedLabel)
    }
}
