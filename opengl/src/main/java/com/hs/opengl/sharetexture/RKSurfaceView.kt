package com.hs.opengl.sharetexture

import android.content.Context
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.hs.opengl.sharetexture.egl.EGLEnv

/**
 *
 * Date: 2022/9/18
 * Time: 21:54
 * @author shun.he
 */
class RKSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, TextureListener {

    private var glHandler: Handler? = null
    private var glThread: HandlerThread? = null
    private var mGlContext: EGLContext? = null


    private fun bindGLContext(glContext: EGLContext) {
        env?.release()
        mGlContext = glContext
        glHandler?.post {
            if (holder.surface != null && holder.surface.isValid) {
                internalBindGLContext(glContext)
            } else {
                holder.addCallback(this)
            }
        }

    }


    private var TAG = RKSurfaceView::class.java.simpleName
    private var env: EGLEnv? = null
    private fun internalBindGLContext(glContext: EGLContext) {
        glHandler?.post {
            if (holder.surface.isValid){
                kotlin.runCatching {
                    env = EGLEnv(this.context, glContext, holder.surface, width, height)
                }.onFailure {
                    Log.e(TAG, "create share opengl env failed:${it.message}")
                }
            }
            else {
                Log.e(TAG, "internalBindGLContext failed, surface is invalid")
            }
        }
    }

    private fun destroyThread() {
        glHandler?.let {
            glHandler = null
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            try {
                glThread?.join()
            } catch (ex: Exception) {

            }
            glThread = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mGlContext?.let {
            internalBindGLContext(it)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    override fun onDrawTexture(textureId: Int, timestamp: Long) {
        glHandler?.post {
            env?.draw(textureId, timestamp)
        }
    }

    override fun onGLContextReady(eglContext: EGLContext) {
        createGLThread()
        bindGLContext(eglContext)
    }

    private fun createGLThread() {
        glHandler?.let {
            destroyThread()
        }
        glThread = HandlerThread("debug_surface")
        glThread!!.start()
        glHandler = Handler(glThread!!.looper)
    }

    fun release(){
        destroyGLEnv()
    }

    private fun destroyGLEnv() {
        glHandler?.post {
            env?.let {
                env = null
                it.release()
            }
            destroyThread()
        }
    }

    override fun onGLContextDestroyed() {
        destroyGLEnv()
    }

}