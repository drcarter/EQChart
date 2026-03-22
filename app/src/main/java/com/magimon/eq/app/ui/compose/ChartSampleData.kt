package com.magimon.eq.app.ui.compose

import android.graphics.Color
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.heatmap.StockHeatmapHelper
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.cycle.CycleLink
import com.magimon.eq.cycle.CycleNode
import com.magimon.eq.funnel.FunnelStage
import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import com.magimon.eq.histogram.HistogramBin
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarSeries
import com.magimon.eq.rangebar.RangeBarEntry
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import com.magimon.eq.sunburst.SunburstNode
import com.magimon.eq.stepflow.StepFlowHubContent
import com.magimon.eq.stepflow.StepFlowStep
import com.magimon.eq.waterfall.WaterfallEntry
import com.magimon.eq.waterfall.WaterfallEntryKind
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

    fun gaugeValue(): GaugeValue {
        return GaugeValue(
            value = 72.0,
            minValue = 0.0,
            maxValue = 100.0,
            label = "CPU usage",
            payload = "CPU usage",
        )
    }

    fun gaugeRanges(): List<GaugeRange> {
        return listOf(
            GaugeRange(0.0, 50.0, Color.parseColor("#13C3A3"), "Healthy"),
            GaugeRange(50.0, 80.0, Color.parseColor("#FF9F1C"), "Warning"),
            GaugeRange(80.0, 100.0, Color.parseColor("#EF476F"), "Critical"),
        )
    }

    fun sankeyNodes(): List<SankeyNode> {
        return listOf(
            SankeyNode("direct", "Direct", Color.parseColor("#2B80FF")),
            SankeyNode("search", "Search", Color.parseColor("#13C3A3")),
            SankeyNode("landing", "Landing", Color.parseColor("#6F8695")),
            SankeyNode("pricing", "Pricing", Color.parseColor("#FF9F1C")),
            SankeyNode("trial", "Trial", Color.parseColor("#8A79FF")),
            SankeyNode("paid", "Paid", Color.parseColor("#2A9D8F")),
            SankeyNode("churn", "Churn", Color.parseColor("#EF476F")),
        )
    }

    fun sankeyLinks(): List<SankeyLink> {
        return listOf(
            SankeyLink("direct", "landing", 28.0),
            SankeyLink("search", "landing", 34.0),
            SankeyLink("search", "pricing", 12.0),
            SankeyLink("landing", "trial", 30.0),
            SankeyLink("pricing", "trial", 18.0),
            SankeyLink("trial", "paid", 16.0),
            SankeyLink("trial", "churn", 32.0),
        )
    }

    fun sunburstNodes(): List<SunburstNode> {
        return listOf(
            SunburstNode(
                label = "Company",
                value = 1000.0,
                color = Color.parseColor("#0F172A"),
                children = listOf(
                    SunburstNode(
                        label = "Growth",
                        value = 420.0,
                        color = Color.parseColor("#2563EB"),
                        children = listOf(
                            SunburstNode("Paid", 180.0, Color.parseColor("#3B82F6"), payload = "paid"),
                            SunburstNode("Organic", 150.0, Color.parseColor("#60A5FA"), payload = "organic"),
                            SunburstNode("Referral", 90.0, Color.parseColor("#93C5FD"), payload = "referral"),
                        ),
                        payload = "growth",
                    ),
                    SunburstNode(
                        label = "Product",
                        value = 360.0,
                        color = Color.parseColor("#14B8A6"),
                        children = listOf(
                            SunburstNode("Subscriptions", 240.0, Color.parseColor("#2DD4BF"), payload = "subscriptions"),
                            SunburstNode("Services", 120.0, Color.parseColor("#5EEAD4"), payload = "services"),
                        ),
                        payload = "product",
                    ),
                    SunburstNode(
                        label = "Ops",
                        value = 220.0,
                        color = Color.parseColor("#F97316"),
                        children = listOf(
                            SunburstNode("Support", 120.0, Color.parseColor("#FB923C"), payload = "support"),
                            SunburstNode("Logistics", 100.0, Color.parseColor("#FDBA74"), payload = "logistics"),
                        ),
                        payload = "ops",
                    ),
                ),
                payload = "company",
            ),
        )
    }

    fun cycleNodes(): List<CycleNode> {
        return listOf(
            CycleNode("plan", "Plan", Color.parseColor("#2B80FF"), "Plan"),
            CycleNode("build", "Build", Color.parseColor("#13C3A3"), "Build"),
            CycleNode("launch", "Launch", Color.parseColor("#FF9F1C"), "Launch"),
            CycleNode("measure", "Measure", Color.parseColor("#8A79FF"), "Measure"),
            CycleNode("learn", "Learn", Color.parseColor("#EF476F"), "Learn"),
        )
    }

    fun cycleLinks(): List<CycleLink> {
        return listOf(
            CycleLink("plan", "build", 18.0, label = "18", payload = "plan-build"),
            CycleLink("build", "launch", 14.0, label = "14", payload = "build-launch"),
            CycleLink("launch", "measure", 11.0, label = "11", payload = "launch-measure"),
            CycleLink("measure", "learn", 16.0, label = "16", payload = "measure-learn"),
            CycleLink("learn", "plan", 20.0, label = "20", payload = "learn-plan"),
            CycleLink("measure", "plan", 7.0, label = "7", payload = "measure-plan"),
        )
    }

    fun funnelStages(): List<FunnelStage> {
        return listOf(
            FunnelStage("Visits", 2_400.0, payload = "visits"),
            FunnelStage("Qualified", 1_650.0, payload = "qualified"),
            FunnelStage("Demo", 920.0, payload = "demo"),
            FunnelStage("Proposal", 410.0, payload = "proposal"),
            FunnelStage("Won", 180.0, payload = "won"),
        )
    }

    fun rangeBarEntries(): List<RangeBarEntry> {
        return listOf(
            RangeBarEntry(
                label = "Discovery",
                start = 0.0,
                end = 2.0,
                color = Color.parseColor("#2563EB"),
                payload = "Discovery",
            ),
            RangeBarEntry(
                label = "Design",
                start = 1.0,
                end = 4.0,
                color = Color.parseColor("#14B8A6"),
                payload = "Design",
            ),
            RangeBarEntry(
                label = "Platform",
                start = 3.0,
                end = 7.0,
                color = Color.parseColor("#7C3AED"),
                payload = "Platform",
            ),
            RangeBarEntry(
                label = "QA",
                start = 6.0,
                end = 8.0,
                color = Color.parseColor("#F59E0B"),
                payload = "QA",
            ),
            RangeBarEntry(
                label = "Launch",
                start = 8.0,
                end = 9.0,
                color = Color.parseColor("#EF4444"),
                payload = "Launch",
            ),
        )
    }

    fun stepFlowHubContent(): StepFlowHubContent {
        return StepFlowHubContent(
            eyebrow = "INFOGRAPHIC",
            title = "STEPS",
            description = "Move from discovery to growth with one readable flow.",
            payload = "step-flow-hub",
        )
    }

    fun stepFlowSteps(): List<StepFlowStep> {
        return listOf(
            StepFlowStep(
                id = "discover",
                badgeLabel = "STEP 01",
                title = "Discover",
                description = "Gather context and shape the problem.",
                accentColor = Color.parseColor("#9B5DE5"),
                iconText = "!",
                payload = "discover",
            ),
            StepFlowStep(
                id = "design",
                badgeLabel = "STEP 02",
                title = "Design",
                description = "Translate insight into a concrete direction.",
                accentColor = Color.parseColor("#7B61FF"),
                iconText = "\u2699",
                payload = "design",
            ),
            StepFlowStep(
                id = "target",
                badgeLabel = "STEP 03",
                title = "Target",
                description = "Choose the right audience and intent.",
                accentColor = Color.parseColor("#60A5FA"),
                iconText = "\u25CE",
                payload = "target",
            ),
            StepFlowStep(
                id = "budget",
                badgeLabel = "STEP 04",
                title = "Budget",
                description = "Align resources and commit to execution.",
                accentColor = Color.parseColor("#FBBF24"),
                iconText = "$",
                payload = "budget",
            ),
            StepFlowStep(
                id = "grow",
                badgeLabel = "STEP 05",
                title = "Grow",
                description = "Measure outcomes and expand what works.",
                accentColor = Color.parseColor("#A3E635"),
                iconText = "\u2197",
                payload = "grow",
            ),
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

    fun histogramBins(): List<HistogramBin> {
        return listOf(
            HistogramBin(0.0, 10.0, 4.0, payload = "0-10"),
            HistogramBin(10.0, 20.0, 9.0, payload = "10-20"),
            HistogramBin(20.0, 30.0, 13.0, payload = "20-30"),
            HistogramBin(30.0, 40.0, 8.0, payload = "30-40"),
            HistogramBin(40.0, 50.0, 3.0, payload = "40-50"),
        )
    }

    fun waterfallEntries(): List<WaterfallEntry> {
        return listOf(
            WaterfallEntry("Revenue", 240.0, payload = "Revenue"),
            WaterfallEntry("Returns", -42.0, payload = "Returns"),
            WaterfallEntry("Services", 68.0, payload = "Services"),
            WaterfallEntry("Subtotal", 0.0, kind = WaterfallEntryKind.SUBTOTAL, payload = "Subtotal"),
            WaterfallEntry("Marketing", -36.0, payload = "Marketing"),
            WaterfallEntry("Support", -22.0, payload = "Support"),
            WaterfallEntry("Total", 0.0, kind = WaterfallEntryKind.TOTAL, payload = "Total"),
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
