package com.kozzzak.ios.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.WindowInsets
import android.widget.LinearLayout
import kotlin.math.abs

// Programmatic gesture container; individual KeyViews provide accessible click targets.
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class KeyboardSurface(context: Context, private val canDismiss: () -> Boolean, private val dismiss: () -> Unit) : LinearLayout(context) {
    private var startX=0f
    private var startY=0f
    private var tracking=false
    private val d=resources.displayMetrics.density
    private var navigationBottom=0
    private val contentBottom=(6*d).toInt()
    init {
        layoutDirection=LAYOUT_DIRECTION_LTR; orientation=VERTICAL; isMotionEventSplittingEnabled=true
        setPadding((2*d).toInt(),(4*d).toInt(),(2*d).toInt(),contentBottom)
        // IME windows extend behind the bottom navigation bar on modern Android.
        // Only reserve navigation space, never the IME inset (our own height).
        setOnApplyWindowInsetsListener { _, insets ->
            navigationBottom=if(Build.VERSION.SDK_INT>=30)
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            else @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            setPadding(paddingLeft,paddingTop,paddingRight,contentBottom+navigationBottom)
            insets
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestApplyInsets()
    }
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { startX=event.x; startY=event.y; tracking=false }
            MotionEvent.ACTION_MOVE -> if (canDismiss() && event.pointerCount == 1 && event.y-startY > 64*d && abs(event.x-startX) < (event.y-startY)/2) {
                tracking=true; dismiss(); return true
            }
        }
        return super.onInterceptTouchEvent(event)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean = tracking || super.onTouchEvent(event)
}
