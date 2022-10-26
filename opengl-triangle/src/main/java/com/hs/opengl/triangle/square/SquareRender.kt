package com.hs.opengl.triangle.square

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * Date: 2022/8/9
 * Time: 16:44
 * @author shun.he
 */
class SquareRender : BaseRender() {


    companion object {
        private const val vertexShaderCode = "attribute vec4 vPosition;" +
                "uniform mat4 vMatrix;" +
                "varying vec4 vColor;" +
                "attribute vec4 aColor;" +
                "void main() {" +
                "gl_Position = vMatrix * vPosition;" +
                "vColor = aColor;" +
                "}"
        private const val fragShaderCode = "precision mediump float;" +
                "varying vec4 vColor;" +
                "void main(){" +
                "gl_FragColor = vColor;" +
                "}"

        private const val A_COLOR = "aColor"
        private const val COORDS_PER_VERTEX = 3
    }

    private var aColorHandle = -1
    private var vertexPosition = floatArrayOf(
        -0.5F, 0.5F, 0.0F,
        0.5F, 0.5F, 0.0F,
        0.5F, -0.5F, 0.0F,
        -0.5F, -0.5F, 0.0F,
    )
    private var index = shortArrayOf(
        0, 1, 2, 0, 2, 3
    )
    private val vertexStride = 3 * 4//每个顶点有x\y\z，每个坐标都是float（4个字节）
    private var colors = floatArrayOf(
        0.0F, 0.0F, 1.0F, 1.0F,
        1.0F, 0.0F, 0.0F, 1.0F,
        0.0F, 1.0F, 0.0F, 1.0F,
        1.0F, 1.0F, 0.0F, 0.0F,
    )
    private var mProjectionMatrix = FloatArray(16)
    private var mViewMatrix = FloatArray(16)
    private var mVPMatrix = FloatArray(16)
    private var colorBuffer =
        ByteBuffer.allocateDirect(colors.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexPosition.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    private var indexBuffer =
        ByteBuffer.allocateDirect(colors.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.generateProgram(vertexShaderCode, fragShaderCode)
        vertexBuffer.put(vertexPosition)
        vertexBuffer.position(0)
        aColorHandle = GLES20.glGetAttribLocation(mProgram, A_COLOR)
        colorBuffer.put(colors)
        colorBuffer.position(0)
        indexBuffer.put(index)
        indexBuffer.position(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        //透视投影
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1F, 1F, 3F, 7F)
        //相机矩阵
        Matrix.setLookAtM(mViewMatrix, 0, 0F, 0F, 7F, 0F, 0F, 0F, 0F, 1F, 0F)
        //矩阵变换
        Matrix.multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)

    }

    override fun onDrawFrame(gl: GL10?) {
        super.clearColor()
        super.useProgram()
        drawFrame()
    }


    private fun drawFrame() {
        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, mVPMatrix, 0)
        GLES20.glEnableVertexAttribArray(vPositionHandle)
        GLES20.glVertexAttribPointer(
            vPositionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aColorHandle)
        GLES20.glVertexAttribPointer(aColorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexPosition.size / 3)
//        GLES20.glDrawElements(
//            GLES20.GL_TRIANGLES,
//            index.size,
//            GLES20.GL_UNSIGNED_SHORT,
//            indexBuffer
//        )
        GLES20.glDisableVertexAttribArray(aColorHandle)
        GLES20.glDisableVertexAttribArray(vPositionHandle)
    }


}