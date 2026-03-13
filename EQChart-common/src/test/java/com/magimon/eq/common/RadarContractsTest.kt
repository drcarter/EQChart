package com.magimon.eq.common

import com.magimon.eq.radar.RadarAxis
import com.magimon.eq.radar.RadarChartPresentationOptions
import com.magimon.eq.radar.RadarChartStyleOptions
import com.magimon.eq.radar.RadarSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarContractsTest {

    @Test
    fun radarAxisAndSeries_preserveProvidedValues() {
        val payload = listOf("banana")
        val axis = RadarAxis(label = "Taste")
        val defaultPayloadSeries = RadarSeries(
            name = "Apple",
            color = 0xFFB899FF.toInt(),
            values = listOf(48.0, 80.0, 84.0),
        )
        val series = RadarSeries(
            name = "Banana",
            color = 0xFF6F8695.toInt(),
            values = listOf(30.0, 40.0, 90.0),
            payload = payload,
        )
        val copied = series.copy(name = "Apple")

        assertEquals("Taste", axis.label)
        assertEquals("Apple", defaultPayloadSeries.name)
        assertEquals(0xFFB899FF.toInt(), defaultPayloadSeries.color)
        assertEquals(listOf(48.0, 80.0, 84.0), defaultPayloadSeries.values)
        assertNull(defaultPayloadSeries.payload)

        assertEquals("Banana", series.name)
        assertEquals(0xFF6F8695.toInt(), series.color)
        assertEquals(listOf(30.0, 40.0, 90.0), series.values)
        assertSame(payload, series.payload)

        assertEquals("Apple", copied.name)
        assertSame(payload, copied.payload)
    }

    @Test
    fun radarChartPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = RadarChartPresentationOptions()
        val custom = RadarChartPresentationOptions(
            showLegend = false,
            showAxisLabels = false,
            showPoints = false,
            gridLevels = 6,
            animateOnDataChange = false,
            enterAnimationDurationMs = 520L,
            enterAnimationDelayMs = 80L,
            startAngleDeg = 45f,
            legendTextSizeSp = 14f,
            axisLabelTextSizeSp = 16f,
            legendLeftMarginDp = 9f,
            legendTopMarginDp = 10f,
        )

        assertTrue(defaults.showLegend)
        assertTrue(defaults.showAxisLabels)
        assertTrue(defaults.showPoints)
        assertEquals(5, defaults.gridLevels)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(700L, defaults.enterAnimationDurationMs)
        assertEquals(40L, defaults.enterAnimationDelayMs)
        assertEquals(-90f, defaults.startAngleDeg, 0.0f)
        assertEquals(13f, defaults.legendTextSizeSp, 0.0f)
        assertEquals(15f, defaults.axisLabelTextSizeSp, 0.0f)
        assertEquals(4f, defaults.legendLeftMarginDp, 0.0f)
        assertEquals(4f, defaults.legendTopMarginDp, 0.0f)

        assertFalse(custom.showLegend)
        assertFalse(custom.showAxisLabels)
        assertFalse(custom.showPoints)
        assertEquals(6, custom.gridLevels)
        assertFalse(custom.animateOnDataChange)
        assertEquals(520L, custom.enterAnimationDurationMs)
        assertEquals(80L, custom.enterAnimationDelayMs)
        assertEquals(45f, custom.startAngleDeg, 0.0f)
        assertEquals(14f, custom.legendTextSizeSp, 0.0f)
        assertEquals(16f, custom.axisLabelTextSizeSp, 0.0f)
        assertEquals(9f, custom.legendLeftMarginDp, 0.0f)
        assertEquals(10f, custom.legendTopMarginDp, 0.0f)
    }

    @Test
    fun radarChartStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = RadarChartStyleOptions()
        val custom = RadarChartStyleOptions(
            backgroundColor = 0xFFF0F0F0.toInt(),
            gridColor = 0xFF111111.toInt(),
            axisColor = 0xFF222222.toInt(),
            axisLabelColor = 0xFF333333.toInt(),
            legendTextColor = 0xFF444444.toInt(),
            polygonStrokeWidthDp = 2.0f,
            gridStrokeWidthDp = 1.5f,
            axisStrokeWidthDp = 1.3f,
            axisDashLengthDp = 6f,
            axisDashGapDp = 5f,
            fillAlpha = 120,
            pointRadiusDp = 5f,
            pointCoreRadiusDp = 2.5f,
            pointGlowRadiusDp = 13f,
            pointGlowAlpha = 90,
            selectedPointRadiusDp = 8f,
            selectedStrokeWidthDp = 3f,
            contentPaddingDp = 14f,
            axisLabelOffsetDp = 20f,
            legendMarkerSizeDp = 12f,
            legendItemGapDp = 11f,
            legendRowGapDp = 9f,
            legendBottomGapDp = 12f,
            touchHitRadiusDp = 22f,
        )
        val defaultBackgroundColor = defaults.backgroundColor
        val defaultGridColor = defaults.gridColor
        val defaultAxisColor = defaults.axisColor
        val defaultAxisLabelColor = defaults.axisLabelColor
        val defaultLegendTextColor = defaults.legendTextColor

        assertEquals(defaultBackgroundColor, defaults.backgroundColor)
        assertEquals(defaultGridColor, defaults.gridColor)
        assertEquals(defaultAxisColor, defaults.axisColor)
        assertEquals(defaultAxisLabelColor, defaults.axisLabelColor)
        assertEquals(defaultLegendTextColor, defaults.legendTextColor)
        assertEquals(1.8f, defaults.polygonStrokeWidthDp, 0.0f)
        assertEquals(1f, defaults.gridStrokeWidthDp, 0.0f)
        assertEquals(1f, defaults.axisStrokeWidthDp, 0.0f)
        assertEquals(4f, defaults.axisDashLengthDp, 0.0f)
        assertEquals(4f, defaults.axisDashGapDp, 0.0f)
        assertEquals(78, defaults.fillAlpha)
        assertEquals(4.6f, defaults.pointRadiusDp, 0.0f)
        assertEquals(2f, defaults.pointCoreRadiusDp, 0.0f)
        assertEquals(12f, defaults.pointGlowRadiusDp, 0.0f)
        assertEquals(72, defaults.pointGlowAlpha)
        assertEquals(7.5f, defaults.selectedPointRadiusDp, 0.0f)
        assertEquals(2.4f, defaults.selectedStrokeWidthDp, 0.0f)
        assertEquals(12f, defaults.contentPaddingDp, 0.0f)
        assertEquals(18f, defaults.axisLabelOffsetDp, 0.0f)
        assertEquals(10f, defaults.legendMarkerSizeDp, 0.0f)
        assertEquals(10f, defaults.legendItemGapDp, 0.0f)
        assertEquals(8f, defaults.legendRowGapDp, 0.0f)
        assertEquals(10f, defaults.legendBottomGapDp, 0.0f)
        assertEquals(18f, defaults.touchHitRadiusDp, 0.0f)

        assertEquals(0xFFF0F0F0.toInt(), custom.backgroundColor)
        assertEquals(0xFF111111.toInt(), custom.gridColor)
        assertEquals(0xFF222222.toInt(), custom.axisColor)
        assertEquals(0xFF333333.toInt(), custom.axisLabelColor)
        assertEquals(0xFF444444.toInt(), custom.legendTextColor)
        assertEquals(2.0f, custom.polygonStrokeWidthDp, 0.0f)
        assertEquals(1.5f, custom.gridStrokeWidthDp, 0.0f)
        assertEquals(1.3f, custom.axisStrokeWidthDp, 0.0f)
        assertEquals(6f, custom.axisDashLengthDp, 0.0f)
        assertEquals(5f, custom.axisDashGapDp, 0.0f)
        assertEquals(120, custom.fillAlpha)
        assertEquals(5f, custom.pointRadiusDp, 0.0f)
        assertEquals(2.5f, custom.pointCoreRadiusDp, 0.0f)
        assertEquals(13f, custom.pointGlowRadiusDp, 0.0f)
        assertEquals(90, custom.pointGlowAlpha)
        assertEquals(8f, custom.selectedPointRadiusDp, 0.0f)
        assertEquals(3f, custom.selectedStrokeWidthDp, 0.0f)
        assertEquals(14f, custom.contentPaddingDp, 0.0f)
        assertEquals(20f, custom.axisLabelOffsetDp, 0.0f)
        assertEquals(12f, custom.legendMarkerSizeDp, 0.0f)
        assertEquals(11f, custom.legendItemGapDp, 0.0f)
        assertEquals(9f, custom.legendRowGapDp, 0.0f)
        assertEquals(12f, custom.legendBottomGapDp, 0.0f)
        assertEquals(22f, custom.touchHitRadiusDp, 0.0f)
    }
}
