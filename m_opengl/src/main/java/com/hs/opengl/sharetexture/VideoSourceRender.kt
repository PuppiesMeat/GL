package com.hs.opengl.sharetexture

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLContext
import android.opengl.GLSurfaceView
import android.util.Log
import com.hs.opengl.sharetexture.filter.*
import com.rokid.rkdronecontrol.baseui.view.filter.*
import java.util.concurrent.CopyOnWriteArrayList
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
    private var mFilters = CopyOnWriteArrayList<AbsFilter>()
    private var mtx = FloatArray(16)
    private var eglContext: EGLContext? = null
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    private var isBinding = false
    private fun bindingReady(isForce: Boolean = false) {
        if (isBinding && !isForce) return
        mCameraView.queueEvent {
            mSurfaceTexture?.let {
                isBinding = true
                texName = IntArray(1)
//                it.detachFromGLContext()
                it.attachToGLContext(texName!![0])
                it.setOnFrameAvailableListener(this)
                mCameraView.requestRender()
            }
        }

    }

    private var TAG = VideoSourceRender::class.java.simpleName
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        Log.i(TAG, "VideoSourceRender: onSurfaceCreated $mSurfaceTexture")
        mSurfaceTexture?.let {
            bindingReady(true)
        }

        eglContext = EGL14.eglGetCurrentContext()
        mCameraFilter = CameraFilter(mCameraView.context)
        mRecordFilter = RecordFilter(mCameraView.context, eglContext!!)
        mFilters.add(mCameraFilter!!)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        Log.i(TAG, "VideoSourceRender: onSurfaceChanged")
        mSurfaceWidth = width
        mSurfaceHeight = height
        mFilters.forEach { it.setSize(width, height) }
        mRecordFilter?.setSize(mSurfaceWidth, mSurfaceHeight)

    }

    override fun onDrawFrame(p0: GL10?) {
        Log.i(TAG, "VideoSourceRender: onDrawFrame: $mSurfaceTexture")
        mSurfaceTexture?.let {
            texName ?: return@let
            it.updateTexImage()
            it.getTransformMatrix(mtx)
            mCameraFilter?.setTransformMatrix(mtx)
            var textureId: Int? = texName!![0]
            mFilters.forEach {
                textureId = it.onDraw(textureId!!, mSurfaceTexture!!.timestamp)
            }
            readPixelFilter?.onDraw(textureId!!, it.timestamp)
            textureId = mRecordFilter?.onDraw(textureId!!, it.timestamp)
        }
    }

    fun bindSurfaceTexture(surface: SurfaceTexture) {
        mSurfaceTexture = surface
        Log.i(TAG, "VideoSourceRender: bindSurfaceTexture: $mSurfaceTexture")
        if (!isBinding) {
            bindingReady()
        } else {
            mCameraView.requestRender()
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        Log.i(TAG, "VideoSourceRender: onFrameAvailable: $mSurfaceTexture")
        if (!isBinding) {
            mSurfaceTexture = surfaceTexture
            bindingReady()
        } else {
            mCameraView.requestRender()
        }
    }

    fun release() {
        stopRecord()
        mFilters.forEach { it.release() }
        mFilters.clear()
    }

    fun startRecord(speed: Float, filePath: String, isH264: Boolean = false) {
        mCameraView.queueEvent {
            mRecordFilter?.startRecord(speed, filePath, isH264)
        }
    }

    fun insertSeiData(data: ByteArray) {
        mCameraView.queueEvent {
            mRecordFilter?.insertSeiData(data)
        }
    }


    fun stopRecord() {
        mCameraView.queueEvent {
            mRecordFilter?.stopRecord()
        }
    }


    fun addFilter(filter: AbsFilter) {
        filter.setSize(mSurfaceWidth, mSurfaceHeight)
        mFilters.add(filter)
    }

    fun removeFilter(filter: AbsFboFilter) {
        mFilters.remove(filter)
    }

    fun setSpeed(speed: Float) {
        mCameraView.queueEvent {
            mRecordFilter?.setSpeed(speed)
        }
    }

    private var readPixelFilter: ReadPixelFilter? = null
    fun startReadPixel(readPixelDataListener: ReadPixelDataListener) {
        mCameraView.queueEvent {
            readPixelFilter = ReadPixelFilter(mCameraView.context, eglContext!!)
            readPixelFilter!!.setSize(mSurfaceWidth, mSurfaceHeight)
//            mFilters.add(readPixelFilter)
            readPixelFilter!!.startReadPixels(readPixelDataListener)
        }
    }

    fun stopReadPixel() {
        mCameraView.queueEvent {
            mFilters.remove(readPixelFilter)
            readPixelFilter?.release()
            readPixelFilter = null
        }
    }
}