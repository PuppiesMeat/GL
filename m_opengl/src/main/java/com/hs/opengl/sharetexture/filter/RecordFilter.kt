package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.EGLContext
import com.hs.opengl.sharetexture.R

/**
 *
 * Date: 2022/5/13
 * Time: 01:49
 * @author shun.he
 */
class RecordFilter(var context: Context, private var eglContext: EGLContext) :
    AbsFilter(context, R.raw.base_vert, R.raw.base_frag) {
    private var mMediaRecorder: MediaRecorder? = null
    private var mSpeed = 1.0f

    fun insertSeiData(data: ByteArray) {
        mMediaRecorder?.insertSeiData(data)
    }

    fun startRecord(speed: Float, filePath: String, isH264: Boolean = false) {
        mSpeed = speed
        mMediaRecorder =
            MediaRecorder(mWidth, mHeight, context, eglContext)
        mMediaRecorder!!.start(mSpeed, filePath, isH264)
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