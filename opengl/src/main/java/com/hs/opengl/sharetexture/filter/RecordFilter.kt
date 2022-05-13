package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLContext
import com.hs.opengl.sharetexture.R
import com.hs.opengl.sharetexture.util.MediaRecorder

/**
 *
 * Date: 2022/5/13
 * Time: 01:49
 * @author shun.he
 */
class RecordFilter(var context: Context, private var eglContext:EGLContext) : AbsFilter(context, R.raw.base_vert, R.raw.base_frag) {
    private var mMediaRecorder: MediaRecorder? = null
    private var mSpeed = 1.0f

    fun startRecord(speed: Float) {
        mSpeed = speed
        mMediaRecorder =
            MediaRecorder(1920, 1080, context, eglContext)
        mMediaRecorder!!.start(mSpeed)
    }

    fun setSpeed(speed: Float) {
        mSpeed = speed
        mMediaRecorder?.setSpeed(speed)
    }

    override fun onDraw(texName: Int, timestamp: Long): Int {
        val textureId = super.onDraw(texName, timestamp)
        mMediaRecorder?.inputFrame(textureId, timestamp)
        return textureId
    }

    fun stopRecord() {
        mMediaRecorder?.stop()
        mMediaRecorder = null
    }

    override fun release() {
        super.release()
        mMediaRecorder?.stop()
        mMediaRecorder = null
    }


}