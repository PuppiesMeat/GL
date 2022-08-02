package com.hs.opengl.sharetexture

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * Date: 2022/7/28
 * Time: 16:03
 * @author shun.he
 */
class H264Decoder {
    private lateinit var codec: MediaCodec
    private var decodeThread: HandlerThread? = null
    private var decodeHandler: Handler? = null
    private var bis: BufferedInputStream? = null
    private var filePath: String? = null
    private var fileThread: HandlerThread? = null
    private var fileHandler: Handler? = null
    private var bufferQueue = LinkedBlockingQueue<H264Data>(10)
    private var isStart = false

    fun create() {
        decodeThread = HandlerThread("decoder")
        decodeThread!!.start()
        decodeHandler = Handler(decodeThread!!.looper)
        fileThread = HandlerThread("decoder")
        fileThread!!.start()
        fileHandler = Handler(fileThread!!.looper)
    }

    private fun configCodec(surface: Surface) {
        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 2264, 1080)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        codec.configure(format, surface, null, 0)
        codec.start()
    }

    private fun startStream() {
        bis = BufferedInputStream(FileInputStream(filePath!!))
        fileHandler?.post {
            readFile()
        }

    }

    private fun internalFindHead(data: ByteArray, startIndex: Int, info: IntArray): Int {
        for (i in (startIndex..(data.size - 4))) {
            if (data[i].toInt() == 0x00 && data[i + 1].toInt() == 0x00 && data[i + 2].toInt() == 0x00 && data[i + 3].toInt() == 0x01) {
                info[0] = i
                info[1] = i + 4
                return i
            } else if (data[i].toInt() == 0x00 && data[i + 1].toInt() == 0x00 && data[i + 2].toInt() == 0x01) {
                info[0] = i
                info[1] = i + 3
                return i
            }
        }
        return -1
    }

    var isFileEnd = false
    private fun readFile() {
        var startIndex = 0
        var nextFrameStart = 0
        val defaultSize = 1024 * 1024 * 5
        val info = IntArray(2)
        var data = ByteArray(defaultSize)

        var totalReadSize = 0L
        var len = bis!!.read(data, 0, defaultSize)
        totalReadSize += len
        while (len > 0 && isStart) {
            internalFindHead(data, startIndex, info)
            var currentTypeIndex = info[1]
            var pos =
                internalFindHead(data, startIndex + 2, info)
            if (pos < 0 && bis!!.available() > 0) {
                val cacheBuf = ByteArray(data.size - startIndex + defaultSize)
                System.arraycopy(data, startIndex, cacheBuf, 0, data.size - startIndex)
                len = bis!!.read(cacheBuf, data.size - startIndex, defaultSize)
                data = cacheBuf
                startIndex = 0
                totalReadSize += len
                continue
            } else if (pos < 0 && bis!!.available() <= 0) {
                break
            }
            nextFrameStart = pos
            val naluType = data[currentTypeIndex].toInt()
            val h264Buf = ByteArray(nextFrameStart - startIndex)
            System.arraycopy(data, startIndex, h264Buf, 0, h264Buf.size)
            bufferQueue.put(H264Data(naluType, h264Buf, currentTypeIndex - startIndex))

            Log.i(
                "Decoder",
                "startIndex: $startIndex, info: ${info[1].toInt()}, type:$naluType, ${frames.incrementAndGet()}"
            )
            startIndex = nextFrameStart

        }
        isFileEnd = true
        bis?.close()
        bis = null
        Log.i(
            "Decoder",
            "read file done!================== size: $totalReadSize"
        )
    }
    private var frames = AtomicInteger(0)


    private var mListener: H264SeiDataListener? = null
    fun start(path: String, outputSurface: Surface, listener: H264SeiDataListener) {
        mListener = listener
        isStart = true
        filePath = path
        configCodec(outputSurface)
        startDecode()
        startStream()

    }

    private fun startDecode() {
        decodeHandler?.post {
            internalDecode()
        }
    }

    private var mCount = 0L
    private var TIME_INTERNAL = 5L
    private fun internalDecode() {
        Log.i("decoder", "decoder queue start ----------------")
        var startTime = System.currentTimeMillis()
        val perFrameTime = 1000 / 25
        while (isStart) {
            val poll = bufferQueue.peek()
            if (null == poll && isFileEnd) break
            poll?.let {
                Log.i("decoder", "decoder queue ${it.naluType}, ${it.data.size}, ${it.dataIndex}")
                if (it.naluType == 0x06) {
                    bufferQueue.remove()
                    var size = 0
                    var startSizeIndex = 2
                    while (true) {
                        val sizeTmp = it.data[it.dataIndex + startSizeIndex++].toInt() and 0xFF
                        size += sizeTmp
                        if (sizeTmp != 0xFF) break
                    }
                    size -= 16
                    if (size <= 0) return@let
                    Log.i("decoder", "decoder queue seiSize $size")
                    val seiData = ByteArray(size)
                    System.arraycopy(it.data, it.dataIndex + 19, seiData, 0, size)
                    Log.i("decoder", "decoder queue seiData ${String(seiData)}, frames:  ${frames.decrementAndGet()}")
                    mListener?.onSeiData(seiData)
                } else {
                    var index = codec.dequeueInputBuffer(-1)
                    if (!isStart) return@let
                    if (index >= 0) {
                        val inputBuffer = codec.getInputBuffer(index)
                        inputBuffer!!.clear()
                        inputBuffer!!.put(it.data)
                        codec.queueInputBuffer(
                            index,
                            0,
                            it.data.size,
                            mCount * 1000000 / perFrameTime,
                            0
                        )
                        mCount++
                        bufferQueue.remove()
                        Log.i("decoder", "decoder queue h264 frames:  ${frames.decrementAndGet()}")

                    }
                    if (!isStart) return@let
                    val bufferInfo = MediaCodec.BufferInfo()
                    var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 20000)
                    if (!isStart) return@let
                    while (outputIndex > 0 && isStart) {
                        codec.releaseOutputBuffer(outputIndex, true)
                        var l = perFrameTime + startTime - System.currentTimeMillis()
                        Log.i("FileDecoder", "decoder $l")
                        if (l > 0)
                            Thread.sleep(l)
                        startTime = System.currentTimeMillis()
                        outputIndex = codec.dequeueOutputBuffer(bufferInfo, 20000)
                    }
                }

            }

        }
        Log.i("decoder", "decoder queue end!!!! ----------------")

    }

    fun stop() {
        isStart = false
        codec.stop()
        bufferQueue.clear()
    }

    fun dispose() {
        stop()
        codec.release()
        decodeHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            decodeHandler = null
        }
        decodeThread?.let {
            it.quitSafely()
            it.join(2000)
            decodeThread = null
        }

        fileHandler?.let {
            it.removeCallbacksAndMessages(null)
            it.looper.quitSafely()
            fileHandler = null
        }
        fileThread?.let {
            it.quitSafely()
            it.join(2000)
            fileThread = null
        }
    }

}

class H264Data(var naluType: Int, var data: ByteArray, var dataIndex: Int)

interface H264SeiDataListener {
    fun onSeiData(data: ByteArray)
}