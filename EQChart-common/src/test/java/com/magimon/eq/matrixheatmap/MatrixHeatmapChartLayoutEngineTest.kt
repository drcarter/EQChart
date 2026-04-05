package com.magimon.eq.matrixheatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixHeatmapChartLayoutEngineTest {

    private fun config() = MatrixHeatmapChartLayoutConfig(
        widthPx = 420f,
        heightPx = 300f,
        contentPaddingPx = 12f,
        xAxisLabelHeightPx = 20f,
        yAxisLabelWidthPx = 44f,
        axisLabelGapPx = 8f,
        cellGapPx = 4f,
    )

    @Test
    fun resolveMatrixHeatmapChartLayout_buildsGridAndKeepsAxisOrder() {
        val result = resolveMatrixHeatmapChartLayout(
            data = MatrixHeatmapData(
                xLabels = listOf("Mon", "Tue"),
                yLabels = listOf("Morning", "Evening"),
                cells = listOf(
                    MatrixHeatmapCell("Mon", "Morning", 1.0, payload = "m1"),
                    MatrixHeatmapCell("Tue", "Evening", -2.0, payload = "t2"),
                ),
            ),
            config = config(),
        )

        assertTrue(result.isRenderable)
        assertEquals(listOf("Mon", "Tue"), result.xAxisLabels.map { it.label })
        assertEquals(listOf("Morning", "Evening"), result.yAxisLabels.map { it.label })
        assertEquals(4, result.cellLayouts.size)
        assertEquals("m1", result.cellLayouts.first().cell?.payload)
        assertNull(result.cellLayouts[1].cell)
    }

    @Test
    fun resolveMatrixHeatmapChartLayout_ignoresUnknownKeysAndUsesLastDuplicate() {
        val result = resolveMatrixHeatmapChartLayout(
            data = MatrixHeatmapData(
                xLabels = listOf("Q1", "Q1", "Q2"),
                yLabels = listOf("North", "South"),
                cells = listOf(
                    MatrixHeatmapCell("Q1", "North", 3.0, label = "first"),
                    MatrixHeatmapCell("Q1", "North", 9.0, label = "last"),
                    MatrixHeatmapCell("Bad", "South", 2.0),
                ),
            ),
            config = config(),
        )

        assertTrue(result.isRenderable)
        assertEquals(listOf("Q1", "Q2"), result.xAxisLabels.map { it.label })
        assertEquals(4, result.cellLayouts.size)
        assertEquals(9.0, result.cellLayouts.first().value ?: 0.0, 0.0)
        assertEquals("last", result.cellLayouts.first().resolvedText)
    }

    @Test
    fun resolveMatrixHeatmapChartLayout_usesDivergingScaleForMixedSigns() {
        val style = MatrixHeatmapChartStyleOptions(
            minColor = 0xFFAA0000.toInt(),
            neutralColor = 0xFFF5F5F5.toInt(),
            maxColor = 0xFF00AA55.toInt(),
        )
        val result = resolveMatrixHeatmapChartLayout(
            data = MatrixHeatmapData(
                xLabels = listOf("A", "B", "C"),
                yLabels = listOf("R1"),
                cells = listOf(
                    MatrixHeatmapCell("A", "R1", -4.0),
                    MatrixHeatmapCell("B", "R1", 0.0),
                    MatrixHeatmapCell("C", "R1", 5.0),
                ),
            ),
            config = config(),
            style = style,
        )

        val colors = result.cellLayouts.map { it.fillColor }
        assertEquals(style.minColor, colors[0])
        assertEquals(style.neutralColor, colors[1])
        assertEquals(style.maxColor, colors[2])
    }

    @Test
    fun resolveMatrixHeatmapChartLayout_returnsEmptyForInvalidInput() {
        val result = resolveMatrixHeatmapChartLayout(
            data = MatrixHeatmapData(
                xLabels = listOf(""),
                yLabels = listOf(""),
                cells = listOf(MatrixHeatmapCell("X", "Y", Double.NaN)),
            ),
            config = config(),
        )

        assertFalse(result.isRenderable)
        assertTrue(result.cellLayouts.isEmpty())
        assertEquals("no valid axes", result.emptyReason)
    }

    @Test
    fun findMatrixHeatmapHit_returnsOnlyPopulatedCells() {
        val style = MatrixHeatmapChartStyleOptions(emptyCellColor = 0xFFE0E0E0.toInt())
        val result = resolveMatrixHeatmapChartLayout(
            data = MatrixHeatmapData(
                xLabels = listOf("A", "B"),
                yLabels = listOf("R1", "R2"),
                cells = listOf(MatrixHeatmapCell("A", "R1", 4.0)),
            ),
            config = config(),
            style = style,
        )
        val populated = result.cellLayouts.first { it.value != null }
        val empty = result.cellLayouts.first { it.value == null }

        val hit = findMatrixHeatmapHit(
            layout = result,
            x = populated.rect.left + populated.rect.width * 0.5f,
            y = populated.rect.top + populated.rect.height * 0.5f,
        )
        val miss = findMatrixHeatmapHit(
            layout = result,
            x = empty.rect.left + empty.rect.width * 0.5f,
            y = empty.rect.top + empty.rect.height * 0.5f,
        )

        assertEquals(4.0, hit?.value ?: 0.0, 0.0)
        assertNull(miss)
        assertNotEquals(style.emptyCellColor, populated.fillColor)
    }
}
