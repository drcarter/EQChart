package com.magimon.eq.common

import com.magimon.eq.bubble3d.Bubble3DAxisOptions
import com.magimon.eq.bubble3d.Bubble3DCameraOptions
import com.magimon.eq.bubble3d.Bubble3DDatum
import com.magimon.eq.bubble3d.Bubble3DPresentationOptions
import com.magimon.eq.bubble3d.Bubble3DScaleOverride
import android.graphics.Color
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class Bubble3DContractsTest {

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun bubble3DAxisOptions_defaultsExposeFlagsAndFormatters() {
        val options = Bubble3DAxisOptions()

        assertTrue(options.showAxes)
        assertTrue(options.showGridPlanes)
        assertTrue(options.showTicks)
        assertEquals(4, options.gridDivisions)
        assertNull(options.xAxisTitle)
        assertNull(options.yAxisTitle)
        assertNull(options.zAxisTitle)
        assertEquals("1.0B", options.xLabelFormatter(1_000_000_000.0))
        assertEquals("500", options.yLabelFormatter(500.0))
        assertEquals("9.50", options.zLabelFormatter(9.5))
    }

    @Test
    fun bubble3DAxisOptions_customFormattersAreUsed() {
        val options = Bubble3DAxisOptions(
            showAxes = false,
            showGridPlanes = false,
            showTicks = false,
            gridDivisions = 6,
            xAxisTitle = "Talent",
            yAxisTitle = "Funding",
            zAxisTitle = "Scale",
            xLabelFormatter = { value -> "x=${value.toInt()}" },
            yLabelFormatter = { value -> "y=${value.toInt()}" },
            zLabelFormatter = { value -> "z=${value.toInt()}" },
        )

        assertFalse(options.showAxes)
        assertFalse(options.showGridPlanes)
        assertFalse(options.showTicks)
        assertEquals(6, options.gridDivisions)
        assertEquals("Talent", options.xAxisTitle)
        assertEquals("Funding", options.yAxisTitle)
        assertEquals("Scale", options.zAxisTitle)
        assertEquals("x=4", options.xLabelFormatter(4.8))
        assertEquals("y=5", options.yLabelFormatter(5.1))
        assertEquals("z=6", options.zLabelFormatter(6.9))
    }

    @Test
    fun bubble3DModels_preserveProvidedValues() {
        val payload = linkedMapOf("id" to 9)
        val defaultDatum = Bubble3DDatum(
            x = 1.0,
            y = 2.0,
            z = 3.0,
            size = 4.0,
            color = 0xFF000000.toInt(),
        )
        val datum = Bubble3DDatum(
            x = 12.5,
            y = -4.25,
            z = 7.75,
            size = 99.0,
            color = 0xFF336699.toInt(),
            label = "Node A",
            legendGroup = "Series A",
            payload = payload,
        )
        val copied = datum.copy(size = 100.0, label = "Copied")
        val defaultCamera = Bubble3DCameraOptions()
        val customCamera = Bubble3DCameraOptions(
            yawDegrees = -20f,
            pitchDegrees = 18f,
            distance = 6.5f,
            minDistance = 3f,
            maxDistance = 12f,
            fovDegrees = 52f,
        )
        val defaultScaleOverride = Bubble3DScaleOverride()
        val scaleOverride = Bubble3DScaleOverride(
            xMin = 1.0,
            xMax = 2.0,
            yMin = 3.0,
            yMax = 4.0,
            zMin = 5.0,
            zMax = 6.0,
            sizeMin = 7.0,
            sizeMax = 8.0,
        )

        assertEquals(1.0, defaultDatum.x, 0.0)
        assertEquals(2.0, defaultDatum.y, 0.0)
        assertEquals(3.0, defaultDatum.z, 0.0)
        assertEquals(4.0, defaultDatum.size, 0.0)
        assertEquals(0xFF000000.toInt(), defaultDatum.color)
        assertNull(defaultDatum.label)
        assertNull(defaultDatum.legendGroup)
        assertNull(defaultDatum.payload)

        assertEquals(12.5, datum.x, 0.0)
        assertEquals(-4.25, datum.y, 0.0)
        assertEquals(7.75, datum.z, 0.0)
        assertEquals(99.0, datum.size, 0.0)
        assertEquals(0xFF336699.toInt(), datum.color)
        assertEquals("Node A", datum.label)
        assertEquals("Series A", datum.legendGroup)
        assertSame(payload, datum.payload)

        assertEquals(100.0, copied.size, 0.0)
        assertEquals("Copied", copied.label)
        assertSame(payload, copied.payload)

        assertEquals(-36f, defaultCamera.yawDegrees, 0.0f)
        assertEquals(24f, defaultCamera.pitchDegrees, 0.0f)
        assertEquals(4.6f, defaultCamera.distance, 0.0f)
        assertEquals(2.2f, defaultCamera.minDistance, 0.0f)
        assertEquals(10f, defaultCamera.maxDistance, 0.0f)
        assertEquals(45f, defaultCamera.fovDegrees, 0.0f)

        assertEquals(-20f, customCamera.yawDegrees, 0.0f)
        assertEquals(18f, customCamera.pitchDegrees, 0.0f)
        assertEquals(6.5f, customCamera.distance, 0.0f)
        assertEquals(3f, customCamera.minDistance, 0.0f)
        assertEquals(12f, customCamera.maxDistance, 0.0f)
        assertEquals(52f, customCamera.fovDegrees, 0.0f)

        assertNull(defaultScaleOverride.xMin)
        assertNull(defaultScaleOverride.xMax)
        assertNull(defaultScaleOverride.yMin)
        assertNull(defaultScaleOverride.yMax)
        assertNull(defaultScaleOverride.zMin)
        assertNull(defaultScaleOverride.zMax)
        assertNull(defaultScaleOverride.sizeMin)
        assertNull(defaultScaleOverride.sizeMax)

        assertEquals(1.0, requireNotNull(scaleOverride.xMin), 0.0)
        assertEquals(2.0, requireNotNull(scaleOverride.xMax), 0.0)
        assertEquals(3.0, requireNotNull(scaleOverride.yMin), 0.0)
        assertEquals(4.0, requireNotNull(scaleOverride.yMax), 0.0)
        assertEquals(5.0, requireNotNull(scaleOverride.zMin), 0.0)
        assertEquals(6.0, requireNotNull(scaleOverride.zMax), 0.0)
        assertEquals(7.0, requireNotNull(scaleOverride.sizeMin), 0.0)
        assertEquals(8.0, requireNotNull(scaleOverride.sizeMax), 0.0)
    }

    @Test
    fun bubble3DPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = Bubble3DPresentationOptions()
        val custom = Bubble3DPresentationOptions(
            backgroundColor = 0xFF010203.toInt(),
            gridColor = 0xFF223344.toInt(),
            xAxisColor = 0xFF334455.toInt(),
            yAxisColor = 0xFF445566.toInt(),
            zAxisColor = 0xFF556677.toInt(),
            ambientLight = 0.4f,
            minBubbleRadius = 0.1f,
            maxBubbleRadius = 0.3f,
        )

        assertEquals(Color.parseColor("#09111B"), defaults.backgroundColor)
        assertEquals(Color.parseColor("#334155"), defaults.gridColor)
        assertEquals(Color.parseColor("#F97316"), defaults.xAxisColor)
        assertEquals(Color.parseColor("#22C55E"), defaults.yAxisColor)
        assertEquals(Color.parseColor("#38BDF8"), defaults.zAxisColor)
        assertEquals(0.32f, defaults.ambientLight, 0.0f)
        assertEquals(0.08f, defaults.minBubbleRadius, 0.0f)
        assertEquals(0.26f, defaults.maxBubbleRadius, 0.0f)

        assertEquals(0xFF010203.toInt(), custom.backgroundColor)
        assertEquals(0xFF223344.toInt(), custom.gridColor)
        assertEquals(0xFF334455.toInt(), custom.xAxisColor)
        assertEquals(0xFF445566.toInt(), custom.yAxisColor)
        assertEquals(0xFF556677.toInt(), custom.zAxisColor)
        assertEquals(0.4f, custom.ambientLight, 0.0f)
        assertEquals(0.1f, custom.minBubbleRadius, 0.0f)
        assertEquals(0.3f, custom.maxBubbleRadius, 0.0f)
    }
}
