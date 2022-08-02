package com.hs.opengl.sharetexture.inter

import android.graphics.SurfaceTexture
import java.nio.ByteBuffer

/**
 *
 * Date: 2022/7/25
 * Time: 15:54
 * @author shun.he
 * 选择其中一种方式渲染即可
 */
interface VideoSink {

    fun onByteBufferFrame(
        mByteBuffer: ByteBuffer,
        format: Int,
        width: Int,
        height: Int,
        orientation: Int,
        timestamp: Long
    )

    fun onByteArrayFrame(
        mBufferArray: ByteArray,
        format: Int,
        width: Int,
        height: Int,
        orientation: Int,
        timestamp: Long
    )

    fun bindSurfaceTexture(surfaceTexture: SurfaceTexture)

    fun bindTextureId(textureId: Int)
}