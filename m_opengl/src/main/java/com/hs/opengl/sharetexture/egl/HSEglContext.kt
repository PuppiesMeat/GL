package com.hs.opengl.sharetexture.egl

import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import javax.microedition.khronos.egl.EGL11


/**
 *
 * Date: 2022/7/27
 * Time: 19:56
 * @author shun.he
 */
class HSEglContext {

    private var mEglDisplay: EGLDisplay? = null
    private var TAG = HSEglContext::class.java.simpleName
    private var mEglConfig: EGLConfig? = null
    private var mEglSurface: EGLSurface? = null
    private var eglContext: EGLContext? = null
    fun createEglContext() {
        //initialize display
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL11.EGL_NO_DISPLAY) {
            Log.e(TAG, "Unable to get access to Native Display")
        }

        val configAttributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val numConfigs = IntArray(1)
        val eglConfigs = arrayOfNulls<EGLConfig>(1)
        if (!EGL14.eglChooseConfig(
                mEglDisplay,
                configAttributes,
                0,
                eglConfigs,
                0,
                eglConfigs.size,
                numConfigs,
                0
            )
        ) {
            throw RuntimeException("EGL error: ${EGL14.eglGetError()}")
        }

        mEglConfig = eglConfigs[0]

        val surfaceAttributesList = intArrayOf(EGL14.EGL_NONE)

        mEglSurface = EGL14.eglCreateWindowSurface(
            mEglDisplay,
            mEglConfig,
            EGL14.EGL_NO_SURFACE,
            surfaceAttributesList,
            0
        )
        if (mEglSurface === EGL11.EGL_NO_SURFACE) {
            Log.e("SimpleGLView", "Unable to create surface")
            val error: Int = EGL14.eglGetError()
            when (error) {
                EGL11.EGL_BAD_CONFIG -> Log.e("SimpleGLView", "Invalid configuration selected")
                EGL11.EGL_BAD_NATIVE_WINDOW -> Log.e("SimpleGLView", "Bad native window used")
                EGL11.EGL_BAD_ALLOC -> Log.e(
                    "SimpleGLView",
                    "Not enough resources to create a surface"
                )
            }
            return
        }
        val contextAttributeList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            mEglDisplay,
            mEglConfig,
            EGL14.EGL_NO_CONTEXT,
            contextAttributeList,
            0
        )
        if (eglContext == null) {
            Log.i(TAG, "Create Context failed")
            return
        }

        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, eglContext)) {
            Log.i(TAG, "Made current")
        }
    }

    fun getEglContext() = eglContext

    fun getEglSurface() = mEglSurface

    fun dispose() {
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface)
        EGL14.eglMakeCurrent(
            mEglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(mEglDisplay)
    }

}