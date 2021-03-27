package com.powerapps.designdiff

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class FileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        startActivityForResult(Intent.createChooser(intent,
                "Complete action using"), IMAGE_PICKET_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IMAGE_PICKET_REQUEST_CODE && resultCode == RESULT_OK) {
            sendBroadcast(MainService.getFileReceiverIntent(data?.dataString))
        }

        finish()
    }

    companion object {
        private const val IMAGE_PICKET_REQUEST_CODE = 2054
    }
}