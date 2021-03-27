package com.powerapps.designdiff

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), PERMISSION_REQUEST_CODE)
        } else {
            startMainServiceAndFinish()
        }
    }

    private fun startMainServiceAndFinish() {
        startService(Intent(this, MainService::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startMainServiceAndFinish()
            } else {
                Toast.makeText(this, "No overdraw permission granted :(", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 3041
    }
}