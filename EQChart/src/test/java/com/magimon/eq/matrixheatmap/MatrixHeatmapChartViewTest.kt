package com.magimon.eq.matrixheatmap

import com.magimon.eq.testutil.layoutAndDraw
import com.magimon.eq.testutil.readPrivate
import com.magimon.eq.testutil.touchUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MatrixHeatmapChartViewTest {

    @Test
    fun setData_buildsLayoutAndDispatchesCellClick() {
        val context = RuntimeEnvironment.getApplication()
        val view = MatrixHeatmapChartView(context)
        var clickedPayload: Any? = null

        view.setOnCellClickListener { clickedPayload = it.payload }
        view.setData(
            MatrixHeatmapData(
                xLabels = listOf("Mon", "Tue"),
                yLabels = listOf("Morning", "Evening"),
                cells = listOf(
                    MatrixHeatmapCell("Mon", "Morning", 3.5, payload = "mon-morning"),
                ),
            ),
        )

        layoutAndDraw(view, width = 420, height = 280)

        val layout = view.readPrivate<MatrixHeatmapChartLayoutResult>("layout")
        val first = layout.cellLayouts.first { it.value != null }
        touchUp(
            view,
            first.rect.left + first.rect.width * 0.5f,
            first.rect.top + first.rect.height * 0.5f,
        )

        assertEquals("mon-morning", clickedPayload)
    }

    @Test
    fun emptyCells_doNotDispatchClickCallbacks() {
        val context = RuntimeEnvironment.getApplication()
        val view = MatrixHeatmapChartView(context)
        var clickedPayload: Any? = null

        view.setOnCellClickListener { clickedPayload = it.payload }
        view.setData(
            MatrixHeatmapData(
                xLabels = listOf("Mon", "Tue"),
                yLabels = listOf("Morning", "Evening"),
                cells = listOf(
                    MatrixHeatmapCell("Mon", "Morning", 1.0, payload = "payload"),
                ),
            ),
        )

        layoutAndDraw(view, width = 420, height = 280)

        val layout = view.readPrivate<MatrixHeatmapChartLayoutResult>("layout")
        val empty = layout.cellLayouts.first { it.value == null }
        touchUp(
            view,
            empty.rect.left + empty.rect.width * 0.5f,
            empty.rect.top + empty.rect.height * 0.5f,
        )

        assertNull(clickedPayload)
    }
}
