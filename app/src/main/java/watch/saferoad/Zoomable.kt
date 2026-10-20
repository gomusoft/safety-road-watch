package watch.saferoad

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max

/**
 * Pinch to zoom, drag to pan while zoomed, double tap to zoom in and back out. Moves and scales [target], which must
 * be centred in [stage]; touches on [stage] drive it. Works for a TextureView and for an ImageView alike.
 */
@SuppressLint("ClickableViewAccessibility")
class Zoomable(private val stage: View, private val target: View, private val maxScale: Float = 8f) {
    private var scale = 1f
    private var tx = 0f
    private var ty = 0f
    private var lastX = 0f
    private var lastY = 0f

    private val pinch = ScaleGestureDetector(stage.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            zoomTo(scale * d.scaleFactor, d.focusX, d.focusY)
            return true
        }
    })

    private val taps = GestureDetector(stage.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (scale > 1.01f) reset() else zoomTo(3f, e.x, e.y)
            return true
        }
    })

    init {
        stage.setOnTouchListener { _, ev ->
            taps.onTouchEvent(ev)
            pinch.onTouchEvent(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                    val i = if (ev.actionMasked == MotionEvent.ACTION_POINTER_UP) (if (ev.actionIndex == 0) 1 else 0) else 0
                    lastX = ev.getX(i)
                    lastY = ev.getY(i)
                }
                MotionEvent.ACTION_MOVE -> if (ev.pointerCount == 1 && !pinch.isInProgress && scale > 1f) {
                    tx += ev.x - lastX
                    ty += ev.y - lastY
                    lastX = ev.x
                    lastY = ev.y
                    apply()
                }
            }
            true
        }
    }

    fun reset() {
        scale = 1f
        tx = 0f
        ty = 0f
        apply()
    }

    /** Zoom to [to] keeping the point (x, y) of the stage where it is under the fingers. */
    private fun zoomTo(to: Float, x: Float, y: Float) {
        val new = to.coerceIn(1f, maxScale)
        val fx = x - stage.width / 2f
        val fy = y - stage.height / 2f
        tx = fx - (fx - tx) * (new / scale)
        ty = fy - (fy - ty) * (new / scale)
        scale = new
        apply()
    }

    private fun apply() {
        val mx = max(0f, (target.width * scale - stage.width) / 2f)
        val my = max(0f, (target.height * scale - stage.height) / 2f)
        tx = tx.coerceIn(-mx, mx)
        ty = ty.coerceIn(-my, my)
        target.pivotX = target.width / 2f
        target.pivotY = target.height / 2f
        target.scaleX = scale
        target.scaleY = scale
        target.translationX = tx
        target.translationY = ty
    }
}
