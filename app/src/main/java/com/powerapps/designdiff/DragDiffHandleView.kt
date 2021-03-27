package com.powerapps.designdiff

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.powerapps.designdiff.utils.UnitsConverter

@SuppressLint("ViewConstructor")
class DragDiffHandleView(context: Context, private val callback: (separatorPosition: Int) -> Unit) : FrameLayout(context) {

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val halfSeparatorWidthPx by lazy { UnitsConverter.dpToPixels(context, 1f) }

    init {
        LayoutInflater.from(context).inflate(R.layout.diff_drag_button, this, true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val xOffset = width / 2
                val x = event.rawX.toInt() - xOffset
                val y = this.y.toInt()

                val params = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                        WindowManager.LayoutParams(
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                x, y,
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                PixelFormat.TRANSLUCENT)
                    else -> WindowManager.LayoutParams(
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,
                            x, y,
                            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            PixelFormat.TRANSLUCENT)
                }

                params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                windowManager.updateViewLayout(this, params)

                val rightMax = (getDisplayWidth() - 2 * xOffset).toFloat()

                when {
                    x < 0 -> translationX = x.toFloat()
                    x > rightMax -> translationX = x - rightMax
                    translationX != 0f -> translationX = 0f
                }

                val updatedSeparatorPosition = x + xOffset - halfSeparatorWidthPx

                callback(updatedSeparatorPosition)
            }
        }

        return true
    }

    private fun getDisplayWidth(): Int {
        val orientation = context.resources.configuration.orientation
        val size = Point()
        windowManager.defaultDisplay.getRealSize(size)

        val x = size.x
        val y = size.y

        return if (orientation == Configuration.ORIENTATION_PORTRAIT) x else y
    }
}