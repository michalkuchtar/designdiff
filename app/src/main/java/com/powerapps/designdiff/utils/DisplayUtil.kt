package com.powerapps.designdiff.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.view.Display

class DisplayUtil {

    companion object {
        fun getDisplayWidth(context: Context, display: Display): Int {
            val orientation = context.resources.configuration.orientation
            val size = Point()

            display.getRealSize(size)

            val x = size.x
            val y = size.y

            return if (orientation == Configuration.ORIENTATION_PORTRAIT) x else y
        }

        fun getDisplayHeight(context: Context, display: Display): Int {
            val orientation = context.resources.configuration.orientation
            val size = Point()

            display.getRealSize(size)

            val x = size.x
            val y = size.y

            return if (orientation == Configuration.ORIENTATION_PORTRAIT) y else x
        }
    }
}