package com.example.edgedetectionviewer.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import org.opencv.core.Mat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * OpenGL ES 2.0 renderer that displays frames coming from OpenCV (Mat) as a texture.
 *
 * Usage:
 * - Call setFrame(mat) from camera thread with an RGBA (preferred) or grayscale Mat.
 * - Attach this renderer to a GLSurfaceView (setEGLContextClientVersion(2) and setRenderer(this)).
 * - The renderer throttles to ~15 FPS to balance CPU/GPU usage.
 */
class GLRenderer : GLSurfaceView.Renderer {

    // GL program and attribute/uniform handles
    private var programId: Int = 0
    private var aPositionHandle: Int = -1
    private var aTexCoordHandle: Int = -1
    private var uTextureHandle: Int = -1

    // Texture state
    private var textureId: Int = 0
    private var texWidth: Int = 0
    private var texHeight: Int = 0
    private var texFormat: Int = GLES20.GL_RGBA // default; will adapt to source

    // Vertex data (two triangles forming a full-screen quad)
    private val vertexData: FloatBuffer = floatArrayOf(
        // X, Y (clip space)
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f,
    ).toFloatBuffer()

    // Texture coordinates
    private val texCoordData: FloatBuffer = floatArrayOf(
        // u, v (note: v flipped to match typical camera frame origin)
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f,
    ).toFloatBuffer()

    // Incoming frame buffer (copied from Mat on producer thread)
    private var pixelBuffer: ByteBuffer? = null
    private val frameAvailable = AtomicBoolean(false)
    private val lock = Any()

    // FPS control (~15 FPS => 66.6 ms per frame)
    private val targetFrameMillis = 66L
    private var lastFrameTimeNs: Long = 0L

    override fun onSurfaceCreated(unused: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aTexCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uTextureHandle = GLES20.glGetUniformLocation(programId, "uTexture")

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Default empty 2x2 texture until the first frame arrives
        texWidth = 2
        texHeight = 2
        texFormat = GLES20.GL_RGBA
        val empty = ByteBuffer.allocateDirect(texWidth * texHeight * 4).order(ByteOrder.nativeOrder())
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, texFormat,
            texWidth, texHeight, 0,
            texFormat, GLES20.GL_UNSIGNED_BYTE, empty
        )

        checkGlError("onSurfaceCreated")
        lastFrameTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(unused: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: javax.microedition.khronos.opengles.GL10?) {
        // Simple FPS cap to ~15
        val now = System.nanoTime()
        val elapsedMs = (now - lastFrameTimeNs) / 1_000_000L
        if (elapsedMs < targetFrameMillis) {
            try { Thread.sleep(targetFrameMillis - elapsedMs) } catch (_: InterruptedException) {}
        }
        lastFrameTimeNs = System.nanoTime()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(programId)

        // Update texture if we have a new frame
        if (frameAvailable.compareAndSet(true, false)) {
            synchronized(lock) {
                pixelBuffer?.let { buffer ->
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                    GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
                    buffer.position(0)
                    GLES20.glTexImage2D(
                        GLES20.GL_TEXTURE_2D,
                        0,
                        texFormat,
                        texWidth,
                        texHeight,
                        0,
                        texFormat,
                        GLES20.GL_UNSIGNED_BYTE,
                        buffer
                    )
                }
            }
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        }

        // Set up vertex attributes
        vertexData.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexData)

        texCoordData.position(0)
        GLES20.glEnableVertexAttribArray(aTexCoordHandle)
        GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordData)

        // Bind texture to unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        // Draw two triangles (strip not used; 4 vertices => triangle strip forms quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTexCoordHandle)

        checkGlError("onDrawFrame")
    }

    /**
     * Provide a new frame from OpenCV.
     * The Mat should be continuous. Preferred format: CV_8UC4 (RGBA). Also supports CV_8UC1 (grayscale) and CV_8UC3 (RGB).
     */
    fun setFrame(mat: Mat) {
        require(!mat.empty()) { "Input Mat is empty" }
        require(mat.isContinuous()) { "Input Mat must be continuous" }

        val width = mat.cols()
        val height = mat.rows()
        val channels = mat.channels()
        val format = when (channels) {
            1 -> GLES20.GL_LUMINANCE // grayscale
            3 -> GLES20.GL_RGB
            4 -> GLES20.GL_RGBA
            else -> throw IllegalArgumentException("Unsupported channel count: $channels")
        }

        // Allocate/resize buffer if needed
        val capacity = width * height * max(channels, 1)
        synchronized(lock) {
            if (pixelBuffer == null || pixelBuffer?.capacity() != capacity) {
                pixelBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder())
            }
            // Copy Mat data into ByteBuffer
            val arr = ByteArray(capacity)
            mat.get(0, 0, arr)
            pixelBuffer!!.clear()
            pixelBuffer!!.put(arr)
            pixelBuffer!!.flip()

            texWidth = width
            texHeight = height
            texFormat = format
            frameAvailable.set(true)
        }
    }

    fun release() {
        val textures = intArrayOf(textureId)
        GLES20.glDeleteTextures(1, textures, 0)
        textureId = 0
        programId = 0
    }

    // --- Shader helpers ---

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val v = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val f = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, v)
        GLES20.glAttachShader(program, f)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw RuntimeException("Program link failed: $log")
        }
        GLES20.glDeleteShader(v)
        GLES20.glDeleteShader(f)
        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }

    private fun checkGlError(where: String) {
        val err = GLES20.glGetError()
        if (err != GLES20.GL_NO_ERROR) {
            throw RuntimeException("GL error after $where: 0x${err.toString(16)}")
        }
    }

    //Vertex and fragment shaders
    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}

private fun FloatArray.toFloatBuffer(): FloatBuffer =
    ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(this@toFloatBuffer)
        position(0)
    }
