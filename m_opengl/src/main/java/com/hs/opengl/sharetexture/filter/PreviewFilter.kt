package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.hs.opengl.sharetexture.R
import com.hs.opengl.sharetexture.egl.EGLEnv

/**
 *
 * Date: 2022/5/13
 * Time: 01:49
 * @author shun.he
 */
class PreviewFilter(var context: Context, private var eglContext: EGLContext) :
    AbsFilter(context, R.raw.base_vert, R.raw.base_frag) {

    private var mEglHandler: Handler? = null
    private var mEglEnv: EGLEnv? = null
    private var mEglThread:HandlerThread? = null

    fun startPreview(surface: Surface){
        mEglHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            mEglEnv?.release()
        }
        mEglThread = HandlerThread("preview")
        mEglThread!!.start()
        mEglHandler = Handler(mEglThread!!.looper)
        mEglHandler?.post {
            mEglEnv = EGLEnv(context, eglContext, surface, mWidth, mHeight)
        }
    }
    fun stopPreview(){

    }

    override fun onDraw(texName: Int, timestamp: Long): Int {
        val textureId = super.onDraw(texName, timestamp)
        mEglHandler?.post {
            drawFrame(textureId, timestamp)
        }
        return textureId
    }

    private fun drawFrame(texName: Int, timestamp: Long) {
        mEglEnv?.draw(texName, timestamp)
    }


    override fun release() {
        super.release()
    }


}