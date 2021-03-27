package com.powerapps.designdiff.utils.screenshots

import android.graphics.Bitmap

interface ScreenshotCallback {
    fun onScreenshotTaken(screenshot: Bitmap)
    fun onTakeScreenshotError()
}
