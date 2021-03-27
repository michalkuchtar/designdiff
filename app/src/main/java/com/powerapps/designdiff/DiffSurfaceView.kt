package com.powerapps.designdiff

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.*
import android.view.MotionEvent.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.powerapps.designdiff.utils.UnitsConverter


class DiffSurfaceView(context: Context) : FrameLayout(context) {

    private val halfSeparatorWidthPx by lazy { UnitsConverter.dpToPixels(context, 1f) }

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val fullContent by lazy { findViewById<ViewGroup>(R.id.content) }
    private val diffImage: DesignImageView by lazy { findViewById<DesignImageView>(R.id.diffImage) }
    private val pinchTutorialImage: ImageView by lazy { findViewById<ImageView>(R.id.pinchTutorialImage) }
    private val diffDragSeparator: View by lazy { findViewById<View>(R.id.diffDragSeparator) }

    private val horizontalRulerViewHolder by lazy { HorizontalGuidesViewHolder(fullContent) }
    private val verticalRulerViewHolder by lazy { VerticalGuidesViewHolder(fullContent) }

    private val guidesBackground by lazy { ContextCompat.getDrawable(context, R.drawable.guides_background_repeat) }

    private val scaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    private var isScaling = false
    private var currentScale = 1.0f

    private var activePointerId = INVALID_POINTER_ID
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f

    private var isFirstTransformation = true
    private var isTransformationEnabled = false

