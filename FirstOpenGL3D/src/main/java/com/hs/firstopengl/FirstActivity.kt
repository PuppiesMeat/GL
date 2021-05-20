package com.hs.firstopengl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FirstActivity : AppCompatActivity() {
    lateinit var glSurfaceView: LifeGLSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_first)
        glSurfaceView = LifeGLSurfaceView(this)
        glSurfaceView.bindLifecycle(this)
        glSurfaceView.setRenderer(FirstRender(this))
        setContentView(glSurfaceView)
    }

}