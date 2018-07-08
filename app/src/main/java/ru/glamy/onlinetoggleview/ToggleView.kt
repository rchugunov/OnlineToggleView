package ru.glamy.onlinetoggleview

import android.content.Context
import android.support.animation.DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
import android.support.animation.FloatValueHolder
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.util.AttributeSet
import android.view.*
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
    var friction = 1f
    private var circleX = 0f
    private var xVelocity = 0f
    private var velocityTracker: VelocityTracker? = null
    private var xFling: SpringAnimation? = null

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
                stopFlingAnimations()
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)

                scrollStartedPosition = event.x
                scrollCurrentPosition = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {

                if (friction < MAX_FRICTION) {
                    velocityTracker?.let {
                        it.addMovement(event)
                        it.computeCurrentVelocity(500, 10000f)
                        xVelocity = it.getXVelocity(event.getPointerId(0))
                    }
                }

                val delta = event.x - scrollCurrentPosition
                val validDelta = calculateValidDelta(bnd.rightPart.left.toFloat(), delta)
                moveView(bnd.leftPart, validDelta)
                moveView(bnd.rightPart, validDelta)
                scrollCurrentPosition = event.x
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.recycle()
                velocityTracker = null
                if (friction < MAX_FRICTION) {
                    startXFlingAnimation(xVelocity < 0)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null

            }
        }
        return true
    }

    private fun startXFlingAnimation(springToLeft: Boolean) {
        val springFinalPosition = calculateSpringFinalPosition(springToLeft)
        xFling = SpringAnimation(FloatValueHolder(bnd.rightPart.left.toFloat()))
//                .setStartVelocity(xVelocity)
//                .setMaxValue(rightLimit.toFloat())
//                .setMinValue(leftLimit.toFloat())
                .setMinimumVisibleChange(MIN_VISIBLE_CHANGE_PIXELS)
                .setSpring(SpringForce(springFinalPosition)
                        .apply {
                            stiffness = SpringForce.STIFFNESS_LOW
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        })
//                .setFriction(friction)
                .apply {
                    addUpdateListener({ _, newX, _ ->
                        moveView(bnd.leftPart, newX - bnd.rightPart.left)
                        moveView(bnd.rightPart, newX - bnd.rightPart.left)
                    })
                    addEndListener({ _, canceled, _, velocity ->
                        if (!canceled && Math.abs(velocity) > 0) {
                            xVelocity = -velocity
                            startXFlingAnimation(xVelocity < 0)
                        }
                    })
                    start()
                }
    }

    private fun calculateSpringFinalPosition(springToLeft: Boolean): Float {
        return if (springToLeft) leftLimit.toFloat() else rightLimit.toFloat()
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

    private fun stopFlingAnimations() {
        xFling?.cancel()
        xFling = null
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        initialOffsets()
    }

    private fun moveView(view: View, delta: Float) {
        view.offsetLeftAndRight(delta.toInt())
    }

    companion object {
        val MAX_FRICTION = Float.MAX_VALUE
    }
}
