package ru.glamy.onlinetoggleview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import ru.glamy.onlinetoggleview.databinding.ViewToggleBinding

class ToggleView : FrameLayout {

    private lateinit var bnd: ViewToggleBinding
    private lateinit var viewConfiguration: ViewConfiguration
    private val leftLimit: Int = 0
    private val rightLimit: Int = screenWidth()
    private var mIsScrolling: Boolean = false
    private var moveStartedPosition: Float = 0f
    private var scrollStartedPosition = 0f
    private var scrollCurrentPosition = 0f

    constructor(context: Context) : super(context) {

        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {

        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        init()
    }

    private fun init() {
        bnd = ViewToggleBinding.inflate(layoutInflater(), this, false)
        bnd.rightPart.layoutParams.width = screenWidth()
        viewConfiguration = ViewConfiguration.get(context)
        addView(bnd.root)

        initialOffsets()
    }

    private fun initialOffsets() {
        moveView(bnd.leftPart, (screenWidth() - bnd.leftPart.layoutParams.width).toFloat())
        moveView(bnd.rightPart, screenWidth().toFloat())
    }

    private fun calculateDistanceX(ev: MotionEvent): Int = ev.x.toInt() - moveStartedPosition.toInt()

    private fun screenWidth(): Int = resources.displayMetrics.widthPixels

    private fun layoutInflater(): LayoutInflater = LayoutInflater.from(context)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        val action = ev?.action

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            mIsScrolling = false
            return false // Do not intercept touch event, let the child handle it
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                moveStartedPosition = ev.x
            }
            MotionEvent.ACTION_MOVE -> {
                if (mIsScrolling) {
                    // We're currently scrolling, so yes, intercept the
                    // touch event!
                    return true
                }

                // If the user has dragged her finger horizontally more than
                // the touch slop, start the scroll

                // left as an exercise for the reader
                val xDiff = calculateDistanceX(ev)

                // Touch slop should be calculated using ViewConfiguration
                // constants.
                if (xDiff > viewConfiguration.scaledTouchSlop) {
                    // Start scrolling!
                    mIsScrolling = true
                    return true
                }
            }
        }

        // In general, we don't want to intercept touch events. They should be
        // handled by the child view.
        return false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                scrollStartedPosition = event.x
                scrollCurrentPosition = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = event.x - scrollCurrentPosition
                val validDelta = calculateValidDelta(bnd.rightPart.left.toFloat(), delta)
                moveView(bnd.leftPart, validDelta)
                moveView(bnd.rightPart, validDelta)
                scrollCurrentPosition = event.x
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true

            }
            else -> {
                return super.onTouchEvent(event)
            }
        }
    }

    private fun calculateValidDelta(viewLeft: Float, delta: Float): Float {
        var resultDelta = delta
        if (viewLeft + delta > rightLimit) {
            resultDelta = rightLimit - viewLeft
        }

        if (viewLeft + delta < leftLimit) {
            resultDelta = leftLimit - viewLeft
        }

        return resultDelta
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        initialOffsets()
    }

    private fun moveView(view: View, delta: Float) {
        view.offsetLeftAndRight(delta.toInt())
    }
}
