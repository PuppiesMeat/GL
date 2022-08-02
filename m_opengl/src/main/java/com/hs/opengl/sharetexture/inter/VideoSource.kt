package com.hs.opengl.sharetexture.inter

import android.content.Context
import android.graphics.ImageFormat
import android.view.Surface

/**
 *
 * Date: 2022/7/25
 * Time: 15:47
 * @author shun.he
 */
interface VideoSource {

    fun create(context: Context)

    fun start(vsParams: VSParams, videoSink: VideoSink?, bindSurfaces:List<Surface>?)

    fun stop()

    fun dispose()
}

class VSParams {
    var width: Int = 1280
    var height: Int = 720
    var fps: Int = 30
    var imageType: @ImageType Int = ImageType.I420
    var facing: @FacingType Int = FacingType.FRONT

    /**
     * 如果需要预览数据，ImageReader最大image缓存数可以外部设置，默认为1
     */
    var maxImages = 1

    /**
     * 如果需要预览数据，可以设定是接收buffer或者byte[]，如果不需要可以不关注
     */
    var useBuffer = true
}

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class FacingType {
    companion object {
        val UNKNOWN = 0
        val FRONT = 1
        val BACK = 2
    }
}

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImageType {
    companion object {
//        const val NV12 = ImageFormat.
        const val NV21 = ImageFormat.NV21
        const val I420 = ImageFormat.YUV_420_888
    }
}