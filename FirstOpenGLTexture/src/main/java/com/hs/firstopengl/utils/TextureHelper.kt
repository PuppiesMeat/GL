package com.hs.firstopengl.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.util.Log

object TextureHelper {
    val TAG = TextureHelper::class.simpleName

    public fun loadTexture(context: Context, resourceId: Int): Int {
        val textureObjectId = IntArray(1)
        glGenTextures(1, textureObjectId, 0)
        if (0 == textureObjectId[0]) {
            Log.w(TAG, "create openGl texture failed")
            return 0
        }

        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
        if (null == bitmap) {
            Log.e(TAG, "decode bitmap failed $resourceId")
            glDeleteTextures(1, textureObjectId, 0)
            return 0;
        }
        /**
         * 第一个参数告诉OpenGL这应该被当成一个2D纹理对待，
         * 第二个参数告诉OpenGL要绑定到哪个纹理对象的ID
         */
        glBindTexture(GL_TEXTURE_2D, textureObjectId[0])
        return textureObjectId[0]
    }


}