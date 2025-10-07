package com.example.edgedetectionviewer

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.view.View
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.TextView
import android.util.Log
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

    // UI elements
    private lateinit var toggleButton: Button
    private lateinit var fpsText: TextView

    // State: true => show edge detection (GL); false => show raw camera
    private var showEdges = false

    // FPS calculation
    private var lastFrameTimeNs: Long = 0L

    // Track OpenCV loaded state
    private var openCvLoaded = false

    // Frame heartbeat
    private var frameCountSinceResume = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var cameraFallbackAttempt = 0
    private val maxFallbackAttempts = 3

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        private const val CAMERA_PERMISSION_REQUEST = 1
        private const val TAG = "MainActivity"
    }

    external fun processFrame(matAddr: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraView = findViewById(R.id.camera_view)
        // Keep camera view present so the surface is created; GL view will overlay output
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
                // show GL output, keep camera running in background
                glView.visibility = View.VISIBLE
                cameraView.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.show_raw)
            } else {
                // show raw camera preview
                glView.visibility = View.GONE
                cameraView.visibility = View.VISIBLE
                toggleButton.text = getString(R.string.show_edges)
            }
        }

        // Set initial mode
        if (showEdges) {
            glView.visibility = View.VISIBLE
            cameraView.visibility = View.VISIBLE
            toggleButton.text = getString(R.string.show_raw)
        } else {
            // Default to raw camera to make debugging easier
            glView.visibility = View.GONE
            cameraView.visibility = View.VISIBLE
            toggleButton.text = getString(R.string.show_edges)
        }

        // If permission not granted, request it; otherwise enable camera when OpenCV loads
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            // Mark granted state for OpenCV bridge early
            setOpenCvCameraPermissionGrantedIfAllowed()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: initializing OpenCV")
        // Initialize OpenCV (static init) and enable camera if permission granted
        openCvLoaded = OpenCVLoader.initDebug()
        if (!openCvLoaded) {
            Log.w(TAG, "OpenCV initDebug() failed in onResume")
            runOnUiThread { fpsText.text = "OpenCV init failed" }
        } else {
            runOnUiThread { fpsText.text = "OpenCV: OK" }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                try {
                    // Ensure the OpenCV camera bridge sees permission granted
                    setOpenCvCameraPermissionGrantedIfAllowed()
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY)
                    cameraView.enableView()
                    Log.d(TAG, "Camera enabled in onResume")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to enable camera in onResume: ${e.message}")
                }
            }
        }
        glView.onResume()
        lastFrameTimeNs = System.nanoTime()
        // Reset frame counter and start watchdog to ensure frames arrive
        frameCountSinceResume = 0
        cameraFallbackAttempt = 0
        handler.removeCallbacks(checkFramesRunnable)
        handler.postDelayed(checkFramesRunnable, 2000)
    }

    override fun onPause() {
        super.onPause()
        if (openCvLoaded) {
            try {
                cameraView.disableView()
                Log.d(TAG, "Camera disabled in onPause")
            } catch (e: Exception) {
                Log.w(TAG, "Error disabling camera: ${e.message}")
            }
        }
        glView.onPause()
        handler.removeCallbacks(checkFramesRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted by user")
                runOnUiThread { fpsText.text = "Permission: Granted" }
                // Let OpenCV bridge know about permission
                setOpenCvCameraPermissionGrantedIfAllowed()
                if (openCvLoaded) {
                    try {
                        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY)
                        cameraView.enableView()
                        Log.d(TAG, "Camera enabled after permission grant")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to enable camera after permission grant: ${e.message}")
                    }
                }
            } else {
                Log.d(TAG, "Camera permission denied by user")
                runOnUiThread { fpsText.text = "Permission: Denied" }
            }
        }
    }

    private val checkFramesRunnable = Runnable {
        if (frameCountSinceResume == 0) {
            // No frames received within the window; try fallback
            cameraFallbackAttempt++
            if (cameraFallbackAttempt <= maxFallbackAttempts) {
                runOnUiThread { fpsText.text = "No frames - retrying camera ($cameraFallbackAttempt)" }
                trySwitchCameraIndex()
            } else {
                runOnUiThread { fpsText.text = "No frames - all retries failed" }
            }
        } else {
            runOnUiThread { fpsText.text = "Frames OK: $frameCountSinceResume" }
        }
    }

    private fun trySwitchCameraIndex() {
        try {
            cameraView.disableView()
        } catch (_: Exception) {}
        try {
            when (cameraFallbackAttempt % 3) {
                1 -> cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK)
                2 -> cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
                else -> cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set camera index during fallback: ${e.message}")
        }
        try {
            // Ensure bridge sees permission before re-enabling
            setOpenCvCameraPermissionGrantedIfAllowed()
            cameraView.enableView()
            Log.d(TAG, "Attempted fallback enableView #$cameraFallbackAttempt")
            runOnUiThread { fpsText.text = "Retrying camera (#$cameraFallbackAttempt)" }
        } catch (e: Exception) {
            Log.w(TAG, "enableView failed during fallback: ${e.message}")
            runOnUiThread { fpsText.text = "enableView failed: ${e.message}" }
        }
        // Schedule another check
        handler.postDelayed(checkFramesRunnable, 2000)
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val t0 = System.nanoTime()

        val mat = inputFrame!!.rgba()

        // Heartbeat
        frameCountSinceResume++

        // Log a short heartbeat when frames are received
        Log.v(TAG, "onCameraFrame: received frame ${mat.cols()}x${mat.rows()} channels=${mat.channels()}")

        if (showEdges) {
            // Let native processing modify the mat in-place
            processFrame(mat.nativeObjAddr)

            // Ensure we provide an RGBA Mat to the GL renderer for consistent texture format
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
                Log.w(TAG, "Error in GL update: ${e.message}")
            }
        } else {
            // Raw mode: do not run native processing
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

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "onCameraViewStarted: ${width}x${height}")
        runOnUiThread { fpsText.text = "Camera started: ${width}x${height}" }
    }
    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped")
        runOnUiThread { fpsText.text = "Camera stopped" }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Ensure texture deletion happens on GL thread
            glView.queueEvent {
                try { glRenderer.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    // Inform the OpenCV camera view that runtime camera permission is granted (if it is)
    private fun setOpenCvCameraPermissionGrantedIfAllowed() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraView.setCameraPermissionGranted()
                Log.d(TAG, "setCameraPermissionGranted() called")
            }
        } catch (e: Throwable) {
            // Be defensive in case OpenCV version differs
            Log.w(TAG, "setCameraPermissionGranted failed: ${e.message}")
        }
    }
}