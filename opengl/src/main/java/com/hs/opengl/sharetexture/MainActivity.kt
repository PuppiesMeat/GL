package com.hs.opengl.sharetexture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.hs.opengl.sharetexture.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), Preview.OnPreviewOutputUpdateListener {
    private lateinit var mCameraHelper: CameraHelper
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(NeedPermissions, 1000)
        } else {
            start()
        }
    }

    private var isStart = false
    private var speed = 1.0f
    private fun start() {
        mCameraHelper = CameraHelper(this, this)
        mCameraHelper.startPreview(1920, 1080, CameraX.LensFacing.FRONT )
        binding.btRecord.setOnClickListener {
            if (!isStart){
                isStart = true
                binding.cameraView.startRecord(speed)
            } else {
                isStart = false
                binding.cameraView.stopRecord()
            }
        }
    }

    override fun onUpdated(output: Preview.PreviewOutput?) {
        output?.surfaceTexture?.let {
            binding.cameraView.bindSurfaceTexture(it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000){
            if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                finish()
            } else {
                start()
            }
        }


    }

    companion object {
        val NeedPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}