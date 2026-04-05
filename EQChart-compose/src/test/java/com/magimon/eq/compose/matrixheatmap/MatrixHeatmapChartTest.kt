package com.magimon.eq.compose.matrixheatmap

import com.magimon.eq.matrixheatmap.MatrixHeatmapChartPresentationOptions
import com.magimon.eq.matrixheatmap.MatrixHeatmapChartStyleOptions
import com.magimon.eq.matrixheatmap.MatrixHeatmapCell
import com.magimon.eq.matrixheatmap.MatrixHeatmapData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MatrixHeatmapChartTest {

    @Test
    fun computeMatrixHeatmapLayout_buildsGridAndCellTexts() {
        val computed = computeMatrixHeatmapLayout(
            widthPx = 360f,
            heightPx = 240f,
            data = MatrixHeatmapData(
                xLabels = listOf("Mon", "Tue"),
                yLabels = listOf("AM", "PM"),
                cells = listOf(
                    MatrixHeatmapCell("Mon", "AM", 3.0),
                    MatrixHeatmapCell("Tue", "PM", -1.0, label = "drop"),
                ),
            ),
            style = MatrixHeatmapChartStyleOptions(),
            presentation = MatrixHeatmapChartPresentationOptions(),
            density = 1f,
            scaledDensity = 1f,
        )

        assertTrue(computed.isRenderable)
        assertEquals(4, computed.cellLayouts.size)
        assertEquals("3", computed.cellLayouts.first().resolvedText)
        assertEquals("drop", computed.cellLayouts.last().resolvedText)
    }

    @Test
    fun computeMatrixHeatmapLayout_returnsNonRenderableForInvalidBounds() {
        val computed = computeMatrixHeatmapLayout(
            widthPx = 0f,
            heightPx = 240f,
            data = MatrixHeatmapData(
                xLabels = listOf("Mon"),
                yLabels = listOf("AM"),
                cells = listOf(MatrixHeatmapCell("Mon", "AM", 3.0)),
            ),
            style = MatrixHeatmapChartStyleOptions(),
            presentation = MatrixHeatmapChartPresentationOptions(),
            density = 1f,
            scaledDensity = 1f,
        )

        assertFalse(computed.isRenderable)
        assertTrue(computed.cellLayouts.isEmpty())
    }
}
