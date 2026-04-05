package com.magimon.eq.compose.integration

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.magimon.eq.bubble3d.Bubble3DDatum
import com.magimon.eq.compose.bubble3d.Bubble3DChart
import com.magimon.eq.compose.pointcloud3d.PointCloud3DChart
import com.magimon.eq.compose.pointline3d.PointLine3DChart
import com.magimon.eq.compose.bar.BarChart
import com.magimon.eq.compose.bubble.BubbleChart
import com.magimon.eq.compose.gauge.GaugeChart
import com.magimon.eq.compose.heatmap.StockHeatmapChart
import com.magimon.eq.compose.heatmap.StockHeatmapChartFromItems
import com.magimon.eq.compose.line.AreaChart
import com.magimon.eq.compose.line.LineChart
import com.magimon.eq.compose.pie.DonutChart
import com.magimon.eq.compose.pie.PieChart
import com.magimon.eq.compose.radar.RadarChart
import com.magimon.eq.compose.rangebar.RangeBarChart
import com.magimon.eq.compose.sankey.SankeyChart
import com.magimon.eq.compose.waveform.PcmWaveformChart
import com.magimon.eq.compose.waveform.PcmWaveformController
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.pointcloud3d.PointCloud3DDatum
import com.magimon.eq.pointline3d.PointLine3DDatum
import com.magimon.eq.pointline3d.PointLine3DSeries
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarSeries
import com.magimon.eq.rangebar.RangeBarChartPresentationOptions
import com.magimon.eq.rangebar.RangeBarEntry
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import com.magimon.eq.waveform.PcmWaveFormStyleOptions
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeChartsSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun barChart_rendersAndHandlesTap() {
        composeRule.setContent {
            BarChart(
                series = listOf(
                    BarSeries(
                        name = "Revenue",
                        color = 0xFF3366CC.toInt(),
                        points = listOf(
                            BarDatum("Jan", 10.0),
                            BarDatum("Feb", 18.0),
                        ),
                    ),
                ),
                modifier = Modifier.size(320.dp, 240.dp),
                presentationOptions = BarChartPresentationOptions(animateOnDataChange = false),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun lineChart_rendersAndHandlesTap() {
        val series = listOf(
            LineSeries(
                name = "alpha",
                color = 0xFFE53935.toInt(),
                points = listOf(
                    LineDatum(0.0, 5.0),
                    LineDatum(1.0, 7.5),
                    LineDatum(2.0, 3.5),
                ),
            ),
        )

        composeRule.setContent {
            LineChart(
                series = series,
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = LineChartPresentationOptions(animateOnDataChange = false),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun areaChart_rendersAndHandlesTap() {
        val series = listOf(
            LineSeries(
                name = "alpha",
                color = 0xFFE53935.toInt(),
                points = listOf(
                    LineDatum(0.0, 5.0),
                    LineDatum(1.0, 7.5),
                    LineDatum(2.0, 3.5),
                ),
            ),
        )

        composeRule.setContent {
            AreaChart(
                series = series,
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = LineChartPresentationOptions(
                    animateOnDataChange = false,
                    showAreaFill = true,
                ),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun pieChart_rendersAndHandlesTap() {
        val slices = listOf(
            PieSlice("A", 60.0, 0xFF1E88E5.toInt()),
            PieSlice("B", 40.0, 0xFF43A047.toInt()),
        )

        composeRule.setContent {
            PieChart(
                slices = slices,
                modifier = Modifier.size(280.dp, 240.dp),
                presentationOptions = PieDonutPresentationOptions(animateOnDataChange = false),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun donutChart_rendersAndHandlesTap() {
        val slices = listOf(
            PieSlice("A", 60.0, 0xFF1E88E5.toInt()),
            PieSlice("B", 40.0, 0xFF43A047.toInt()),
        )

        composeRule.setContent {
            DonutChart(
                slices = slices,
                modifier = Modifier.size(280.dp, 240.dp),
                presentationOptions = PieDonutPresentationOptions(
                    animateOnDataChange = false,
                    centerText = "Total",
                    centerSubText = "2026",
                ),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun radarChart_rendersAndHandlesTap() {
        composeRule.setContent {
            RadarChart(
                axes = listOf(
                    RadarAxis("Speed"),
                    RadarAxis("Power"),
                    RadarAxis("Control"),
                    RadarAxis("Range"),
                ),
                series = listOf(
                    RadarSeries("Alpha", 0xFFEF5350.toInt(), listOf(70.0, 80.0, 90.0, 65.0)),
                    RadarSeries("Beta", 0xFF42A5F5.toInt(), listOf(50.0, 60.0, 55.0, 72.0)),
                ),
                modifier = Modifier.size(320.dp, 280.dp),
                presentationOptions = RadarChartPresentationOptions(showLegend = true, showPoints = true),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun gaugeChart_renders() {
        composeRule.setContent {
            GaugeChart(
                value = GaugeValue(value = 72.0, minValue = 0.0, maxValue = 100.0, label = "CPU"),
                ranges = listOf(
                    GaugeRange(0.0, 50.0, 0xFF43A047.toInt()),
                    GaugeRange(50.0, 80.0, 0xFFFDD835.toInt()),
                    GaugeRange(80.0, 100.0, 0xFFE53935.toInt()),
                ),
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = GaugeChartPresentationOptions(animateOnValueChange = false),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun rangeBarChart_rendersAndHandlesTap() {
        composeRule.setContent {
            RangeBarChart(
                entries = listOf(
                    RangeBarEntry(label = "Plan", start = 0.0, end = 2.0, color = 0xFF2563EB.toInt()),
                    RangeBarEntry(label = "Build", start = 2.0, end = 5.0, color = 0xFF14B8A6.toInt()),
                ),
                modifier = Modifier.size(320.dp, 240.dp),
                presentationOptions = RangeBarChartPresentationOptions(
                    animateOnDataChange = false,
                    showBarLabels = true,
                ),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun bubble3DChart_rendersInsideCompose() {
        // Robolectric unit tests do not provide the OpenGL ES GL10 classes that
        // GLSurfaceView initializes against, so only run this smoke test when
        // the JVM test environment exposes those platform types.
        assumeTrue(isOpenGlEsUnitTestEnvironmentAvailable())

        composeRule.setContent {
            Bubble3DChart(
                data = listOf(
                    Bubble3DDatum(
                        x = 0.0,
                        y = 0.0,
                        z = 0.0,
                        size = 120.0,
                        color = 0xFF7C3AED.toInt(),
                        label = "Center",
                    ),
                ),
                modifier = Modifier.size(320.dp, 240.dp),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun pointLine3DChart_rendersInsideCompose() {
        assumeTrue(isOpenGlEsUnitTestEnvironmentAvailable())

        composeRule.setContent {
            PointLine3DChart(
                series = listOf(
                    PointLine3DSeries(
                        name = "Flight Path",
                        lineColor = 0xFF38BDF8.toInt(),
                        points = listOf(
                            PointLine3DDatum(x = 0.0, y = 0.0, z = 0.0, label = "A"),
                            PointLine3DDatum(x = 1.0, y = 1.5, z = 0.8, label = "B"),
                            PointLine3DDatum(x = 2.0, y = 2.2, z = 1.4, label = "C"),
                        ),
                    ),
                ),
                modifier = Modifier.size(320.dp, 240.dp),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun pointCloud3DChart_rendersInsideCompose() {
        assumeTrue(isOpenGlEsUnitTestEnvironmentAvailable())

        composeRule.setContent {
            PointCloud3DChart(
                data = listOf(
                    PointCloud3DDatum(
                        x = 0.0,
                        y = 0.0,
                        z = 0.0,
                        size = 12.0,
                        color = 0xFF38BDF8.toInt(),
                        label = "Center",
                    ),
                    PointCloud3DDatum(
                        x = 1.2,
                        y = 0.8,
                        z = 1.5,
                        size = 18.0,
                        color = 0xFFF97316.toInt(),
                        label = "Edge",
                    ),
                    PointCloud3DDatum(
                        x = -0.8,
                        y = 1.6,
                        z = 0.4,
                        size = 10.0,
                        color = 0xFF22C55E.toInt(),
                        label = "Lift",
                    ),
                ),
                modifier = Modifier.size(320.dp, 240.dp),
            )
        }

        composeRule.waitForIdle()
    }

    private fun isOpenGlEsUnitTestEnvironmentAvailable(): Boolean {
        return runCatching {
            Class.forName("javax.microedition.khronos.opengles.GL10")
        }.isSuccess
    }

    @Test
    fun sankeyChart_rendersAndHandlesTap() {
        composeRule.setContent {
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
                modifier = Modifier.size(360.dp, 260.dp),
                presentationOptions = SankeyChartPresentationOptions(
                    animateOnDataChange = false,
                    showNodeLabels = true,
                    showLinkValues = true,
                ),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun stockHeatmapChart_rendersAndHandlesTap() {
        val items = listOf(
            StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
            StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
            StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0),
        )

        composeRule.setContent {
            StockHeatmapChart(
                sections = listOf(
                    StockHeatmapSection("Tech", 0xFF1565C0.toInt(), items.take(2)),
                    StockHeatmapSection("Finance", 0xFF2E7D32.toInt(), items.drop(2)),
                ),
                modifier = Modifier.size(360.dp, 260.dp),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun stockHeatmapChartFromItems_rendersAndHandlesTap() {
        val items = listOf(
            StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
            StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
            StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0),
        )

        composeRule.setContent {
            StockHeatmapChartFromItems(
                items = items,
                modifier = Modifier.size(360.dp, 260.dp),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun bubbleChart_rendersInScatterMode() {
        val data = listOf(
            BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech"),
            BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance"),
            BubbleDatum(3.0, 3.5, 10.0, 0xFFFB8C00.toInt(), "C", "Tech"),
        )

        composeRule.setContent {
            BubbleChart(
                data = data,
                modifier = Modifier.size(340.dp, 260.dp),
                presentationOptions = BubblePresentationOptions(),
                layoutMode = BubbleLayoutMode.SCATTER,
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun bubbleChart_rendersInPackedMode() {
        val data = listOf(
            BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech"),
            BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance"),
            BubbleDatum(3.0, 3.5, 10.0, 0xFFFB8C00.toInt(), "C", "Tech"),
        )

        composeRule.setContent {
            BubbleChart(
                data = data,
                modifier = Modifier.size(340.dp, 260.dp),
                layoutMode = BubbleLayoutMode.PACKED,
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click() }
        composeRule.waitForIdle()
    }

    @Test
    fun pcmWaveformChart_rendersBufferedSamples() {
        val controller = PcmWaveformController(sampleRateHz = 44_100, windowDurationMs = 500)
        controller.setPcm16Mono(shortArrayOf(0, 1000, -2000, 3000, -1500, 0, 800, -900))

        composeRule.setContent {
            PcmWaveformChart(
                controller = controller,
                modifier = Modifier.size(360.dp, 180.dp),
                styleOptions = PcmWaveFormStyleOptions(),
            )
        }

        composeRule.waitForIdle()
    }
}
