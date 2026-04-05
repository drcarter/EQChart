package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.rangebar.RangeBarChartLayoutConfig
import com.magimon.eq.rangebar.RangeBarChartPresentationOptions
import com.magimon.eq.rangebar.RangeBarChartStyleOptions
import com.magimon.eq.rangebar.RangeBarEntry
import com.magimon.eq.rangebar.formatRangeBarAxisValue
import com.magimon.eq.rangebar.hitTestRangeBarEntry
import com.magimon.eq.rangebar.rangeBarAxisTicks
import com.magimon.eq.rangebar.resolveRangeBarChartLayout
import com.magimon.eq.rangebar.resolveRangeBarChartPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeBarContractsTest {

    @Test
    fun rangeBarEntry_preservesValuesAndDefaults() {
        val payload = mapOf("phase" to "build")
        val entry = RangeBarEntry(label = "Build", start = 2.0, end = 6.0)
        val custom = RangeBarEntry(
            label = "Launch",
            start = 8.0,
            end = 3.0,
            color = 0xFF123456.toInt(),
            title = "Milestone",
            payload = payload,
        )
        val copied = custom.copy(title = "GA")

        assertEquals("Build", entry.label)
        assertEquals(2.0, entry.start, 0.0)
        assertEquals(6.0, entry.end, 0.0)
        assertNull(entry.color)
        assertNull(entry.title)
        assertNull(entry.payload)

        assertEquals("Launch", custom.label)
        assertEquals(0xFF123456.toInt(), custom.color)
        assertEquals("Milestone", custom.title)
        assertSame(payload, custom.payload)
        assertEquals("GA", copied.title)
    }

    @Test
    fun rangeBarPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = RangeBarChartPresentationOptions()
        val custom = RangeBarChartPresentationOptions(
            showGrid = false,
            showAxes = false,
            showBarLabels = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 15L,
            animationDirection = false,
            emptyText = "EMPTY",
            axisLabelTextSizeSp = 13f,
            barLabelTextSizeSp = 14f,
            xLabelFormatter = { value -> "x:${value.toInt()}" },
            rowLabelFormatter = { value -> value.uppercase() },
            barLabelFormatter = { entry -> "span:${entry.label}" },
            xTickCount = 7,
        )

        val entry = resolveRangeBarChartLayout(
            listOf(RangeBarEntry(label = "Plan", start = 1.0, end = 3.0, title = "Alpha")),
        ).entries.single()

        assertTrue(defaults.showGrid)
        assertTrue(defaults.showAxes)
        assertTrue(defaults.showBarLabels)
        assertTrue(defaults.animateOnDataChange)
        assertTrue(defaults.animationDirection)
        assertEquals("No data", defaults.emptyText)
        assertEquals(11.5f, defaults.axisLabelTextSizeSp, 0.0f)
        assertEquals(11.5f, defaults.barLabelTextSizeSp, 0.0f)
        assertEquals("4", defaults.xLabelFormatter(4.0))
        assertEquals("4.5", defaults.xLabelFormatter(4.5))
        assertEquals("1.5K", defaults.xLabelFormatter(1_500.0))
        assertEquals("2.5M", defaults.xLabelFormatter(2_500_000.0))
        assertEquals("0", defaults.xLabelFormatter(Double.NaN))
        assertEquals("Plan", defaults.rowLabelFormatter("Plan"))
        assertEquals("Alpha", defaults.barLabelFormatter(entry))
        assertEquals(6, defaults.xTickCount)

        assertFalse(custom.showGrid)
        assertFalse(custom.showAxes)
        assertFalse(custom.showBarLabels)
        assertFalse(custom.animateOnDataChange)
        assertFalse(custom.animationDirection)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(15L, custom.enterAnimationDelayMs)
        assertEquals("EMPTY", custom.emptyText)
        assertEquals(13f, custom.axisLabelTextSizeSp, 0.0f)
        assertEquals(14f, custom.barLabelTextSizeSp, 0.0f)
        assertEquals("x:4", custom.xLabelFormatter(4.8))
        assertEquals("PLAN", custom.rowLabelFormatter("Plan"))
        assertEquals("span:Plan", custom.barLabelFormatter(entry))
        assertEquals(7, custom.xTickCount)
    }

    @Test
    fun rangeBarStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = RangeBarChartStyleOptions()
        val custom = RangeBarChartStyleOptions(
            backgroundColor = 1,
            gridColor = 2,
            axisColor = 3,
            axisLabelColor = 4,
            barLabelTextColor = 5,
            defaultBarColor = 6,
            selectedBarStrokeColor = 7,
            rowSpacingDp = 8f,
            barHeightRatio = 0.72f,
            barCornerRadiusDp = 9f,
            selectedBarPaddingDp = 10f,
            contentPaddingDp = 11f,
            minBarWidthDp = 12f,
        )

        assertEquals(Color.WHITE, defaults.backgroundColor)
        assertEquals(Color.parseColor("#D7DFE8"), defaults.gridColor)
        assertEquals(Color.parseColor("#8F9CAB"), defaults.axisColor)
        assertEquals(Color.parseColor("#5E6878"), defaults.axisLabelColor)
        assertEquals(Color.parseColor("#263244"), defaults.barLabelTextColor)
        assertEquals(Color.parseColor("#2563EB"), defaults.defaultBarColor)
        assertEquals(Color.parseColor("#0F172A"), defaults.selectedBarStrokeColor)
        assertEquals(12f, defaults.rowSpacingDp, 0.0f)
        assertEquals(0.58f, defaults.barHeightRatio, 0.0f)
        assertEquals(4f, defaults.barCornerRadiusDp, 0.0f)
        assertEquals(1.5f, defaults.selectedBarPaddingDp, 0.0f)
        assertEquals(14f, defaults.contentPaddingDp, 0.0f)
        assertEquals(6f, defaults.minBarWidthDp, 0.0f)

        assertEquals(1, custom.backgroundColor)
        assertEquals(7, custom.selectedBarStrokeColor)
        assertEquals(8f, custom.rowSpacingDp, 0.0f)
        assertEquals(0.72f, custom.barHeightRatio, 0.0f)
        assertEquals(11f, custom.contentPaddingDp, 0.0f)
        assertEquals(12f, custom.minBarWidthDp, 0.0f)
    }

    @Test
    fun rangeBarLayoutEngine_sanitizesIntervalsAndBuildsTicks() {
        val payload = mapOf("id" to 7)
        val layout = resolveRangeBarChartLayout(
            entries = listOf(
                RangeBarEntry("Plan", 1.0, 3.0, payload = payload),
                RangeBarEntry("Build", 6.0, 4.0, color = 0xFF234567.toInt(), title = "Sprint 1"),
                RangeBarEntry("", 2.0, 5.0),
                RangeBarEntry("Bad", Double.NaN, 5.0),
                RangeBarEntry("AlsoBad", 2.0, Double.NaN),
            ),
            style = RangeBarChartStyleOptions(defaultBarColor = 11),
            tickCount = 5,
        )

        assertEquals(2, layout.entries.size)
        assertEquals(0, layout.entries[0].index)
        assertEquals("Plan", layout.entries[0].label)
        assertEquals(1.0, layout.entries[0].rawStartValue, 0.0)
        assertEquals(3.0, layout.entries[0].rawEndValue, 0.0)
        assertEquals(1.0, layout.entries[0].startValue, 0.0)
        assertEquals(3.0, layout.entries[0].endValue, 0.0)
        assertEquals(2.0, layout.entries[0].duration, 0.0)
        assertEquals(11, layout.entries[0].color)
        assertSame(payload, layout.entries[0].payload)

        assertEquals("Build", layout.entries[1].label)
        assertEquals("Sprint 1", layout.entries[1].title)
        assertEquals(6.0, layout.entries[1].rawStartValue, 0.0)
        assertEquals(4.0, layout.entries[1].rawEndValue, 0.0)
        assertEquals(4.0, layout.entries[1].startValue, 0.0)
        assertEquals(6.0, layout.entries[1].endValue, 0.0)
        assertEquals(2.0, layout.entries[1].duration, 0.0)
        assertEquals(0xFF234567.toInt(), layout.entries[1].color)

        assertEquals(1.0, layout.minValue, 0.0)
        assertEquals(6.0, layout.maxValue, 0.0)
        assertEquals(5, layout.ticks.size)
        assertEquals(1.0, layout.ticks.first(), 0.0)
        assertEquals(6.0, layout.ticks.last(), 0.0)
        assertEquals(listOf(1.0, 6.0), rangeBarAxisTicks(1.0, 6.0, 1))
    }

    @Test
    fun rangeBarLayoutEngine_handlesEmptyAndCollapsedRanges() {
        val empty = resolveRangeBarChartLayout(emptyList())
        val collapsed = resolveRangeBarChartLayout(
            listOf(RangeBarEntry("Milestone", 3.0, 3.0)),
        )

        assertTrue(empty.entries.isEmpty())
        assertEquals(-1.0, empty.minValue, 0.0)
        assertEquals(1.0, empty.maxValue, 0.0)
        assertEquals(-1.0, empty.ticks.first(), 0.0)
        assertEquals(1.0, empty.ticks.last(), 0.0)

        assertEquals(1, collapsed.entries.size)
        assertEquals(3.0, collapsed.entries[0].startValue, 0.0)
        assertEquals(3.0, collapsed.entries[0].endValue, 0.0)
        assertEquals(0.0, collapsed.entries[0].duration, 0.0)
        assertEquals(2.0, collapsed.minValue, 0.0)
        assertEquals(4.0, collapsed.maxValue, 0.0)
    }

    @Test
    fun rangeBarLayoutEngine_normalizesNegativeRangesAndBlankTitles() {
        val layout = resolveRangeBarChartLayout(
            entries = listOf(
                RangeBarEntry("Backlog", -6.0, -2.0, title = " "),
                RangeBarEntry("Cutover", -1.0, 3.0, title = "Window"),
            ),
        )

        assertEquals(2, layout.entries.size)
        assertNull(layout.entries[0].title)
        assertEquals(-6.0, layout.entries[0].startValue, 0.0)
        assertEquals(-2.0, layout.entries[0].endValue, 0.0)
        assertEquals("Window", layout.entries[1].title)
        assertTrue(layout.minValue < 0.0)
        assertTrue(layout.maxValue > 0.0)
    }

    @Test
    fun rangeBarLayoutEngine_updatesMinWhenLaterEntryStartsEarlier() {
        val layout = resolveRangeBarChartLayout(
            entries = listOf(
                RangeBarEntry("Late", 5.0, 8.0),
                RangeBarEntry("Early", 1.0, 2.0),
            ),
        )

        assertEquals(1.0, layout.minValue, 0.0)
        assertEquals(8.0, layout.maxValue, 0.0)
    }

    @Test
    fun rangeBarPlacementEngine_resolvesPixelGeometryAndHitTesting() {
        val layout = resolveRangeBarChartLayout(
            entries = listOf(
                RangeBarEntry("Plan", 0.0, 2.0, title = "Kickoff"),
                RangeBarEntry("Ship", 4.0, 4.0, title = "Launch"),
            ),
        )
        val placement = resolveRangeBarChartPlacement(
            layout = layout,
            config = RangeBarChartLayoutConfig(
                widthPx = 300f,
                heightPx = 180f,
                contentPaddingPx = 12f,
                rowSpacingPx = 10f,
                minBarWidthPx = 8f,
                rowLabelReservedWidthPx = 60f,
                xAxisLabelReservedHeightPx = 24f,
            ),
            presentation = RangeBarChartPresentationOptions(
                xLabelFormatter = { value -> "t${value.toInt()}" },
                rowLabelFormatter = { value -> "R:$value" },
                barLabelFormatter = { entry -> "B:${entry.title ?: entry.label}" },
            ),
        )

        assertEquals(2, placement.entries.size)
        assertEquals(6, placement.ticks.size)
        assertEquals(2, placement.rowLabels.size)
        assertTrue(placement.plotLeftPx < placement.plotRightPx)
        assertTrue(placement.plotTopPx < placement.plotBottomPx)
        assertTrue(placement.rowHeightPx > 0f)
        assertEquals(layout.entries[0], placement.entries[0].entry)
        assertEquals("B:Kickoff", placement.entries[0].labelText)
        assertEquals(0, placement.rowLabels[0].index)
        assertEquals("R:Plan", placement.rowLabels[0].label)
        assertEquals(placement.entries[0].centerYPx, placement.rowLabels[0].centerYPx, 0.0f)
        assertEquals(0.0, placement.ticks.first().value, 0.0)
        assertTrue(placement.ticks.first().xPx >= placement.plotLeftPx)
        assertEquals("t0", placement.ticks.first().label)
        assertTrue(placement.entries[1].rightPx - placement.entries[1].leftPx >= 8f)

        val hit = hitTestRangeBarEntry(
            placement = placement,
            xPx = placement.entries[0].centerXPx,
            yPx = placement.entries[0].centerYPx,
        )
        assertEquals(placement.entries[0], hit)
        assertNull(hitTestRangeBarEntry(placement, 4f, 4f))
    }

    @Test
    fun rangeBarPlacementEngine_returnsEmptyWhenPlotAreaIsInvalid() {
        val layout = resolveRangeBarChartLayout(listOf(RangeBarEntry("Plan", 0.0, 1.0)))
        val placement = resolveRangeBarChartPlacement(
            layout = layout,
            config = RangeBarChartLayoutConfig(
                widthPx = 10f,
                heightPx = 10f,
                contentPaddingPx = 12f,
                rowSpacingPx = 4f,
                minBarWidthPx = 6f,
                rowLabelReservedWidthPx = 40f,
                xAxisLabelReservedHeightPx = 20f,
            ),
        )

        assertTrue(placement.entries.isEmpty())
        assertTrue(placement.ticks.isEmpty())
        assertTrue(placement.rowLabels.isEmpty())
        assertEquals(0f, placement.rowHeightPx, 0.0f)
    }

    @Test
    fun rangeBarPlacementEngine_returnsEmptyWhenPlotHeightIsInvalidOnly() {
        val layout = resolveRangeBarChartLayout(listOf(RangeBarEntry("Plan", 0.0, 1.0)))
        val placement = resolveRangeBarChartPlacement(
            layout = layout,
            config = RangeBarChartLayoutConfig(
                widthPx = 180f,
                heightPx = 20f,
                contentPaddingPx = 8f,
                rowSpacingPx = 4f,
                minBarWidthPx = 6f,
                rowLabelReservedWidthPx = 20f,
                xAxisLabelReservedHeightPx = 24f,
            ),
        )

        assertTrue(placement.entries.isEmpty())
        assertTrue(placement.ticks.isEmpty())
        assertTrue(placement.rowLabels.isEmpty())
    }

    @Test
    fun rangeBarPlacementEngine_returnsEmptyForEmptyLayouts() {
        val placement = resolveRangeBarChartPlacement(
            layout = resolveRangeBarChartLayout(emptyList()),
            config = RangeBarChartLayoutConfig(
                widthPx = 220f,
                heightPx = 120f,
                contentPaddingPx = 10f,
                rowSpacingPx = 8f,
                minBarWidthPx = 12f,
                rowLabelReservedWidthPx = 48f,
                xAxisLabelReservedHeightPx = 20f,
            ),
        )

        assertTrue(placement.entries.isEmpty())
        assertTrue(placement.ticks.isEmpty())
        assertTrue(placement.rowLabels.isEmpty())
    }

    @Test
    fun rangeBarPlacementEngine_coversDefaultFormatterAndZeroSpanPlacement() {
        val layoutEntry = resolveRangeBarChartLayout(
            listOf(RangeBarEntry("Review", 2.0, 2.0, title = "")),
        ).entries.single()
        val defaults = RangeBarChartPresentationOptions()

        assertEquals("Review: 2-2", defaults.barLabelFormatter(layoutEntry))

        val flatPlacement = resolveRangeBarChartPlacement(
            layout = com.magimon.eq.rangebar.RangeBarChartLayout(
                entries = listOf(layoutEntry),
                minValue = 5.0,
                maxValue = 5.0,
                ticks = listOf(5.0, 5.0),
            ),
            config = RangeBarChartLayoutConfig(
                widthPx = 220f,
                heightPx = 120f,
                contentPaddingPx = 10f,
                rowSpacingPx = 8f,
                minBarWidthPx = 12f,
                rowLabelReservedWidthPx = 48f,
                xAxisLabelReservedHeightPx = 20f,
            ),
        )

        assertEquals(1, flatPlacement.entries.size)
        assertTrue(flatPlacement.entries[0].rightPx - flatPlacement.entries[0].leftPx >= 12f)
        assertEquals(flatPlacement.plotLeftPx, flatPlacement.ticks.first().xPx, 0.0f)

        val middleCollapsedPlacement = resolveRangeBarChartPlacement(
            layout = resolveRangeBarChartLayout(
                listOf(RangeBarEntry("Middle", 5.0, 5.0)),
            ),
            config = RangeBarChartLayoutConfig(
                widthPx = 260f,
                heightPx = 120f,
                contentPaddingPx = 10f,
                rowSpacingPx = 8f,
                minBarWidthPx = 12f,
                rowLabelReservedWidthPx = 40f,
                xAxisLabelReservedHeightPx = 20f,
            ),
        )
        assertTrue(middleCollapsedPlacement.entries[0].rightPx - middleCollapsedPlacement.entries[0].leftPx >= 12f)
    }

    @Test
    fun rangeBarPlacementEngine_handlesNegativeRowSpacingAndRightEdgeMinWidth() {
        val placement = resolveRangeBarChartPlacement(
            layout = resolveRangeBarChartLayout(
                listOf(RangeBarEntry("Release", 9.0, 9.0)),
            ),
            config = RangeBarChartLayoutConfig(
                widthPx = 180f,
                heightPx = 110f,
                contentPaddingPx = 10f,
                rowSpacingPx = -12f,
                minBarWidthPx = 28f,
                rowLabelReservedWidthPx = 30f,
                xAxisLabelReservedHeightPx = 18f,
            ),
        )

        assertEquals(1, placement.entries.size)
        assertTrue(placement.entries[0].rightPx <= placement.plotRightPx)
        assertTrue(placement.entries[0].rightPx - placement.entries[0].leftPx >= 28f)
        assertTrue(placement.entries[0].leftPx >= placement.plotLeftPx)
        assertEquals(72f, placement.rowHeightPx, 0.0f)
    }

    @Test
    fun formatRangeBarAxisValue_formatsStandaloneCases() {
        assertEquals("0", formatRangeBarAxisValue(Double.NEGATIVE_INFINITY))
        assertEquals("999", formatRangeBarAxisValue(999.0))
        assertEquals("1K", formatRangeBarAxisValue(1_000.0))
        assertEquals("1.2K", formatRangeBarAxisValue(1_250.0))
        assertEquals("1M", formatRangeBarAxisValue(1_000_000.0))
        assertEquals("1.2M", formatRangeBarAxisValue(1_250_000.0))
    }

    @Test
    fun hitTestRangeBarEntry_handlesYAxisMissesAndEmptyPlacements() {
        val layout = resolveRangeBarChartLayout(
            entries = listOf(RangeBarEntry("Plan", 0.0, 2.0)),
        )
        val placement = resolveRangeBarChartPlacement(
            layout = layout,
            config = RangeBarChartLayoutConfig(
                widthPx = 240f,
                heightPx = 120f,
                contentPaddingPx = 10f,
                rowSpacingPx = 8f,
                minBarWidthPx = 8f,
                rowLabelReservedWidthPx = 48f,
                xAxisLabelReservedHeightPx = 20f,
            ),
        )

        assertNull(
            hitTestRangeBarEntry(
                placement = placement,
                xPx = placement.entries[0].centerXPx,
                yPx = placement.entries[0].bottomPx + 4f,
            ),
        )
        assertNull(
            hitTestRangeBarEntry(
                placement = placement,
                xPx = placement.entries[0].centerXPx,
                yPx = placement.entries[0].topPx - 4f,
            ),
        )
        assertNull(
            hitTestRangeBarEntry(
                placement = placement,
                xPx = placement.entries[0].leftPx - 2f,
                yPx = placement.entries[0].centerYPx,
            ),
        )
        assertNull(
            hitTestRangeBarEntry(
                placement = placement,
                xPx = placement.entries[0].rightPx + 2f,
                yPx = placement.entries[0].centerYPx,
            ),
        )
        assertNull(
            hitTestRangeBarEntry(
                placement = resolveRangeBarChartPlacement(
                    layout = resolveRangeBarChartLayout(emptyList()),
                    config = RangeBarChartLayoutConfig(
                        widthPx = 240f,
                        heightPx = 120f,
                        contentPaddingPx = 10f,
                        rowSpacingPx = 8f,
                        minBarWidthPx = 8f,
                        rowLabelReservedWidthPx = 48f,
                        xAxisLabelReservedHeightPx = 20f,
                    ),
                ),
                xPx = 20f,
                yPx = 20f,
            ),
        )
    }
}
