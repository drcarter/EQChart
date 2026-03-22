package com.magimon.eq.common

import android.graphics.Color
import com.magimon.eq.pointcloud3d.PointCloud3DDatum
import com.magimon.eq.pointcloud3d.PointCloud3DPresentationOptions
import com.magimon.eq.pointcloud3d.PointCloud3DScaleOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PointCloud3DContractsTest {

    @Test
    fun pointCloud3DModels_preserveProvidedValues() {
        val payload = linkedMapOf("id" to 42)
        val datum = PointCloud3DDatum(
            x = 12.5,
            y = -4.25,
            z = 7.75,
            size = 88.0,
            color = 0xFF336699.toInt(),
            label = "Cluster A",
            payload = payload,
        )
        val copied = datum.copy(size = 99.0, label = "Cluster B")
        val scaleOverride = PointCloud3DScaleOverride(
            xMin = 1.0,
            xMax = 2.0,
            yMin = 3.0,
            yMax = 4.0,
            zMin = 5.0,
            zMax = 6.0,
            sizeMin = 7.0,
            sizeMax = 8.0,
        )

        assertEquals(12.5, datum.x, 0.0)
        assertEquals(-4.25, datum.y, 0.0)
        assertEquals(7.75, datum.z, 0.0)
        assertEquals(88.0, datum.size, 0.0)
        assertEquals(0xFF336699.toInt(), datum.color)
        assertEquals("Cluster A", datum.label)
        assertSame(payload, datum.payload)

        assertEquals(99.0, copied.size, 0.0)
        assertEquals("Cluster B", copied.label)
        assertSame(payload, copied.payload)

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
    fun pointCloud3DModels_defaultValuesRemainAccessible() {
        val datum = PointCloud3DDatum(
            x = 1.0,
            y = 2.0,
            z = 3.0,
            color = 0xFF123456.toInt(),
        )
        val scaleOverride = PointCloud3DScaleOverride()

        assertEquals(1.0, datum.size, 0.0)
        assertEquals(0xFF123456.toInt(), datum.color)
        assertNull(datum.label)
        assertNull(datum.payload)

        assertNull(scaleOverride.xMin)
        assertNull(scaleOverride.xMax)
        assertNull(scaleOverride.yMin)
        assertNull(scaleOverride.yMax)
        assertNull(scaleOverride.zMin)
        assertNull(scaleOverride.zMax)
        assertNull(scaleOverride.sizeMin)
        assertNull(scaleOverride.sizeMax)
    }

    @Test
    fun pointCloud3DPresentationOptions_defaultsAndCustomValues_areAccessible() {
        val defaults = PointCloud3DPresentationOptions()
        val custom = PointCloud3DPresentationOptions(
            backgroundColor = 0xFF010203.toInt(),
            gridColor = 0xFF223344.toInt(),
            xAxisColor = 0xFF334455.toInt(),
            yAxisColor = 0xFF445566.toInt(),
            zAxisColor = 0xFF556677.toInt(),
            minPointSize = 4f,
            maxPointSize = 20f,
            pointAlpha = 0.65f,
            selectedPointScale = 1.8f,
        )

        assertEquals(Color.parseColor("#09111B"), defaults.backgroundColor)
        assertEquals(Color.parseColor("#334155"), defaults.gridColor)
        assertEquals(Color.parseColor("#F97316"), defaults.xAxisColor)
        assertEquals(Color.parseColor("#22C55E"), defaults.yAxisColor)
        assertEquals(Color.parseColor("#38BDF8"), defaults.zAxisColor)
        assertEquals(6f, defaults.minPointSize, 0.0f)
        assertEquals(18f, defaults.maxPointSize, 0.0f)
        assertEquals(0.95f, defaults.pointAlpha, 0.0f)
        assertEquals(1.4f, defaults.selectedPointScale, 0.0f)

        assertEquals(0xFF010203.toInt(), custom.backgroundColor)
        assertEquals(0xFF223344.toInt(), custom.gridColor)
        assertEquals(0xFF334455.toInt(), custom.xAxisColor)
        assertEquals(0xFF445566.toInt(), custom.yAxisColor)
        assertEquals(0xFF556677.toInt(), custom.zAxisColor)
        assertEquals(4f, custom.minPointSize, 0.0f)
        assertEquals(20f, custom.maxPointSize, 0.0f)
        assertEquals(0.65f, custom.pointAlpha, 0.0f)
        assertEquals(1.8f, custom.selectedPointScale, 0.0f)
        assertTrue(custom.maxPointSize > custom.minPointSize)
    }
}
