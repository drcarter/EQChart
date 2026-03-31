package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.boxplot.BoxPlotChartPresentationOptions
import com.magimon.eq.boxplot.BoxPlotChartStyleOptions
import com.magimon.eq.boxplot.BoxPlotEntry
import com.magimon.eq.boxplot.boxPlotAxisTicks
import com.magimon.eq.boxplot.formatBoxPlotAxisValue
import com.magimon.eq.boxplot.resolveBoxPlotChartLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxPlotContractsTest {

    @Test
    fun boxPlotEntry_preservesValuesAndDefaults() {
        val payload = linkedMapOf("series" to "A")
        val entry = BoxPlotEntry(label = "A", min = 2.0, q1 = 4.0, median = 6.0, q3 = 8.0, max = 10.0)
        val custom = BoxPlotEntry(
            label = "B",
            min = 1.0,
            q1 = 3.0,
            median = 5.0,
            q3 = 7.0,
            max = 11.0,
            outliers = listOf(-1.0, 15.0),
            color = 0xFF224466.toInt(),
            title = "Beta",
            payload = payload,
        )

        assertEquals("A", entry.label)
        assertTrue(entry.outliers.isEmpty())
        assertNull(entry.color)
        assertNull(entry.title)
        assertNull(entry.payload)

        assertEquals(2, custom.outliers.size)
        assertEquals(0xFF224466.toInt(), custom.color)
        assertEquals("Beta", custom.title)
        assertSame(payload, custom.payload)
    }

    @Test
    fun boxPlotPresentationAndStyleDefaults_areAccessible() {
        val defaults = BoxPlotChartPresentationOptions()
        val custom = BoxPlotChartPresentationOptions(
            showGrid = false,
            showAxes = false,
            showValueLabels = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            emptyText = "EMPTY",
            axisLabelTextSizeSp = 13f,
            valueLabelTextSizeSp = 14f,
            yLabelFormatter = { value -> "y:${value.toInt()}" },
            valueLabelFormatter = { entry -> "m:${entry.median.toInt()}" },
            xLabelFormatter = { entry -> "x:${entry.label}" },
            yTickCount = 7,
        )
        val style = BoxPlotChartStyleOptions()

        assertTrue(defaults.showGrid)
        assertTrue(defaults.showAxes)
        assertTrue(defaults.showValueLabels)
        assertTrue(defaults.animateOnDataChange)
        assertTrue(defaults.animationDirection)
        assertEquals("1.5K", defaults.yLabelFormatter(1_500.0))
        assertEquals(6, defaults.yTickCount)

        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showValueLabels)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals("y:4", custom.yLabelFormatter(4.8))

        assertEquals(Color.WHITE, style.backgroundColor)
        assertEquals(Color.parseColor("#2B80FF"), style.defaultBoxColor)
        assertEquals(Color.parseColor("#0F172A"), style.medianLineColor)
        assertEquals(Color.parseColor("#EF4444"), style.outlierColor)
    }

    @Test
    fun boxPlotLayout_sanitizesAndSortsValues() {
        val original = BoxPlotEntry(
            label = "Latency",
            min = 20.0,
            q1 = 14.0,
            median = 18.0,
            q3 = 16.0,
            max = 30.0,
            outliers = listOf(9.0, 42.0, Double.NaN),
            color = 0xFF335577.toInt(),
            title = "P95 spread",
            payload = "latency",
        )
        val layout = resolveBoxPlotChartLayout(
            entries = listOf(
                original,
                BoxPlotEntry("Bad", Double.NaN, 1.0, 2.0, 3.0, 4.0),
                BoxPlotEntry("", 1.0, 2.0, 3.0, 4.0, 5.0),
            ),
            style = BoxPlotChartStyleOptions(defaultBoxColor = 10),
            presentation = BoxPlotChartPresentationOptions(yTickCount = 5),
        )

        assertEquals(1, layout.entries.size)
        val entry = layout.entries.single()
        assertEquals(14.0, entry.min, 0.0)
        assertEquals(16.0, entry.q1, 0.0)
        assertEquals(18.0, entry.median, 0.0)
        assertEquals(20.0, entry.q3, 0.0)
        assertEquals(30.0, entry.max, 0.0)
        assertEquals(listOf(9.0, 42.0), entry.outliers)
        assertEquals(0xFF335577.toInt(), entry.color)
        assertSame(original, entry.sourceEntry)
        assertEquals(9.0, layout.minValue, 0.0)
        assertEquals(42.0, layout.maxValue, 0.0)
        assertEquals(5, layout.ticks.size)
        assertEquals(listOf(9.0, 42.0), listOf(layout.ticks.first(), layout.ticks.last()))
    }

    @Test
    fun boxPlotLayout_handlesEmptyAndFlatRanges() {
        val empty = resolveBoxPlotChartLayout(
            entries = listOf(
                BoxPlotEntry("Bad", 1.0, 2.0, Double.NaN, 4.0, 5.0),
                BoxPlotEntry("   ", 1.0, 2.0, 3.0, 4.0, 5.0),
            ),
            presentation = BoxPlotChartPresentationOptions(yTickCount = 4),
        )
        val flat = resolveBoxPlotChartLayout(
            entries = listOf(
                BoxPlotEntry("Flat", 5.0, 5.0, 5.0, 5.0, 5.0),
            ),
        )

        assertTrue(empty.entries.isEmpty())
        assertEquals(-1.0, empty.minValue, 0.0)
        assertEquals(1.0, empty.maxValue, 0.0)
        assertEquals(4, empty.ticks.size)

        assertEquals(4.0, flat.minValue, 0.0)
        assertEquals(6.0, flat.maxValue, 0.0)
    }

    @Test
    fun boxPlotHelpers_generateExpectedTicksAndLabels() {
        val ticks = boxPlotAxisTicks(2.0, 8.0, 4)

        assertEquals("2.5M", formatBoxPlotAxisValue(2_500_000.0))
        assertEquals(4, ticks.size)
        assertEquals(2.0, ticks[0], 0.0)
        assertEquals(8.0, ticks[3], 0.0)
    }
}
