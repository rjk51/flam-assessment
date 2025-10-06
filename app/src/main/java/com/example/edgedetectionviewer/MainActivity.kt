package com.example.edgedetectionviewer

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat



class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView: CameraBridgeViewBase

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
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val mat = inputFrame!!.rgba()
        processFrame(mat.nativeObjAddr)
        return mat
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}
}