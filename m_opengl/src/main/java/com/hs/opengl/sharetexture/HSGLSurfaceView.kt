package com.hs.opengl.sharetexture

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.Surface
import com.hs.opengl.sharetexture.filter.AbsFilter
import com.hs.opengl.sharetexture.filter.ReadPixelDataListener

/**
 *
 * Date: 2022/5/11
 * Time: 22:10
 * @author shun.he
 */
class HSGLSurfaceView(context: Context?, attributeSet: AttributeSet?) :
    GLSurfaceView(context, attributeSet) {

    fun bindSurfaceTexture(surface: SurfaceTexture) {
        mRender.bindSurfaceTexture(surface)
    }

    fun startRecord(speed: Float, filePath: String, isH264: Boolean = false) {
        mRender.startRecord(speed, filePath, isH264)
    }

    fun stopRecord() {
        mRender.stopRecord()
    }

    fun insertSeiData(data: ByteArray) {
        mRender.insertSeiData(data)
    }

    fun setSpeed(speed: Float) {
        mRender.setSpeed(speed)
    }

    fun addFboFilter(faceFboFilter: AbsFilter) {
        mRender.addFilter(faceFboFilter)
    }

    fun startReadPixel(readPixelDataListener: ReadPixelDataListener) {
        mRender.startReadPixel(readPixelDataListener)
    }

    fun stopReadPixel() {
        mRender.stopReadPixel()
    }

    private var surface: Surface? = null
    private var h264Decoder: H264Decoder? = null
    private var surfaceTex:SurfaceTexture? = null
    fun startH264FilePath(path: String, listener: H264SeiDataListener) {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        surfaceTex = SurfaceTexture(texture[0])
        surfaceTex!!.setOnFrameAvailableListener {
            bindSurfaceTexture(it)
        }
        surfaceTex!!.setDefaultBufferSize(1920, 1080)
        surface = Surface(surfaceTex)
        h264Decoder = H264Decoder()
        h264Decoder?.create()
        h264Decoder?.start(path, surface!!, listener)
    }

    fun stopH264File() {
        surfaceTex?.release()
        surfaceTex = null
        surface?.release()
        surface = null
        h264Decoder?.stop()
        h264Decoder?.dispose()
        h264Decoder = null

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