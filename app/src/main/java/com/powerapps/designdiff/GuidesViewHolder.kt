package com.powerapps.designdiff

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.powerapps.designdiff.utils.UnitsConverter

abstract class GuidesViewHolder(private val parent: ViewGroup, private val mode: Mode) {
    abstract val firstGuide: View
    abstract val secondsGuide: View
    abstract val guidesTextInfo: TextView

    private var surfaceScale = 1.0f

    var isVisible = true
        set(value) {
            field = value
            when (value) {
                true -> {
                    firstGuide.visibility = View.VISIBLE
                    secondsGuide.visibility = View.VISIBLE
                    guidesTextInfo.visibility = View.VISIBLE

                    updateGuidesTextInfo()
                }
                false -> {
                    firstGuide.visibility = View.GONE
                    secondsGuide.visibility = View.GONE
                    guidesTextInfo.visibility = View.GONE
                }
            }
        }

    private var isFirstGuideDrag = false
    private var isSecondGuideDrag = false

    fun updateScale(newScale: Float, surfaceCenter: Float) {
        val currentScale = surfaceScale
        surfaceScale = newScale

        when (mode) {
            Mode.HORIZONTAL -> {
                val buttonHalfSize = firstGuide.width / 2f
                val firstButtonCurrentCenterPosition = firstGuide.translationX + buttonHalfSize
                val secondsButtonCurrentCenterPosition = secondsGuide.translationX + buttonHalfSize

                val firstButtonCurrentDistanceToSurfaceCenter = firstButtonCurrentCenterPosition - surfaceCenter
                val secondButtonCurrentDistanceToSurfaceCenter = secondsButtonCurrentCenterPosition - surfaceCenter

                val updatedFirstButtonCurrentDistanceToSurfaceCenter = firstButtonCurrentDistanceToSurfaceCenter * newScale / currentScale
                val updatedSecondButtonCurrentDistanceToSurfaceCenter = secondButtonCurrentDistanceToSurfaceCenter * newScale / currentScale

                val firstButtonDx = updatedFirstButtonCurrentDistanceToSurfaceCenter - firstButtonCurrentDistanceToSurfaceCenter
                val secondButtonDx = updatedSecondButtonCurrentDistanceToSurfaceCenter - secondButtonCurrentDistanceToSurfaceCenter

                updateGuidePosition(firstGuide, firstGuide.translationX + firstButtonDx + buttonHalfSize)
                updateGuidePosition(secondsGuide, secondsGuide.translationX + secondButtonDx + buttonHalfSize)
                updateGuidesTextInfo()
            }
            Mode.VERTICAL -> {
                val buttonHalfSize = firstGuide.height / 2f
                val firstButtonCurrentCenterPosition = firstGuide.translationY + (firstGuide.height / 2)
                val secondsButtonCurrentCenterPosition = secondsGuide.translationY + (secondsGuide.height / 2)

                val firstButtonCurrentDistanceToSurfaceCenter = firstButtonCurrentCenterPosition - surfaceCenter
                val secondButtonCurrentDistanceToSurfaceCenter = secondsButtonCurrentCenterPosition - surfaceCenter

                val updatedFirstButtonCurrentDistanceToSurfaceCenter = firstButtonCurrentDistanceToSurfaceCenter * newScale / currentScale
                val updatedSecondButtonCurrentDistanceToSurfaceCenter = secondButtonCurrentDistanceToSurfaceCenter * newScale / currentScale

                val firstButtonDy = updatedFirstButtonCurrentDistanceToSurfaceCenter - firstButtonCurrentDistanceToSurfaceCenter
                val secondButtonDy = updatedSecondButtonCurrentDistanceToSurfaceCenter - secondButtonCurrentDistanceToSurfaceCenter

                updateGuidePosition(firstGuide, firstGuide.translationY + firstButtonDy + buttonHalfSize)
                updateGuidePosition(secondsGuide, secondsGuide.translationY + secondButtonDy + buttonHalfSize)
                updateGuidesTextInfo()
            }
        }
    }

