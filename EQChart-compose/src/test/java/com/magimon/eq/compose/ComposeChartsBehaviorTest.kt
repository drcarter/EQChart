package com.magimon.eq.compose

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeValue
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeChartsBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun barChart_dispatchesClickForComputedBar() {
        val series = listOf(
            BarSeries(
                "Revenue",
                0xFF3366CC.toInt(),
                listOf(BarDatum("Jan", 10.0, "jan"), BarDatum("Feb", 18.0, "feb")),
                payload = "series",
            ),
        )
        val options = BarChartPresentationOptions(
            animateOnDataChange = false,
            layoutMode = BarLayoutMode.GROUPED,
            orientation = BarOrientation.VERTICAL,
            showLegend = false,
        )
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.BarChartKt",
            "computeBarLayout",
            320f,
            220f,
            series,
            com.magimon.eq.bar.BarChartStyleOptions(),
            options,
            1f,
            1f,
            1f,
        ) ?: error("Expected computed bar layout")
        val firstBar = computed.readField<List<Any>>("bars").first()
        val tap = Offset(
            (firstBar.readField<Float>("left") + firstBar.readField<Float>("right")) * 0.5f,
            (firstBar.readField<Float>("top") + firstBar.readField<Float>("bottom")) * 0.5f,
        )

        var clickedCategory: Int? = null
        var clickedPayload: Any? = null
        composeRule.setContent {
            BarChart(
                series = series,
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = options,
                onBarClick = { _, categoryIndex, _, payload ->
                    clickedCategory = categoryIndex
                    clickedPayload = payload
                },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals(0, clickedCategory)
        assertEquals("jan", clickedPayload)
    }

    @Test
    fun lineChart_dispatchesClickForComputedPoint() {
        val series = listOf(
            LineSeries(
                "alpha",
                0xFFE53935.toInt(),
                listOf(LineDatum(0.0, 2.0), LineDatum(1.0, 4.0, "p1"), LineDatum(2.0, 3.0)),
                payload = "series-alpha",
            ),
        )
        val options = LineChartPresentationOptions(animateOnDataChange = false, showLegend = false)
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.LineChartKt",
            "computeLineChart",
            320f,
            220f,
            series,
            com.magimon.eq.line.LineChartStyleOptions(),
            options,
            1f,
            1f,
        ) ?: error("Expected computed line chart")
        val point = computed.readField<List<List<Any>>>("seriesPoints").first()[1]
        val tap = Offset(point.readField("x"), point.readField("y"))

        var callbackPayload: Any? = null
        composeRule.setContent {
            LineChart(
                series = series,
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = options,
                onPointClick = { _, _, _, payload -> callbackPayload = payload },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals("p1", callbackPayload)
    }

    @Test
    fun radarChart_dispatchesClickForComputedPoint() {
        val axes = listOf(RadarAxis("A"), RadarAxis("B"), RadarAxis("C"), RadarAxis("D"))
        val series = listOf(RadarSeries("one", 0xFFE53935.toInt(), listOf(70.0, 65.0, 80.0, 50.0), payload = "payload"))
        val options = RadarChartPresentationOptions(showLegend = false, showPoints = true)
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.RadarChartKt",
            "computeRadarChart",
            320f,
            280f,
            axes,
            series,
            100.0,
            com.magimon.eq.radar.RadarChartStyleOptions(),
            options,
            1f,
            1f,
        ) ?: error("Expected computed radar chart")
        val point = computed.readField<List<List<Any>>>("pointsBySeries").first().first()
        val tap = Offset(point.readField("x"), point.readField("y"))

        var clickedPayload: Any? = null
        composeRule.setContent {
            RadarChart(
                axes = axes,
                series = series,
                modifier = Modifier.size(320.dp, 280.dp),
                presentationOptions = options,
                onPointClick = { _, _, _, payload -> clickedPayload = payload },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals("payload", clickedPayload)
    }

    @Test
    fun bubbleChart_dispatchesClickForComputedBubble() {
        val data = listOf(
            BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech", "a"),
            BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance", "b"),
        )
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.BubbleChartKt",
            "computeBubbleChart",
            340f,
            260f,
            data,
            BubbleAxisOptions(),
            BubblePresentationOptions(),
            null,
            BubbleLayoutMode.SCATTER,
            emptyList<Any>(),
            1f,
            1f,
        ) ?: error("Expected computed bubble chart")
        val bubble = computed.readField<List<Any>>("layouts").first()
        val tap = Offset(bubble.readField("centerX"), bubble.readField("centerY"))

        var clicked: BubbleDatum? = null
        composeRule.setContent {
            BubbleChart(
                data = data,
                modifier = Modifier.size(340.dp, 260.dp),
                onBubbleClick = { clicked = it },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertNotNull(clicked)
        assertTrue(clicked?.payload in setOf("a", "b"))
    }

    @Test
    fun stockHeatmapChart_dispatchesClickForComputedBlock() {
        val items = listOf(
            StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
            StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
            StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0),
        )
        val sections = listOf(
            StockHeatmapSection("Tech", 0xFF1565C0.toInt(), items.take(2)),
            StockHeatmapSection("Finance", 0xFF2E7D32.toInt(), items.drop(2)),
        )
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.StockHeatmapChartKt",
            "computeHeatmapLayout",
            sections,
            360f,
            260f,
            1f,
        ) ?: error("Expected computed heatmap")
        val block = computed.readField<List<Any>>("blocks").first()
        val rect = block.readField<android.graphics.RectF>("rect")
        val tap = Offset(rect.centerX(), rect.centerY())

        var clicked: StockHeatmapItem? = null
        composeRule.setContent {
            StockHeatmapChart(
                sections = sections,
                modifier = Modifier.size(360.dp, 260.dp),
                onItemClick = { clicked = it },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertNotNull(clicked)
    }

    @Test
    fun pieChart_handlesEmptyState() {
        composeRule.setContent {
            PieChart(
                slices = listOf(PieSlice("bad", 0.0, 0xFF000000.toInt())),
                modifier = Modifier.size(280.dp, 240.dp),
                presentationOptions = PieDonutPresentationOptions(animateOnDataChange = false, emptyText = "Empty"),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun gaugeChart_handlesEmptyState() {
        composeRule.setContent {
            GaugeChart(
                value = GaugeValue(value = 10.0, minValue = 5.0, maxValue = 5.0),
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = GaugeChartPresentationOptions(
                    animateOnValueChange = false,
                    emptyText = "Empty",
                ),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rememberPcmWaveformController_updatesAndRenders() {
        composeRule.setContent {
            val controller = rememberPcmWaveformController(sampleRateHz = 8_000, windowDurationMs = 200)
            LaunchedEffect(Unit) {
                controller.setPcm16Mono(shortArrayOf(0, 500, -500, 1000, -1000))
            }
            PcmWaveformChart(
                controller = controller,
                modifier = Modifier.size(360.dp, 180.dp),
            )
        }

        composeRule.waitForIdle()
    }
}
