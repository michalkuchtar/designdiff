package com.powerapps.designdiff.utils.screenshots

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

class ScreenshotMaker(private val listener: ScreenshotCallback, private val screenWidth: Int, private val screenHeight: Int) : ImageReader.OnImageAvailableListener {

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    private var imageReader: ImageReader? = null
    private var screenShotTaken = false

    fun takeScreenshot(context: Context, resultCode: Int, data: Intent) {
        screenShotTaken = false
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
        val mediaProjectionManager = context
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
        if (mediaProjection == null) {
            listener.onTakeScreenshotError()
            return
        }

        try {
            virtualDisplay = mediaProjection!!.createVirtualDisplay("DesignDiff Ruler Mode",
                    screenWidth, screenHeight, 50,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader!!.surface, null, null)
            imageReader!!.setOnImageAvailableListener(this@ScreenshotMaker, null)
        } catch (e: Exception) {
            listener.onTakeScreenshotError()
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        if (screenShotTaken) {
            return
        }

        val image: Image? = reader.acquireLatestImage()
        if (image == null) {
            listener.onTakeScreenshotError()
            return
        }

        val planes = image.planes
        val buffer = planes[0].buffer.rewind()
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * screenWidth

        val bitmap = Bitmap.createBitmap(screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        tearDown()
        image.close()

        screenShotTaken = true
        if (bitmap.width != screenWidth || bitmap.height != screenHeight) {
            try {
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                listener.onScreenshotTaken(croppedBitmap)

                bitmap.recycle()
            } catch (e: Exception) {
                listener.onScreenshotTaken(bitmap)
            }
        } else {
            listener.onScreenshotTaken(bitmap)
        }
    }

    private fun tearDown() {
        virtualDisplay!!.release()
        mediaProjection?.stop()
        mediaProjection = null
        imageReader = null
    }
}
