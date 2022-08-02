package com.hs.opengl.sharetexture.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import com.hs.opengl.sharetexture.inter.FacingType
import com.hs.opengl.sharetexture.inter.VSParams
import com.hs.opengl.sharetexture.inter.VideoSink
import com.hs.opengl.sharetexture.inter.VideoSource
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 *
 * Date: 2022/7/25
 * Time: 16:00
 * @author shun.he
 */
class Camera2Source : VideoSource {
    private var mPreviewHandler: Handler? = null
    private var mPreviewThread: HandlerThread? = null
    private var mImageReader: ImageReader? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mCameraStateCallback: CameraDevice.StateCallback? = null
    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var mPreviewRequest: CaptureRequest? = null
    private var mCameraCaptureSessionCallback: CameraCaptureSession.CaptureCallback? = null
    private var mBufferArray: ByteArray? = null
    private var mByteBuffer: ByteBuffer? = null
    private var mCameraId: String? = null
    private var mImageFormat: Int = ImageFormat.NV21
    private var mCameraOrientation = 0
    private var mVideoSink: VideoSink? = null
    private var mBindSurfaces: List<Surface>? = null

    companion object {
        private val cameraOpenCloseLock = Semaphore(1)
        private val TAG = Camera2Source::class.java.simpleName
        private fun readImageIntoBuffer(image: Image, data: ByteArray) {
            val width: Int = image.width
            val height: Int = image.height
            val planes: Array<Image.Plane> = image.planes
            var index = 0
            for (plane in planes.indices) {
                val buffer = planes[plane].buffer
                val rowStride = planes[plane].rowStride
                val pixelStride = planes[plane].pixelStride
                val l1 = if (plane == 0) width else width / 2
                val l2 = if (plane == 0) height else height / 2
                if (pixelStride == 1 && rowStride == l1) {
                    buffer[data, index, l1 * l2]
                    index += l1 * l2
                } else {
                    val row = ByteArray(rowStride)
                    var rowIndex = 0
                    while (rowIndex < l2 - 1) {
                        buffer[row, 0, rowStride]
                        for (i in 0 until l1) {
                            data[index++] = row[i * pixelStride]
                        }
                        ++rowIndex
                    }
                    buffer[row, 0, min(rowStride, buffer.remaining())]
                    rowIndex = 0
                    while (rowIndex < l1) {
                        data[index++] = row[rowIndex * pixelStride]
                        ++rowIndex
                    }
                }
            }
        }

        private fun getNV21Data(image: Image): ByteArray? {
            val format = image.format
            val w = image.width
            val h = image.height
            //        Logger.e("getNV21Data----------->format = " + format + "; width = " + w + "; height = " + h);
            try {
                val i420Size = w * h * 3 shr 1
                val planes = image.planes
                val remaining0 = planes[0].buffer.remaining()
                val remaining1 = planes[1].buffer.remaining()
                val remaining2 = planes[2].buffer.remaining()
                val pixelStride = planes[2].pixelStride
                val rowOffset = planes[2].rowStride
                val nv21: ByteArray = ByteArray(i420Size)
                val yRawSrcBytes: ByteArray = ByteArray(remaining0)
                val uRawSrcBytes: ByteArray = ByteArray(remaining1)
                val vRawSrcBytes: ByteArray = ByteArray(remaining2)
                planes[0].buffer[yRawSrcBytes]
                planes[1].buffer[uRawSrcBytes]
                planes[2].buffer[vRawSrcBytes]
                if (pixelStride == w) {
                    System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffset * h)
                    System.arraycopy(vRawSrcBytes, 0, nv21, rowOffset * h, rowOffset * h / 2 - 1)
                } else {
                    val ySrcBytes: ByteArray = ByteArray(w * h)
                    val vSrcBytes: ByteArray = ByteArray(w * h / 2 - 1)
                    for (row in 0 until h) {
                        System.arraycopy(yRawSrcBytes, rowOffset * row, ySrcBytes, w * row, w)
                        if (row % 2 == 0) {
                            if (row == h - 2) {
                                System.arraycopy(
                                    vRawSrcBytes,
                                    rowOffset * row / 2,
                                    vSrcBytes,
                                    w * row / 2,
                                    w - 1
                                )
                            } else {
                                System.arraycopy(
                                    vRawSrcBytes,
                                    rowOffset * row / 2,
                                    vSrcBytes,
                                    w * row / 2,
                                    w
                                )
                            }
                        }
                    }
                    System.arraycopy(ySrcBytes, 0, nv21, 0, w * h)
                    System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1)
                }
                return nv21
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    private var vsParams: VSParams? = null
    private lateinit var applicationContext: Context

    override fun create(context: Context) {
        applicationContext = context
    }

    @SuppressLint("MissingPermission")
    override fun start(vsParams: VSParams, videoSink: VideoSink?, bindSurfaces: List<Surface>?) {
        if (null == videoSink && bindSurfaces.isNullOrEmpty()) throw RuntimeException("Camera2Source#create must has videoSink or bindSurface!")
        this.vsParams = vsParams
        this.mBindSurfaces = bindSurfaces
        mVideoSink = videoSink
        if (!cameraOpenCloseLock.tryAcquire(
                2000,
                TimeUnit.MILLISECONDS
            )
        ) throw RuntimeException("open camera try lock failed.")
        config()
        val cm = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cm.openCamera(mCameraId!!, mCameraStateCallback!!, mPreviewHandler!!)
    }

    private fun config() {
        mImageFormat = vsParams!!.imageType
        startPreviewThread()
        selectCamera()
        if (null != mVideoSink) {
            mallocBuffer()
            mImageReader =
                ImageReader.newInstance(
                    vsParams!!.width,
                    vsParams!!.height,
                    mImageFormat,
                    vsParams!!.maxImages
                ).apply {
                    setOnImageAvailableListener(
                        { imageReader -> internalImageAvailable(imageReader) },
                        mPreviewHandler
                    )
                }
            mImageReader!!.surface
        }

        mCameraStateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                mCameraDevice = device
                cameraOpenCloseLock.release()
                createPreviewSession()
            }

            override fun onDisconnected(device: CameraDevice) {
                device.close()
                mCameraDevice = null
                cameraOpenCloseLock.release()
            }

            override fun onError(device: CameraDevice, p1: Int) {
                device.close()
                mCameraDevice = null
                cameraOpenCloseLock.release()
            }

        }

