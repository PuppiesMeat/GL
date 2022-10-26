package com.hs.opengl.triangle.square

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.hs.opengl.triangle.OpenGLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 * Date: 2022/8/10
 * Time: 14:50
 * @author shun.he
 */
class BitmapRender : GLSurfaceView.Renderer {


    companion object {
        private const val vertexShaderCode = "attribute vec4 vPosition;" +
                "attribute vec2 vCoordinate;" +
                "varying vec2 aCoordinate;" +
                "uniform mat4 vMatrix;" +
                "void main(){" +
                "gl_Position = vMatrix*vPosition;" +
                "aCoordinate = vCoordinate;}"
        private const val fragShaderCode = "precision mediump float;" +
                "uniform sampler2D vTexture;" +
                "varying vec2 aCoordinate;" +
                "void main(){" +
                "gl_FragColor = texture2D(vTexture, aCoordinate);}"
    }


//    private var vertexPoints = floatArrayOf(
//        -1.0F, 1.0F,//左上
//        -1.0F, -1.0F,//左下
//        1.0F, 1.0F, //右上
//        1.0F, -1.0F,//右下
//    )

    private var vertexPoints = floatArrayOf(
        -1.0F, -1.0F,//左下
        1.0F, -1.0F,//右下
        -1.0F, 1.0F,//左上
        1.0F, 1.0F, //右上
    )

//    private var texturePoints = floatArrayOf(
//        0.0F, 0.0F,
//        0.0F, 1.0F,
//        1.0F, 0.0F,
//        1.0F, 1.0F,
//    )

    private var texturePoints = floatArrayOf(
        0.0F, 1.0F,
        1.0F, 1.0F,
        0.0F, 0.0F,
        1.0F, 0.0F,


    )

    private var vertexBuffer =
        ByteBuffer.allocateDirect(vertexPoints.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    private var textureBuffer =
        ByteBuffer.allocateDirect(texturePoints.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()

    private var vPositionHandle = -1
    private var vCoordinateHandle = -1
    private var vMatrixHandle = -1
    private var mProgram = -1
    private var surfaceWidth = -1
    private var surfaceHeight = -1
    private var bitmapWidth = -1
    private var bitmapHeight = -1
    private var vTexturePointer = -1
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        mProgram = OpenGLUtils.loadProgram(vertexShaderCode, fragShaderCode)
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        vCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vCoordinate")
        vTexturePointer = GLES20.glGetUniformLocation(mProgram, "vTexture")
        vMatrixHandle = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        vertexBuffer.put(vertexPoints)
        vertexBuffer.position(0)
        textureBuffer.put(texturePoints)
        textureBuffer.position(0)
        textureId = createBitmapTexture(mBitmap!!)

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
        calculateMatrix()
    }

    private fun calculateMatrix() {
        val bitmapWidthHeight = bitmapWidth.toFloat() / bitmapHeight
        val surfaceWidthHeight = surfaceWidth.toFloat() / surfaceHeight

        if (surfaceWidth > surfaceHeight) {
            if (bitmapWidthHeight > surfaceWidthHeight) {
                Matrix.orthoM(
                    mProjMatrix,
                    0,
                    -surfaceWidthHeight * bitmapWidthHeight,
                    surfaceWidthHeight * bitmapWidthHeight,
                    -1F,
                    1F,
                    3F,
                    7F
                )
            } else {
                Matrix.orthoM(
                    mProjMatrix,
                    0,
                    -surfaceWidthHeight / bitmapWidthHeight,
                    surfaceWidthHeight / bitmapWidthHeight,
                    -1F,
                    1F,
                    3F,
                    7F
                )
            }
        } else {
            if (bitmapWidthHeight > surfaceWidthHeight) {
                Matrix.orthoM(
                    mProjMatrix,
                    0,
                    -1F,
                    1F,
                    -1 / surfaceWidthHeight * bitmapWidthHeight,
                    1 / surfaceWidthHeight * bitmapWidthHeight,
                    3F,
                    7F
                )

            } else {
                Matrix.orthoM(
                    mProjMatrix,
                    0,
                    -1F,
                    1F,
                    -bitmapWidthHeight / surfaceWidthHeight,
                    bitmapWidthHeight / surfaceWidthHeight,
                    3F,
                    7F
                )

            }
        }
        Matrix.setLookAtM(mViewMatrix, 0, 0F, 0F, 7F, 0F, 0F, 0F, 0F, 1.0F, 0F)
        Matrix.multiplyMM(mVpMatrix, 0, mProjMatrix, 0, mViewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (mBitmap != null && bitmapHeight > 0 && surfaceHeight > 0) {
            GLES20.glClearColor(1.0F, 1.0F, 0.8F, 1.0F)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(mProgram)
            GLES20.glUniformMatrix4fv(vMatrixHandle, 1, false, mVpMatrix, 0)
            GLES20.glEnableVertexAttribArray(vPositionHandle)
            GLES20.glEnableVertexAttribArray(vCoordinateHandle)
            GLES20.glVertexAttribPointer(
                vPositionHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                vCoordinateHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                textureBuffer
            )
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(vTexturePointer, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glDisableVertexAttribArray(vCoordinateHandle)
            GLES20.glDisableVertexAttribArray(vPositionHandle)
        }
    }

    private var textureId = -1
    private var mProjMatrix = FloatArray(16)
    private var mViewMatrix = FloatArray(16)
    private var mVpMatrix = FloatArray(16)
    private var mBitmap: Bitmap? = null
    fun renderBitmap(bitmap: Bitmap) {
        //计算转换矩阵
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height
        mBitmap = bitmap
        calculateMatrix()

    }

    private fun createBitmapTexture(bitmap: Bitmap): Int {
        val texture = IntArray(1)
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return texture[0]
    }
}