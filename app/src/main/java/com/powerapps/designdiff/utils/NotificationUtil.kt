package com.powerapps.designdiff.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.powerapps.designdiff.R

object NotificationUtil {

    private const val CHANNEL_ID = "com.powerapps.designdiff.main"

    fun getChannelId(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val notificationChannel = NotificationChannel(CHANNEL_ID,
                        context.getString(R.string.app_name),
                        NotificationManager.IMPORTANCE_LOW)

                notificationManager.createNotificationChannel(notificationChannel)
            }

            return CHANNEL_ID
        }

        return ""
    }
}