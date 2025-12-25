package com.example.productscanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import android.util.Log
import kotlin.math.min


class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000") // semi-transparent black
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val cropRect = RectF() // the crop rectangle (in view coordinates)

    private val minCropSize = 120f * resources.displayMetrics.density // min size (in px)

    // how close you have to be to the corner to catch it
    private val handleTouchRadius = 20f * resources.displayMetrics.density
    private val handleVisualRadius = 6f * resources.displayMetrics.density

    private var lastX = 0f
    private var lastY = 0f

    private enum class Mode { NONE, MOVE, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR,
        RESIZE_TOP, RESIZE_BOTTOM, RESIZE_LEFT, RESIZE_RIGHT }
    private var mode: Mode = Mode.NONE

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // initial: a centered rectangle: -70% of width / height
        val padX = w * 0.15f
        val padY = h * 0.25f

        cropRect.set(
            padX,
            padY,
            w - padX,
            h - padY
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val clearPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        val saved = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 1. the dark overlay is drawn everywhere
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // 2. transparent hole in the crop area
        canvas.drawRect(cropRect, clearPaint)
        canvas.restoreToCount(saved)

        // 3.outline
        canvas.drawRect(cropRect, borderPaint)

        // 4. corner handles
        drawHandle(canvas, cropRect.left, cropRect.top) // TL
        drawHandle(canvas, cropRect.right, cropRect.top) // TR
        drawHandle(canvas, cropRect.left, cropRect.bottom) // BL
        drawHandle(canvas, cropRect.right, cropRect.bottom) // BR
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, handleVisualRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = hitTest(x, y)
                Log.d("CropOverlay", "DOWN x=$x, y=$y, mode=$mode, rect=$cropRect")
                lastX = x
                lastY = y

                if (mode != Mode.NONE) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    Log.d("CropOverlay", "DISALLOW intercept = true")
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - lastX
                val dy = y - lastY

                if (mode == Mode.NONE) {
                    return false
                }

                parent?.requestDisallowInterceptTouchEvent(true)

                Log.d("CropOverlay", "x=$x, y=$y, dx=$dx, dy=$dy,  mode=$mode, rect+$cropRect")

                when (mode) {
                    Mode.MOVE -> {
                        moveRect(dx, dy)
                    }
                    Mode.RESIZE_TL -> {
                        resizeLeft(dx); resizeTop(dy)
                    }
                    Mode.RESIZE_TR -> {
                        resizeRight(dx); resizeTop(dy)
                    }
                    Mode.RESIZE_BL -> {
                        resizeLeft(dx); resizeBottom(dy)
                    }
                    Mode.RESIZE_BR -> {
                        resizeRight(dx); resizeBottom(dy)
                    }
                    Mode.RESIZE_TOP -> {
                        resizeTop(dy)
                    }

                    Mode.RESIZE_BOTTOM -> {
                        resizeBottom(dy)
                    }
                    Mode.RESIZE_LEFT -> {
                        resizeLeft(dx)
                    }
                    Mode.RESIZE_RIGHT -> {
                        resizeRight(dx)
                    }
                    else -> Unit
                }

                lastX = x
                lastY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val wasActive = (mode != Mode.NONE)
                parent?.requestDisallowInterceptTouchEvent(false)
                mode = Mode.NONE
                return wasActive
            }
        }

        return super.onTouchEvent(event)
    }

    private fun hitTest(x: Float, y: Float): Mode {
        // corners (priority)
        if (isNear(x, y, cropRect.left, cropRect.top)) return Mode.RESIZE_TL
        if (isNear(x, y, cropRect.right, cropRect.top)) return Mode.RESIZE_TR
        if (isNear(x, y, cropRect.left, cropRect.bottom)) return Mode.RESIZE_BL
        if (isNear(x, y, cropRect.right, cropRect.bottom)) return Mode.RESIZE_BR

        // edges
        val baseEdgeSlop = 24f * resources.displayMetrics.density // 24dp
        val edgeSlop = baseEdgeSlop.coerceAtMost(min(cropRect.width(), cropRect.height()) / 3f)
        val insideX = x in cropRect.left..cropRect.right
        val insideY = y in cropRect.top..cropRect.bottom

        if (insideX && abs(y - cropRect.top) <= edgeSlop) {
            return Mode.RESIZE_TOP
        }

        if (insideX && abs(y - cropRect.bottom) <= edgeSlop) {
            return Mode.RESIZE_BOTTOM
        }

        if (insideY && abs(x - cropRect.left) <= edgeSlop) {
            return Mode.RESIZE_LEFT
        }

        if (insideY && abs(x - cropRect.right) <= edgeSlop) {
            return Mode.RESIZE_RIGHT
        }


        // inside => move
        val inset = edgeSlop.coerceAtMost(min(cropRect.width(), cropRect.height()) / 2f - 1f)
        val inner = RectF(cropRect).apply { inset(inset, inset) }
        if (inner.contains(x, y)) return Mode.MOVE

        return Mode.NONE
    }

    private fun isNear(x: Float, y: Float, hx: Float, hy: Float): Boolean {
        return abs(x - hx) <= handleTouchRadius && abs (y - hy) <= handleTouchRadius
    }

    private fun moveRect(dx: Float, dy: Float) {
        val newLeft = cropRect.left + dx
        val newTop = cropRect.top + dy
        val newRight = cropRect.right + dx
        val newBottom = cropRect.bottom + dy

        // clamps to bounds view
        val clampedDx = when {
            newLeft < 0f -> -cropRect.left
            newRight > width -> width - cropRect.right
            else -> dx
        }
        val clampedDy = when {
            newTop < 0f -> -cropRect.top
            newBottom > height -> height - cropRect.bottom
            else -> dy
        }

        cropRect.offset(clampedDx, clampedDy)
    }

    private fun resizeLeft(dx: Float) {
        val newLeft = cropRect.left + dx
        val maxLeft = cropRect.right - minCropSize
        cropRect.left = newLeft.coerceIn(0f, maxLeft)
    }

    private fun resizeRight(dx: Float) {
        val newRight = cropRect.right + dx
        val minRight = cropRect.left + minCropSize
        cropRect.right = newRight.coerceIn(minRight, width.toFloat())
    }

    private fun resizeTop(dy: Float) {
        val newTop = cropRect.top + dy
        val maxTop = cropRect.bottom - minCropSize
        cropRect.top = newTop.coerceIn(0f, maxTop)
    }

    private fun resizeBottom(dy: Float) {
        val newBottom = cropRect.bottom + dy
        val minBottom = cropRect.top + minCropSize
        cropRect.bottom = newBottom.coerceIn(minBottom, height.toFloat())
    }

    fun getCropRectInOverlay(): RectF {
        // returns current crop rectangle in overlay (view) coordinates
        return RectF(cropRect) // copy, so external code can't modify internal rect
    }
}