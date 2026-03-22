package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

/**
 * An overlay view that covers the right half of the screen and draws
 * an identical copy of the left-half browser content for SBS (Side-By-Side) mode.
 *
 * The captured bitmap spans [0, halfWidth - inwardMarginPx] of the left pane.
 * When drawn, it is placed at [inwardMarginPx, halfWidth] inside this overlay so
 * a black inward margin of [inwardMarginPx] appears on the inner (left) edge of the
 * right eye pane — symmetrically matching the black bar on the right edge of the
 * left pane.  No horizontal flip is applied; the content is an identical copy.
 */
class SbsCopyOverlayView(context: Context) : View(context) {

    private val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private var copyBitmap: Bitmap? = null
    private val bitmapLock = Any()

    /** Inward margin in pixels — must match the value used during capture. */
    var inwardMarginPx: Int = 0

    // Pre-allocated to avoid allocations during draw
    private val srcRect = Rect()
    private val dstRect = RectF()

    /** Update the bitmap to display. The view takes ownership and will recycle the old one. */
    fun updateCopyBitmap(bmp: Bitmap) {
        val oldBitmap: Bitmap?
        synchronized(bitmapLock) {
            oldBitmap = copyBitmap
            copyBitmap = bmp
        }
        oldBitmap?.recycle()
        postInvalidate()
    }

    /** Release bitmap resources. Call when the view is no longer needed. */
    fun release() {
        val bmp: Bitmap?
        synchronized(bitmapLock) {
            bmp = copyBitmap
            copyBitmap = null
        }
        bmp?.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        // Black background for the entire right half (covers inward margin + any remainder)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val bmp: Bitmap?
        synchronized(bitmapLock) { bmp = copyBitmap }

        bmp?.let { bitmap ->
            if (!bitmap.isRecycled) {
                srcRect.set(0, 0, bitmap.width, bitmap.height)
                // Offset dst by inwardMarginPx so the inner edge of the right pane is black.
                // The bitmap width equals (overlayWidth - inwardMarginPx), giving a 1:1 pixel
                // mapping (no stretching).
                dstRect.set(inwardMarginPx.toFloat(), 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