    private var isRulersModeEnabled: Boolean = false
    private var lastShownImageInfo: LastImageInfo? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.diff_surface, this, true)
        isFirstTransformation = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_SHOW_TRANSFORM_TUTORIAL, true)
    }

    fun enableTransformation() {
        if (isFirstTransformation) {
            showTransformationTutorial()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val diffSurfaceParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)

            windowManager.removeViewImmediate(this)
            windowManager.addView(this, diffSurfaceParams)
        } else {
            val diffSurfaceParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)

            windowManager.updateViewLayout(this, diffSurfaceParams)
        }

        isTransformationEnabled = true
    }

    private fun showTransformationTutorial() {
        pinchTutorialImage.visibility = View.VISIBLE

        val translateYPx = UnitsConverter.dpToPixels(context, TRANSFORM_TUTORIAL_MOVE_Y_DP).toFloat()

        AnimatorSet().apply {
            playSequentially(
                    ObjectAnimator.ofFloat(diffImage, View.TRANSLATION_Y, -translateYPx),
                    ObjectAnimator.ofFloat(diffImage, View.TRANSLATION_Y, 2 * translateYPx),
                    ObjectAnimator.ofFloat(diffImage, View.TRANSLATION_Y, translateYPx),
                    AnimatorSet().apply {
                        startDelay = 300
                        playTogether(
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_X, 1.2f),
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_Y, 1.2f)
                        )
                    },
                    AnimatorSet().apply {
                        playTogether(
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_X, 0.8f),
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_Y, 0.8f)
                        )
                    },
                    AnimatorSet().apply {
                        playTogether(
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_X, 1f),
                                ObjectAnimator.ofFloat(diffImage, View.SCALE_Y, 1f)
                        )
                    }
            )

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    ViewCompat.postOnAnimationDelayed(pinchTutorialImage, {
                        pinchTutorialImage.visibility = View.GONE
                    }, 500)
                }
            })
            startDelay = 100
        }.start()
    }

    fun disableTransformation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val diffSurfaceParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)

            windowManager.updateViewLayout(this, diffSurfaceParams)
        } else {
            val diffSurfaceParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)

            windowManager.removeViewImmediate(this)
            windowManager.addView(this, diffSurfaceParams)
        }

        isTransformationEnabled = false
    }

    fun loadImage(imageUri: Uri?) {
        val imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        diffImage.setImageBitmap(imageBitmap)

        diffImage.translationX = 0f
        diffImage.translationY = 0f
        diffImage.scaleX = 1f
        diffImage.scaleY = 1f

        setImageVisibility(true)
    }

    fun setImageVisibility(surfaceVisible: Boolean) {
        diffImage.visibility = if (surfaceVisible) View.VISIBLE else View.GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (horizontalRulerViewHolder.onTouchEvent(event)) {
            return true
        }

        if (verticalRulerViewHolder.onTouchEvent(event)) {
            return true
        }

        scaleDetector.onTouchEvent(event)

        if (isScaling) {
            return true
        }

        when (event.action and ACTION_MASK) {
            ACTION_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                lastTouchX = x
                lastTouchY = y
                activePointerId = event.getPointerId(0)
            }
            ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)

                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                val dx = x - lastTouchX
                val dy = y - lastTouchY

                diffImage.translationX += dx
                diffImage.translationY += dy

                lastTouchX = x
                lastTouchY = y

                if (isRulersModeEnabled) {
                    if (horizontalRulerViewHolder.isVisible) {
                        horizontalRulerViewHolder.updateGuidesPositions(dx)
                    }
                    if (verticalRulerViewHolder.isVisible) {
                        verticalRulerViewHolder.updateGuidesPositions(dy)
                    }
                }

                if (isFirstTransformation) {
                    isFirstTransformation = false
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putBoolean(KEY_SHOW_TRANSFORM_TUTORIAL, false)
                            .apply()
                }
            }

            ACTION_UP, ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER_ID
            }
            ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    fun setImageAlpha(alphaValue: Float) {
        diffImage.alpha = alphaValue
    }

    fun updateImageScale(scaleTransformType: ControlPanelView.ScaleTransformType, factor: Int) {
        if (scaleTransformType == ControlPanelView.ScaleTransformType.INCREASE
                || scaleTransformType == ControlPanelView.ScaleTransformType.DECREASE) {
            val unitPx = UnitsConverter.dpToPixels(context, when (scaleTransformType) {
                ControlPanelView.ScaleTransformType.INCREASE -> 1f * factor
                else -> -1f * factor
            }).toFloat()

            val drawableScaleX = diffImage.width.toFloat() / diffImage.drawable.intrinsicWidth.toFloat()
            val drawableScaleY = diffImage.height.toFloat() / diffImage.drawable.intrinsicHeight.toFloat()
            val drawableScale = Math.min(Math.min(drawableScaleX, drawableScaleY), 1f)

            val imageWidth = diffImage.drawable.intrinsicWidth.toFloat() * drawableScale
            val updatedWidth = imageWidth + unitPx

            currentScale += (updatedWidth / imageWidth) - 1f

            diffImage.scaleX = currentScale
            diffImage.scaleY = currentScale
        } else if (scaleTransformType == ControlPanelView.ScaleTransformType.FIT_VERTICAL
                || scaleTransformType == ControlPanelView.ScaleTransformType.FIT_HORIZONTAL) {
            val drawableScaleX = diffImage.width.toFloat() / diffImage.drawable.intrinsicWidth.toFloat()
            val drawableScaleY = diffImage.height.toFloat() / diffImage.drawable.intrinsicHeight.toFloat()
            val drawableScale = Math.min(Math.min(drawableScaleX, drawableScaleY), 1f)

            val imageHeight = diffImage.drawable.intrinsicHeight.toFloat() * drawableScale
            val imageWidth = diffImage.drawable.intrinsicWidth.toFloat() * drawableScale

            val orientation = context.resources.configuration.orientation

            val size = Point()
            windowManager.defaultDisplay.getRealSize(size)

            val x = size.x
            val y = size.y

            currentScale = when (scaleTransformType) {
                ControlPanelView.ScaleTransformType.FIT_VERTICAL ->
                    getHeight(x, y, orientation) / imageHeight
                else -> getWidth(x, y, orientation) / imageWidth
            }

            diffImage.scaleX = currentScale
            diffImage.scaleY = currentScale
            diffImage.translationX = 0f
            diffImage.translationY = 0f
        }
    }

    private fun getWidth(x: Int, y: Int, orientation: Int): Int =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) x else y

    private fun getHeight(x: Int, y: Int, orientation: Int): Int =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) y else x

    fun increaseImageTranslation(xPx: Int, yPx: Int) {
        diffImage.translationX += xPx
        diffImage.translationY += yPx
    }

    fun restoreImageTransform() {
        diffImage.scaleX = 1f
        diffImage.scaleY = 1f
        diffImage.translationX = 0f
        diffImage.translationY = 0f
    }

    fun setDragDiffSeparatorVisible(visible: Boolean) {
        diffDragSeparator.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            val diffDragSeparatorLayoutParams = diffDragSeparator.layoutParams as LayoutParams
            diffDragSeparatorLayoutParams.gravity = Gravity.CENTER

            diffDragSeparator.x = 0f
            diffDragSeparator.translationX = 0f

            diffDragSeparator.layoutParams = diffDragSeparatorLayoutParams

            val scaledWidth: Float = diffImage.width * diffImage.scaleX
            val x = diffImage.width / 2
            val scaledX = (scaledWidth / diffImage.width) * x + ((diffImage.width - scaledWidth) / 2f)

            diffImage.visibleX = (x - ((scaledX - x + diffImage.translationX) / diffImage.scaleX)).toInt()
        } else {
            diffImage.visibleX = diffImage.width
        }
    }

    fun updateDiffDrag(x: Int) {
        diffDragSeparator.x = x.toFloat()

        val scaledWidth: Float = diffImage.width * diffImage.scaleX
        val scaledX = (scaledWidth / diffImage.width) * x + ((diffImage.width - scaledWidth) / 2f)

        diffImage.visibleX = (x - ((scaledX - x + diffImage.translationX) / diffImage.scaleX)).toInt() + halfSeparatorWidthPx
    }

    fun updateHorizontalRulerVisibility(horizontalRulerVisible: Boolean) {
        horizontalRulerViewHolder.isVisible = horizontalRulerVisible
    }

    fun updateVerticalRulerVisibility(verticalRulerVisible: Boolean) {
        verticalRulerViewHolder.isVisible = verticalRulerVisible
    }

    fun enableRulersMode(screenshot: Bitmap) {
        isRulersModeEnabled = true
        lastShownImageInfo = LastImageInfo(
                (diffImage.drawable as BitmapDrawable).bitmap,
                diffImage.translationX,
                diffImage.translationY,
                diffImage.scaleX,
                diffImage.alpha
        )

        fullContent.background = guidesBackground

        diffImage.scaleX = 1f
        diffImage.scaleY = 1f
        diffImage.translationX = 0f
        diffImage.translationY = 0f
        diffImage.alpha = 1f

        diffImage.setImageBitmap(screenshot)
        enableTransformation()

        horizontalRulerViewHolder.onFirstGuideButtonDrag(diffImage.width / 3f)
        horizontalRulerViewHolder.onSecondGuideButtonDrag(diffImage.width * 2f / 3f)
        verticalRulerViewHolder.onFirstGuideButtonDrag(diffImage.height / 3f)
        verticalRulerViewHolder.onSecondGuideButtonDrag(diffImage.height * 2f / 3f)
    }

    fun disableRulersMode() {
        isRulersModeEnabled = false
        disableTransformation()

        fullContent.setBackgroundResource(0)

        if (lastShownImageInfo != null) {
            diffImage.setImageBitmap(lastShownImageInfo!!.image)
            diffImage.translationX = lastShownImageInfo!!.translationX
            diffImage.translationY = lastShownImageInfo!!.translationY
            diffImage.scaleX = lastShownImageInfo!!.scale
            diffImage.scaleY = lastShownImageInfo!!.scale
            diffImage.alpha = lastShownImageInfo!!.alpha
        }
    }

    inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            currentScale *= detector.scaleFactor
            currentScale = Math.max(0.1f, Math.min(currentScale, 5.0f))

            diffImage.scaleY = currentScale
            diffImage.scaleX = currentScale

            if (isRulersModeEnabled) {
                if (horizontalRulerViewHolder.isVisible) {
                    horizontalRulerViewHolder.updateScale(currentScale, diffImage.translationX + (diffImage.width / 2))
                }

                if (verticalRulerViewHolder.isVisible) {
                    verticalRulerViewHolder.updateScale(currentScale, diffImage.translationY + (diffImage.height / 2))
                }
            }

            return true
        }

        override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
            isScaling = true
            return true
        }

        override fun onScaleEnd(p0: ScaleGestureDetector?) {
            isScaling = false
        }
    }

    inner class LastImageInfo(val image: Bitmap,
                              val translationX: Float,
                              val translationY: Float,
                              val scale: Float,
                              val alpha: Float)

    companion object {
        private const val KEY_SHOW_TRANSFORM_TUTORIAL = "showTransformTutorial"
        private const val TRANSFORM_TUTORIAL_MOVE_Y_DP = 28f
    }
}