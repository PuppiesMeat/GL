package com.hs.opengl.sharetexture

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 *
 * Date: 2022/5/11
 * Time: 22:10
 * @author shun.he
 */
class CameraView(context: Context?, attributeSet: AttributeSet?) :
    GLSurfaceView(context, attributeSet) {

    fun bindSurfaceTexture(surface: SurfaceTexture) {
        mRender.bindSurfaceTexture(surface)
    }

    fun startRecord(speed:Float) {
       mRender.startRecord(speed)
    }

    fun stopRecord() {
       mRender.stopRecord()
    }

    constructor(context: Context?) : this(context, null)

    private val mRender: VideoSourceRender

    init {
        setEGLContextClientVersion(2)
        mRender = VideoSourceRender(this)
        setRenderer(mRender)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

}