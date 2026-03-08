package com.magimon.eq.app.ui.compose

import android.graphics.Color
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.heatmap.StockHeatmapHelper
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarSeries
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
