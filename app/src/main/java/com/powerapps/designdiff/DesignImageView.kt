package com.powerapps.designdiff

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class DesignImageView(context: Context, attributeSet: AttributeSet?) : AppCompatImageView(context, attributeSet) {
    var visibleX: Int? = null
        set(value) {
            field = value
            invalidate()
        }

    private var lastVisibleX: Int? = 0

    private val imagePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskImagePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val rectangleMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val clearMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var maskBitmap: Bitmap? = null

    init {
        setWillNotDraw(false)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val imageCanvas = Canvas(imageBitmap)

        super.onDraw(imageCanvas)

        if (maskBitmap == null || visibleX != lastVisibleX) {
            if (visibleX == null) {
                visibleX = width
            }
            lastVisibleX = visibleX

            maskBitmap = createMask(width, height)
        }

        maskBitmap?.let { imageCanvas.drawBitmap(it, 0f, 0f, maskImagePaint) }
        canvas.drawBitmap(imageBitmap, 0f, 0f, imagePaint)
    }

    private fun createMask(width: Int, height: Int) = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8).apply {
        val maskCanvas = Canvas(this)

        maskCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), rectangleMaskPaint)
        maskCanvas.drawRect(0f, 0f, visibleX!!.toFloat(), height.toFloat(), clearMaskPaint)
    }
}