    fun updateGuidesPositions(updatedPositionBy: Float) {
        val currentFirstButtonTranslation = when (mode) {
            Mode.HORIZONTAL -> firstGuide.translationX
            Mode.VERTICAL -> firstGuide.translationY
        }
        val currentSecondButtonTranslation = when (mode) {
            Mode.HORIZONTAL -> secondsGuide.translationX
            Mode.VERTICAL -> secondsGuide.translationY
        }
        val buttonsHalfSize = when (mode) {
            Mode.HORIZONTAL -> firstGuide.width / 2
            Mode.VERTICAL -> firstGuide.height / 2
        }

        updateGuidePosition(firstGuide, currentFirstButtonTranslation + buttonsHalfSize + updatedPositionBy)
        updateGuidePosition(secondsGuide, currentSecondButtonTranslation + buttonsHalfSize + updatedPositionBy)

        updateGuidesTextInfo()
    }

    fun onFirstGuideButtonDrag(updatedPosition: Float) {
        updateGuidePosition(firstGuide, updatedPosition)
        updateGuidesTextInfo()
    }

    fun onSecondGuideButtonDrag(updatedPosition: Float) {
        updateGuidePosition(secondsGuide, updatedPosition)
        updateGuidesTextInfo()
    }

    private fun updateGuidePosition(guideView: View, updatedPosition: Float) {
        when (mode) {
            Mode.HORIZONTAL -> {
                val inParentRangePosition = Math.min(parent.width.toFloat(), Math.max(0f, updatedPosition))
                guideView.translationX = (inParentRangePosition - (guideView.width / 2))
            }
            Mode.VERTICAL -> {
                val inParentRangePosition = Math.min(parent.height.toFloat(), Math.max(0f, updatedPosition))
                guideView.translationY = inParentRangePosition - (guideView.height / 2)
            }
        }
    }

    private fun updateGuidesTextInfo() {
        if (mode == Mode.HORIZONTAL) {
            val distanceBetweenPx = Math.abs(firstGuide.translationX - secondsGuide.translationX)
            val distanceBetweenDp = UnitsConverter.pixelsToDp(firstGuide.context, distanceBetweenPx)

            guidesTextInfo.text = String.format("%.1f", distanceBetweenDp / surfaceScale)
            guidesTextInfo.measure(0, 0)

            val infoTextWidth = guidesTextInfo.measuredWidth

            val nearestLeftButtonPosition = Math.min(firstGuide.translationX, secondsGuide.translationX)

            guidesTextInfo.translationX = nearestLeftButtonPosition + (distanceBetweenPx / 2) - (infoTextWidth / 2) + (firstGuide.width / 2)
        } else {
            val distanceBetweenPx = Math.abs(firstGuide.translationY - secondsGuide.translationY)
            val distanceBetweenDp = UnitsConverter.pixelsToDp(firstGuide.context, distanceBetweenPx)

            guidesTextInfo.text = String.format("%.1f", distanceBetweenDp / surfaceScale)
            val nearestTopButtonPosition = Math.min(firstGuide.translationY, secondsGuide.translationY)

            guidesTextInfo.translationY = nearestTopButtonPosition + (distanceBetweenPx / 2) - (guidesTextInfo.height / 2) + (firstGuide.height / 2)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (isVisible) {
                    isFirstGuideDrag = isViewUnderTouchEvent(firstGuide, event)
                    isSecondGuideDrag = isViewUnderTouchEvent(secondsGuide, event)
                }

                return isFirstGuideDrag || isSecondGuideDrag
            }
            MotionEvent.ACTION_MOVE -> {
                val updatedPosition = when (mode) {
                    Mode.HORIZONTAL -> event.x
                    Mode.VERTICAL -> event.y
                }

                if (isFirstGuideDrag) {
                    onFirstGuideButtonDrag(updatedPosition)
                    return true
                }
                if (isSecondGuideDrag) {
                    onSecondGuideButtonDrag(updatedPosition)
                    return true
                }

                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isFirstGuideDrag || isSecondGuideDrag) {
                    isFirstGuideDrag = false
                    isSecondGuideDrag = false
                    return true
                }

                return false
            }
        }

        return false
    }

    private fun isViewUnderTouchEvent(view: View, event: MotionEvent): Boolean {
        if (!isVisible) {
            return false
        }

        val hitRect = Rect()
        view.getHitRect(hitRect)

        return hitRect.contains(event.x.toInt(), event.y.toInt())
    }

    enum class Mode {
        HORIZONTAL,
        VERTICAL
    }
}