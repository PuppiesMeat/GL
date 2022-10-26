package com.hs.opengl.triangle.square

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.hs.opengl.triangle.OpenGLUtils

/**
 *
 * Date: 2022/8/9
 * Time: 16:51
 * @author shun.he
 */
abstract class BaseRender : GLSurfaceView.Renderer {

    companion object {
        private const val V_POSITION = "vPosition"
        private const val V_COLOR = "vColor"
        private const val V_MATRIX = "vMatrix"
    }

    protected var mProgram = -1
    protected var vColorHandle = -1
    protected var vPositionHandle = -1
    protected var vMatrixHandle = -1

    fun generateProgram(vertexShaderCode: String, fragShaderCode: String) {
        mProgram = OpenGLUtils.loadProgram(vertexShaderCode, fragShaderCode)
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, V_POSITION)
        vColorHandle = GLES20.glGetUniformLocation(mProgram, V_COLOR)
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, V_MATRIX)
    }

    protected open fun clearColor() {
        GLES20.glClearColor(1.0F, 1.0F, 1.0F, 1.0F)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    protected open fun useProgram() {
        GLES20.glUseProgram(mProgram)
    }
}