package com.hs.opengl.triangle.square

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.hs.opengl.triangle.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * Date: 2022/8/13
 * Time: 12:03
 * @author shun.he
 */
class OvalRender : GLSurfaceView.Renderer {

    companion object {
        const val vertexShaderCode = "attribute vec4 vPosition;" +
                "uniform mat4 vMatrix;" +
                "void main(){" +
                "gl_Position = vMatrix*vPosition;}"

        const val fragShaderCode = "precision mediump float;" +
                "uniform vec4 aCoordinate;" +
                "void main(){" +
                "gl_FragColor = aCoordinate;}"
    }

    private var mProgram = -1
    private var vPositionHandle = -1
    private var aCoordinateHandle = -1

//    private var indexs = shortArrayOf(
//        0, 1, 2, 0, 2, 3, 0, 1, 3
//    )

    private var color = floatArrayOf(
        1.0f, 1.0f, 0.8f, 1.0f
    )
    private var shapes = floatArrayOf()
    private var mvpMatrix = FloatArray(16)
    private var mViewMatrix = FloatArray(16)
    private var mProjectionMatrix = FloatArray(16)

//    private var indexBuffer =
//        ByteBuffer.allocateDirect(indexs.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()

    private lateinit var shareBuffer: FloatBuffer
    private var vMatrixHandle = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mProgram = OpenGLUtils.loadProgram(vertexShaderCode, fragShaderCode)
        vPositionHandle = getAttribute(mProgram, "vPosition")
        aCoordinateHandle = getUniform(mProgram, "aCoordinate")
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
//        indexBuffer.put(indexs)
//        indexBuffer.position(0)
        shapes = createPositions()
        shareBuffer = ByteBuffer.allocateDirect(shapes.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        shareBuffer.put(shapes)
        shareBuffer.position(0)
    }

    fun getAttribute(program: Int, attrName: String): Int {
        return GLES20.glGetAttribLocation(program, attrName)
    }

    fun getUniform(program: Int, attrName: String): Int {
        return GLES20.glGetUniformLocation(program, attrName)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        //计算宽高比
        val ratio = width.toFloat() / height
        //设置透视投影
        //设置透视投影
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        //设置相机位置
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        //计算变换矩阵
        //计算变换矩阵
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(vPositionHandle)
        GLES20.glVertexAttribPointer(
            vPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            shareBuffer
        )

        GLES20.glUniform4fv(aCoordinateHandle, 1, color, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, shapes.size / 3)
        GLES20.glDisableVertexAttribArray(vPositionHandle)
    }

    private var n = 360
    private val height = 100F
    private val radius = 100
    private fun createPositions(): FloatArray {
        val data = ArrayList<Float>()
        data.add(0.0f) //设置圆心坐标
        data.add(0.0f)
        data.add(height)
        val angDegSpan: Float = 360f / n
        run {
            var i = 0f
            while (i < 360 + angDegSpan) {
                data.add((radius * Math.sin(i * Math.PI / 180f)).toFloat())
                data.add((radius * Math.cos(i * Math.PI / 180f)).toFloat())
                data.add(height)
                i += angDegSpan
            }
        }
        val f = FloatArray(data.size)
        for (i in f.indices) {
            f[i] = data[i]
        }
        return f
    }
}