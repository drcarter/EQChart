package com.magimon.eq.bubble3d

import android.opengl.GLES20

/**
 * Shader compilation and program-link helpers for the 3D bubble renderer.
 */
internal object Bubble3DShaderProgram {

    /**
     * Handles required to render shaded bubble spheres.
     */
    data class SphereProgram(
        val programId: Int,
        val positionHandle: Int,
        val normalHandle: Int,
        val mvpMatrixHandle: Int,
        val modelMatrixHandle: Int,
        val colorHandle: Int,
        val lightDirectionHandle: Int,
        val ambientHandle: Int,
    )

    /**
     * Handles required to render axes, grid lines, and tick marks.
     */
    data class LineProgram(
        val programId: Int,
        val positionHandle: Int,
        val mvpMatrixHandle: Int,
        val colorHandle: Int,
    )

    /**
     * Compiles and links the lit sphere shader program.
     */
    fun createSphereProgram(): SphereProgram {
        val programId = linkProgram(
            vertexSource = """
                uniform mat4 uMvpMatrix;
                uniform mat4 uModelMatrix;
                attribute vec3 aPosition;
                attribute vec3 aNormal;
                varying vec3 vNormal;

                void main() {
                    vNormal = normalize((uModelMatrix * vec4(aNormal, 0.0)).xyz);
                    gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
                }
            """.trimIndent(),
            fragmentSource = """
                precision mediump float;
                uniform vec4 uColor;
                uniform vec3 uLightDirection;
                uniform float uAmbient;
                varying vec3 vNormal;

                void main() {
                    vec3 normal = normalize(vNormal);
                    float diffuse = max(dot(normal, normalize(uLightDirection)), 0.0);
                    float lighting = clamp(uAmbient + ((1.0 - uAmbient) * diffuse), 0.0, 1.0);
                    vec3 color = uColor.rgb * lighting;
                    gl_FragColor = vec4(color, uColor.a);
                }
            """.trimIndent(),
        )

        return SphereProgram(
            programId = programId,
            positionHandle = GLES20.glGetAttribLocation(programId, "aPosition"),
            normalHandle = GLES20.glGetAttribLocation(programId, "aNormal"),
            mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix"),
            modelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix"),
            colorHandle = GLES20.glGetUniformLocation(programId, "uColor"),
            lightDirectionHandle = GLES20.glGetUniformLocation(programId, "uLightDirection"),
            ambientHandle = GLES20.glGetUniformLocation(programId, "uAmbient"),
        )
    }

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
     * Links a vertex/fragment shader pair into a GLES program object.
     */
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

    /**
     * Compiles one GLES shader stage from source.
     */
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
