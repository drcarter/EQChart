package com.magimon.eq.common

import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class Point3DContractsTest {

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
    fun point3DAxisOptions_defaultsExposeFlagsAndFormatters() {
        val options = Point3DAxisOptions()

        assertTrue(options.showAxes)
        assertTrue(options.showGridPlanes)
        assertTrue(options.showTicks)
        assertEquals(4, options.gridDivisions)
        assertNull(options.xAxisTitle)
        assertNull(options.yAxisTitle)
        assertNull(options.zAxisTitle)
        assertEquals("1.0B", options.xLabelFormatter(1_000_000_000.0))
        assertEquals("2.0M", options.xLabelFormatter(2_000_000.0))
        assertEquals("1.5K", options.yLabelFormatter(1_500.0))
        assertEquals("500", options.yLabelFormatter(500.0))
        assertEquals("42.5", options.zLabelFormatter(42.5))
        assertEquals("9.50", options.zLabelFormatter(9.5))
    }

    @Test
    fun point3DAxisOptions_customFormattersAndTitlesAreAccessible() {
        val options = Point3DAxisOptions(
            showAxes = false,
            showGridPlanes = false,
            showTicks = false,
            gridDivisions = 6,
            xAxisTitle = "Distance",
            yAxisTitle = "Altitude",
            zAxisTitle = "Energy",
            xLabelFormatter = { value -> "x=${value.toInt()}" },
            yLabelFormatter = { value -> "y=${value.toInt()}" },
            zLabelFormatter = { value -> "z=${value.toInt()}" },
        )

        assertFalse(options.showAxes)
        assertFalse(options.showGridPlanes)
        assertFalse(options.showTicks)
        assertEquals(6, options.gridDivisions)
        assertEquals("Distance", options.xAxisTitle)
        assertEquals("Altitude", options.yAxisTitle)
        assertEquals("Energy", options.zAxisTitle)
        assertEquals("x=4", options.xLabelFormatter(4.8))
        assertEquals("y=5", options.yLabelFormatter(5.1))
        assertEquals("z=6", options.zLabelFormatter(6.9))
    }

    @Test
    fun point3DCameraOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = Point3DCameraOptions()
        val custom = Point3DCameraOptions(
            yawDegrees = -20f,
            pitchDegrees = 18f,
            distance = 6.5f,
            minDistance = 3f,
            maxDistance = 12f,
            fovDegrees = 52f,
        )

        assertEquals(-36f, defaults.yawDegrees, 0.0f)
        assertEquals(24f, defaults.pitchDegrees, 0.0f)
        assertEquals(4.6f, defaults.distance, 0.0f)
        assertEquals(2.2f, defaults.minDistance, 0.0f)
        assertEquals(10f, defaults.maxDistance, 0.0f)
        assertEquals(45f, defaults.fovDegrees, 0.0f)

        assertEquals(-20f, custom.yawDegrees, 0.0f)
        assertEquals(18f, custom.pitchDegrees, 0.0f)
        assertEquals(6.5f, custom.distance, 0.0f)
        assertEquals(3f, custom.minDistance, 0.0f)
        assertEquals(12f, custom.maxDistance, 0.0f)
        assertEquals(52f, custom.fovDegrees, 0.0f)
    }
}
