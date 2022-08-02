package com.hs.opengl.sharetexture.filter

import android.content.Context
import android.media.*
import android.opengl.EGLContext
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.hs.opengl.sharetexture.HexUtil
import com.hs.opengl.sharetexture.egl.EGLEnv
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.*
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
    private var mEglThread: HandlerThread? = null
    private var mH264FileStream: BufferedOutputStream? = null
    private var mByteArray:ByteArray? = null

    companion object {
        private var count = 0
    }

    private var currentCount = 1

    init {
        currentCount = count++
    }

    private var uuid = "dc45e9bd-e6d9-48b7-962c-d820d923eeef"
    private var uuidByte = HexUtil.asBytes(UUID.fromString(uuid))!!
    private var sb = StringBuffer()
    fun insertSeiData(data: ByteArray) {
        mEglHandler?.post {
            val sei: ByteArray = data
            sb.delete(0, sb.length)
            sb.append("0000010605")
            var len = uuidByte.size + sei.size
            while (len > 0) {
                sb.append(if (len > 0xFF) 0xFF else Integer.toHexString(len))
                len -= 0xff
                if (len == 0) sb.append(0x00)
                if (len < 0) break
            }
            val hex = sb.toString()
            Log.e("Recorder", "hexStr: $hex")
            mH264FileStream?.write(HexUtil.hex2Bytes(hex, null))
            mH264FileStream?.write(uuidByte)
            mH264FileStream?.write(data)
        }

    }

    fun start(speed: Float, filePath: String, isH264: Boolean = false) {
        mSpeed = speed
        mEglHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            mEglEnv?.release()
        }

        val format =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)

            }
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mInputSurface = createInputSurface()
            start()
        }
        savePath = filePath
        if (isH264) {
            mH264FileStream = BufferedOutputStream(FileOutputStream(savePath))
        } else {
            mMuxer = MediaMuxer(
                savePath
                    ?: "${Environment.getExternalStorageState()}/${System.currentTimeMillis()}.mp4",
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
        }

        mEglThread = HandlerThread("recorder")
        mEglThread!!.start()
        mEglHandler = Handler(mEglThread!!.looper)
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
//            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
//                mMuxer?.apply {
//                    track = addTrack(mMediaCodec!!.outputFormat)
//                    start()
//                }
//            }
            else -> {
                if (index < 0) return
                bufferInfo.presentationTimeUs =
                    (bufferInfo.presentationTimeUs / mSpeed).toLong()
                if (bufferInfo.presentationTimeUs <= mLastPresentationTimeUs
                    || bufferInfo.presentationTimeUs - mLastPresentationTimeUs > 1_000_000 / frameRate / mSpeed
                ) {
                    bufferInfo.presentationTimeUs =
                        (mLastPresentationTimeUs + 1_000_000 / frameRate / mSpeed).toLong()
                }
                mLastPresentationTimeUs = bufferInfo.presentationTimeUs
                val outputBuffer = mMediaCodec!!.getOutputBuffer(index)
//                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    bufferInfo.size = 0
//                }
                if (bufferInfo.size != 0) {
                    outputBuffer!!.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    Log.e("Record", "Recorder=============== $currentCount")
                    mMuxer?.writeSampleData(track, outputBuffer, bufferInfo)
                    mH264FileStream?.let {
                        if (null == mByteArray || mByteArray!!.size < bufferInfo.size){
                            mByteArray = ByteArray(bufferInfo.size)
                        }
                        outputBuffer.get(mByteArray!!, 0, bufferInfo.size)
                        mH264FileStream?.write(mByteArray, 0, bufferInfo.size)
                    }
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
            mEglHandler!!.removeCallbacksAndMessages(null)
            mEglHandler!!.looper.quitSafely()
            mEglThread!!.quitSafely()
            mEglThread!!.join(2000)
            mEglHandler = null

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
            mH264FileStream?.let {
                it.flush()
                it.close()
                mH264FileStream = null
            }
            mSemaphore.release()
        }

    }

    fun setSpeed(speed: Float) {
        Log.i("MediaRecorder: ", "setSpeed: $speed")
        mSpeed = speed
    }
}