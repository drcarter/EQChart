package com.magimon.eq.common

import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.radar.RadarChartPresentationOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationOptionsContractTest {

    @Test
    fun pieDonutDefaults_matchContract() {
        val options = PieDonutPresentationOptions()

        assertTrue(options.showLegend)
        assertTrue(options.showLabels)
        assertEquals(PieLabelPosition.AUTO, options.labelPosition)
        assertFalse(options.enableSelectionExpand)
        assertEquals(-90f, options.startAngleDeg, 0.0001f)
        assertTrue(options.clockwise)
        assertEquals("No data", options.emptyText)
    }

    @Test
    fun pieDonutCustomValues_areApplied() {
        val options = PieDonutPresentationOptions(
            showLegend = false,
            showLabels = false,
            labelPosition = PieLabelPosition.INSIDE,
            startAngleDeg = 180f,
            clockwise = false,
            centerText = "TOTAL",
            emptyText = "EMPTY",
        )

        assertFalse(options.showLegend)
        assertFalse(options.showLabels)
        assertEquals(PieLabelPosition.INSIDE, options.labelPosition)
        assertEquals(180f, options.startAngleDeg, 0.0001f)
        assertFalse(options.clockwise)
        assertEquals("TOTAL", options.centerText)
        assertEquals("EMPTY", options.emptyText)
    }

    @Test
    fun radarDefaults_matchContract() {
        val options = RadarChartPresentationOptions()

        assertTrue(options.showLegend)
        assertTrue(options.showAxisLabels)
        assertTrue(options.showPoints)
        assertEquals(5, options.gridLevels)
        assertEquals(-90f, options.startAngleDeg, 0.0001f)
        assertEquals(700L, options.enterAnimationDurationMs)
    }
}
