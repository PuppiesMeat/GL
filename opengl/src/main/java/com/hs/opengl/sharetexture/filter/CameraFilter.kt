package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.opengl.GLES20
import com.hs.opengl.sharetexture.R

/**
 *
 * Date: 2022/5/13
 * Time: 00:29
 * @author shun.he
 */
class CameraFilter(context: Context) :
    AbsFboFilter(context, R.raw.camera_vert, R.raw.camera_frag) {

    init {
        vMatrix = GLES20.glGetUniformLocation(program, VMATRIX_NAME)

    }

    override fun beforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mMtx, 0)
    }
}