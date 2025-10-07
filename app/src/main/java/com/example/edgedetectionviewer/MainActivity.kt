package com.example.edgedetectionviewer

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.view.View
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import com.example.edgedetectionviewer.gl.GLRenderer
import java.util.Locale


class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView: CameraBridgeViewBase
    private lateinit var glView: GLSurfaceView
    private lateinit var glRenderer: GLRenderer

    private lateinit var toggleButton: Button
    private lateinit var fpsText: TextView

    // State: true => show edge detection (GL); false => show raw camera
    private var showEdges = true

    // FPS calculation
    private var lastFrameTimeNs: Long = 0L

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun processFrame(matAddr: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraView = findViewById(R.id.camera_view)
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.setCvCameraViewListener(this)

        // Initialize GLSurfaceView and renderer
        glView = findViewById(R.id.gl_view)
        glView.setEGLContextClientVersion(2)
        glRenderer = GLRenderer()
        glView.setRenderer(glRenderer)
        // We'll request renders when new frames are available
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // UI bindings
        toggleButton = findViewById(R.id.toggleButton)
        fpsText = findViewById(R.id.fpsText)

        toggleButton.setOnClickListener {
            showEdges = !showEdges
            if (showEdges) {
                // show GL output, hide raw camera preview
                glView.visibility = View.VISIBLE
                cameraView.visibility = View.GONE
                toggleButton.text = getString(R.string.show_raw)
            } else {
                // show raw camera preview, hide GL overlay
                glView.visibility = View.GONE
                cameraView.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.show_edges)
            }
        }

        // Set initial mode
        if (showEdges) {
            glView.visibility = View.VISIBLE
            cameraView.visibility = View.GONE
            toggleButton.text = getString(R.string.show_raw)
        } else {
            glView.visibility = View.GONE
            cameraView.visibility = View.VISIBLE
            toggleButton.text = getString(R.string.show_edges)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initDebug()
        cameraView.enableView()
        glView.onResume()
        lastFrameTimeNs = System.nanoTime()
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
        glView.onPause()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val t0 = System.nanoTime()

        val mat = inputFrame!!.rgba()

        if (showEdges) {
            // Let native processing modify the mat in-place
            processFrame(mat.nativeObjAddr)

            // Ensure we provide an RGBA Mat to the GL renderer for consistent texture format
            // If input has 4 channels, we can use it directly; otherwise convert
            try {
                if (mat.channels() == 4) {
                    glRenderer.setFrame(mat)
                } else {
                    // Convert to RGBA
                    val rgba = Mat()
                    when (mat.channels()) {
                        1 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA)
                        3 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_RGB2RGBA)
                        else -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_RGB2RGBA)
                    }
                    glRenderer.setFrame(rgba)
                    rgba.release()
                }
                glView.requestRender()
            } catch (e: Exception) {
                // If renderer isn't ready yet or setFrame fails, ignore to avoid crashing the camera thread
                e.printStackTrace()
            }
        } else {
            // Raw mode: do not run native processing; ensure GL view hidden in UI thread
            // (visibility is managed by button but keep safe)
        }

        // Calculate timing and FPS
        val t1 = System.nanoTime()
        val frameMs = (t1 - t0) / 1_000_000.0
        val fps = if (frameMs > 0) (1000.0 / frameMs) else 0.0

        // Update FPS overlay on UI thread
        runOnUiThread {
            fpsText.text = String.format(Locale.US, "FPS: %.1f\nMS: %.1f", fps, frameMs)
        }

        // Return the appropriate mat to be displayed by the camera view (we return original mat)
        return mat
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Ensure texture deletion happens on GL thread
            glView.queueEvent {
                try { glRenderer.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}