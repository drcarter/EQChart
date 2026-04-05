package com.magimon.eq.app.ui.compose

import com.magimon.eq.violin.ViolinPlotSeries

object ViolinPlotSampleData {

    fun series(): List<ViolinPlotSeries> {
        return listOf(
            ViolinPlotSeries(
                label = "API",
                samples = listOf(82.0, 88.0, 90.0, 94.0, 96.0, 101.0, 104.0, 108.0, 113.0, 118.0, 124.0, 138.0),
                color = 0xFF2563EB.toInt(),
                payload = "API",
            ),
            ViolinPlotSeries(
                label = "Worker",
                samples = listOf(58.0, 63.0, 67.0, 70.0, 74.0, 77.0, 82.0, 86.0, 92.0, 98.0, 108.0, 124.0),
                color = 0xFF14B8A6.toInt(),
                payload = "Worker",
            ),
            ViolinPlotSeries(
                label = "Cache",
                samples = listOf(24.0, 26.0, 27.0, 28.0, 29.0, 30.0, 31.0, 33.0, 35.0, 38.0, 44.0, 52.0),
                color = 0xFF7C3AED.toInt(),
                payload = "Cache",
            ),
            ViolinPlotSeries(
                label = "Search",
                samples = listOf(112.0, 118.0, 126.0, 130.0, 137.0, 144.0, 152.0, 168.0, 184.0, 206.0, 224.0, 246.0),
                color = 0xFFF59E0B.toInt(),
                payload = "Search",
            ),
        )
    }
}
