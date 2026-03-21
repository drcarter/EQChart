package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.sunburst.SunburstChartPresentationOptions
import com.magimon.eq.sunburst.SunburstChartStyleOptions
import com.magimon.eq.sunburst.SunburstSegmentLayout
import com.magimon.eq.sunburst.SunburstChartLayoutResult
import com.magimon.eq.sunburst.SunburstNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SunburstContractsTest {

    @Test
    fun sunburstNode_preservesValuesAndDefaults() {
        val payload = mapOf("group" to "growth")
        val defaultNode = SunburstNode(label = "Marketing", value = 20.0)
        val custom = SunburstNode(
            label = "Product",
            value = 40.0,
            color = 0xFF114477.toInt(),
            children = listOf(SunburstNode("Feature", 15.0)),
            payload = payload,
        )
        val copied = custom.copy(label = "Platform")

        assertEquals("Marketing", defaultNode.label)
        assertEquals(20.0, defaultNode.value, 0.0)
        assertNull(defaultNode.color)
        assertTrue(defaultNode.children.isEmpty())
        assertNull(defaultNode.payload)

        assertEquals("Product", custom.label)
        assertEquals(40.0, custom.value, 0.0)
        assertEquals(0xFF114477.toInt(), custom.color)
        assertEquals(1, custom.children.size)
        assertSame(payload, custom.payload)
        assertEquals("Platform", copied.label)
    }

    @Test
    fun sunburstPresentationDefaultsAndCustomValues_areAccessible() {
        val defaults = SunburstChartPresentationOptions()
        val custom = SunburstChartPresentationOptions(
            showLabels = false,
            minLabelSweepDeg = 8f,
            innerHoleRatio = 0.12f,
            ringGapRatio = 0.04f,
            startAngleDeg = 180f,
            clockwise = false,
            animateOnDataChange = false,
            enterAnimationDurationMs = 320L,
            enterAnimationDelayMs = 14L,
            centerText = "Revenue",
            centerSubText = "FY26",
            emptyText = "EMPTY",
        )

        assertTrue(defaults.showLabels)
        assertEquals(14f, defaults.minLabelSweepDeg, 0.0f)
        assertEquals(0.22f, defaults.innerHoleRatio, 0.0f)
        assertEquals(0.025f, defaults.ringGapRatio, 0.0f)
        assertEquals(-90f, defaults.startAngleDeg, 0.0f)
        assertTrue(defaults.clockwise)
        assertTrue(defaults.animateOnDataChange)
        assertEquals(650L, defaults.enterAnimationDurationMs)
        assertEquals("No data", defaults.emptyText)

        assertFalse(custom.showLabels)
        assertEquals(8f, custom.minLabelSweepDeg, 0.0f)
        assertEquals(0.12f, custom.innerHoleRatio, 0.0f)
        assertEquals(0.04f, custom.ringGapRatio, 0.0f)
        assertEquals(180f, custom.startAngleDeg, 0.0f)
        assertFalse(custom.clockwise)
        assertFalse(custom.animateOnDataChange)
        assertEquals(320L, custom.enterAnimationDurationMs)
        assertEquals(14L, custom.enterAnimationDelayMs)
        assertEquals("Revenue", custom.centerText)
        assertEquals("FY26", custom.centerSubText)
        assertEquals("EMPTY", custom.emptyText)
    }

    @Test
    fun sunburstStyleDefaultsAndCustomValues_areAccessible() {
        val defaults = SunburstChartStyleOptions()
        val custom = SunburstChartStyleOptions(
            backgroundColor = 1,
            segmentStrokeColor = 2,
            selectedSegmentStrokeColor = 3,
            segmentStrokeWidthDp = 4f,
            labelTextColor = 5,
            labelTextSizeSp = 6f,
            contentPaddingDp = 7f,
            centerTextColor = 8,
            centerTextSizeSp = 9f,
            centerSubTextColor = 10,
            centerSubTextSizeSp = 11f,
            segmentColors = listOf(12, 13),
        )
        val partial = SunburstChartStyleOptions(segmentStrokeWidthDp = 2f).copy(labelTextSizeSp = 10f)

        assertEquals(Color.WHITE, defaults.backgroundColor)
        assertEquals(Color.WHITE, defaults.segmentStrokeColor)
        assertEquals(Color.parseColor("#1F2937"), defaults.selectedSegmentStrokeColor)
        assertEquals(1.4f, defaults.segmentStrokeWidthDp, 0.0f)
        assertEquals(Color.parseColor("#243040"), defaults.labelTextColor)
        assertEquals(11.5f, defaults.labelTextSizeSp, 0.0f)
        assertEquals(12f, defaults.contentPaddingDp, 0.0f)
        assertEquals(Color.parseColor("#2563EB"), defaults.segmentColors.first())
        assertEquals(Color.parseColor("#06B6D4"), defaults.segmentColors.last())

        assertEquals(1, custom.backgroundColor)
        assertEquals(2, custom.segmentStrokeColor)
        assertEquals(3, custom.selectedSegmentStrokeColor)
        assertEquals(4f, custom.segmentStrokeWidthDp, 0.0f)
        assertEquals(5, custom.labelTextColor)
        assertEquals(6f, custom.labelTextSizeSp, 0.0f)
        assertEquals(7f, custom.contentPaddingDp, 0.0f)
        assertEquals(8, custom.centerTextColor)
        assertEquals(9f, custom.centerTextSizeSp, 0.0f)
        assertEquals(10, custom.centerSubTextColor)
        assertEquals(11f, custom.centerSubTextSizeSp, 0.0f)
        assertEquals(listOf(12, 13), custom.segmentColors)

        assertEquals(2f, partial.segmentStrokeWidthDp, 0.0f)
        assertEquals(10f, partial.labelTextSizeSp, 0.0f)
    }

    @Test
    fun sunburstLayoutModels_supportDefaultsAndAccessors() {
        val segment = SunburstSegmentLayout(
            label = "North America",
            value = 42.0,
            depth = 1,
            startAngleDeg = -90f,
            sweepAngleDeg = 135f,
            innerRadiusRatio = 0.3f,
            outerRadiusRatio = 0.6f,
            color = 0xFF224466.toInt(),
            payload = "na",
            path = listOf(0, 1),
            hasChildren = true,
        )
        val result = SunburstChartLayoutResult(
            segments = listOf(segment),
            depthCount = 2,
            totalValue = 42.0,
            isRenderable = true,
        )

        assertEquals(listOf(0, 1), segment.path)
        assertTrue(segment.hasChildren)
        assertNull(result.emptyReason)
        assertEquals(1, result.segments.size)
        assertEquals(84.0, result.copy(totalValue = 84.0).totalValue, 0.0)
    }
}
