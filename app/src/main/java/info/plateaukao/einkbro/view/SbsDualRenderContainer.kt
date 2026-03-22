package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Hosts the normal browser content as child 0 and, when enabled, draws that same
 * child twice in one pass (left eye + right eye copy) without bitmap capture.
 */
class SbsDualRenderContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    private var sbsCopyEnabled = false
    private var inwardMarginPx = 0
    private val blackPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    fun setSbsCopyEnabled(enabled: Boolean, inwardMarginPx: Int) {
        val normalizedMargin = inwardMarginPx.coerceAtLeast(0)
        if (sbsCopyEnabled == enabled && this.inwardMarginPx == normalizedMargin) return

        sbsCopyEnabled = enabled
        this.inwardMarginPx = normalizedMargin
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        val height = measuredHeight
        val childWidth = if (sbsCopyEnabled) (width / 2).coerceAtLeast(1) else width

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            )
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val childWidth = if (sbsCopyEnabled) (width / 2).coerceAtLeast(1) else width

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            child.layout(0, 0, childWidth, height)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (!sbsCopyEnabled || childCount == 0) {
            super.dispatchDraw(canvas)
            return
        }

        val w = width
        val h = height
        if (w <= 1 || h <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        val half = w / 2
        val safeMargin = inwardMarginPx.coerceAtMost(half)
        val visibleContentWidth = (half - safeMargin).coerceAtLeast(1)

        // Fill the whole surface first so uncovered areas are black.
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), blackPaint)

        // Draw the normal browser content in the left pane.
        canvas.save()
        canvas.clipRect(0, 0, half, h)
        super.dispatchDraw(canvas)
        canvas.restore()

        // Paint symmetric inward margins around the center split.
        val centerStart = (half - safeMargin).coerceAtLeast(0)
        val centerEnd = (half + safeMargin).coerceAtMost(w)
        canvas.drawRect(centerStart.toFloat(), 0f, centerEnd.toFloat(), h.toFloat(), blackPaint)

        // Draw an identical, non-flipped copy into the right pane.
        val rightStart = (half + safeMargin).coerceAtMost(w)
        val rightWidth = visibleContentWidth.coerceAtMost(w - rightStart)
        if (rightWidth > 0) {
            canvas.save()
            canvas.translate(rightStart.toFloat(), 0f)
            canvas.clipRect(0, 0, rightWidth, h)
            super.dispatchDraw(canvas)
            canvas.restore()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (sbsCopyEnabled && ev.x >= width / 2f) {
            // Right pane is visual-only copy; swallow input.
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (sbsCopyEnabled && event.x >= width / 2f) {
            return true
        }
        return super.onTouchEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): android.view.ViewParent? {
        if (sbsCopyEnabled) {
            // Force full redraw so mirrored right pane refreshes with left-pane updates.
            invalidate()
        }
        return super.invalidateChildInParent(location, dirty)
    }
}

