package ru.glamy.onlinetoggleview

import android.content.Context
import android.support.animation.DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
import android.support.animation.FloatValueHolder
import android.support.animation.SpringAnimation
import android.support.animation.SpringForce
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import ru.glamy.onlinetoggleview.databinding.ViewToggleBinding

class ToggleView : FrameLayout {

    private lateinit var bnd: ViewToggleBinding
    private lateinit var viewConfiguration: ViewConfiguration
    private val leftLimit: Int = 0
    private val rightLimit: Int = screenWidth()
    private var isScrolling: Boolean = false
    private var moveStartedPosition: Float = 0f
    private var scrollCurrentPosition = 0f
    private var friction = 1f
    private var xVelocity = 0f
    private var velocityTracker: VelocityTracker? = null
    private var xFling: SpringAnimation? = null
    private var clipBoundsOffset: Int = 0

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
        clipBoundsOffset = (20 * resources.displayMetrics.density).toInt()

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

        val action = ev?.action ?: throw NullPointerException()

        Log.d(ToggleView::class.java.simpleName + " onInterceptTouchEvent",
                "isScrolling $isScrolling, action ${MotionEvent.actionToString(action)}")

        // Always handle the case of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the scroll.
            isScrolling = false
            return false // Do not intercept touch event, let the child handle it
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                moveStartedPosition = ev.x
            }
        }

        // In general, we don't want to intercept touch events. They should be
        // handled by the child view.
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(ToggleView::class.java.simpleName + " onTouchEvent",
                "isScrolling $isScrolling, action ${MotionEvent.actionToString(event.action)}")

        if (!isScrolling && !isWithinBounds(bnd.leftPart, event) && !isWithinBounds(bnd.rightPart, event)) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                stopFlingAnimations()
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker?.clear()
                }
                velocityTracker?.addMovement(event)
                scrollCurrentPosition = event.x
            }
            MotionEvent.ACTION_MOVE -> {

                if (friction < MAX_FRICTION) {
                    velocityTracker?.let {
                        it.addMovement(event)
                        it.computeCurrentVelocity(500, 10000f)
                        xVelocity = it.getXVelocity(event.getPointerId(0))
                    }
                }
                isScrolling = true
                handleOnMoveEvent(event)
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.recycle()
                velocityTracker = null
                isScrolling = false
                if (friction < MAX_FRICTION) {
                    startXFlingAnimation(xVelocity < 0)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                isScrolling = false
                if (friction < MAX_FRICTION) {
                    startXFlingAnimation(xVelocity < 0)
                }
            }
        }
        return true
    }

    private fun handleOnMoveEvent(event: MotionEvent) {
        val delta = event.x - scrollCurrentPosition
        val validDelta = calculateValidDelta(bnd.rightPart.left.toFloat(), delta)
        moveView(bnd.leftPart, validDelta)
        moveView(bnd.rightPart, validDelta)
        scrollCurrentPosition = event.x
    }

    private fun startXFlingAnimation(springToLeft: Boolean) {
        val springFinalPosition = calculateSpringFinalPosition(springToLeft)
        xFling = SpringAnimation(FloatValueHolder(bnd.rightPart.left.toFloat()))
                .setMinimumVisibleChange(MIN_VISIBLE_CHANGE_PIXELS)
                .setSpring(SpringForce(springFinalPosition)
                        .apply {
                            stiffness = SpringForce.STIFFNESS_LOW
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        })
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

    private fun isWithinBounds(view: View, ev: MotionEvent): Boolean {
        val xPoint = Math.round(ev.rawX)
        val yPoint = Math.round(ev.rawY)
        val l = IntArray(2)
        view.getLocationOnScreen(l)
        val x = l[0] - clipBoundsOffset
        val y = l[1] - clipBoundsOffset
        val w = view.width + clipBoundsOffset * 2
        val h = view.height + clipBoundsOffset * 2
        return !(xPoint < x || xPoint > x + w || yPoint < y || yPoint > y + h)
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
