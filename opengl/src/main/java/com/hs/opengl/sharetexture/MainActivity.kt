package com.hs.opengl.sharetexture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.opengl.GLES20
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import androidx.camera.core.CameraX
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.hs.opengl.sharetexture.databinding.ActivityMainBinding
import com.hs.opengl.sharetexture.egl.HSEglContext
import com.hs.opengl.sharetexture.filter.NV21ReadPixelDataListener
import com.hs.opengl.sharetexture.filter.NV21ReadPixelFilter
import com.hs.opengl.sharetexture.filter.ReadPixelDataListener
import com.rokid.android.camera.api.CameraVideoCapturer
import com.rokid.android.camera.api.CapturerObserver
import com.rokid.android.camera.capturer.RKCamera1Capturer
import com.rokid.android.camera.capturer.RKCamera1Enumerator
import java.io.ByteArrayOutputStream

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
//        val capturer = RKCamera1Capturer("1", object : CameraVideoCapturer.CameraEventsHandler{
//            override fun onCameraClosed() {
//
//            }
//
//            override fun onCameraDisconnected() {
//            }
//
//            override fun onCameraError(errorDescription: String?) {
//            }
//
//            override fun onCameraFreezed(errorDescription: String?) {
//            }
//
//            override fun onCameraOpening(cameraName: String) {
//            }
//
//            override fun onFirstFrameAvailable() {
//            }
//
//        })
//        val textureIds = IntArray(1)
//        GLES20.glGenTextures(1, textureIds, 0)
//        val texture = SurfaceTexture(textureIds[0])
//        texture.setDefaultBufferSize(1920, 1080)
//        capturer.initialize(texture, applicationContext, "dsfaf", object : CapturerObserver{
//            override fun onCapturerStarted(success: Boolean) {
//
//            }
//
//            override fun onCapturerStopped() {
//            }
//
//            override fun onFrameCaptured(surfaceTexture: SurfaceTexture, rotation: Int) {
//                hsFilters?.bindSurfaceTexture(surfaceTexture)
//
//            }
//
//        })
//        capturer.startCapture(1920, 1080, 30)
        binding.btRecord.setOnClickListener {
            if (!isStart){
                isStart = true
//                binding.cameraView.startRecord(speed, "/sdcard/text.h264", true)
                showPreview()
            } else {
                isStart = false
//                binding.cameraView.stopRecord()
                hidePreview()
            }
        }
        hsFilters?.init(this, 1920, 1080)
        hsFilters?.startReadPixel(object : NV21ReadPixelDataListener{
            override fun onNv21Data(nv21Data: ByteArray, width: Int, height: Int) {
                Log.d(TAG, "onNv21Data ${nv21Data.size}, $width x $height")
                val bitmap  = nv21ToBitmap(nv21Data, width, height)
                runOnUiThread {
                    binding.ivPreview.setImageBitmap(bitmap)
                }
                Thread.sleep(50)
                bitmap.recycle()
            }

        })

    }

    fun nv21ToBitmap(data:ByteArray, width:Int, height:Int):Bitmap{
        val image = YuvImage(data, ImageFormat.NV21, width, height, null)
        val bos = ByteArrayOutputStream()
        image.compressToJpeg(Rect(0, 0, width, height), 100, bos)
        try {
            return BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size())
        } finally {
            try {
                bos.close()
            } catch (ex:Exception){
                ex.printStackTrace()
            }
        }
    }

    private fun hidePreview() {

    }

    private var surface: Surface? = null
    private fun showPreview() {
        if (binding.cameraView.holder.surface != null){
            surface = binding.cameraView.holder.surface
            hsFilters?.startShow(binding.cameraView.holder.surface, binding.cameraView.width, binding.cameraView.height)
            return
        }
        binding.cameraView.holder.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface
                hsFilters?.startShow(holder.surface, binding.cameraView.width, binding.cameraView.height)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                hsFilters?.changePreviewSize(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

        })
    }

    private var hsFilters:HSFilters? = HSFilters()
    private var TAG = MainActivity::class.java.simpleName

    override fun onUpdated(output: Preview.PreviewOutput?) {
        output?.surfaceTexture?.let {
//            binding.cameraView.bindSurfaceTexture(it)
            Log.d(TAG, "onUpdate==========: ${Thread.currentThread().name}")
            hsFilters?.bindSurfaceTexture(it)
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