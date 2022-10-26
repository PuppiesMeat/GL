package com.hs.opengl.triangle

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.hs.opengl.triangle.square.BitmapRender
import com.hs.opengl.triangle.square.Oval
import com.hs.opengl.triangle.square.OvalRender
import com.hs.opengl.triangle.square.SquareRender
import com.hs.opengl.triangle.triangle.TriangleColorfulRender

/**
 *
 * Date: 2022/8/9
 * Time: 11:30
 * @author shun.he
 */
class HSGLSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

//    private val glColorfulRender = TriangleColorfulRender()
//    private val glColorfulRender = SquareRender()
    private val glColorfulRender = BitmapRender()
//    private val glColorfulRender = Oval()
//    private val glColorfulRender = TriangleNormalRender()
//    private val glColorfulRender = TriangleMatrixRender()


    init {
        setEGLContextClientVersion(2)
        setRenderer(glColorfulRender)
        renderMode = RENDERMODE_WHEN_DIRTY
        glColorfulRender.renderBitmap(BitmapFactory.decodeResource(resources, R.raw.test))
    }
}