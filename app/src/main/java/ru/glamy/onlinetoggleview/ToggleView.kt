package ru.glamy.onlinetoggleview

import android.content.Context
import android.graphics.Rect
import android.support.v4.view.ViewCompat
import android.support.v4.view.WindowInsetsCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import ru.glamy.onlinetoggleview.databinding.ViewToggleBinding

class ToggleView : FrameLayout {

    private lateinit var bnd: ViewToggleBinding

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
        clipChildren = false
        addView(bnd.root)

        ViewCompat.setOnApplyWindowInsetsListener(this,
                object : android.support.v4.view.OnApplyWindowInsetsListener {
                    private val mTempRect = Rect()

                    override fun onApplyWindowInsets(v: View,
                                                     originalInsets: WindowInsetsCompat): WindowInsetsCompat {
                        // First let the ViewPager itself try and consume them...
                        val applied = ViewCompat.onApplyWindowInsets(v, originalInsets)
                        if (applied.isConsumed) {
                            // If the ViewPager consumed all insets, return now
                            return applied
                        }

                        // Now we'll manually dispatch the insets to our children. Since ViewPager
                        // children are always full-height, we do not want to use the standard
                        // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them,
                        // the rest of the children will not receive any insets. To workaround this
                        // we manually dispatch the applied insets, not allowing children to
                        // consume them from each other. We do however keep track of any insets
                        // which are consumed, returning the union of our children's consumption
                        val res = mTempRect
                        res.left = applied.systemWindowInsetLeft
                        res.top = applied.systemWindowInsetTop
                        res.right = applied.systemWindowInsetRight
                        res.bottom = applied.systemWindowInsetBottom

                        var i = 0
                        val count = childCount
                        while (i < count) {
                            val childInsets = ViewCompat
                                    .dispatchApplyWindowInsets(getChildAt(i), applied)
                            // Now keep track of any consumed by tracking each dimension's min
                            // value
                            res.left = Math.min(childInsets.systemWindowInsetLeft,
                                    res.left)
                            res.top = Math.min(childInsets.systemWindowInsetTop,
                                    res.top)
                            res.right = Math.min(childInsets.systemWindowInsetRight,
                                    res.right)
                            res.bottom = Math.min(childInsets.systemWindowInsetBottom,
                                    res.bottom)
                            i++
                        }

                        // Now return a new WindowInsets, using the consumed window insets
                        return applied.replaceSystemWindowInsets(
                                res.left, res.top, res.right, res.bottom)
                    }
                })
    }

    private fun screenWidth(): Int =
            resources.displayMetrics.widthPixels

    private fun layoutInflater(): LayoutInflater =
            LayoutInflater.from(context)

    private var startedPosition: Float = 0f

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startedPosition = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                moveView(event.x)
                startedPosition = event.x
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

    private fun moveView(x: Float) {
        bnd.root.offsetLeftAndRight(x.toInt() - startedPosition.toInt())
    }
}
