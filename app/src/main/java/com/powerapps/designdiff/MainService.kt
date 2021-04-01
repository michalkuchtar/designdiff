package com.powerapps.designdiff

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.powerapps.designdiff.utils.DisplayUtil
import com.powerapps.designdiff.utils.NotificationUtil
import com.powerapps.designdiff.utils.UnitsConverter
import com.powerapps.designdiff.utils.screenshots.RequestScreenshotPermissionActivity
import com.powerapps.designdiff.utils.screenshots.ScreenshotCallback
import com.powerapps.designdiff.utils.screenshots.ScreenshotMaker


class MainService : Service(), ControlPanelView.Listener, ScreenshotCallback {

    private lateinit var diffSurface: DiffSurfaceView
    private lateinit var controlPanelView: ControlPanelView
    private lateinit var dragDiffHandleView: DragDiffHandleView

    private var fileReceiverRegistered: Boolean = false
    private var screenshotReceiverRegistered = false

    private var dragDiffEnabled: Boolean = false

    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private val screenshotMaker by lazy {
        ScreenshotMaker(this,
                DisplayUtil.getDisplayWidth(this, windowManager.defaultDisplay),
                DisplayUtil.getDisplayHeight(this, windowManager.defaultDisplay))
    }

    private val fileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            unregisterReceiver(this)
            fileReceiverRegistered = false

            try {
                val imageUri = Uri.parse(intent.getStringExtra(KEY_IMAGE_URI))
                diffSurface.loadImage(imageUri)

                controlPanelView.fileReceived()
            } catch (e: Exception) {
                Toast.makeText(this@MainService, "Get image error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestScreenshotPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val resultCode = (application as DesignDiffApplication).screenshotResultCode
            val data = (application as DesignDiffApplication).screenshotDataIntent

            addDiffSurfaceToOverlay()

            Handler().postDelayed({
                if (resultCode != null && data != null) {
                    screenshotMaker.takeScreenshot(this@MainService, resultCode, data)
                } else {
                    onTakeScreenshotError()
                }
            }, 300)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        (application as DesignDiffApplication).isMainServiceRunning = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, NotificationCompat.Builder(this, NotificationUtil.getChannelId(this))
                    .setContentTitle(getString(R.string.notification_title))
                    .setSmallIcon(R.drawable.ic_drag_diff_tile)
                    .build())
        }

        diffSurface = DiffSurfaceView(this)
        controlPanelView = ControlPanelView(this, this)
        dragDiffHandleView = DragDiffHandleView(this) { diffSurface.updateDiffDrag(it) }

