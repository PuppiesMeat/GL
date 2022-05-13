package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.GLES20

/**
 *
 * Date: 2022/5/13
 * Time: 01:10
 * @author shun.he
 */
abstract class AbsFboFilter(context: Context, vertexShaderId: Int, fragmentShaderId: Int) :
    AbsFilter(context, vertexShaderId, fragmentShaderId) {

    private var frameBuffers: IntArray? = null//离屏渲染gpu-buffer索引
    private var textureBuffers: IntArray? = null//纹理索引

    override fun setSize(width: Int, height: Int) {
        super.setSize(width, height)
        //创建GPU buffer
        frameBuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, frameBuffers, 0)
        //创建纹理
        textureBuffers = IntArray(1)
        GLES20.glGenTextures(1, textureBuffers, 0)
        //配置纹理
        (0 until textureBuffers!!.size).forEach {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBuffers!![it])
            //放大过滤
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            //缩小过滤
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            //通知GPU
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureBuffers!![0])
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            textureBuffers!![0],
            0
        )
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    override fun onDraw(texName: Int, timestamp:Long): Int {
        //渲染数据到fbo
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers!![0])
        super.onDraw(texName, timestamp)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return textureBuffers!![0]
    }
}