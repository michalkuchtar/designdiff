package com.powerapps.designdiff.utils

import android.content.Context

class UnitsConverter {
    companion object {

        @JvmStatic
        fun dpToPixels(context: Context, dpValue: Float): Int {
            return dpToPixels(getScreenDensity(context), dpValue)
        }

        private fun dpToPixels(screenDensity: Float, dpValue: Float): Int {
            return (dpValue * screenDensity + 0.5f).toInt()
        }

        @JvmStatic
        fun pixelsToDp(context: Context, pixelsValue: Float): Float {
            return pixelsValue / getScreenDensity(context)
        }

        private fun getScreenDensity(context: Context): Float {
            return context.resources.displayMetrics.density
        }
    }
}
