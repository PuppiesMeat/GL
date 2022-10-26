package com.hs.opengl.triangle.triangle

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.hs.opengl.triangle.OpenGLUtils
import com.hs.opengl.triangle.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * Date: 2022/8/9
 * Time: 11:31
 * @author shun.he
 */
class TriangleNormalRender : GLSurfaceView.Renderer {
    private val vertexShaderCode = "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "}"

    private val fragmentShaderCode = "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}"
    private var triangleCoords = floatArrayOf(
        0.5f, 0.5f, 0.0f, // top
        -0.5f, -0.5f, 0.0f, // bottom left
        0.5f, -0.5f, 0.0f  // bottom right
    )

    private var triangleColor = floatArrayOf(
        0.0f, 1.0f, 0.0f, 1.0f,
    )

    private var clearColors = floatArrayOf(
        0.5f, 0.5f, 0.5f, 1.0f//灰色
    )
    private var clearColors1 = floatArrayOf(1.0f, 1.0f, 0.8f, 0.4f)
    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mVColorHandle = 0
    private val vertexCount = triangleCoords.size / COORDS_PER_VERTEX
    private val vertexStride = COORDS_PER_VERTEX * 4


    companion object {

        private const val V_POSITION = "vPosition"
        private const val V_COLOR = "vColor"
        private const val COORDS_PER_VERTEX = 3
    }

    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(triangleCoords.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()


    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        clearColor(clearColors)
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)
        mProgram = generateProgram(R.raw.triangle_vert, R.raw.triangle_frag)
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, V_POSITION)
        mVColorHandle = GLES20.glGetUniformLocation(mProgram, V_COLOR)
    }

    private fun generateProgram(triangleVert: Int, triangleFrag: Int): Int {
        return OpenGLUtils.loadProgram(
            vertexShaderCode,
            fragmentShaderCode
//            OpenGLUtils.readRawTextFile(triangleVert),
//            OpenGLUtils.readRawTextFile(triangleFrag)
        )
    }

    private fun clearColor(clearColors: FloatArray) {
        GLES20.glClearColor(clearColors[0], clearColors[1], clearColors[2], clearColors[3])
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        clearColor(clearColors)
        useProgram()
        drawFrame()
    }

    private fun drawFrame() {
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )
        GLES20.glUniform4fv(mVColorHandle, 1, triangleColor, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    private fun useProgram() {
        GLES20.glUseProgram(mProgram)
    }
}