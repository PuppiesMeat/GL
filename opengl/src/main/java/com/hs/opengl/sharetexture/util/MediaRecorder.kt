package com.hs.opengl.sharetexture.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGLContext
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.hs.opengl.sharetexture.egl.EGLEnv
import java.util.concurrent.Semaphore


/**
 *
 * Date: 2022/5/13
 * Time: 01:54
 * @author shun.he
 */
class MediaRecorder(width: Int, height: Int, var context: Context, var eglContext: EGLContext) {

    private var mWidth = width
    private var mHeight = height
    private var savePath: String? = null
    private var frameRate = 25
    private var mMediaCodec: MediaCodec? = null
    private var mInputSurface: Surface? = null
    private var mSemaphore = Semaphore(1)
    private var mEglHandler: Handler? = null
    private var mEglEnv: EGLEnv? = null
    private var mMuxer: MediaMuxer? = null
    private var track = 0
    private var mSpeed: Float = 1.0f
    private var mLastPresentationTimeUs = -1L

    fun start(speed: Float) {
        mSpeed = speed
        mEglHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            mEglEnv?.release()
        }

        val format =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, mWidth, mHeight).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)

            }
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mInputSurface = createInputSurface()
            start()
        }
        savePath = "/sdcard/rokid/${System.currentTimeMillis()}-recorder.mp4"
        mMuxer = MediaMuxer(
            savePath
                ?: "${Environment.getExternalStorageState()}/${System.currentTimeMillis()}.mp4",
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        val eglThread = HandlerThread("recorder")
        eglThread.start()
        mEglHandler = Handler(eglThread.looper)
        mEglHandler?.post {
            mEglEnv = EGLEnv(context, eglContext, mInputSurface!!, mWidth, mHeight)
        }

    }


    fun inputFrame(textureId: Int, timestamp: Long) {
        mSemaphore.acquire()
        mEglHandler?.post {
            mMediaCodec?.let { codec ->
                mEglEnv?.draw(textureId, timestamp)
                codecFrame(false)
            }
        }
        mSemaphore.release()
    }

    private fun codecFrame(endOfStream: Boolean) {
        val bufferInfo = MediaCodec.BufferInfo()
        val index = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, 10_000)
        if (endOfStream) mMediaCodec!!.signalEndOfInputStream()
        when (index) {
            MediaCodec.INFO_TRY_AGAIN_LATER -> {
                if (endOfStream) return
            }
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                mMuxer?.apply {
                    track = addTrack(mMediaCodec!!.outputFormat)
                    start()
                }
            }
            else -> {
                bufferInfo.presentationTimeUs =
                    (bufferInfo.presentationTimeUs / mSpeed).toLong()
                if (bufferInfo.presentationTimeUs <= mLastPresentationTimeUs) {
                    bufferInfo.presentationTimeUs =
                        (mLastPresentationTimeUs + 1_000_000 / 25 / mSpeed).toLong()
                }
                mLastPresentationTimeUs = bufferInfo.presentationTimeUs
                val outputBuffer = mMediaCodec!!.getOutputBuffer(index)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    outputBuffer!!.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    mMuxer?.writeSampleData(track, outputBuffer, bufferInfo)
                }
                mMediaCodec?.releaseOutputBuffer(index, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    return
                }
            }

        }
    }

    fun stop() {
        mEglHandler?.post {
            mSemaphore.acquire()
            mMediaCodec?.let {
                codecFrame(true)
                it.stop()
                it.release()
                mMediaCodec = null
            }
            mMuxer?.let {
                it.stop()
                it.release()
                mMuxer = null
            }
            mSemaphore.release()
        }

    }

    fun setSpeed(speed: Float) {
        mSpeed = speed
    }
}