package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.hs.opengl.sharetexture.R
import com.hs.opengl.sharetexture.egl.EGLEnv

/**
 *
 * Date: 2022/7/27
 * Time: 16:55
 * @author shun.he
 */
class ReadPixelFilter(var context: Context, private var eglContext: EGLContext) :
    AbsFilter(context, R.raw.base_vert, R.raw.base_frag) {
    private var isStart = false
    private var mEglHandler: Handler? = null
    private var mEglEnv: EGLEnv? = null
    private var mImageReader: ImageReader? = null
    private var eglThread: HandlerThread? = null
    private var imageReaderThread: HandlerThread? = null
    private var imageReadHandler: Handler? = null
    private var mDataListener: ReadPixelDataListener? = null
    fun startReadPixels(readPixelDataListener: ReadPixelDataListener) {
        mDataListener = readPixelDataListener
        isStart = true
        createEglEnv()
    }

    private fun createEglEnv() {
        mEglHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            eglThread?.quitSafely()
            eglThread?.join()
            imageReadHandler?.removeCallbacksAndMessages(null)
            imageReadHandler?.looper?.quitSafely()
            imageReaderThread?.quitSafely()
            imageReaderThread?.join()
            mEglEnv?.release()
            mImageReader?.close()
        }
        eglThread = HandlerThread("recorder")
        eglThread!!.start()
        mEglHandler = Handler(eglThread!!.looper)
        imageReaderThread = HandlerThread("image-reader")
        imageReaderThread?.start()
        imageReadHandler = Handler(imageReaderThread!!.looper)

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2)
        mImageReader?.setOnImageAvailableListener(object : ImageReader.OnImageAvailableListener {
            override fun onImageAvailable(reader: ImageReader?) {
                Log.e("onImageAvailable", "===============")
                reader?.acquireNextImage()?.let {
                    mDataListener?.onNv21Data(imageRgba8888ToNv21(it, it.width, it.height), it.width, it.height)
                    it.close()
                }
            }

        }, imageReadHandler)
        mEglHandler?.post {
            mEglEnv = EGLEnv(context, eglContext, mImageReader!!.surface, mWidth, mHeight)
        }

    }

    override fun onDraw(texName: Int, timestamp: Long): Int {
        if (!isStart) return super.onDraw(texName, timestamp)
        val textureId = super.onDraw(texName, timestamp)
        internalDraw(textureId, timestamp)
        return textureId
    }

    private fun internalDraw(textureId: Int, timestamp: Long) {
        mEglHandler?.post {
            mEglEnv?.draw(textureId, timestamp)
        }
    }

    override fun release() {
        super.release()
        isStart = false
        mEglHandler?.post {
            mEglHandler?.looper?.quitSafely()
            mEglHandler = null
            eglThread?.quitSafely()
            eglThread?.join(2000)
            imageReadHandler?.looper?.quitSafely()
            imageReadHandler = null
            imageReaderThread?.quitSafely()
            imageReaderThread?.join(2000)
            imageReaderThread = null
            mEglEnv?.release()
            mImageReader?.close()
            mImageReader = null
            mDataListener = null
        }

    }


    companion object {
        /**
         * Image中的rgba8888转为nv21
         * @param image ImageReader 中的image对象
         * @param width 录制时指定的宽
         * @param height 录制时指定的高
         */
        fun imageRgba8888ToNv21(image: Image, width: Int, height: Int): ByteArray {
            //获取基本参数
            val mWidth: Int = image.width                            //image中图像数据得到的宽
            val mHeight: Int = image.height                          //image中图像数据得到的高
            val planes = image.planes                                //image图像数据对象
            val buffer = planes[0].getBuffer()
            val pixelStride = planes[0].getPixelStride()            //像素步幅
            val rowStride = planes[0].getRowStride()                    //行距
            val rowPadding =
                rowStride - pixelStride * mWidth         //行填充数据（得到的数据宽度大于指定的width,多出来的数据就是填充数据）

            /*
              以480*640尺寸为例
              Image对象获取到的RGBA_8888数据格式分析如下
              1.getPixelStride方法获取相邻像素间距 = 4
              2.getRowStride方法获取行跨度 = 2048
              3.2048-4*480 = 128，说明每行多出128个byte，这128个byte表示行填充的值，里面的数据为无效数据。
              4.转换：需要将每行的最后128个数据忽略掉，再进行转换
             */

            //得到bytes类型的rgba数据
            val len: Int = buffer.limit() - buffer.position()
            val rgba = ByteArray(len)
            buffer.get(rgba)

            var yIndex = 0
            var uvIndex = width * height
            var argbIndex = 0
            //nv21数据初始化
            val nv21 = ByteArray(width * height * 3 / 2)

            for (j in 0 until height) {
                for (i in 0 until width) {
                    var r: Int = rgba.get(argbIndex++).toInt()
                    var g: Int = rgba.get(argbIndex++).toInt()
                    var b: Int = rgba.get(argbIndex++).toInt()
                    argbIndex++ //跳过a
                    r = r and 0x000000FF
                    g = g and 0x000000FF
                    b = b and 0x000000FF
                    val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                    nv21[yIndex++] = (if (y > 0xFF) 0xFF else if (y < 0) 0 else y).toByte()
                    if (j and 1 == 0 && argbIndex shr 2 and 1 == 0 && uvIndex < nv21.size - 2) {
                        val u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                        val v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                        nv21[uvIndex++] = (if (v > 0xFF) 0xFF else if (v < 0) 0 else v).toByte()
                        nv21[uvIndex++] = (if (u > 0xFF) 0xFF else if (u < 0) 0 else u).toByte()
                    }
                }
                argbIndex += rowPadding    //跳过rowPadding长度的填充数据
            }
            return nv21;
        }
    }

}

interface ReadPixelDataListener {
    fun onNv21Data(nv21Data: ByteArray, width: Int, height: Int)
}