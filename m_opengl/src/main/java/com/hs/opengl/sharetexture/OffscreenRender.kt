package com.hs.opengl.sharetexture

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.hs.opengl.sharetexture.egl.OpenGLUtils
import com.hs.opengl.sharetexture.filter.NV21ReadPixelDataListener
import com.hs.opengl.sharetexture.filter.NV21ReadPixelFilter
import com.hs.opengl.sharetexture.filter.RecordFilter
import com.hs.opengl.sharetexture.filter.ScreenFilter
import com.rokid.rkdronecontrol.baseui.view.filter.CameraFilter

/**
 *
 * Date: 2022/9/16
 * Time: 17:39
 * @author shun.he
 */
class OffscreenRender {

    private var mSurfaceTexture: SurfaceTexture? = null
    private var glContextWrapper: OffscreenEglContext? = null
    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null
    private var mWidth = 0
    private var mHeight = 0
    private var mCameraFilter: CameraFilter? = null
    private var mScreenFilter: ScreenFilter? = null
    private var shareContext: EGLContext? = null

    fun create(context: Context, width: Int, height: Int, shareContext: EGLContext?) {
        mWidth = width
        mHeight = height
        this.shareContext = shareContext
        createGLThread()
        createGLEnv()
        queueEvent {
            mScreenFilter = ScreenFilter(context)
            mScreenFilter?.setSize(mWidth, mHeight)
            mCameraFilter = CameraFilter(context)
            mCameraFilter?.setSize(mWidth, mHeight)
        }
    }

    private fun createGLThread() {
        glThread = HandlerThread("OffscreenGL")
        glThread?.start()
        glHandler = Handler(glThread!!.looper)
    }

    private fun disposeGLThread() {
        queueEvent {
            glHandler?.let {
                glHandler = null
                it.removeCallbacksAndMessages(null)
                it.looper.quitSafely()
                glThread?.join()
                glThread = null
            }
        }

    }

    private fun queueEvent(func: () -> Unit) {
        if (Looper.myLooper() == glThread?.looper) {
            func.invoke()
        } else {
            glHandler?.post(func)
        }
    }

    private fun createGLEnv() {
        queueEvent {
            glContextWrapper = OffscreenEglContext(shareContext)
            glContextWrapper?.createEglContext(mWidth, mHeight)
            glContextWrapper?.getEglContext()?.let { context ->
                mListeners.forEach {
                    it.onGLContextReady(context)
                }
            }
        }
    }

    private fun disposeGLEnv() {
        queueEvent {
            glContextWrapper?.dispose()
        }
    }

    private var textureIds = IntArray(1)
    fun createSurfaceTexture(): SurfaceTexture {
        GLES20.glGenTextures(1, textureIds, 0)
        mSurfaceTexture = SurfaceTexture(textureIds[0])
        mSurfaceTexture?.setDefaultBufferSize(mWidth, mHeight)
        val target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(target, textureIds[0])
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glBindTexture(target, GLES20.GL_NONE)
        return mSurfaceTexture!!
    }


    private var mtx = FloatArray(16)
    fun onDrawFrame(rotation: Int) {
        queueEvent {
            mSurfaceTexture?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (it.isReleased) {
                        return@queueEvent
                    }
                }
                val matrix = calculateMatrix(it, rotation)
                it.updateTexImage()
//                it.getTransformMatrix(matrix)
                mCameraFilter?.setTransformMatrix(matrix)
                var tex = mCameraFilter!!.onDraw(textureIds[0], it.timestamp)
                mListeners.forEach { listener ->
                    listener.onDrawTexture(tex, it.timestamp)
                }
                recordFilter?.onDraw(tex, it.timestamp)
                readPixelFilter?.onDraw(tex, it.timestamp)
            }
        }

    }

    private fun calculateMatrix(surfaceTexture: SurfaceTexture, rotation: Int): FloatArray {
        val stMatrix = FloatArray(16)
        surfaceTexture.getTransformMatrix(stMatrix)
//        val rotateStMatrix = FloatArray(16)
//        val rotateMatrix = FloatArray(16)
//        Matrix.setIdentityM(rotateMatrix, 0)
//        Matrix.translateM(rotateMatrix, 0, 0.5f, 0.5f, 0f)
//        Matrix.rotateM(rotateMatrix, 0, (rotation + 90) * 1.0F, 0f, 0f, -1f)
//        Matrix.translateM(rotateMatrix, 0, -0.5f, -0.5f, 0f)
//        Matrix.multiplyMM(rotateStMatrix, 0, rotateMatrix, 0, stMatrix, 0)
        return stMatrix


    }

    fun dispose() {
        queueEvent {
            mListeners.forEach {
                it.onGLContextDestroyed()
            }
            mListeners.clear()
            mCameraFilter?.release()
            mCameraFilter = null
            mScreenFilter?.release()
            mScreenFilter = null
            readPixelFilter?.release()
            readPixelFilter = null
            disposeGLEnv()
            disposeGLThread()
            mSurfaceTexture?.release()
            mSurfaceTexture = null
        }


    }

    private var mListeners: MutableSet<TextureListener> = mutableSetOf()
    fun registerDrawListener(listener: TextureListener) {
        queueEvent {
            mSurfaceTexture?.updateTexImage()
            glContextWrapper?.getEglContext()?.let {
                listener.onGLContextReady(it)
            }
            mListeners.add(listener)
        }
    }

    fun unregisterDrawListener(listener: TextureListener) {
        queueEvent {
            mListeners.remove(listener)
        }
    }

    private var readPixelFilter: NV21ReadPixelFilter? = null

    fun startReadPixel(nV21ReadPixelDataListener: NV21ReadPixelDataListener) {
        queueEvent {
            readPixelFilter = NV21ReadPixelFilter(
                OpenGLUtils.applicationContext,
                glContextWrapper!!.getEglContext()!!
            )
            readPixelFilter?.setSize(mWidth, mHeight)
            readPixelFilter?.startReadPixels(nV21ReadPixelDataListener)
        }
    }

    fun stopReadPixel() {
        queueEvent {
            readPixelFilter?.let {
                readPixelFilter = null
                it.release()
            }
        }
    }

    private var recordFilter: RecordFilter? = null
    fun startRecord() {
        queueEvent {
            recordFilter = RecordFilter(OpenGLUtils.applicationContext, glContextWrapper!!.getEglContext()!!)
            recordFilter?.setSize(mWidth, mHeight)
            val path = "${OpenGLUtils.applicationContext.externalCacheDir!!.absolutePath}/a.h264"
            recordFilter?.startRecord(1.0F, path, true)
        }
    }

    fun stopRecord(){
        queueEvent {
            recordFilter?.stopRecord()
            recordFilter = null
        }
    }
}

interface TextureListener {
    fun onDrawTexture(textureId: Int, timestamp: Long)

    fun onGLContextReady(eglContext: EGLContext)

    fun onGLContextDestroyed()
}