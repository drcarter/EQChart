package com.magimon.eq.common

import com.magimon.eq.gauge.GaugeChartPresentationOptions
import com.magimon.eq.gauge.GaugeChartStyleOptions
import com.magimon.eq.gauge.GaugeRange
import com.magimon.eq.gauge.GaugeValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeContractsTest {

    @Test
    fun gaugeValue_preservesPropertiesAndCopy() {
        val payload = mutableMapOf("source" to "api")
        val defaultValue = GaugeValue(value = 42.0)
        val value = GaugeValue(
            value = 72.0,
            minValue = -20.0,
            maxValue = 120.0,
            label = "CPU",
            payload = payload,
        )
        val copied = value.copy(value = 64.0)

        assertEquals(42.0, defaultValue.value, 0.0)
        assertEquals(0.0, defaultValue.minValue, 0.0)
        assertEquals(100.0, defaultValue.maxValue, 0.0)
        assertNull(defaultValue.label)
        assertNull(defaultValue.payload)

        assertEquals(72.0, value.value, 0.0)
        assertEquals(-20.0, value.minValue, 0.0)
        assertEquals(120.0, value.maxValue, 0.0)
        assertEquals("CPU", value.label)
        assertSame(payload, value.payload)

        assertEquals(64.0, copied.value, 0.0)
        assertSame(payload, copied.payload)
    }

    @Test
    fun gaugeRange_preservesProperties() {
        val defaultRange = GaugeRange(
            startValue = 0.0,
            endValue = 50.0,
            color = 0xFF13C3A3.toInt(),
        )
        val range = GaugeRange(
            startValue = 50.0,
            endValue = 80.0,
            color = 0xFFFF9F1C.toInt(),
            label = "Warning",
        )

        assertEquals(0.0, defaultRange.startValue, 0.0)
        assertEquals(50.0, defaultRange.endValue, 0.0)
        assertNull(defaultRange.label)

        assertEquals(50.0, range.startValue, 0.0)
        assertEquals(80.0, range.endValue, 0.0)
        assertEquals(0xFFFF9F1C.toInt(), range.color)
        assertEquals("Warning", range.label)
    }

    @Test
    fun gaugePresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = GaugeChartPresentationOptions()
        val custom = GaugeChartPresentationOptions(
            startAngleDeg = 135f,
            sweepAngleDeg = 270f,
            showTicks = false,
            tickCount = 8,
            showMinMaxLabels = false,
            showValueText = false,
            showCenterLabel = false,
            animateOnValueChange = false,
            animationDurationMs = 420L,
            emptyText = "EMPTY",
        )

        assertEquals(180f, defaults.startAngleDeg, 0.0f)
        assertEquals(180f, defaults.sweepAngleDeg, 0.0f)
        assertTrue(defaults.showTicks)
        assertEquals(5, defaults.tickCount)
        assertTrue(defaults.showMinMaxLabels)
        assertTrue(defaults.showValueText)
        assertTrue(defaults.showCenterLabel)
        assertTrue(defaults.animateOnValueChange)
        assertEquals(650L, defaults.animationDurationMs)
        assertEquals("No data", defaults.emptyText)

        assertEquals(135f, custom.startAngleDeg, 0.0f)
        assertEquals(270f, custom.sweepAngleDeg, 0.0f)
        assertEquals(false, custom.showTicks)
        assertEquals(8, custom.tickCount)
        assertEquals(false, custom.showMinMaxLabels)
        assertEquals(false, custom.showValueText)
        assertEquals(false, custom.showCenterLabel)
        assertEquals(false, custom.animateOnValueChange)
        assertEquals(420L, custom.animationDurationMs)
        assertEquals("EMPTY", custom.emptyText)
    }

    @Test
    fun gaugeStyleOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = GaugeChartStyleOptions()
        val custom = GaugeChartStyleOptions(
            backgroundColor = 0xFFF7FAFC.toInt(),
            trackColor = 0xFFE5E7EB.toInt(),
            progressColor = 0xFF13C3A3.toInt(),
            progressThicknessDp = 20f,
            tickColor = 0xFF9CA3AF.toInt(),
            tickLengthDp = 12f,
            tickThicknessDp = 3f,
            indicatorColor = 0xFF111827.toInt(),
            indicatorThicknessDp = 4f,
            indicatorCenterColor = 0xFF0F172A.toInt(),
            indicatorCenterRadiusDp = 6f,
            valueTextColor = 0xFF020617.toInt(),
            valueTextSizeSp = 28f,
            labelTextColor = 0xFF475569.toInt(),
            labelTextSizeSp = 14f,
            minMaxTextColor = 0xFF64748B.toInt(),
            minMaxTextSizeSp = 13f,
            contentPaddingDp = 18f,
        )

        assertEquals(18f, defaults.progressThicknessDp, 0.0f)
        assertEquals(10f, defaults.tickLengthDp, 0.0f)
        assertEquals(2f, defaults.tickThicknessDp, 0.0f)
        assertEquals(3f, defaults.indicatorThicknessDp, 0.0f)
        assertEquals(5f, defaults.indicatorCenterRadiusDp, 0.0f)
        assertEquals(24f, defaults.valueTextSizeSp, 0.0f)
        assertEquals(13f, defaults.labelTextSizeSp, 0.0f)
        assertEquals(12f, defaults.minMaxTextSizeSp, 0.0f)
        assertEquals(16f, defaults.contentPaddingDp, 0.0f)

        assertEquals(0xFFF7FAFC.toInt(), custom.backgroundColor)
        assertEquals(0xFFE5E7EB.toInt(), custom.trackColor)
        assertEquals(0xFF13C3A3.toInt(), custom.progressColor)
        assertEquals(20f, custom.progressThicknessDp, 0.0f)
        assertEquals(0xFF9CA3AF.toInt(), custom.tickColor)
        assertEquals(12f, custom.tickLengthDp, 0.0f)
        assertEquals(3f, custom.tickThicknessDp, 0.0f)
        assertEquals(0xFF111827.toInt(), custom.indicatorColor)
        assertEquals(4f, custom.indicatorThicknessDp, 0.0f)
        assertEquals(0xFF0F172A.toInt(), custom.indicatorCenterColor)
        assertEquals(6f, custom.indicatorCenterRadiusDp, 0.0f)
        assertEquals(0xFF020617.toInt(), custom.valueTextColor)
        assertEquals(28f, custom.valueTextSizeSp, 0.0f)
        assertEquals(0xFF475569.toInt(), custom.labelTextColor)
        assertEquals(14f, custom.labelTextSizeSp, 0.0f)
        assertEquals(0xFF64748B.toInt(), custom.minMaxTextColor)
        assertEquals(13f, custom.minMaxTextSizeSp, 0.0f)
        assertEquals(18f, custom.contentPaddingDp, 0.0f)
    }
}
