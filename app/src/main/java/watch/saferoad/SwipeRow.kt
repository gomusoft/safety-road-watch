package watch.saferoad

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.min

/** A row that follows a sideways swipe. Swiped over a third of its width, it calls [onSwiped]; otherwise it slides back. */
class SwipeRow(context: Context, private val onSwiped: () -> Unit) : LinearLayout(context) {
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    init {
        isClickable = true // so a swipe that starts between the buttons still reaches us
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (!dragging && abs(dx) > slop * 2 && abs(dx) > abs(dy) * 2) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true) // do not let the list scroll while swiping
                }
            }
        }
        return dragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                translationX = ev.rawX - downX
                alpha = 1f - 0.6f * min(abs(translationX) / width, 1f)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val far = ev.actionMasked == MotionEvent.ACTION_UP && abs(translationX) > width / 3f
                dragging = false
                animate().translationX(0f).alpha(1f).setDuration(150).start()
                if (far) onSwiped()
            }
        }
        return true
    }
}
