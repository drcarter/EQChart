package com.magimon.eq.compose.integration

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.magimon.eq.compose.bar.BarChart
import com.magimon.eq.compose.bubble.BubbleChart
import com.magimon.eq.compose.bubble.computeBubbleChart
import com.magimon.eq.compose.gauge.GaugeChart
import com.magimon.eq.compose.heatmap.StockHeatmapChart
import com.magimon.eq.compose.histogram.HistogramChart
import com.magimon.eq.compose.line.LineChart
import com.magimon.eq.compose.pie.PieChart
import com.magimon.eq.compose.pie.buildPieSegments
import com.magimon.eq.compose.pie.computePieChartGeometry
import com.magimon.eq.compose.radar.RadarChart
import com.magimon.eq.compose.sankey.SankeyChart
import com.magimon.eq.compose.sankey.computeSankeyLayout
import com.magimon.eq.compose.sankey.sankeyLinkCenterPoint
import com.magimon.eq.compose.waveform.PcmWaveformChart
import com.magimon.eq.compose.waterfall.WaterfallChart
import com.magimon.eq.compose.waveform.rememberPcmWaveformController
import com.magimon.eq.compose.internal.degreeToOffset
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
import com.magimon.eq.histogram.HistogramBin
import com.magimon.eq.histogram.HistogramChartPresentationOptions
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarSeries
import com.magimon.eq.sankey.SankeyChartPresentationOptions
import com.magimon.eq.sankey.SankeyLink
import com.magimon.eq.sankey.SankeyNode
import com.magimon.eq.waterfall.WaterfallChartPresentationOptions
import com.magimon.eq.waterfall.WaterfallEntry
import com.magimon.eq.waterfall.WaterfallEntryKind
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
            "com.magimon.eq.compose.bar.BarChartKt",
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
    fun histogramChart_dispatchesClickForComputedBar() {
        val bins = listOf(
            HistogramBin(0.0, 10.0, 4.0, payload = "a"),
            HistogramBin(10.0, 20.0, 9.0, payload = "b"),
        )
        val options = HistogramChartPresentationOptions(
            animateOnDataChange = false,
        )
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.histogram.HistogramChartKt",
            "computeHistogramLayout",
            320f,
            220f,
            bins,
            com.magimon.eq.histogram.HistogramChartStyleOptions(),
            options,
            1f,
            1f,
            1f,
        ) ?: error("Expected computed histogram layout")
        val firstBar = computed.readField<List<Any>>("bars").first()
        val tap = Offset(
            (firstBar.readField<Float>("left") + firstBar.readField<Float>("right")) * 0.5f,
            (firstBar.readField<Float>("top") + firstBar.readField<Float>("bottom")) * 0.5f,
        )

        var clickedIndex: Int? = null
        var clickedPayload: Any? = null
        var clickedLabel: String? = "seed"
        var clickedColor: Int? = Int.MIN_VALUE
        composeRule.setContent {
            HistogramChart(
                bins = bins,
                modifier = Modifier.size(320.dp, 220.dp),
                presentationOptions = options,
                onBinClick = { index, bin, _ ->
                    clickedIndex = index
                    clickedPayload = bin.payload
                    clickedLabel = bin.label
                    clickedColor = bin.color
                },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals(0, clickedIndex)
        assertEquals("a", clickedPayload)
        assertEquals(null, clickedLabel)
        assertEquals(null, clickedColor)
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
            "com.magimon.eq.compose.line.LineChartKt",
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
            "com.magimon.eq.compose.radar.RadarChartKt",
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
            "com.magimon.eq.compose.bubble.BubbleChartKt",
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
    fun pieChart_dispatchesClickForComputedSlice() {
        val slices = listOf(
            PieSlice("A", 60.0, 0xFF1E88E5.toInt(), payload = "slice-a"),
            PieSlice("B", 40.0, 0xFF43A047.toInt(), payload = "slice-b"),
        )
        val options = PieDonutPresentationOptions(animateOnDataChange = false, showLegend = false)
        val geometry = computePieChartGeometry(
            availableWidth = 280f,
            availableHeight = 240f,
            segments = buildPieSegments(slices, 270f, true),
            styleOptions = com.magimon.eq.pie.PieDonutStyleOptions(),
            presentationOptions = options,
            density = Density(1f, 1f),
            legendTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f },
            donutInnerRatio = 0f,
        )
        val tap = geometry.center + degreeToOffset(90f, geometry.radius * 0.7f)

        var clickedPayload: Any? = null
        composeRule.setContent {
            PieChart(
                slices = slices,
                modifier = Modifier.size(280.dp, 240.dp),
                presentationOptions = options,
                onSliceClick = { _, _, payload -> clickedPayload = payload },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals("slice-a", clickedPayload)
    }

    @Test
    fun waterfallChart_dispatchesClickForComputedBar() {
        val entries = listOf(
            WaterfallEntry("Revenue", 120.0, payload = "revenue"),
            WaterfallEntry("Costs", -35.0, payload = "costs"),
            WaterfallEntry("Subtotal", 0.0, kind = WaterfallEntryKind.SUBTOTAL, payload = "subtotal"),
        )
        val computed = invokePrivateTopLevel(
            "com.magimon.eq.compose.waterfall.WaterfallChartKt",
            "computeWaterfallLayout",
            320f,
            240f,
            entries,
            com.magimon.eq.waterfall.WaterfallChartStyleOptions(),
            WaterfallChartPresentationOptions(
                animateOnDataChange = false,
                showConnectorLines = true,
            ),
            1f,
            1f,
            1f,
        ) ?: error("Expected computed waterfall layout")
        val firstBar = computed.readField<List<Any>>("bars").first()
        val tap = Offset(
            (firstBar.readField<Float>("left") + firstBar.readField<Float>("right")) * 0.5f,
            (firstBar.readField<Float>("top") + firstBar.readField<Float>("bottom")) * 0.5f,
        )

        var clickedLabel: String? = null
        var clickedTotal: Double? = null
        composeRule.setContent {
            WaterfallChart(
                entries = entries,
                modifier = Modifier.size(320.dp, 240.dp),
                presentationOptions = WaterfallChartPresentationOptions(
                    animateOnDataChange = false,
                    showConnectorLines = true,
                ),
                onEntryClick = { _, entry, cumulativeTotal ->
                    clickedLabel = entry.label
                    clickedTotal = cumulativeTotal
                },
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.waitForIdle()

        assertEquals("Revenue", clickedLabel)
        assertEquals(120.0, clickedTotal ?: Double.NaN, 0.0)
    }

    @Test
    fun sankeyChart_acceptsComputedNodeAndLinkTaps() {
        val nodes = listOf(
            SankeyNode("a", "A", 0xFF1E88E5.toInt(), payload = "node-a"),
            SankeyNode("b", "B", 0xFF43A047.toInt(), payload = "node-b"),
            SankeyNode("c", "C", 0xFFFB8C00.toInt(), payload = "node-c"),
        )
        val links = listOf(
            SankeyLink("a", "b", 10.0, label = "10", payload = "link-ab"),
            SankeyLink("b", "c", 6.0, label = "6", payload = "link-bc"),
        )
        val options = SankeyChartPresentationOptions(
            animateOnDataChange = false,
            showNodeLabels = true,
            showLinkValues = true,
        )
        var density: Density? = null
        val chartTag = "sankey-chart"
        composeRule.setContent {
            density = LocalDensity.current
            SankeyChart(
                nodes = nodes,
                links = links,
                modifier = Modifier.size(360.dp, 260.dp).testTag(chartTag),
                presentationOptions = options,
            )
        }

        composeRule.waitForIdle()
        val widthPx = with(density ?: error("Expected density")) { 360.dp.toPx() }
        val heightPx = with(density ?: error("Expected density")) { 260.dp.toPx() }
        val layout = computeSankeyLayout(
            nodes = nodes,
            links = links,
            widthPx = widthPx,
            heightPx = heightPx,
            density = density ?: error("Expected density"),
            styleOptions = com.magimon.eq.sankey.SankeyChartStyleOptions(),
            presentationOptions = options,
        )
        val node = layout.nodeLayouts.first()
        val link = layout.linkLayouts.last()
        val nodeTap = Offset((node.left + node.right) * 0.5f, (node.top + node.bottom) * 0.5f)
        val linkTap = sankeyLinkCenterPoint(link, t = 0.1f)
        composeRule.onNodeWithTag(chartTag).performTouchInput { click(nodeTap) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(chartTag).performTouchInput { click(linkTap) }
        composeRule.waitForIdle()
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
            "com.magimon.eq.compose.heatmap.StockHeatmapChartKt",
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
