package com.magimon.eq.compose

import android.graphics.Paint
import androidx.compose.ui.unit.Density
import com.magimon.eq.bar.BarChartPresentationOptions
import com.magimon.eq.bar.BarChartStyleOptions
import com.magimon.eq.bar.BarDatum
import com.magimon.eq.bar.BarLayoutMode
import com.magimon.eq.bar.BarOrientation
import com.magimon.eq.bar.BarSeries
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubblePresentationOptions
import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeChartStyleOptions
import com.magimon.eq.heatmap.StockHeatmapItem
import com.magimon.eq.heatmap.StockHeatmapSection
import com.magimon.eq.line.LineChartPresentationOptions
import com.magimon.eq.line.LineChartStyleOptions
import com.magimon.eq.line.LineDatum
import com.magimon.eq.line.LineSeries
import com.magimon.eq.pie.PieDonutPresentationOptions
import com.magimon.eq.pie.PieDonutStyleOptions
import com.magimon.eq.pie.PieSlice
import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import com.magimon.eq.radar.RadarSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposePrivateLayoutTest {

    @Test
    fun computeBarLayout_buildsExpectedBarsAndTicks() {
        val computed = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.BarChartKt",
            methodName = "computeBarLayout",
            320f,
            220f,
            listOf(
                BarSeries(
                    "A",
                    0xFF3366CC.toInt(),
                    listOf(
                        BarDatum("Jan", 10.0),
                        BarDatum("Feb", -4.0),
                    ),
                ),
                BarSeries(
                    "B",
                    0xFF43A047.toInt(),
                    listOf(
                        BarDatum("Jan", 8.0),
                        BarDatum("Feb", 6.0),
                    ),
                ),
            ),
            BarChartStyleOptions(),
            BarChartPresentationOptions(
                animateOnDataChange = false,
                layoutMode = BarLayoutMode.STACKED,
                orientation = BarOrientation.HORIZONTAL,
            ),
            1f,
            1f,
            1f,
        ) ?: error("Expected computed layout")

        val categories = computed.readField<List<String>>("categories")
        val ticks = computed.readField<List<Double>>("ticks")
        val bars = computed.readField<List<*>>("bars")

        assertEquals(listOf("Jan", "Feb"), categories)
        assertEquals(5, ticks.size)
        assertEquals(4, bars.size)
        assertTrue(computed.readField<Double>("baselineValue") <= 0.0)
    }

    @Test
    fun computeLineChart_sanitizesInvalidPointsAndBuildsTicks() {
        val computed = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.LineChartKt",
            methodName = "computeLineChart",
            360f,
            240f,
            listOf(
                LineSeries(
                    "alpha",
                    0xFFE53935.toInt(),
                    listOf(
                        LineDatum(2.0, 4.0),
                        LineDatum(Double.NaN, 1.0),
                        LineDatum(0.0, 2.0),
                        LineDatum(1.0, 3.0),
                    ),
                ),
            ),
            LineChartStyleOptions(),
            LineChartPresentationOptions(animateOnDataChange = false),
            1f,
            1f,
        ) ?: error("Expected computed layout")

        val seriesPoints = computed.readField<List<List<Any>>>("seriesPoints")
        val xTicks = computed.readField<List<Double>>("xTicks")
        val yTicks = computed.readField<List<Double>>("yTicks")

        assertEquals(1, seriesPoints.size)
        assertEquals(3, seriesPoints.first().size)
        assertEquals(6, xTicks.size)
        assertEquals(6, yTicks.size)
        assertTrue(computed.readField<Float>("baselineY") > 0f)
    }

    @Test
    fun buildPieSegments_andLegendReserve_handleInvalidAndWrappingData() {
        val segments = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.PieDonutChartKt",
            methodName = "buildPieSegments",
            listOf(
                PieSlice("A", 40.0, 0xFF1E88E5.toInt()),
                PieSlice("B", 60.0, 0xFF43A047.toInt()),
                PieSlice("bad", 0.0, 0xFF000000.toInt()),
            ),
            270f,
            false,
        ) as List<*>

        assertEquals(2, segments.size)
        assertTrue((segments.first() ?: error("Expected first segment")).readField<Float>("sweep") < 0f)

        val reserve = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.PieDonutChartKt",
            methodName = "resolvePieLegendReservedHeight",
            180f,
            segments,
            PieDonutStyleOptions(),
            PieDonutPresentationOptions(showLegend = true),
            Density(1f, 1f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f },
        ) as Float

        assertTrue(reserve > 0f)
    }

    @Test
    fun computeRadarChart_andNearestPoint_buildPointsAndHits() {
        val computed = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.RadarChartKt",
            methodName = "computeRadarChart",
            360f,
            300f,
            listOf(RadarAxis("A"), RadarAxis("B"), RadarAxis("C"), RadarAxis("D")),
            listOf(
                RadarSeries("one", 0xFFE53935.toInt(), listOf(70.0, 65.0, 80.0, 50.0)),
                RadarSeries("bad", 0xFF000000.toInt(), listOf(1.0, 2.0)),
            ),
            100.0,
            RadarChartStyleOptions(),
            RadarChartPresentationOptions(showLegend = true, showAxisLabels = true),
            1f,
            1f,
        ) ?: error("Expected computed radar chart")

        val pointsBySeries = computed.readField<List<List<Any>>>("pointsBySeries")
        val legendItems = computed.readField<List<*>>("legendItems")
        val firstPoint = pointsBySeries.first().first()

        val hit = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.RadarChartKt",
            methodName = "radarNearestPoint",
            firstPoint.readField<Float>("x"),
            firstPoint.readField<Float>("y"),
            pointsBySeries,
            16f,
        )

        assertEquals(1, pointsBySeries.size)
        assertFalse(legendItems.isEmpty())
        assertNotNull(hit)
        assertEquals(0, hit!!.readField<Int>("seriesIndex"))
    }

    @Test
    fun computeHeatmapLayout_createsBlocksForSections_andFormatsColors() {
        val computed = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.StockHeatmapChartKt",
            methodName = "computeHeatmapLayout",
            listOf(
                StockHeatmapSection(
                    "Tech",
                    0xFF1565C0.toInt(),
                    listOf(
                        StockHeatmapItem("AAPL", "Apple", "Tech", 210.0, 2.1, 3000.0),
                        StockHeatmapItem("MSFT", "Microsoft", "Tech", 420.0, -1.4, 2800.0),
                    ),
                ),
                StockHeatmapSection(
                    "Finance",
                    0xFF2E7D32.toInt(),
                    listOf(StockHeatmapItem("JPM", "JPMorgan", "Finance", 200.0, 0.7, 800.0)),
                ),
            ),
            520f,
            420f,
            1f,
        ) ?: error("Expected computed heatmap")

        val blocks = computed.readField<List<*>>("blocks")
        val headers = computed.readField<List<*>>("sectionHeaders")
        val formatted = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.StockHeatmapChartKt",
            methodName = "heatmapFormatChange",
            -1.25,
        ) as String
        val sectorColor = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.StockHeatmapChartKt",
            methodName = "heatmapMapSectorToColor",
            "Tech",
        ) as Int

        assertFalse(blocks.isEmpty())
        assertFalse(headers.isEmpty())
        assertEquals("-1.25%", formatted)
        assertTrue(sectorColor != 0)
    }

    @Test
    fun computeBubbleChart_buildsScatterAndPackedLayouts() {
        val data = listOf(
            BubbleDatum(1.0, 2.0, 12.0, 0xFF1E88E5.toInt(), "A", "Tech"),
            BubbleDatum(2.5, 1.5, 18.0, 0xFF43A047.toInt(), "B", "Finance"),
            BubbleDatum(3.0, 3.5, 10.0, 0xFFFB8C00.toInt(), "C", "Tech"),
        )

        val scatter = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.BubbleChartKt",
            methodName = "computeBubbleChart",
            360f,
            260f,
            data,
            BubbleAxisOptions(),
            BubblePresentationOptions(),
            null,
            BubbleLayoutMode.SCATTER,
            emptyList<Any>(),
            1f,
            1f,
        ) ?: error("Expected scatter chart")
        val packed = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.BubbleChartKt",
            methodName = "computeBubbleChart",
            360f,
            260f,
            data,
            BubbleAxisOptions(),
            BubblePresentationOptions(),
            null,
            BubbleLayoutMode.PACKED,
            emptyList<Any>(),
            1f,
            1f,
        ) ?: error("Expected packed chart")

        assertEquals(3, scatter.readField<List<*>>("layouts").size)
        assertFalse(scatter.readField<List<*>>("xTicks").isEmpty())
        assertEquals(3, packed.readField<List<*>>("layouts").size)
    }

    @Test
    fun waveformMinMaxPerPixel_andGaugeGeometry_coverEdgeCalculations() {
        val minMax = invokePrivateTopLevel(
            ownerClassName = "com.magimon.eq.compose.PcmWaveformChartKt",
            methodName = "minMaxPerPixel",
            shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE, 0),
            2,
        ) as FloatArray
        val geometry = resolveComposeGaugeGeometry(
            width = 320f,
            height = 220f,
            density = Density(1f, 1f),
            styleOptions = GaugeChartStyleOptions(),
            presentationOptions = GaugeChartPresentationOptions(showMinMaxLabels = true),
        )

        assertEquals(4, minMax.size)
        assertTrue(minMax[0] <= -1f)
        assertTrue(minMax[3] >= 1f)
        assertTrue(geometry.radius > 0f)
        assertTrue(geometry.arcRect.width > 0f)
    }
}
