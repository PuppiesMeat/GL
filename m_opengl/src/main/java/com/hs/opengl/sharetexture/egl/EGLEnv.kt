package com.hs.opengl.sharetexture.egl

import android.content.Context
import android.opengl.*
import android.view.Surface
import com.hs.opengl.sharetexture.filter.ScreenFilter

/**
 *
 * Date: 2022/5/13
 * Time: 02:32
 * @author shun.he
 */
class EGLEnv(context: Context,private var eglContext: EGLContext, surface: Surface, width: Int, height: Int) {
    private var mEglDisplay: EGLDisplay? = null
    private var mEglConfig: EGLConfig? = null
    private var mEglSurface: EGLSurface? = null
    private var mScreenFilter: ScreenFilter? = null

    init {
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("create egl display failed")
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
        val contextAttributeList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, eglContext, contextAttributeList, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("EGL error: ${EGL14.eglGetError()}")
        }
        val surfaceAttributesList = intArrayOf(EGL14.EGL_NONE)
        mEglSurface =
            EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, surfaceAttributesList, 0)
        if (mEglConfig == null) {
            throw RuntimeException("EGL error: ${EGL14.eglGetError()}")
        }
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, eglContext)) {
            throw RuntimeException("EGL error: ${EGL14.eglGetError()}")
        }
        mScreenFilter = ScreenFilter(context).apply {
            setSize(width, height)
        }
    }

    fun draw(textureId: Int, timestamp: Long) {
        mScreenFilter?.onDraw(textureId, timestamp)
        //帧缓冲 时间戳
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, timestamp)
        //双缓冲
        EGL14.eglSwapBuffers(mEglDisplay, mEglSurface)
    }

    fun release() {
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface)
        EGL14.eglMakeCurrent(
            mEglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglReleaseThread()
        EGL14.eglTerminate(mEglDisplay)
        mScreenFilter?.release()
    }
}