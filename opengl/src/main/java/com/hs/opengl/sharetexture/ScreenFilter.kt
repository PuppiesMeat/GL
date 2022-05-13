//package com.hs.opengl.sharetexture
//
//import android.content.Context
//import android.opengl.GLES20
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//
///**
// *
// * Date: 2022/5/11
// * Time: 23:40
// * @author shun.he
// */
//class ScreenFilter(context: Context) {
//    private var mWidth = 0
//    private var mHeight = 0
//    private var mMtx: FloatArray? = null
//    private var TEXTURE = floatArrayOf(
//        0.0f, 0.0f,
//        1.0f, 0.0f,
//        0.0f, 1.0f,
//        1.0f, 1.0f
//    )
//    private var textureBuffer: FloatBuffer? = null
//
//    val VERTEX = floatArrayOf(
//        -1.0f, -1.0f,
//        1.0f, -1.0f,
//        -1.0f, 1.0f,
//        1.0f, 1.0f
//    )
//    private var vertexBuffer: FloatBuffer//顶点坐标缓冲区
//    private var vPosition: Int = 0
//    private var VPOSITION_NAME = "vPosition"
//    private var program: Int = 0
//    private var vCoord = 0
//    private var VCOORD_NAME = "vCoord"
//    private var vTexture = 0
//    private var VTEXTURE_NAME = "vTexture"
//    private var vMatrix = 0
//    private var VMATRIX_NAME = "vMatrix"
//
//    init {
//        vertexBuffer = ByteBuffer.allocateDirect(VERTEX.size * 4).order(ByteOrder.nativeOrder())
//            .asFloatBuffer().also {
//                it.clear()
//                it.put(VERTEX)
//            }
//        textureBuffer = ByteBuffer.allocateDirect(TEXTURE.size * 4).order(ByteOrder.nativeOrder())
//            .asFloatBuffer().also {
//                it.clear()
//                it.put(TEXTURE)
//            }
//
//        //加载顶点着色器程序
//        val vertexShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_vert)
//        val fragShader = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag)
//        program = OpenGLUtils.loadProgram(vertexShader, fragShader)
//        vPosition = GLES20.glGetAttribLocation(program, VPOSITION_NAME)
//        vCoord = GLES20.glGetAttribLocation(program, VCOORD_NAME)
//        vTexture = GLES20.glGetUniformLocation(program, VTEXTURE_NAME)
//        vMatrix = GLES20.glGetUniformLocation(program, VMATRIX_NAME)
//
//    }
//
//    fun onDraw(texName: Int) {
//        //渲染范围
//        GLES20.glViewport(0, 0, mWidth, mHeight)
//        GLES20.glUseProgram(program)
//        vertexBuffer.position(0)
//        GLES20.glVertexAttribPointer(
//            vPosition,
//            2
//            /**每个顶点坐标数*/
//            ,
//            GLES20.GL_FLOAT,
//            false
//            /**标准化/非标准化*/
//            ,
//            0, vertexBuffer
//        )
//        GLES20.glEnableVertexAttribArray(vPosition)
//        textureBuffer!!.position(0)
//        GLES20.glVertexAttribPointer(
//            vCoord, 2, GLES20.GL_FLOAT,
//            false, 0, textureBuffer
//        )
//        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
//        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
//        GLES20.glEnableVertexAttribArray(vCoord)
//
////        形状就确定了
//
////         32  数据
////gpu    获取读取    //使用第几个图层
//
////        形状就确定了
//
////         32  数据
////gpu    获取读取    //使用第几个图层
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//
////生成一个采样
//
////生成一个采样
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texName)
//        GLES20.glUniform1i(vTexture, 0)
//        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMtx, 0);
////通知 渲染画面 画画，  渲染 到 屏幕    物理设备  屏幕  屏幕
//        //通知 渲染画面 画画，  渲染 到 屏幕    物理设备  屏幕  屏幕
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//    }
//
//    fun setSize(width: Int, height: Int) {
//        mWidth = width
//        mHeight = height
//    }
//
//    fun setTransformMatrix(mtx: FloatArray) {
//        mMtx = mtx
//    }
//
//
//
//
//}