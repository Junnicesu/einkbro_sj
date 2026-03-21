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
 * a horizontally-mirrored bitmap of the left half content for SBS (Side-By-Side) mode.
 *
 * The left inward margin is baked into the captured bitmap (captured area is
 * [0, halfWidth - inwardMarginPx], so the black margin bar on the left half's
 * inner edge appears on the right edge when mirrored → appears on the LEFT edge
 * of the mirrored image, providing the right eye's inward margin automatically).
 */
class SbsMirrorOverlayView(context: Context) : View(context) {

    private val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private var mirrorBitmap: Bitmap? = null
    private val bitmapLock = Any()

    // Pre-allocated to avoid allocations during draw
    private val srcRect = Rect()
    private val dstRect = RectF()

    /** Update the bitmap to display. The view takes ownership and will recycle it. */
    fun updateMirrorBitmap(bmp: Bitmap) {
        val oldBitmap: Bitmap?
        synchronized(bitmapLock) {
            oldBitmap = mirrorBitmap
            mirrorBitmap = bmp
        }
        oldBitmap?.recycle()
        postInvalidate()
    }

    /** Release bitmap resources. Call when the view is no longer needed. */
    fun release() {
        val bmp: Bitmap?
        synchronized(bitmapLock) {
            bmp = mirrorBitmap
            mirrorBitmap = null
        }
        bmp?.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        // Black background for the entire right half
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val bmp: Bitmap?
        synchronized(bitmapLock) { bmp = mirrorBitmap }

        bmp?.let { bitmap ->
            if (!bitmap.isRecycled) {
                srcRect.set(0, 0, bitmap.width, bitmap.height)
                dstRect.set(0f, 0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}
