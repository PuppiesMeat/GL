package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.GLES20
import com.hs.opengl.sharetexture.R

/**
 *
 * Date: 2022/5/13
 * Time: 14:11
 * @author shun.he
 */
class BeautyFilter(context: Context): AbsFboFilter(context, R.raw.base_vert, R.raw.beauty_frag) {
    private var width = 0
    private var height = 0
    init {
        width = GLES20.glGetUniformLocation(program, "width")
        height = GLES20.glGetUniformLocation(program, "height")
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES20.glUniform1i(width, mWidth)
        GLES20.glUniform1i(height, mHeight)
    }
}