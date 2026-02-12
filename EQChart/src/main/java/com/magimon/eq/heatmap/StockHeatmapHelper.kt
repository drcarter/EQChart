package com.magimon.eq.heatmap

/**
 * Helper class for creating sample data and utilities for StockHeatmapView
 */
object StockHeatmapHelper {

    /**
     * Creates sample stock data for testing/demo purposes
     */
    fun createSampleData(): List<StockHeatmapItem> {
        return listOf(
            // Technology Sector
            StockHeatmapItem("AAPL", "Apple Inc.", 2.5f, 3_000_000_000_000L, "Technology", 175.50f),
            StockHeatmapItem("MSFT", "Microsoft Corp.", 1.8f, 2_800_000_000_000L, "Technology", 380.20f),
            StockHeatmapItem("GOOGL", "Alphabet Inc.", -0.5f, 1_600_000_000_000L, "Technology", 140.30f),
            StockHeatmapItem("AMZN", "Amazon.com Inc.", 0.9f, 1_500_000_000_000L, "Technology", 145.80f),
            StockHeatmapItem("META", "Meta Platforms Inc.", 3.2f, 1_200_000_000_000L, "Technology", 485.60f),
            StockHeatmapItem("NVDA", "NVIDIA Corp.", 5.1f, 1_800_000_000_000L, "Technology", 520.40f),
            StockHeatmapItem("TSLA", "Tesla Inc.", -1.2f, 800_000_000_000L, "Technology", 245.30f),
            
            // Financial Sector
            StockHeatmapItem("JPM", "JPMorgan Chase", 0.7f, 450_000_000_000L, "Financial", 165.20f),
            StockHeatmapItem("BAC", "Bank of America", 0.3f, 280_000_000_000L, "Financial", 35.80f),
            StockHeatmapItem("WFC", "Wells Fargo", -0.4f, 180_000_000_000L, "Financial", 48.50f),
            StockHeatmapItem("GS", "Goldman Sachs", 1.1f, 120_000_000_000L, "Financial", 385.40f),
            
            // Healthcare Sector
            StockHeatmapItem("JNJ", "Johnson & Johnson", 0.5f, 420_000_000_000L, "Healthcare", 158.30f),
            StockHeatmapItem("UNH", "UnitedHealth Group", 1.3f, 480_000_000_000L, "Healthcare", 520.10f),
            StockHeatmapItem("PFE", "Pfizer Inc.", -0.8f, 150_000_000_000L, "Healthcare", 28.40f),
            StockHeatmapItem("ABBV", "AbbVie Inc.", 0.6f, 280_000_000_000L, "Healthcare", 145.20f),
            
            // Consumer Sector
            StockHeatmapItem("WMT", "Walmart Inc.", 0.4f, 420_000_000_000L, "Consumer", 165.80f),
            StockHeatmapItem("HD", "Home Depot", 0.8f, 380_000_000_000L, "Consumer", 385.60f),
            StockHeatmapItem("NKE", "Nike Inc.", -0.6f, 150_000_000_000L, "Consumer", 95.40f),
            StockHeatmapItem("SBUX", "Starbucks Corp.", 1.2f, 110_000_000_000L, "Consumer", 98.20f),
            
            // Energy Sector
            StockHeatmapItem("XOM", "Exxon Mobil", -1.5f, 450_000_000_000L, "Energy", 105.30f),
            StockHeatmapItem("CVX", "Chevron Corp.", -0.9f, 300_000_000_000L, "Energy", 150.80f),
            
            // Industrial Sector
            StockHeatmapItem("BA", "Boeing Co.", 2.1f, 120_000_000_000L, "Industrial", 185.40f),
            StockHeatmapItem("CAT", "Caterpillar Inc.", 0.9f, 140_000_000_000L, "Industrial", 245.60f),
            StockHeatmapItem("GE", "General Electric", 1.4f, 160_000_000_000L, "Industrial", 125.20f)
        )
    }

    /**
     * Formats market cap for display
     */
    fun formatMarketCap(marketCap: Long): String {
        return when {
            marketCap >= 1_000_000_000_000L -> String.format("%.1fT", marketCap / 1_000_000_000_000.0)
            marketCap >= 1_000_000_000L -> String.format("%.1fB", marketCap / 1_000_000_000.0)
            marketCap >= 1_000_000L -> String.format("%.1fM", marketCap / 1_000_000.0)
            else -> marketCap.toString()
        }
    }

    /**
     * Formats change percentage for display
     */
    fun formatChange(change: Float): String {
        val sign = if (change >= 0) "+" else ""
        return String.format("%s%.2f%%", sign, change)
    }
}

