package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.GLES20
import com.hs.opengl.sharetexture.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 *
 * Date: 2022/5/13
 * Time: 00:17
 * @author shun.he
 */
abstract class AbsFilter(context: Context, vertexShaderId: Int, fragmentShaderId: Int) {

    protected var mWidth = 0
    protected var mHeight = 0
    protected var mMtx: FloatArray? = null
    private var TEXTURE = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )
    private var textureBuffer: FloatBuffer? = null

    val VERTEX = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    )
    private var vertexBuffer: FloatBuffer//顶点坐标缓冲区
    private var vPosition: Int = 0
    private var VPOSITION_NAME = "vPosition"
    protected var program: Int = 0
    private var vCoord = 0
    private var VCOORD_NAME = "vCoord"
    private var vTexture = 0
    private var VTEXTURE_NAME = "vTexture"
    protected var vMatrix = 0
    protected var VMATRIX_NAME = "vMatrix"

    init {
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().also {
                it.clear()
                it.put(VERTEX)
            }
        textureBuffer = ByteBuffer.allocateDirect(TEXTURE.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().also {
                it.clear()
                it.put(TEXTURE)
            }

        //加载顶点着色器程序
        val vertexShader = OpenGLUtils.readRawTextFile(context, vertexShaderId)
        val fragShader = OpenGLUtils.readRawTextFile(context, fragmentShaderId)
        program = OpenGLUtils.loadProgram(vertexShader, fragShader)
        vPosition = GLES20.glGetAttribLocation(program, VPOSITION_NAME)
        vCoord = GLES20.glGetAttribLocation(program, VCOORD_NAME)
        vTexture = GLES20.glGetUniformLocation(program, VTEXTURE_NAME)
    }

    open fun onDraw(texName: Int, timestamp:Long): Int {
        //渲染范围
        GLES20.glViewport(0, 0, mWidth, mHeight)
        GLES20.glUseProgram(program)
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            vPosition,
            2
            /**每个顶点坐标数*/
            ,
            GLES20.GL_FLOAT,
            false
            /**标准化/非标准化*/
            ,
            0, vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(vPosition)
        textureBuffer!!.position(0)
        GLES20.glVertexAttribPointer(
            vCoord, 2, GLES20.GL_FLOAT,
            false, 0, textureBuffer
        )
        GLES20.glEnableVertexAttribArray(vCoord)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texName)
        GLES20.glUniform1i(vTexture, 0)
        beforeDraw()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        return texName
    }

    protected open fun beforeDraw() {

    }

    open fun setSize(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    open fun setTransformMatrix(mtx: FloatArray) {
        mMtx = mtx
    }

    open fun release() {
        GLES20.glDeleteProgram(program)
    }
}