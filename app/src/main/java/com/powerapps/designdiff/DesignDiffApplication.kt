package com.powerapps.designdiff

import android.app.Application
import android.content.Intent

class DesignDiffApplication : Application() {
    var isMainServiceRunning = false

    var screenshotResultCode: Int? = null
    var screenshotDataIntent: Intent? = null
}