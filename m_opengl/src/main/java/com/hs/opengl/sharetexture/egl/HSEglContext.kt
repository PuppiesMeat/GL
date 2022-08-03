package com.hs.opengl.sharetexture.egl

import android.opengl.*
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
    private var mEglContext: EGLContext? = null
    fun createEglContext() {
        //initialize display
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL11.EGL_NO_DISPLAY) {
            Log.e(TAG, "Unable to get access to Native Display")
        }
        val vers = IntArray(2)
        EGL14.eglInitialize(mEglDisplay, vers, 0, vers, 1)
        val configAttr = intArrayOf(
            EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
            EGL14.EGL_LEVEL, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        EGL14.eglChooseConfig(
            mEglDisplay, configAttr, 0,
            configs, 0, 1, numConfig, 0
        )
        if (numConfig[0] == 0) {
            // TROUBLE! No config found.
        }
        mEglConfig = configs[0]
        val surfAttr = intArrayOf(
            EGL14.EGL_WIDTH, 64,
            EGL14.EGL_HEIGHT, 64,
            EGL14.EGL_NONE
        )
        mEglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig, surfAttr, 0)
        val ctxAttrib = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)
        EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)
        val maxSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        Log.d(TAG, "GL_MAX_TEXTURE_SIZE:  ${maxSize[0]}")
    }

    fun getEglContext() = mEglContext

    fun getEglSurface() = mEglSurface

    fun dispose() {
        EGL14.eglMakeCurrent(
            mEglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface)
        EGL14.eglDestroyContext(mEglDisplay, mEglContext)
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(mEglDisplay)
    }

}