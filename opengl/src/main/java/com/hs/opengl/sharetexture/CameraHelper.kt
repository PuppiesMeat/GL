package com.hs.opengl.sharetexture

import android.os.HandlerThread
import android.util.Size
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner

/**
 *
 * Date: 2022/5/11
 * Time: 22:25
 * @author shun.he
 */
class CameraHelper(
    _lifecycleOwner: LifecycleOwner,
    _previewListener: Preview.OnPreviewOutputUpdateListener
) {
    private var lifecycleOwner = _lifecycleOwner
    private var currentFacing = CameraX.LensFacing.FRONT
    private var cameraThread: HandlerThread? = null
    private var previewListener: Preview.OnPreviewOutputUpdateListener = _previewListener


    fun startPreview(width: Int, height: Int, facing: CameraX.LensFacing) {
        cameraThread = HandlerThread("camera-helper")
        cameraThread?.start()
        currentFacing = facing
        CameraX.bindToLifecycle(lifecycleOwner, getPreview(width, height))
    }

    private fun getPreview(width: Int, height: Int) = Preview(
        PreviewConfig.Builder()
            .setTargetResolution(Size(width, height))
            .setLensFacing(currentFacing)
            .build()
    ).apply {
        onPreviewOutputUpdateListener = previewListener
    }

}