        addDiffSurfaceToOverlay()
        addControlPanelToOverlay()
    }

    private fun addDiffSurfaceToOverlay() {
        val diffSurfaceParams: WindowManager.LayoutParams = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
            else -> WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT)
        }

        windowManager.addView(diffSurface, diffSurfaceParams)
    }

    private fun addControlPanelToOverlay() {
        val controlPanelViewParams: WindowManager.LayoutParams = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT)
            else -> WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT)
        }

        controlPanelViewParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.END

        windowManager.addView(controlPanelView, controlPanelViewParams)
    }

    private fun addDragDiffHandleViewToOverlay() {
        val dragDiffHandleViewParams: WindowManager.LayoutParams = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT)
            else -> WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT)
        }

        dragDiffHandleViewParams.gravity = Gravity.CENTER

        windowManager.addView(dragDiffHandleView, dragDiffHandleViewParams)
    }

    override fun onCloseButtonPressed() {
        windowManager.removeViewImmediate(diffSurface)
        windowManager.removeViewImmediate(controlPanelView)

        if (dragDiffEnabled) {
            windowManager.removeViewImmediate(dragDiffHandleView)
        }

        stopSelf()
    }

    override fun onDestroy() {
        if (fileReceiverRegistered) {
            unregisterReceiver(fileReceiver)
        }
        if (screenshotReceiverRegistered) {
            unregisterReceiver(requestScreenshotPermissionReceiver)
        }

        (application as DesignDiffApplication).isMainServiceRunning = false
        super.onDestroy()
    }

    override fun onFileButtonPressed() {
        registerReceiver(fileReceiver, IntentFilter(FILE_RECEIVER_ACTION))
        fileReceiverRegistered = true
        startActivity(Intent(this, FileActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })

        diffSurface.setImageVisibility(false)
    }

    override fun onDragDiffButtonPressed(isDragDiffEnabled: Boolean) {
        if (isDragDiffEnabled) {
            addDragDiffHandleViewToOverlay()
        } else {
            windowManager.removeViewImmediate(dragDiffHandleView)
        }

        dragDiffEnabled = isDragDiffEnabled
        diffSurface.setDragDiffSeparatorVisible(isDragDiffEnabled)
    }

    override fun onAlphaChanged(currentAlphaMode: ControlPanelView.AlphaMode) {
        diffSurface.setImageAlpha(currentAlphaMode.alphaValue)
    }

    override fun performLockSurfaceFromTransform() {
        diffSurface.disableTransformation()
    }

    override fun performUnLockSurfaceToTransform() {
        diffSurface.enableTransformation()
    }

    override fun performScaleTransform(scaleTransformType: ControlPanelView.ScaleTransformType, factor: Int) {
        diffSurface.updateImageScale(scaleTransformType, factor)
    }

    override fun performMoveTransform(moveTransformType: ControlPanelView.MoveTransformType, factor: Int) {
        diffSurface.increaseImageTranslation(
                UnitsConverter.dpToPixels(this, moveTransformType.xDp) * factor,
                UnitsConverter.dpToPixels(this, moveTransformType.yDp) * factor
        )
    }

    override fun onRestoreButtonPressed() {
        diffSurface.restoreImageTransform()
    }

    override fun onGuidesButtonPressed(isGuidesEnabled: Boolean) {
        if (isGuidesEnabled) {
            windowManager.removeViewImmediate(controlPanelView)
            windowManager.removeViewImmediate(diffSurface)
            registerReceiver(requestScreenshotPermissionReceiver, IntentFilter(TAKE_SCREENSHOT_RECEIVER_ACTION))
            screenshotReceiverRegistered = true
            startActivity(Intent(this, RequestScreenshotPermissionActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        } else {
            diffSurface.disableRulersMode()
        }
    }

    override fun onScreenshotTaken(screenshot: Bitmap) {
        addControlPanelToOverlay()
        diffSurface.enableRulersMode(screenshot)
        controlPanelView.onGuidesModeEnabled()
    }

    override fun onTakeScreenshotError() {
        addControlPanelToOverlay()
        Toast.makeText(this@MainService, "Cannot take screenshot to start guides mode", Toast.LENGTH_SHORT).show()
    }

    override fun onHorizontalGuidesButtonPressed(isHorizontalGuidesEnabled: Boolean) {
        diffSurface.updateHorizontalRulerVisibility(isHorizontalGuidesEnabled)
    }

    override fun onVerticalGuidesButtonPressed(isVerticalGuidesEnabled: Boolean) {
        diffSurface.updateVerticalRulerVisibility(isVerticalGuidesEnabled)
    }

    companion object {
        private const val FILE_RECEIVER_ACTION = "com.powerapps.designdiff.FileReceiver"
        private const val TAKE_SCREENSHOT_RECEIVER_ACTION = "com.powerapps.designdiff.TakeScreenshotReceiver"

        private const val KEY_IMAGE_URI = "imageUri"

        fun getFileReceiverIntent(imageUri: String? = null) = Intent(FILE_RECEIVER_ACTION).apply {
            if (imageUri != null) {
                putExtra(KEY_IMAGE_URI, imageUri)
            }
        }

        fun getTakeScreenshotReceiverIntent() = Intent(TAKE_SCREENSHOT_RECEIVER_ACTION)
    }
}