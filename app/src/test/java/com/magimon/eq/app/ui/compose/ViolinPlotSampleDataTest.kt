package com.magimon.eq.app.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolinPlotSampleDataTest {

    @Test
    fun series_returnsExpectedSeriesAndFiniteSamples() {
        val series = ViolinPlotSampleData.series()

        assertEquals(4, series.size)
        assertEquals(listOf("API", "Worker", "Cache", "Search"), series.map { it.label })
        assertTrue(series.all { entry -> entry.samples.isNotEmpty() })
        assertTrue(series.all { entry -> entry.samples.all { it.isFinite() } })
    }
}
