package com.magimon.eq.internal3d

import android.opengl.GLES20

/**
 * Shader compilation and program-link helpers shared by point-series renderers.
 */
internal object PointSeries3DShaderProgram {

    /**
     * Handles required to render flat-color line geometry.
     */
    data class LineProgram(
        val programId: Int,
        val positionHandle: Int,
        val mvpMatrixHandle: Int,
        val colorHandle: Int,
    )

    /**
     * Handles required to render round point markers.
     */
    data class PointProgram(
        val programId: Int,
        val positionHandle: Int,
        val pointSizeHandle: Int,
        val colorHandle: Int,
        val mvpMatrixHandle: Int,
    )

    /**
     * Compiles and links the flat-color line shader program.
     */
    fun createLineProgram(): LineProgram {
        val programId = linkProgram(
            vertexSource = """
                uniform mat4 uMvpMatrix;
                attribute vec3 aPosition;

                void main() {
                    gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
                }
            """.trimIndent(),
            fragmentSource = """
                precision mediump float;
                uniform vec4 uColor;

                void main() {
                    gl_FragColor = uColor;
                }
            """.trimIndent(),
        )

        return LineProgram(
            programId = programId,
            positionHandle = GLES20.glGetAttribLocation(programId, "aPosition"),
            mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix"),
            colorHandle = GLES20.glGetUniformLocation(programId, "uColor"),
        )
    }

    /**
     * Compiles and links the point-marker shader program.
     */
    fun createPointProgram(): PointProgram {
        val programId = linkProgram(
            vertexSource = """
                uniform mat4 uMvpMatrix;
                attribute vec3 aPosition;
                attribute float aPointSize;
                attribute vec4 aColor;
                varying vec4 vColor;

                void main() {
                    gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
                    gl_PointSize = aPointSize;
                    vColor = aColor;
                }
            """.trimIndent(),
            fragmentSource = """
                precision mediump float;
                varying vec4 vColor;

                void main() {
                    vec2 centered = gl_PointCoord - vec2(0.5, 0.5);
                    if (dot(centered, centered) > 0.25) {
                        discard;
                    }
                    gl_FragColor = vColor;
                }
            """.trimIndent(),
        )

        return PointProgram(
            programId = programId,
            positionHandle = GLES20.glGetAttribLocation(programId, "aPosition"),
            pointSizeHandle = GLES20.glGetAttribLocation(programId, "aPointSize"),
            colorHandle = GLES20.glGetAttribLocation(programId, "aColor"),
            mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix"),
        )
    }

    private fun linkProgram(
        vertexSource: String,
        fragmentSource: String,
    ): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val message = GLES20.glGetProgramInfoLog(programId)
            GLES20.glDeleteProgram(programId)
            error("Failed to link OpenGL program: $message")
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        return programId
    }

    private fun compileShader(
        type: Int,
        source: String,
    ): Int {
        val shaderId = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shaderId, source)
        GLES20.glCompileShader(shaderId)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val message = GLES20.glGetShaderInfoLog(shaderId)
            GLES20.glDeleteShader(shaderId)
            error("Failed to compile OpenGL shader: $message")
        }

        return shaderId
    }
}
