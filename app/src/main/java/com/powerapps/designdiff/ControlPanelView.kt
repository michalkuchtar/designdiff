package com.powerapps.designdiff

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.powerapps.designdiff.ControlPanelView.ButtonType.*


@SuppressLint("ViewConstructor")
class ControlPanelView(context: Context, val listener: Listener) : FrameLayout(context) {

    private val windowManager by lazy { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    private val closeButton: ImageView by lazy { findViewById<ImageView>(R.id.closeButton) }
    private val fileButton: View by lazy { findViewById<View>(R.id.fileButton) }

    private val diffButton: View by lazy { findViewById<View>(R.id.diffButton) }
    private val dragDiffButton: View by lazy { findViewById<View>(R.id.dragDiffButton) }
    private val visibilityButton: View by lazy { findViewById<View>(R.id.visibilityButton) }
    private val visibilityButtonImage: ImageView by lazy { findViewById<ImageView>(R.id.visibilityButtonImage) }
    private val alphaButton: View by lazy { findViewById<View>(R.id.alphaButton) }
    private val alphaButtonImage: ImageView by lazy { findViewById<ImageView>(R.id.alphaButtonImage) }

    private val guidesButton: View by lazy { findViewById<View>(R.id.guidesButton) }
    private val horizontalGuidesButton: View by lazy { findViewById<View>(R.id.horizontalGuidesButton) }
    private val horizontalGuidesButtonImage: ImageView by lazy { findViewById<ImageView>(R.id.horizontalGuidesButtonImage) }
    private val verticalGuidesButton: View by lazy { findViewById<View>(R.id.verticalGuidesButton) }
    private val verticalGuidesButtonImage: ImageView by lazy { findViewById<ImageView>(R.id.verticalGuidesButtonImage) }

    private val transformButton: View by lazy { findViewById<View>(R.id.transformButtonHolder) }

    private val scaleDetailButton: View by lazy { findViewById<View>(R.id.scaleDpButton) }
    private val scaleDetailsIncreaseButton: View by lazy { findViewById<View>(R.id.scaleIncreaseButton) }
    private val scaleDetailsDecreaseButton: View by lazy { findViewById<View>(R.id.scaleDecreaseButton) }
    private val scaleDetailsFitVerticalButton: View by lazy { findViewById<View>(R.id.scaleFitVerticalButton) }
    private val scaleDetailsFitHorizontalButton: View by lazy { findViewById<View>(R.id.scaleFitHorizontalButton) }

    private val moveDetailButton: View by lazy { findViewById<View>(R.id.moveDpButton) }
    private val moveDetailUpButton: View by lazy { findViewById<View>(R.id.moveDpUpButton) }
    private val moveDetailDownButton: View by lazy { findViewById<View>(R.id.moveDpDownButton) }
    private val moveDetailLeftButton: View by lazy { findViewById<View>(R.id.moveDpLeftButton) }
    private val moveDetailRightButton: View by lazy { findViewById<View>(R.id.moveDpRightButton) }

    private val restoreButton: View by lazy { findViewById<View>(R.id.restoreButton) }

    private val dragButton: View by lazy { findViewById<View>(R.id.dragButton) }

    private var isControlPanelDragging = false
    private var isButtonPressed = false

    private var longButtonPressedRunnable: Runnable? = null

    private var isSurfaceVisible = true
        set(value) {
            field = value
            when (value) {
                true -> {
                    visibilityButtonImage.setImageResource(R.drawable.ic_visibility)
                    listener.onAlphaChanged(currentAlphaMode)
                }
                false -> {
                    visibilityButtonImage.setImageResource(R.drawable.ic_visibility_off)
                    listener.onAlphaChanged(AlphaMode.NONE)
                }
            }
        }

    private var currentAlphaMode = AlphaMode.HALF
        set(value) {
            field = value

            alphaButtonImage.setImageResource(when (value) {
                AlphaMode.QUARTER -> R.drawable.ic_25
                AlphaMode.HALF -> R.drawable.ic_50
                AlphaMode.THREE_QUARTERS -> R.drawable.ic_75
                AlphaMode.FULL -> R.drawable.ic_100
                AlphaMode.NONE -> R.drawable.ic_50
            })

            if (isSurfaceVisible) {
                listener.onAlphaChanged(value)
            }
        }
    private var lastSetAlphaMode = currentAlphaMode

    private var currentPanelMode = ControlPanelMode.START

    private var isHorizontalGuidesVisible = false
        set(value) {
            field = value

            when (value) {
                true -> horizontalGuidesButtonImage.setImageResource(R.drawable.ic_ruler_horizontal)
                false -> horizontalGuidesButtonImage.setImageResource(R.drawable.ic_ruler_horizontal_disabled)
            }

            listener.onHorizontalGuidesButtonPressed(value)
        }
    private var isVerticalGuidesVisible = false
        set(value) {
            field = value

            when (value) {
                true -> verticalGuidesButtonImage.setImageResource(R.drawable.ic_ruler_vertical)
                false -> verticalGuidesButtonImage.setImageResource(R.drawable.ic_ruler_vertical_disabled)
            }

            listener.onVerticalGuidesButtonPressed(value)
        }

    private val buttonViewHolders by lazy {
        mapOf(
                CLOSE to ButtonViewHolder(closeButton),
                FILE to ButtonViewHolder(fileButton),
                DIFF to ButtonViewHolder(diffButton),
                DRAG_DIFF to ButtonViewHolder(dragDiffButton),
                VISIBILITY to ButtonViewHolder(visibilityButton),
                VISIBILITY_ALPHA to ButtonViewHolder(alphaButton),
                GUIDES to ButtonViewHolder(guidesButton),
                HORIZONTAL_GUIDES to ButtonViewHolder(horizontalGuidesButton),
                VERTICAL_GUIDES to ButtonViewHolder(verticalGuidesButton),
                TRANSFORM to ButtonViewHolder(transformButton),
                DETAIL_SCALE to ButtonViewHolder(scaleDetailButton),
                DETAIL_SCALE_INCREASE to ButtonViewHolder(scaleDetailsIncreaseButton),
                DETAIL_SCALE_DECREASE to ButtonViewHolder(scaleDetailsDecreaseButton),
                DETAIL_SCALE_FIT_VERTICAL to ButtonViewHolder(scaleDetailsFitVerticalButton),
                DETAIL_SCALE_FIT_HORIZONTAL to ButtonViewHolder(scaleDetailsFitHorizontalButton),
                DETAIL_MOVE to ButtonViewHolder(moveDetailButton),
                DETAIL_MOVE_UP to ButtonViewHolder(moveDetailUpButton),
                DETAIL_MOVE_DOWN to ButtonViewHolder(moveDetailDownButton),
                DETAIL_MOVE_LEFT to ButtonViewHolder(moveDetailLeftButton),
                DETAIL_MOVE_RIGHT to ButtonViewHolder(moveDetailRightButton),
                RESTORE to ButtonViewHolder(restoreButton),
                DRAG to ButtonViewHolder(dragButton))
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.control_panel, this, true)

        closeButton.setOnClickListener { onCloseButtonPressed() }
        fileButton.setOnClickListener { onFileButtonPressed() }
        diffButton.setOnClickListener { onDiffButtonPressed() }
        dragDiffButton.setOnClickListener { onDragDiffButtonPressed() }
        visibilityButton.setOnClickListener { onVisibilityButtonPressed() }
        alphaButton.setOnClickListener { onAlphaButtonPressed() }
        guidesButton.setOnClickListener { onGuidesButtonPressed() }
        horizontalGuidesButton.setOnClickListener { isHorizontalGuidesVisible = !isHorizontalGuidesVisible }
        verticalGuidesButton.setOnClickListener { isVerticalGuidesVisible = !isVerticalGuidesVisible }
        transformButton.setOnClickListener { onTransformButtonPressed() }
        scaleDetailButton.setOnClickListener { onDetailScaleButtonPressed() }

        scaleDetailsIncreaseButton.setOnClickListener { listener.performScaleTransform(ScaleTransformType.INCREASE) }
        scaleDetailsIncreaseButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performScaleTransform(ScaleTransformType.INCREASE, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        scaleDetailsIncreaseButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        scaleDetailsDecreaseButton.setOnClickListener { listener.performScaleTransform(ScaleTransformType.DECREASE) }
        scaleDetailsDecreaseButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performScaleTransform(ScaleTransformType.DECREASE, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        scaleDetailsDecreaseButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        scaleDetailsFitVerticalButton.setOnClickListener {
            listener.performScaleTransform(ScaleTransformType.FIT_VERTICAL)
        }
        scaleDetailsFitHorizontalButton.setOnClickListener {
            listener.performScaleTransform(ScaleTransformType.FIT_HORIZONTAL)
        }
        moveDetailButton.setOnClickListener { onDetailMoveButtonPressed() }

        moveDetailUpButton.setOnClickListener { listener.performMoveTransform(MoveTransformType.UP) }
        moveDetailUpButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performMoveTransform(MoveTransformType.UP, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        moveDetailUpButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        moveDetailDownButton.setOnClickListener { listener.performMoveTransform(MoveTransformType.DOWN) }
        moveDetailDownButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performMoveTransform(MoveTransformType.DOWN, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        moveDetailDownButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        moveDetailLeftButton.setOnClickListener { listener.performMoveTransform(MoveTransformType.LEFT) }
        moveDetailLeftButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performMoveTransform(MoveTransformType.LEFT, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        moveDetailLeftButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        moveDetailRightButton.setOnClickListener { listener.performMoveTransform(MoveTransformType.RIGHT) }
        moveDetailRightButton.setOnLongClickListener {
            longButtonPressedRunnable = Runnable {
                listener.performMoveTransform(MoveTransformType.RIGHT, 10)
                handler.postDelayed(longButtonPressedRunnable, 200)
            }
            longButtonPressedRunnable!!.run()
            true
        }
        moveDetailRightButton.setOnTouchListener { _, motionEvent -> handleLongPressUpIfNeed(motionEvent) }

        restoreButton.setOnClickListener { listener.onRestoreButtonPressed() }
    }

    private fun onCloseButtonPressed() {
        if (currentPanelMode == ControlPanelMode.MAIN || currentPanelMode == ControlPanelMode.START) {
            listener.onCloseButtonPressed()
        } else if (currentPanelMode == ControlPanelMode.TRANSFORM) {
            listener.performLockSurfaceFromTransform()
            setControlPanelMode(ControlPanelMode.MAIN)
        } else if (currentPanelMode == ControlPanelMode.DIFF) {
            setControlPanelMode(ControlPanelMode.MAIN)
        } else if (currentPanelMode == ControlPanelMode.DRAG_DIFF) {
            currentAlphaMode = lastSetAlphaMode
            setControlPanelMode(ControlPanelMode.DIFF)

            listener.onDragDiffButtonPressed(false)
        } else if (currentPanelMode == ControlPanelMode.GUIDES) {
            isHorizontalGuidesVisible = false
            isVerticalGuidesVisible = false

            setControlPanelMode(ControlPanelMode.DIFF)

            listener.onGuidesButtonPressed(false)
        } else if (currentPanelMode == ControlPanelMode.DETAIL_SCALE
                || currentPanelMode == ControlPanelMode.DETAIL_MOVE) {
            setControlPanelMode(ControlPanelMode.TRANSFORM)
        }
    }

    private fun handleLongPressUpIfNeed(motionEvent: MotionEvent): Boolean {
        if ((motionEvent.actionMasked == MotionEvent.ACTION_UP
                        || motionEvent.actionMasked == MotionEvent.ACTION_CANCEL)
                && longButtonPressedRunnable != null) {
            handler.removeCallbacks(longButtonPressedRunnable)
        }

        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> {
                if (isButtonPressed) {
                    isButtonPressed = false
                }
                val pressedButtonViewHolder =
                        buttonViewHolders.filterKeys { currentPanelMode.buttonTypes.contains(it) }
                                .values
                                .firstOrNull { it.isViewUnderTouchEvent(event) }

                if (pressedButtonViewHolder != null) {
                    dragControlPanelDone()
                }
            }
            MotionEvent.ACTION_DOWN -> {
                if (buttonViewHolders[DRAG]!!.isViewUnderTouchEvent(event)) {
                    isControlPanelDragging = true
                } else {
                    isButtonPressed = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isControlPanelDragging) {
                    dragControlPanel(event.rawX, event.rawY)
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun onFileButtonPressed() {
        setControlPanelMode(ControlPanelMode.START)
        listener.onFileButtonPressed()
    }

    private fun setControlPanelMode(controlPanelMode: ControlPanelMode) {
        val commonButtonTypesBetweenModes = buttonViewHolders.keys.filter { currentPanelMode.buttonTypes.contains(it) && controlPanelMode.buttonTypes.contains(it) }

        currentPanelMode.buttonTypes.toMutableList()
                .apply { removeAll(commonButtonTypesBetweenModes) }
                .forEach { buttonViewHolders[it]!!.view.visibility = View.GONE }

        controlPanelMode.buttonTypes.toMutableList()
                .apply { removeAll(commonButtonTypesBetweenModes) }
                .forEach { buttonViewHolders[it]!!.view.visibility = View.VISIBLE }

        if (currentPanelMode == ControlPanelMode.MAIN && controlPanelMode == ControlPanelMode.DIFF
                || currentPanelMode == ControlPanelMode.MAIN && controlPanelMode == ControlPanelMode.TRANSFORM) {
            closeButton.setImageResource(R.drawable.ic_back)
        } else if (currentPanelMode == ControlPanelMode.DIFF && controlPanelMode == ControlPanelMode.MAIN
                || currentPanelMode == ControlPanelMode.TRANSFORM && controlPanelMode == ControlPanelMode.MAIN) {
            closeButton.setImageResource(R.drawable.ic_close)
        }

        currentPanelMode = controlPanelMode
    }

    private fun onDiffButtonPressed() {
        if (currentPanelMode == ControlPanelMode.MAIN) {
            setControlPanelMode(ControlPanelMode.DIFF)
        }
    }

    private fun onTransformButtonPressed() {
        if (currentPanelMode == ControlPanelMode.MAIN) {
            listener.performUnLockSurfaceToTransform()
            setControlPanelMode(ControlPanelMode.TRANSFORM)
        }
    }

    private fun onDragDiffButtonPressed() {
        if (currentPanelMode == ControlPanelMode.DIFF) {
            setControlPanelMode(ControlPanelMode.DRAG_DIFF)
            if (currentAlphaMode != AlphaMode.FULL) {
                currentAlphaMode = AlphaMode.FULL
            }
            if (!isSurfaceVisible) {
                isSurfaceVisible = true
            }

            listener.onDragDiffButtonPressed(true)
        }
    }

    private fun onVisibilityButtonPressed() {
        isSurfaceVisible = !isSurfaceVisible
    }

    private fun onAlphaButtonPressed() {
        currentAlphaMode = when (currentAlphaMode) {
            AlphaMode.HALF -> AlphaMode.THREE_QUARTERS
            AlphaMode.THREE_QUARTERS -> AlphaMode.FULL
            AlphaMode.FULL -> AlphaMode.QUARTER
            AlphaMode.QUARTER -> AlphaMode.HALF
            else -> AlphaMode.HALF
        }
        lastSetAlphaMode = currentAlphaMode
    }

    private fun onGuidesButtonPressed() {
        if (currentPanelMode == ControlPanelMode.DIFF) {
            setControlPanelMode(ControlPanelMode.GUIDES)

            listener.onGuidesButtonPressed(true)
        }
    }

    fun onGuidesModeEnabled() {
        isHorizontalGuidesVisible = true
    }

    private fun onDetailScaleButtonPressed() {
        if (currentPanelMode == ControlPanelMode.TRANSFORM) {
            setControlPanelMode(ControlPanelMode.DETAIL_SCALE)
        }
    }

    private fun onDetailMoveButtonPressed() {
        if (currentPanelMode == ControlPanelMode.TRANSFORM) {
            setControlPanelMode(ControlPanelMode.DETAIL_MOVE)
        }
    }

    private fun dragControlPanel(eventX: Float, eventY: Float) {
        val xOffset = dragButton.width / 2

        val yOffset = dragButton.y.toInt() + dragButton.measuredHeight
        val x = eventX.toInt() - xOffset
        val y = eventY.toInt() - yOffset

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

        params.gravity = Gravity.TOP or Gravity.START
        windowManager.updateViewLayout(this, params)
    }

    private fun dragControlPanelDone() {
        isControlPanelDragging = false
    }

    fun fileReceived() {
        setControlPanelMode(ControlPanelMode.MAIN)
    }

    interface Listener {
        fun onCloseButtonPressed()
        fun onFileButtonPressed()
        fun onDragDiffButtonPressed(isDragDiffEnabled: Boolean)
        fun onAlphaChanged(currentAlphaMode: AlphaMode)
        fun performLockSurfaceFromTransform()
        fun performUnLockSurfaceToTransform()
        fun performScaleTransform(scaleTransformType: ScaleTransformType, factor: Int = 1)
        fun performMoveTransform(moveTransformType: MoveTransformType, factor: Int = 1)
        fun onRestoreButtonPressed()
        fun onGuidesButtonPressed(isGuidesEnabled: Boolean)
        fun onHorizontalGuidesButtonPressed(isHorizontalGuidesEnabled: Boolean)
        fun onVerticalGuidesButtonPressed(isVerticalGuidesEnabled: Boolean)
    }

    private class ButtonViewHolder(val view: View) {

        fun isViewUnderTouchEvent(event: MotionEvent): Boolean {
            if (view.visibility == View.GONE) {
                return false
            }

            val hitRect = Rect()
            view.getHitRect(hitRect)

            return hitRect.contains(event.x.toInt(), event.y.toInt())
        }
    }

    private enum class ButtonType {
        CLOSE,
        FILE,
        TRANSFORM,
        DIFF,
        DRAG,
        DETAIL_SCALE,
        DETAIL_MOVE,
        DRAG_DIFF,
        VISIBILITY,
        VISIBILITY_ALPHA,
        GUIDES,
        HORIZONTAL_GUIDES,
        VERTICAL_GUIDES,
        DETAIL_MOVE_UP,
        DETAIL_MOVE_DOWN,
        DETAIL_MOVE_LEFT,
        DETAIL_MOVE_RIGHT,
        DETAIL_SCALE_INCREASE,
        DETAIL_SCALE_DECREASE,
        DETAIL_SCALE_FIT_VERTICAL,
        DETAIL_SCALE_FIT_HORIZONTAL,
        RESTORE
    }

    private enum class ControlPanelMode(val buttonTypes: List<ButtonType>) {
        START(listOf(CLOSE, FILE, DRAG)),
        MAIN(listOf(CLOSE, FILE, ButtonType.DIFF, ButtonType.TRANSFORM, DRAG)),
        DIFF(listOf(CLOSE, ButtonType.DRAG_DIFF, VISIBILITY, VISIBILITY_ALPHA, DRAG, ButtonType.GUIDES)),
        TRANSFORM(listOf(CLOSE, ButtonType.DETAIL_SCALE, ButtonType.DETAIL_MOVE, RESTORE, DRAG)),
        DETAIL_SCALE(listOf(CLOSE, DETAIL_SCALE_INCREASE, DETAIL_SCALE_DECREASE, DETAIL_SCALE_FIT_VERTICAL, DETAIL_SCALE_FIT_HORIZONTAL, DRAG)),
        DETAIL_MOVE(listOf(CLOSE, DETAIL_MOVE_UP, DETAIL_MOVE_DOWN, DETAIL_MOVE_LEFT, DETAIL_MOVE_RIGHT, DRAG)),
        DRAG_DIFF(listOf(CLOSE, DRAG)),
        GUIDES(listOf(CLOSE, HORIZONTAL_GUIDES, VERTICAL_GUIDES, DRAG))
    }

    enum class AlphaMode(val alphaValue: Float) {
        FULL(1.0f),
        THREE_QUARTERS(0.75f),
        HALF(0.5f),
        QUARTER(0.25f),
        NONE(0f)
    }

    enum class ScaleTransformType {
        INCREASE,
        DECREASE,
        FIT_VERTICAL,
        FIT_HORIZONTAL
    }

    enum class MoveTransformType(val xDp: Float, val yDp: Float) {
        UP(0f, -1f),
        DOWN(0f, 1f),
        LEFT(-1f, 0f),
        RIGHT(1f, 0f)
    }
}