        mCameraCaptureSessionCallback = object : CameraCaptureSession.CaptureCallback() {
            //TODO 实现其他逻辑
        }

    }


    private fun createPreviewSession() {
        var surfaces = mutableListOf<Surface>()
        if (null != mVideoSink) {
            surfaces.add(mImageReader!!.surface)
        }
        if (!mBindSurfaces.isNullOrEmpty()) {
            surfaces.addAll(mBindSurfaces!!)
        }
        mPreviewRequestBuilder =
            mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_LOCK, false)
                surfaces.forEach {
                    addTarget(it)
                }
            }

        mCameraDevice!!.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    mCameraCaptureSession = session
                    mPreviewRequest = mPreviewRequestBuilder?.build()
                    mPreviewRequest?.apply {
                        session.setRepeatingRequest(
                            mPreviewRequest!!,
                            mCameraCaptureSessionCallback,
                            mPreviewHandler
                        )
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    session.close()
                }

            },
            mPreviewHandler
        )

    }


    private fun mallocBuffer() {
        var size = 0
        if (mImageFormat == ImageFormat.YUV_420_888) {
            size =
                vsParams!!.width * vsParams!!.height * ImageFormat.getBitsPerPixel(mImageFormat) / 8
        }
        if (vsParams!!.useBuffer) {
            mBufferArray = ByteArray(size)
            mByteBuffer = ByteBuffer.allocateDirect(size)
        } else {
            mBufferArray = ByteArray(size)
        }
    }

    private fun selectCamera() {
        val cm = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraId =
            cm.cameraIdList.filter {
                val cc = cm.getCameraCharacteristics(it)
                mCameraOrientation = cc[CameraCharacteristics.SENSOR_ORIENTATION]!!.toInt()
                cc[CameraCharacteristics.LENS_FACING] == 0
            }[0]
    }

    private fun internalImageAvailable(imageReader: ImageReader?) {
        imageReader ?: return
        var image: Image? = null
        try {
            image = imageReader.acquireLatestImage()
            if (image == null) {
                return
            }
            if (image.format == ImageFormat.YUV_420_888 && image.planes.size == 3) {
                if (imageReader.width == image.width && imageReader.height == image.height) {
                    mBufferArray = getNV21Data(
                        image,
                    )
                    val orientation: Int = getFrameOrientation()
                    mVideoSink?.let {
                        if (vsParams!!.useBuffer) {
                            mByteBuffer!!.rewind()
                            mByteBuffer!!.put(mBufferArray!!, 0, mBufferArray!!.size)
                            it.onByteBufferFrame(
                                mByteBuffer!!,
                                image.format,
                                image.width,
                                image.height,
                                orientation,
                                image.timestamp
                            )
                        } else {
                            it.onByteArrayFrame(
                                mBufferArray!!,
                                image.format,
                                image.width,
                                image.height,
                                orientation,
                                image.timestamp
                            )
                        }
                    }
                    return
                }
                throw IllegalStateException(
                    "ImageReader size " + imageReader.getWidth()
                        .toString() + "x" + imageReader.getHeight()
                        .toString() + " did not match Image size: " + image.width.toString() + "x" + image.height
                )
            }
            Log.e(
                TAG,
                "Unexpected image format: " + image.format + "or planes_size:" + image.planes.size
            )
        } catch (ex1: IllegalStateException) {
            Log.e(TAG, "IllegalStateException :", ex1)
            return
        } catch (ex1: Exception) {
            Log.e(TAG, "Exception: ${ex1.message}")
            return
        } finally {
            image?.close()
        }
    }

    private fun getFrameOrientation(): Int {
        var rotation: Int = this.getDeviceOrientation()
        if (vsParams!!.facing == FacingType.FRONT) {
            rotation = 360 - rotation
        }
        return (mCameraOrientation + rotation) % 360
    }

    private fun getDeviceOrientation(): Int {
        val wm = applicationContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return when (wm.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun startPreviewThread() {
        mPreviewThread = HandlerThread("preview")
        mPreviewThread!!.start()
        mPreviewHandler = Handler(mPreviewThread!!.looper)
    }

    private fun stopPreviewThread() {
        mPreviewThread?.apply {
            quitSafely()
            join()
            mPreviewThread = null
            mPreviewHandler = null
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            mCameraDevice?.close()
            mCameraDevice = null
            mCameraCaptureSession?.close()
            mCameraCaptureSession = null
            mPreviewRequest = null
            mPreviewRequestBuilder = null
            mImageReader?.close()
            mImageReader = null
        } finally {
            cameraOpenCloseLock.release()
        }

    }

    override fun stop() {
        closeCamera()
        stopPreviewThread()
    }

    override fun dispose() {
        stop()
    }


}