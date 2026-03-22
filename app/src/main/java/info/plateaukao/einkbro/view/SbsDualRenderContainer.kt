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
 * child twice in one render pass (left eye + right eye copy) without bitmap capture.
 *
 * No inward margin is applied: the left pane occupies [0, w/2] and the right pane
 * occupies [w/2, w].  Both panes start exactly at their respective left edges, so
 * the content viewport origin (x=0 of the child) is at the left edge of each pane —
 * ensuring perfect optical alignment through Google Cardboard lenses.
 */
class SbsDualRenderContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    private var sbsCopyEnabled = false
    private var edgeSafeMarginPx = 30
    private var centerDividerPx = 2
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }

    fun setSbsCopyEnabled(enabled: Boolean) {
        if (sbsCopyEnabled == enabled) return
        sbsCopyEnabled = enabled
        requestLayout()
        invalidate()
    }

    fun setSbsLayout(edgeSafeMarginPx: Int, centerDividerPx: Int = 2) {
        val newSafe = edgeSafeMarginPx.coerceAtLeast(0)
        val newDivider = centerDividerPx.coerceAtLeast(0)
        if (this.edgeSafeMarginPx == newSafe && this.centerDividerPx == newDivider) return
        this.edgeSafeMarginPx = newSafe
        this.centerDividerPx = newDivider
        requestLayout()
        invalidate()
    }

    private data class SbsMetrics(
        val leftStart: Int,
        val paneWidth: Int,
        val rightStart: Int,
        val dividerLeft: Int,
        val dividerWidth: Int,
    )

    private fun metrics(viewWidth: Int = width): SbsMetrics {
        if (!sbsCopyEnabled) {
            return SbsMetrics(0, viewWidth, 0, 0, 0)
        }

        val maxSafe = ((viewWidth - centerDividerPx) / 2 - 1).coerceAtLeast(0)
        val safe = edgeSafeMarginPx.coerceIn(0, maxSafe)
        val divider = centerDividerPx.coerceAtLeast(0)
        val available = (viewWidth - safe * 2 - divider).coerceAtLeast(2)
        val pane = (available / 2).coerceAtLeast(1)
        val leftStart = safe
        val dividerLeft = leftStart + pane
        val rightStart = dividerLeft + divider
        return SbsMetrics(leftStart, pane, rightStart, dividerLeft, divider)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth
        val height = measuredHeight
        val childWidth = if (sbsCopyEnabled) metrics(width).paneWidth else width

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
        val sbsMetrics = metrics(width)
        val childWidth = if (sbsCopyEnabled) sbsMetrics.paneWidth else width
        val leftStart = if (sbsCopyEnabled) sbsMetrics.leftStart else 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            child.layout(leftStart, 0, leftStart + childWidth, height)
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

        val sbsMetrics = metrics(w)

        // Fill non-pane areas (outer safe margins and any leftover pixels) with black.
        canvas.drawColor(Color.BLACK)

        // Left pane: child drawn at its natural position.
        canvas.save()
        canvas.clipRect(sbsMetrics.leftStart, 0, sbsMetrics.leftStart + sbsMetrics.paneWidth, h)
        super.dispatchDraw(canvas)
        canvas.restore()

        // Right pane: draw the exact same content (not mirrored) in the right viewport.
        canvas.save()
        canvas.translate((sbsMetrics.rightStart - sbsMetrics.leftStart).toFloat(), 0f)
        canvas.clipRect(sbsMetrics.leftStart, 0, sbsMetrics.leftStart + sbsMetrics.paneWidth, h)
        super.dispatchDraw(canvas)
        canvas.restore()

        if (sbsMetrics.dividerWidth > 0) {
            canvas.drawRect(
                sbsMetrics.dividerLeft.toFloat(),
                0f,
                (sbsMetrics.dividerLeft + sbsMetrics.dividerWidth).toFloat(),
                h.toFloat(),
                dividerPaint,
            )
        }
    }

    private fun isInteractiveLeftPane(x: Float): Boolean {
        if (!sbsCopyEnabled) return true
        val sbsMetrics = metrics()
        return x >= sbsMetrics.leftStart && x < (sbsMetrics.leftStart + sbsMetrics.paneWidth)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (sbsCopyEnabled && !isInteractiveLeftPane(ev.x)) {
            // Only the left pane is operable in browser SBS mode.
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (sbsCopyEnabled && !isInteractiveLeftPane(event.x)) {
            return true
        }
        return super.onTouchEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun invalidateChildInParent(location: IntArray?, dirty: Rect?): android.view.ViewParent? {
        if (sbsCopyEnabled) {
            // Force full redraw so the right pane refreshes with left-pane updates.
            invalidate()
        }
        return super.invalidateChildInParent(location, dirty)
    }
}
