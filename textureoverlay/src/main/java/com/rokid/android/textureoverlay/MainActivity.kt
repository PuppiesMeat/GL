package com.rokid.android.textureoverlay

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.media.MediaRouter
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rokid.android.textureoverlay.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btSecondDisplay.setOnClickListener {
            startSecondDisplay()
        }

        binding.btStart.setOnClickListener {
            startRecord()
        }

        binding.btStop.setOnClickListener {
            stopRecord()
        }

        binding.glView.setEGLContextClientVersion(2)
        binding.glView.setRenderer(OverlayRender(binding.glView))
    }

    private class OverlayRender(private var glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer {

        private var mProgram: Int = 0
        private var mAPositionLocation = 0
        private var bitmapTexture = 0
        override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
            mProgram = OpenGLUtils.loadProgram(
                OpenGLUtils.readRawTextFile(R.raw.base_frag),
                OpenGLUtils.readRawTextFile(R.raw.base_vert)
            )
            mVertexData.put(mVertex)
            mVertexData.position(0)
            mAPositionLocation = GLES20.glGetUniformLocation(mProgram, "a_Position")
            bitmapTexture = createBitmapTexture()
        }

        private fun createBitmapTexture(): Int {
            val bitmap = BitmapFactory.decodeResource(glSurfaceView.context.resources, R.mipmap.earth)
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            //设置纹理过滤参数，GL_TEXTURE_MIN_FILTER代表纹理缩写的情况，GL_LINEAR_MIPMAP_LINEAR代表缩小时使用三线性过滤的方式，至于过滤方式以后再详解
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR
            );
            //GL_TEXTURE_MAG_FILTER代表纹理放大，GL_LINEAR代表双线性过滤
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            );
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return textures[0]
        }

        override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        }

        override fun onDrawFrame(p0: GL10?) {
            GLES20.glClear(GL10.GL_COLOR_BUFFER_BIT)
            drawPikachu()
            drawTuzki()
        }

        private var mVertex = floatArrayOf(
            -1.0F, 1.0F,
            1.0F, 1.0F,
            -1.0F, -1.0F,
            1.0F, -1.0F
        )
        private var POSITION_COMPONENT_COUNT = mVertex.size / 2

        private var mVertexData: FloatBuffer = ByteBuffer.allocateDirect(mVertex.size * 4).order(
            ByteOrder.nativeOrder()).asFloatBuffer()

        private fun drawPikachu() {
            mVertexData.position(0)
            GLES20.glVertexAttribPointer(
                mAPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, 0, mVertexData
            )
            GLES20.glEnableVertexAttribArray(mAPositionLocation)

            // 设置当前活动的纹理单元为纹理单元0
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            // 将纹理ID绑定到当前活动的纹理单元上
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTexture)
            GLES20.glUniform1i(uTextureUnitLocation, 0)
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_FAN,
                0,
                POINT_DATA.size / POSITION_COMPONENT_COUNT
            )
        }

        private fun drawTuzki() {
            mVertexData2.position(0)
            GLES20.glVertexAttribPointer(
                mAPositionLocation, POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT, false, 0, mVertexData2
            )
            GLES20.glEnableVertexAttribArray(mAPositionLocation)

            // 绑定新的纹理ID到已激活的纹理单元上
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureBean2!!.textureId)
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_FAN,
                0,
                POINT_DATA.size / POSITION_COMPONENT_COUNT
            )
        }
    }

    private var surfaceTexture: SurfaceTexture? = null
    private fun startRecord() {
        val createBitmapTexture = createBitmapTexture()
        surfaceTexture = createTexture()
    }




    private fun createTexture(): SurfaceTexture {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        return SurfaceTexture(textures[0])
    }

    private fun stopRecord() {

    }

    private fun startSecondDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var basicOptions = ActivityOptions.makeBasic()
            val mediaRouter = getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
            val routerInfo = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
            if (null != routerInfo) {
                val presentationDisplay = routerInfo.presentationDisplay
                basicOptions.launchDisplayId = presentationDisplay.displayId
                val secondDisplayIntent = Intent(this, SecondActivity::class.java)
                secondDisplayIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(secondDisplayIntent, basicOptions.toBundle())
            }

        }

    }
}