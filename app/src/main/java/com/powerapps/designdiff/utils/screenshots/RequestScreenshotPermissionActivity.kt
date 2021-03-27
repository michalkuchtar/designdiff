package com.powerapps.designdiff.utils.screenshots

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.powerapps.designdiff.DesignDiffApplication
import com.powerapps.designdiff.MainService

class RequestScreenshotPermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_TAKE_SCREENSHOT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            (application as DesignDiffApplication).screenshotResultCode = resultCode
            (application as DesignDiffApplication).screenshotDataIntent = data
        }

        sendBroadcast(MainService.getTakeScreenshotReceiverIntent())
        finish()
    }

    companion object {
        private const val REQUEST_CODE_TAKE_SCREENSHOT = 42
    }
}