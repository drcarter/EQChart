package com.magimon.eq.app.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSampleDataTest {

    @Test
    fun matrixHeatmapData_returnsExpectedAxisAndSparseCells() {
        val data = ChartSampleData.matrixHeatmapData()

        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri"), data.xLabels)
        assertEquals(listOf("AM", "Noon", "PM", "Night"), data.yLabels)
        assertTrue(data.cells.isNotEmpty())
        assertTrue(data.cells.any { it.xKey == "Wed" && it.yKey == "Noon" }.not())
        assertTrue(data.cells.any { it.xKey == "Thu" && it.yKey == "PM" })
    }
}
