package com.hs.opengl.sharetexture

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.hs.opengl.sharetexture.egl.EGLEnv
import com.hs.opengl.sharetexture.egl.HSEglContext
import com.hs.opengl.sharetexture.filter.*
import com.rokid.rkdronecontrol.baseui.view.filter.CameraFilter

/**
 *
 * Date: 2022/7/27
 * Time: 19:14
 * @author shun.he
 */
class HSFilters : SurfaceTexture.OnFrameAvailableListener {

    private var mSurfaceTexture: SurfaceTexture? = null
    private var eglContext: HSEglContext? = null
    private var eglHandlerThread: HandlerThread? = null
    private var eglHandler: Handler? = null
    private var mFilters = mutableListOf<AbsFilter>()
    private var applicationContext: Context? = null
    private var mCameraFilter: CameraFilter? = null
    private var mRecordFilter: RecordFilter? = null
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var isGlAttach = false
    private var texture: Int = 0
    private var mtx = FloatArray(16)


    fun init(context: Context, width: Int, height: Int) {
        eglHandler?.let {
            it.looper.quitSafely()
            eglHandlerThread!!.quitSafely()
            eglHandlerThread!!.join(2000)
        }
        mWidth = width
        mHeight = height
        eglHandlerThread = HandlerThread("filter-components").apply { start() }
        eglHandler = Handler(eglHandlerThread!!.looper)
        queueEvent {
            eglContext = HSEglContext().apply {
                this.createEglContext()
            }
            applicationContext = context
            mCameraFilter =
                CameraFilter(context.applicationContext).apply { setSize(width, height) }
            mRecordFilter = RecordFilter(context, eglContext!!.getEglContext()!!).apply {
                setSize(
                    width,
                    height
                )
            }
            mFilters.add(mCameraFilter!!)
            mSurfaceTexture?.let {
                internalAttachGLContext()
            }
        }
    }

    private fun internalAttachGLContext() {
        queueEvent {
            mSurfaceTexture?.let {
                if (isGlAttach) return@queueEvent
                isGlAttach = true
                Log.d(TAG, "internalAttachGLContext")
                val textures = IntArray(1)
                it.attachToGLContext(textures[0])
                texture = textures[0]
                it.setOnFrameAvailableListener(this)
                onFrameAvailable(mSurfaceTexture)
            }

        }
    }

    fun queueEvent(run: () -> Unit) {
        if (Looper.myLooper() == eglHandler?.looper) {
            run.invoke()
        } else {
            eglHandler?.post(run)
        }
    }

    fun dispose() {
        queueEvent {
            eglContext?.dispose()
        }
    }

    fun addFilter(filter: AbsFilter) {
        queueEvent {
            mFilters.add(filter)
        }
    }

    fun removeFilter(filter: AbsFilter) {
        queueEvent {
            mFilters.remove(filter)
            filter.release()
        }

    }

    fun attachFilterContext(surfaceTexture: SurfaceTexture? = null): SurfaceTexture {
        return SurfaceTexture(1)
    }

    fun startRecord() {

    }

    fun stopRecord() {

    }

    fun bindSurfaceTexture(surfaceTexture: SurfaceTexture) {
        if (isGlAttach && surfaceTexture == mSurfaceTexture) {
            mSurfaceTexture = surfaceTexture
            return
        }
        mSurfaceTexture = surfaceTexture
        internalAttachGLContext()
    }

    fun createConsumer(): SurfaceTexture {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        return SurfaceTexture(textures[0])
    }

    private var TAG = HSFilters::class.java.simpleName
    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        queueEvent {
            Log.d(TAG, "onFrameAvailable")
            drawFrame()
        }
    }

    private fun drawFrame() {
        mSurfaceTexture?.let {
            Log.d(TAG, "drawFrame")
            it.updateTexImage()
            it.getTransformMatrix(mtx)
            mCameraFilter?.setTransformMatrix(mtx)
            var textureId: Int? = texture
            Log.d("drawFrame", "textureId start : $textureId")
            mFilters.forEach {
                textureId = it.onDraw(textureId!!, mSurfaceTexture!!.timestamp)
                Log.d("drawFrame", "textureId filter : $textureId")
            }
            Log.d("drawFrame", "textureId end : $textureId")
            readPixelFilter?.onDraw(textureId!!, it.timestamp)
            previewFilter?.onDraw(textureId!!, it.timestamp)
//            textureId = mRecordFilter?.onDraw(textureId!!, it.timestamp)
        }

    }

    private var readPixelFilter:ReadPixelFilter? =null
    fun startReadPixel(listener: ReadPixelDataListener) {
        queueEvent {
            readPixelFilter = ReadPixelFilter(applicationContext!!, eglContext!!.getEglContext()!!)
            readPixelFilter!!.setSize(mWidth, mHeight)
            readPixelFilter!!.startReadPixels(listener)
        }
    }

    private var previewFilter:PreviewFilter? = null
    fun startShow(surface:Surface, width:Int, height:Int) {
//        var textures = IntArray(1)
//        GLES20.glGenTextures(1, textures, 0)
//        previewTexture = SurfaceTexture(textures[0])
        queueEvent {
            previewFilter =  PreviewFilter(applicationContext!!, eglContext!!.getEglContext()!!)
            previewFilter?.setSize(width,height)
            previewFilter?.startPreview(surface)
        }
    }

    fun changePreviewSize(width: Int, height: Int){
        queueEvent {
            previewFilter?.setSize(width, height)
        }
    }


}