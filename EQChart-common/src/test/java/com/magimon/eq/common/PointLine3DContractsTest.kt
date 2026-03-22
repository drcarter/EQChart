package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.pointline3d.PointLine3DDatum
import com.magimon.eq.pointline3d.PointLine3DPresentationOptions
import com.magimon.eq.pointline3d.PointLine3DScaleOverride
import com.magimon.eq.pointline3d.PointLine3DSeries
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class PointLine3DContractsTest {

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
    fun pointLine3DModels_preserveProvidedValues() {
        val pointPayload = linkedMapOf("id" to 9)
        val seriesPayload = linkedMapOf("series" to "north")
        val point = PointLine3DDatum(
            x = 12.5,
            y = -4.25,
            z = 7.75,
            color = 0xFFABCDEF.toInt(),
            label = "Node A",
            payload = pointPayload,
        )
        val copiedPoint = point.copy(label = "Node B")
        val series = PointLine3DSeries(
            name = "North",
            lineColor = 0xFF336699.toInt(),
            points = listOf(point),
            pointColor = 0xFF5588CC.toInt(),
            lineWidthPx = 4.5f,
            pointRadiusScale = 1.4f,
            payload = seriesPayload,
        )
        val scaleOverride = PointLine3DScaleOverride(
            xMin = 1.0,
            xMax = 2.0,
            yMin = 3.0,
            yMax = 4.0,
            zMin = 5.0,
            zMax = 6.0,
        )

        assertEquals(12.5, point.x, 0.0)
        assertEquals(-4.25, point.y, 0.0)
        assertEquals(7.75, point.z, 0.0)
        assertEquals(0xFFABCDEF.toInt(), point.color)
        assertEquals("Node A", point.label)
        assertSame(pointPayload, point.payload)

        assertEquals("Node B", copiedPoint.label)
        assertSame(pointPayload, copiedPoint.payload)

        assertEquals("North", series.name)
        assertEquals(0xFF336699.toInt(), series.lineColor)
        assertEquals(0xFF5588CC.toInt(), series.pointColor)
        assertEquals(4.5f, series.lineWidthPx, 0.0f)
        assertEquals(1.4f, series.pointRadiusScale, 0.0f)
        assertSame(seriesPayload, series.payload)
        assertEquals(1, series.points.size)

        assertEquals(1.0, requireNotNull(scaleOverride.xMin), 0.0)
        assertEquals(2.0, requireNotNull(scaleOverride.xMax), 0.0)
        assertEquals(3.0, requireNotNull(scaleOverride.yMin), 0.0)
        assertEquals(4.0, requireNotNull(scaleOverride.yMax), 0.0)
        assertEquals(5.0, requireNotNull(scaleOverride.zMin), 0.0)
        assertEquals(6.0, requireNotNull(scaleOverride.zMax), 0.0)
    }

    @Test
    fun pointLine3DModels_defaultValuesRemainAccessible() {
        val point = PointLine3DDatum(1.0, 2.0, 3.0)
        val series = PointLine3DSeries(
            name = "Series",
            lineColor = 0xFF123456.toInt(),
            points = listOf(point),
        )
        val scaleOverride = PointLine3DScaleOverride()

        assertNull(point.color)
        assertNull(point.label)
        assertNull(point.payload)
        assertEquals(0xFF123456.toInt(), series.lineColor)
        assertEquals(0xFF123456.toInt(), series.pointColor)
        assertEquals(3f, series.lineWidthPx, 0.0f)
        assertEquals(1f, series.pointRadiusScale, 0.0f)
        assertNull(series.payload)

        assertNull(scaleOverride.xMin)
        assertNull(scaleOverride.xMax)
        assertNull(scaleOverride.yMin)
        assertNull(scaleOverride.yMax)
        assertNull(scaleOverride.zMin)
        assertNull(scaleOverride.zMax)
    }

    @Test
    fun pointLine3DPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = PointLine3DPresentationOptions()
        val custom = PointLine3DPresentationOptions(
            backgroundColor = 0xFF010203.toInt(),
            gridColor = 0xFF223344.toInt(),
            xAxisColor = 0xFF334455.toInt(),
            yAxisColor = 0xFF445566.toInt(),
            zAxisColor = 0xFF556677.toInt(),
            showLines = false,
            lineWidth = 5f,
            lineAlpha = 0.45f,
            showPointMarkers = false,
            pointSize = 15f,
            pointAlpha = 0.7f,
            selectedPointScale = 1.6f,
        )

        assertEquals(Color.parseColor("#09111B"), defaults.backgroundColor)
        assertEquals(Color.parseColor("#334155"), defaults.gridColor)
        assertEquals(Color.parseColor("#F97316"), defaults.xAxisColor)
        assertEquals(Color.parseColor("#22C55E"), defaults.yAxisColor)
        assertEquals(Color.parseColor("#38BDF8"), defaults.zAxisColor)
        assertTrue(defaults.showLines)
        assertEquals(3f, defaults.lineWidth, 0.0f)
        assertEquals(0.92f, defaults.lineAlpha, 0.0f)
        assertTrue(defaults.showPointMarkers)
        assertEquals(11f, defaults.pointSize, 0.0f)
        assertEquals(1f, defaults.pointAlpha, 0.0f)
        assertEquals(1.35f, defaults.selectedPointScale, 0.0f)

        assertEquals(0xFF010203.toInt(), custom.backgroundColor)
        assertEquals(0xFF223344.toInt(), custom.gridColor)
        assertEquals(0xFF334455.toInt(), custom.xAxisColor)
        assertEquals(0xFF445566.toInt(), custom.yAxisColor)
        assertEquals(0xFF556677.toInt(), custom.zAxisColor)
        assertFalse(custom.showLines)
        assertEquals(5f, custom.lineWidth, 0.0f)
        assertEquals(0.45f, custom.lineAlpha, 0.0f)
        assertFalse(custom.showPointMarkers)
        assertEquals(15f, custom.pointSize, 0.0f)
        assertEquals(0.7f, custom.pointAlpha, 0.0f)
        assertEquals(1.6f, custom.selectedPointScale, 0.0f)
    }
}
