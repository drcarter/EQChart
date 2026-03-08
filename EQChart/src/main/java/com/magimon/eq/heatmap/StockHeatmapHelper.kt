package com.magimon.eq.heatmap

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Sample data, color, and formatting utilities for the stock heatmap.
 */
object StockHeatmapHelper {

    /**
     * Creates sample data grouped by section.
     *
     * Each section has its own color and contains five stock items.
     */
    fun createSampleSections(): List<StockHeatmapSection> {
        return listOf(
            StockHeatmapSection(
                name = "Technology",
                color = Color.parseColor("#1E88E5"),
                stocks = listOf(
                    StockHeatmapItem("AAPL", "Apple Inc.", "Technology", 200.0, 1.2, 3_000_000_000_000.0, 24.0),
                    StockHeatmapItem("MSFT", "Microsoft Corp.", "Technology", 380.0, -0.8, 2_800_000_000_000.0, 22.0),
                    StockHeatmapItem("NVDA", "NVIDIA Corp.", "Technology", 900.0, 4.2, 2_200_000_000_000.0, 18.0),
                    StockHeatmapItem("ORCL", "Oracle Corp.", "Technology", 122.0, 0.6, 350_000_000_000.0, 8.0),
                    StockHeatmapItem("ADBE", "Adobe Inc.", "Technology", 590.0, -1.1, 280_000_000_000.0, 6.0),
                ),
            ),
            StockHeatmapSection(
                name = "Communication Services",
                color = Color.parseColor("#00897B"),
                stocks = listOf(
                    StockHeatmapItem("GOOGL", "Alphabet Inc.", "Communication Services", 140.0, 0.5, 1_800_000_000_000.0, 20.0),
                    StockHeatmapItem("META", "Meta Platforms Inc.", "Communication Services", 450.0, -3.1, 1_100_000_000_000.0, 14.0),
                    StockHeatmapItem("NFLX", "Netflix Inc.", "Communication Services", 610.0, 1.8, 260_000_000_000.0, 7.0),
                    StockHeatmapItem("DIS", "Walt Disney Co.", "Communication Services", 101.0, -0.4, 190_000_000_000.0, 5.0),
                    StockHeatmapItem("TMUS", "T-Mobile US Inc.", "Communication Services", 162.0, 0.7, 195_000_000_000.0, 6.0),
                ),
            ),
            StockHeatmapSection(
                name = "Consumer Discretionary",
                color = Color.parseColor("#F4511E"),
                stocks = listOf(
                    StockHeatmapItem("AMZN", "Amazon.com Inc.", "Consumer Discretionary", 160.0, 2.3, 1_700_000_000_000.0, 19.0),
                    StockHeatmapItem("TSLA", "Tesla Inc.", "Consumer Discretionary", 230.0, -2.5, 700_000_000_000.0, 11.0),
                    StockHeatmapItem("HD", "Home Depot Inc.", "Consumer Discretionary", 360.0, -0.2, 360_000_000_000.0, 8.0),
                    StockHeatmapItem("MCD", "McDonald's Corp.", "Consumer Discretionary", 295.0, 0.9, 220_000_000_000.0, 6.0),
                    StockHeatmapItem("NKE", "Nike Inc.", "Consumer Discretionary", 105.0, -1.6, 160_000_000_000.0, 5.0),
                ),
            ),
            StockHeatmapSection(
                name = "Financials",
                color = Color.parseColor("#6D4C41"),
                stocks = listOf(
                    StockHeatmapItem("JPM", "JPMorgan Chase & Co.", "Financials", 180.0, 0.3, 550_000_000_000.0, 12.0),
                    StockHeatmapItem("BAC", "Bank of America Corp.", "Financials", 33.0, -0.9, 280_000_000_000.0, 8.0),
                    StockHeatmapItem("WFC", "Wells Fargo & Co.", "Financials", 58.0, 0.5, 210_000_000_000.0, 6.0),
                    StockHeatmapItem("GS", "Goldman Sachs Group Inc.", "Financials", 410.0, 1.1, 140_000_000_000.0, 4.0),
                    StockHeatmapItem("MS", "Morgan Stanley", "Financials", 92.0, -0.3, 150_000_000_000.0, 5.0),
                ),
            ),
            StockHeatmapSection(
                name = "Health Care",
                color = Color.parseColor("#8E24AA"),
                stocks = listOf(
                    StockHeatmapItem("UNH", "UnitedHealth Group Inc.", "Health Care", 550.0, 0.9, 480_000_000_000.0, 10.0),
                    StockHeatmapItem("LLY", "Eli Lilly and Co.", "Health Care", 760.0, 1.7, 720_000_000_000.0, 13.0),
                    StockHeatmapItem("JNJ", "Johnson & Johnson", "Health Care", 157.0, -0.6, 390_000_000_000.0, 7.0),
                    StockHeatmapItem("PFE", "Pfizer Inc.", "Health Care", 31.0, -1.2, 180_000_000_000.0, 4.0),
                    StockHeatmapItem("MRK", "Merck & Co.", "Health Care", 122.0, 0.4, 310_000_000_000.0, 6.0),
                ),
            ),
        )
    }

