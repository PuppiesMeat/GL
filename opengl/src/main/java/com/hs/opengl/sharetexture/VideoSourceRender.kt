package com.hs.opengl.sharetexture

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import com.hs.opengl.sharetexture.filter.BeautyFilter
import com.hs.opengl.sharetexture.filter.CameraFilter
import com.hs.opengl.sharetexture.filter.RecordFilter
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * Date: 2022/5/11
 * Time: 22:17
 * @author shun.he
 */
class VideoSourceRender(_cameraView: GLSurfaceView) : GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {
    private var mSurfaceTexture: SurfaceTexture? = null
    private var texName: IntArray? = null
    private var mCameraView = _cameraView
    private var mCameraFilter: CameraFilter? = null
    private var mRecordFilter: RecordFilter? = null
    private var mBeautyFilter: BeautyFilter? = null
    private var mtx = FloatArray(16)
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        mSurfaceTexture?.let {
            texName = IntArray(1)
            it.attachToGLContext(texName!![0])
            it.setOnFrameAvailableListener(this)
        }
        mCameraFilter = CameraFilter(mCameraView.context)
        mRecordFilter = RecordFilter(mCameraView.context, EGL14.eglGetCurrentContext())
        mBeautyFilter = BeautyFilter(mCameraView.context)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        mCameraFilter?.setSize(width, height)
        mRecordFilter?.setSize(width, height)
        mBeautyFilter?.setSize(width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        mSurfaceTexture?.let {
            it.updateTexImage()
            it.getTransformMatrix(mtx)
            mCameraFilter?.setTransformMatrix(mtx)
            var textureId = mCameraFilter?.onDraw(texName!![0], it.timestamp)
            textureId = mBeautyFilter?.onDraw(textureId!!, it.timestamp)

            textureId = mRecordFilter?.onDraw(textureId!!, it.timestamp)
        }
    }

    fun bindSurfaceTexture(surface: SurfaceTexture) {
        mSurfaceTexture = surface
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        mCameraView.requestRender()
    }

    fun release() {
        mCameraFilter?.release()
        mRecordFilter?.release()
        mBeautyFilter?.release()
    }

    fun startRecord(speed: Float) {
        mRecordFilter?.startRecord(speed)
    }

    fun stopRecord() {
        mRecordFilter?.stopRecord()
    }

    fun enableBeauty(enable: Boolean) {

    }
}