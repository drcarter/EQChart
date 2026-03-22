package com.magimon.eq.bubble3d

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates reusable sphere geometry for bubble rendering.
 */
internal object Bubble3DMeshFactory {

    /**
     * Indexed sphere mesh buffers shared across all rendered bubbles.
     */
    data class SphereMesh(
        val vertexBuffer: FloatBuffer,
        val normalBuffer: FloatBuffer,
        val indexBuffer: ShortBuffer,
        val indexCount: Int,
    )

    /**
     * Builds a unit sphere mesh with configurable stack/slice density.
     */
    fun createSphereMesh(
        stacks: Int = 18,
        slices: Int = 24,
    ): SphereMesh {
        val vertices = ArrayList<Float>((stacks + 1) * (slices + 1) * 3)
        val normals = ArrayList<Float>((stacks + 1) * (slices + 1) * 3)
        val indices = ArrayList<Short>(stacks * slices * 6)

        for (stack in 0..stacks) {
            val v = stack.toFloat() / stacks.toFloat()
            val phi = Math.PI * (v - 0.5)
            val y = sin(phi).toFloat()
            val ringRadius = cos(phi).toFloat()

            for (slice in 0..slices) {
                val u = slice.toFloat() / slices.toFloat()
                val theta = (Math.PI * 2.0 * u).toFloat()
                val x = ringRadius * cos(theta)
                val z = ringRadius * sin(theta)

                vertices += x
                vertices += y
                vertices += z

                normals += x
                normals += y
                normals += z
            }
        }

        val columns = slices + 1
        for (stack in 0 until stacks) {
            for (slice in 0 until slices) {
                val topLeft = (stack * columns) + slice
                val bottomLeft = ((stack + 1) * columns) + slice
                val topRight = topLeft + 1
                val bottomRight = bottomLeft + 1

                indices += topLeft.toShort()
                indices += bottomLeft.toShort()
                indices += topRight.toShort()

                indices += topRight.toShort()
                indices += bottomLeft.toShort()
                indices += bottomRight.toShort()
            }
        }

        return SphereMesh(
            vertexBuffer = vertices.toFloatArray().toFloatBuffer(),
            normalBuffer = normals.toFloatArray().toFloatBuffer(),
            indexBuffer = indices.toShortArray().toShortBuffer(),
            indexCount = indices.size,
        )
    }

    /**
     * Converts float data into a direct [FloatBuffer] for GLES consumption.
     */
    private fun FloatArray.toFloatBuffer(): FloatBuffer {
        return ByteBuffer
            .allocateDirect(size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(this@toFloatBuffer)
                position(0)
            }
    }

    /**
     * Converts index data into a direct [ShortBuffer] for GLES consumption.
     */
    private fun ShortArray.toShortBuffer(): ShortBuffer {
        return ByteBuffer
            .allocateDirect(size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(this@toShortBuffer)
                position(0)
            }
    }
}