    /**
     * Backward-compatible sample API.
     *
     * Flattens [createSampleSections] into a single list.
     */
    fun createSampleData(): List<StockHeatmapItem> {
        return createSampleSections().flatMap { it.stocks }
    }

    /**
     * Maps a sector name to a section color when only `setData()` is used.
     */
    fun mapSectorToColor(sector: String): Int {
        val palette = listOf(
            Color.parseColor("#1E88E5"),
            Color.parseColor("#00897B"),
            Color.parseColor("#F4511E"),
            Color.parseColor("#6D4C41"),
            Color.parseColor("#8E24AA"),
            Color.parseColor("#3949AB"),
            Color.parseColor("#43A047"),
            Color.parseColor("#FB8C00"),
        )
        val index = abs(sector.hashCode()) % palette.size
        return palette[index]
    }

    /**
     * Maps percentage change to a heatmap block color.
     *
     * - Strong decline: dark red
     * - Neutral: dark gray
     * - Strong rise: dark green
     */
    fun mapChangeToColor(changePct: Double): Int {
        val maxAbs = 6.0
        val clamped = max(-maxAbs, min(maxAbs, changePct))
        val ratio = (clamped / maxAbs).toFloat()

        val neutral = Color.parseColor("#2A2F36")
        val up = Color.parseColor("#1F8F55")
        val down = Color.parseColor("#B23A3A")

        return when {
            ratio > 0f -> interpolateColor(neutral, up, ratio)
            ratio < 0f -> interpolateColor(neutral, down, abs(ratio))
            else -> neutral
        }
    }

    /**
     * Computes an intermediate color by linearly interpolating two colors.
     */
    private fun interpolateColor(from: Int, to: Int, t: Float): Int {
        val clampedT = t.coerceIn(0f, 1f)
        val a1 = Color.alpha(from)
        val r1 = Color.red(from)
        val g1 = Color.green(from)
        val b1 = Color.blue(from)

        val a2 = Color.alpha(to)
        val r2 = Color.red(to)
        val g2 = Color.green(to)
        val b2 = Color.blue(to)

        val a = (a1 + (a2 - a1) * clampedT).toInt()
        val r = (r1 + (r2 - r1) * clampedT).toInt()
        val g = (g1 + (g2 - g1) * clampedT).toInt()
        val b = (b1 + (b2 - b1) * clampedT).toInt()

        return Color.argb(a, r, g, b)
    }

    /**
     * Formats percentage change as `+1.23%`.
     */
    fun formatChange(changePct: Double): String {
        return String.format("%+.2f%%", changePct)
    }

    /**
     * Formats market cap as an abbreviated T/B/M string.
     */
    fun formatMarketCap(marketCap: Double): String {
        val trillion = 1_000_000_000_000.0
        val billion = 1_000_000_000.0
        val million = 1_000_000.0

        return when {
            marketCap >= trillion -> String.format("%.1fT", marketCap / trillion)
            marketCap >= billion -> String.format("%.1fB", marketCap / billion)
            marketCap >= million -> String.format("%.1fM", marketCap / million)
            else -> String.format("%.0f", marketCap)
        }
    }
}
