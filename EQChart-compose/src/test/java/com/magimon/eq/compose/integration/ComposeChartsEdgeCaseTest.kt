package com.magimon.eq.compose.integration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.compose.bar.BarChart
import com.magimon.eq.compose.bubble.BubbleChart
import com.magimon.eq.compose.gauge.GaugeChart
import com.magimon.eq.compose.heatmap.StockHeatmapChart
import com.magimon.eq.compose.heatmap.StockHeatmapChartFromItems
import com.magimon.eq.compose.line.LineChart
import com.magimon.eq.compose.pie.DonutChart
import com.magimon.eq.compose.pie.PieChart
import com.magimon.eq.compose.radar.RadarChart
import com.magimon.eq.compose.sankey.SankeyChart
import com.magimon.eq.compose.waveform.PcmWaveformChart
import com.magimon.eq.compose.waveform.rememberPcmWaveformController
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieLabelPosition
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarSeries
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeChartsEdgeCaseTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun animatedBarLineAndGauge_coverAnimationAndEmptyBranches() {
        composeRule.setContent {
            Column {
                BarChart(
                    series = listOf(
                        BarSeries(
                            "Revenue",
                            0xFF3366CC.toInt(),
                            listOf(BarDatum("Jan", 10.0), BarDatum("Feb", -4.0)),
                        ),
                    ),
                    modifier = Modifier.size(320.dp, 220.dp),
                    presentationOptions = BarChartPresentationOptions(
                        animateOnDataChange = true,
                        enterAnimationDurationMs = 1L,
                        enterAnimationDelayMs = 0L,
                        showLegend = true,
                    ),
                )
                LineChart(
                    series = emptyList(),
                    modifier = Modifier.size(320.dp, 220.dp),
                    presentationOptions = LineChartPresentationOptions(
                        emptyText = "No line data",
                    ),
                )
                GaugeChart(
                    value = GaugeValue(value = 72.0, minValue = 0.0, maxValue = 100.0, label = "CPU"),
                    ranges = listOf(
                        GaugeRange(0.0, 50.0, 0xFF43A047.toInt()),
                        GaugeRange(50.0, 80.0, 0xFFFDD835.toInt()),
                    ),
                    modifier = Modifier.size(320.dp, 220.dp),
                    presentationOptions = GaugeChartPresentationOptions(
                        animateOnValueChange = true,
                        animationDurationMs = 1L,
                        showTicks = true,
                        showCenterLabel = true,
                        showMinMaxLabels = true,
                    ),
                )
                GaugeChart(
                    value = GaugeValue(value = 10.0, minValue = 10.0, maxValue = 10.0, label = "bad"),
                    modifier = Modifier.size(320.dp, 140.dp),
                    presentationOptions = GaugeChartPresentationOptions(emptyText = "Invalid gauge"),
                )
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun animatedPieSankeyAndHeatmap_coverEmptyAndContentBranches() {
        val items = listOf(
            StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
            StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
            StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0),
        )

        composeRule.setContent {
            Column {
                PieChart(
                    slices = emptyList(),
                    modifier = Modifier.size(280.dp, 220.dp),
                    presentationOptions = PieDonutPresentationOptions(emptyText = "No slices"),
                )
                DonutChart(
                    slices = listOf(
                        PieSlice("A", 60.0, 0xFF1E88E5.toInt()),
                        PieSlice("B", 40.0, 0xFF43A047.toInt()),
                    ),
                    modifier = Modifier.size(280.dp, 240.dp),
                    presentationOptions = PieDonutPresentationOptions(
                        animateOnDataChange = true,
                        enterAnimationDurationMs = 1L,
                        enterAnimationDelayMs = 0L,
                        showLegend = true,
                        showLabels = true,
                        labelPosition = PieLabelPosition.OUTSIDE,
                        centerText = "Total",
                        centerSubText = "2026",
                    ),
                )
                SankeyChart(
                    nodes = emptyList(),
                    links = listOf(SankeyLink("a", "b", 10.0)),
                    modifier = Modifier.size(360.dp, 220.dp),
                    presentationOptions = SankeyChartPresentationOptions(emptyText = "No flow"),
                )
                StockHeatmapChart(sections = emptyList(), modifier = Modifier.size(320.dp, 180.dp))
                StockHeatmapChartFromItems(items = items, modifier = Modifier.size(320.dp, 220.dp))
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun animatedBubbleRadarWaveformAndSankey_coverVariantBranches() {
        composeRule.setContent {
            val controller = rememberPcmWaveformController(sampleRateHz = 8_000, windowDurationMs = 400)
            LaunchedEffect(Unit) {
                controller.setPcm16Mono(shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE, 0))
            }

            Column {
                BubbleChart(
                    data = listOf(
                        BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech"),
                        BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance"),
                    ),
                    modifier = Modifier.size(340.dp, 260.dp),
                    layoutMode = BubbleLayoutMode.PACKED,
                    presentationOptions = BubblePresentationOptions(showLegend = true, title = "Packed"),
                )
                BubbleChart(
                    data = listOf(BubbleDatum(Double.NaN, 1.0, 2.0, 1, "bad", null)),
                    modifier = Modifier.size(240.dp, 160.dp),
                    presentationOptions = BubblePresentationOptions(showLegend = false),
                )
                RadarChart(
                    axes = listOf(RadarAxis("Speed"), RadarAxis("Power"), RadarAxis("Control"), RadarAxis("Range")),
                    series = listOf(RadarSeries("Alpha", 0xFFEF5350.toInt(), listOf(70.0, 80.0, 90.0, 65.0))),
                    modifier = Modifier.size(320.dp, 260.dp),
                    presentationOptions = RadarChartPresentationOptions(showLegend = false, showAxisLabels = true, showPoints = true),
                )
                SankeyChart(
                    nodes = listOf(
                        SankeyNode("a", "A", 0xFF1E88E5.toInt()),
                        SankeyNode("b", "B", 0xFF43A047.toInt()),
                        SankeyNode("c", "C", 0xFFFB8C00.toInt()),
                    ),
                    links = listOf(
                        SankeyLink("a", "b", 10.0, label = "10"),
                        SankeyLink("b", "c", 6.0, label = "6"),
                    ),
                    modifier = Modifier.size(360.dp, 240.dp),
                    presentationOptions = SankeyChartPresentationOptions(
                        animateOnDataChange = true,
                        animationDurationMs = 1L,
                        showNodeLabels = true,
                        showLinkValues = true,
                    ),
                )
                PcmWaveformChart(controller = controller, modifier = Modifier.size(320.dp, 120.dp))
            }
        }

        composeRule.waitForIdle()
    }
}
