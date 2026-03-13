package com.magimon.eq.app.ui.compose

import android.graphics.Color
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.heatmap.StockHeatmapHelper
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarSeries
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.sin

object ChartSampleData {

    fun heatmapSections(): List<StockHeatmapSection> = StockHeatmapHelper.createSampleSections()

    fun bubbleData(): List<BubbleDatum> {
        return listOf(
            BubbleDatum(0.0, 0.0, 129_087.0, Color.parseColor("#4A7FB1"), "Food", "Arts", "Food"),
            BubbleDatum(0.0, 0.0, 113_576.0, Color.parseColor("#FF9100"), "Retail", "Goods", "Retail"),
            BubbleDatum(0.0, 0.0, 102_304.0, Color.parseColor("#F84C5A"), "Agriculture", "Labor", "Agriculture"),
            BubbleDatum(0.0, 0.0, 38_822.0, Color.parseColor("#62B8B4"), "Services", "Services", "Services"),
            BubbleDatum(0.0, 0.0, 34_262.0, Color.parseColor("#FF9100"), "Clothing", "Goods", "Clothing"),
            BubbleDatum(0.0, 0.0, 13_854.0, Color.parseColor("#F84C5A"), "Housing", "Labor", "Housing"),
        )
    }

    fun radarAxes(): List<RadarAxis> {
        return listOf(
            RadarAxis("sweet"),
            RadarAxis("price"),
            RadarAxis("color"),
            RadarAxis("fresh"),
            RadarAxis("good"),
        )
    }

    fun radarSeries(): List<RadarSeries> {
        return listOf(
            RadarSeries("Apple", Color.parseColor("#B899FF"), listOf(48.0, 80.0, 84.0, 34.0, 40.0), "Apple"),
            RadarSeries("Banana", Color.parseColor("#6F8695"), listOf(30.0, 40.0, 90.0, 82.0, 62.0), "Banana"),
        )
    }

    fun pieSlices(): List<PieSlice> {
        return listOf(
            PieSlice("Direct", 43.0, Color.parseColor("#2B80FF"), "Direct"),
            PieSlice("Social", 18.0, Color.parseColor("#13C3A3"), "Social"),
            PieSlice("Search", 26.0, Color.parseColor("#FF9F1C"), "Search"),
            PieSlice("Referral", 13.0, Color.parseColor("#EF476F"), "Referral"),
        )
    }

    fun donutSlices(): List<PieSlice> {
        return listOf(
            PieSlice("Engineering", 38.0, Color.parseColor("#2A9D8F"), "Engineering"),
            PieSlice("Marketing", 22.0, Color.parseColor("#3A86FF"), "Marketing"),
            PieSlice("Sales", 27.0, Color.parseColor("#FFBE0B"), "Sales"),
            PieSlice("Ops", 13.0, Color.parseColor("#FB5607"), "Ops"),
        )
    }

    fun lineSeries(): List<LineSeries> {
        val traffic = (0..11).map { month ->
            val base = when (month % 4) {
                0 -> 10.0
                1 -> 16.0
                2 -> 12.0
                else -> 22.0
            }
            val jitter = (month % 3) * 1.25
            LineDatum(month.toDouble(), base + jitter + (month * 0.7), "Jan-${month + 1}")
        }

        val conversion = (0..11).map { month ->
            val base = 12.0 + sin(month * 0.75) * 4.0
            val jitter = if (month % 2 == 0) 2.0 else 0.0
            LineDatum(month.toDouble(), base + jitter + (month * 0.4), "Jan-${month + 1}")
        }

        return listOf(
            LineSeries(
                name = "Traffic",
                color = Color.parseColor("#2B80FF"),
                points = traffic,
                payload = "Traffic",
                areaFillColor = Color.parseColor("#2B80FF"),
            ),
            LineSeries(
                name = "Conversion",
                color = Color.parseColor("#13C3A3"),
                points = conversion,
                payload = "Conversion",
                areaFillColor = Color.parseColor("#13C3A3"),
            ),
        )
    }

    fun areaSeries(): List<LineSeries> {
        val baseline = (0..11).map { month ->
            val value = 50.0 + sin(month * 0.6) * 9.0 + (month * 0.45)
            LineDatum(month.toDouble(), value, "Jan-${month + 1}")
        }
        val baselineShadow = (0..11).map { month ->
            val value = 30.0 + sin(month * 0.55) * 7.0 + (month * 0.35)
            LineDatum(month.toDouble(), value, "Jan-${month + 1}")
        }

        return listOf(
            LineSeries(
                name = "Projected",
                color = Color.parseColor("#FF9F1C"),
                points = baseline,
                payload = "Projected",
            ),
            LineSeries(
                name = "Baseline",
                color = Color.parseColor("#8A79FF"),
                points = baselineShadow,
                payload = "Baseline",
            ),
        )
    }

    fun barSeries(): List<BarSeries> {
        val labels = listOf("Q1", "Q2", "Q3", "Q4", "Q5")
        return listOf(
            BarSeries(
                name = "Desktop",
                color = Color.parseColor("#2B80FF"),
                points = labels.mapIndexed { index, label ->
                    BarDatum(label, (10.0 + index * 4.0 + Random(index).nextDouble(0.0, 1.8)))
                },
            ),
            BarSeries(
                name = "Mobile",
                color = Color.parseColor("#13C3A3"),
                points = labels.mapIndexed { index, label ->
                    BarDatum(label, (7.0 + index * 3.0 + Random(index + 33).nextDouble(0.0, 1.4)))
                },
            ),
            BarSeries(
                name = "Tablet",
                color = Color.parseColor("#FF9F1C"),
                points = labels.mapIndexed { index, label ->
                    BarDatum(label, (3.0 + index * 2.5 + Random(index + 99).nextDouble(0.0, 1.2)))
                },
            ),
        )
    }

    fun generateSineWaveSamples(
        sampleRateHz: Int,
        durationMs: Int,
        frequencyHz: Double,
    ): ShortArray {
        val totalSamples = ((sampleRateHz.toLong() * durationMs) / 1_000L).toInt().coerceAtLeast(1)
        val out = ShortArray(totalSamples)

        for (index in 0 until totalSamples) {
            val t = index.toDouble() / sampleRateHz.toDouble()
            val value = sin(2.0 * PI * frequencyHz * t)
            out[index] = (value * Short.MAX_VALUE.toDouble() * 0.6).toInt().toShort()
        }

        return out
    }
}
