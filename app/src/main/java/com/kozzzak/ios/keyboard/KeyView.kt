package com.kozzzak.ios.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.view.Gravity
import android.graphics.drawable.GradientDrawable

/** A real key, with cancellable repeat and space-trackpad. Never retains editor text. */
@SuppressLint("ViewConstructor")
class KeyView(
    context: Context, var label: String, private val action: String,
    private val dark: Boolean, private val special: Boolean,
    private val preview: Boolean, private val callback: (String) -> Unit,
    private val feedback: () -> Unit, private val cursor: (Int) -> Unit,
    private val alternate: (() -> Unit)? = null
) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyRect = RectF()
    private val shadowRect = RectF()
    private val iconRect = RectF()
    private val iconPath = Path()
    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var lastX = 0f
    private var holding = false
    private var consumed = false
    private var popup: PopupWindow? = null
    private val repeat = object : Runnable {
        override fun run() { if (isPressed) { consumed = true; callback(action); handler.postDelayed(this, 65) } }
    }
    private val longPress = Runnable {
        if (isPressed) {
            if (action == "SPACE") { holding = true; feedback(); invalidate() }
            else if (alternate != null) { consumed = true; dismissPreview(); alternate.invoke() }
        }
    }
    init {
        isClickable = true
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = when (action) {
            "SPACE" -> "Пробел; удерживайте для курсора"
            "DELETE" -> "Удалить"
            "SHIFT" -> "Shift"
            "LANG" -> "Сменить язык; удерживайте для выбора клавиатуры"
            "HIDE" -> "Скрыть клавиатуру"
            "EMOJI" -> "Эмодзи"
            "ENTER" -> label
            else -> label
        }
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: android.view.accessibility.AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Button"
            }
        }
    }
    override fun onDraw(canvas: Canvas) {
        val r = keyRect.apply { set(3*density, 3*density, width-3*density, height-5*density) }
        paint.color = if (dark) Color.rgb(26,26,28) else Color.rgb(145,148,153)
        shadowRect.set(r.left,r.top+1.5f*density,r.right,r.bottom+1.5f*density)
        canvas.drawRoundRect(shadowRect,5*density,5*density,paint)
        paint.color = when { isPressed -> if (dark) Color.rgb(130,130,135) else Color.rgb(190,195,204)
            special -> if (dark) Color.rgb(68,68,73) else Color.rgb(171,178,188)
            else -> if (dark) Color.rgb(105,105,110) else Color.WHITE }
        canvas.drawRoundRect(r,5*density,5*density,paint)
        paint.color = if (dark) Color.WHITE else Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = (if (label.length > 3) 14f else if (special) 21f else 24f)*density
        if (!holding && drawIcon(canvas)) return
        val shown = if (holding) "↔" else label
        canvas.drawText(shown, width/2f, height/2f - (paint.ascent()+paint.descent())/2f-2*density,paint)
    }
    private fun drawIcon(canvas: Canvas): Boolean {
        if (action!="LANG" && action!="EMOJI" && action!="SHIFT" && action!="DELETE" && action!="HIDE") return false
        val x=width/2f; val y=height/2f-2*density; val u=density
        paint.style=Paint.Style.STROKE;paint.strokeWidth=1.5f*u;paint.strokeCap=Paint.Cap.ROUND
        when(action) {
            "LANG" -> {
                canvas.drawCircle(x,y,10*u,paint)
                iconRect.set(x-4.5f*u,y-10*u,x+4.5f*u,y+10*u)
                canvas.drawOval(iconRect,paint)
                canvas.drawLine(x-9*u,y,x+9*u,y,paint)
                canvas.drawLine(x-8*u,y-5*u,x+8*u,y-5*u,paint)
                canvas.drawLine(x-8*u,y+5*u,x+8*u,y+5*u,paint)
            }
            "EMOJI" -> {
                canvas.drawCircle(x,y,10*u,paint)
                iconRect.set(x-6*u,y-5*u,x+6*u,y+6*u)
                canvas.drawArc(iconRect,10f,160f,false,paint)
                paint.style=Paint.Style.FILL
                canvas.drawCircle(x-3.5f*u,y-3*u,1*u,paint);canvas.drawCircle(x+3.5f*u,y-3*u,1*u,paint)
            }
            "SHIFT" -> {
                val path=iconPath.apply {reset();moveTo(x,y-10*u);lineTo(x+10*u,y);lineTo(x+5*u,y);lineTo(x+5*u,y+8*u);lineTo(x-5*u,y+8*u);lineTo(x-5*u,y);lineTo(x-10*u,y);close()}
                if(label=="⇪") paint.style=Paint.Style.FILL
                canvas.drawPath(path,paint)
                if(label=="⇪") canvas.drawLine(x-5*u,y+11*u,x+5*u,y+11*u,paint)
            }
            "DELETE" -> {
                val path=iconPath.apply {reset();moveTo(x-12*u,y);lineTo(x-5*u,y-8*u);lineTo(x+11*u,y-8*u);lineTo(x+11*u,y+8*u);lineTo(x-5*u,y+8*u);close()}
                canvas.drawPath(path,paint)
                canvas.drawLine(x-2*u,y-3*u,x+4*u,y+3*u,paint);canvas.drawLine(x+4*u,y-3*u,x-2*u,y+3*u,paint)
            }
            "HIDE" -> {canvas.drawLine(x-5*u,y-2*u,x,y+3*u,paint);canvas.drawLine(x,y+3*u,x+5*u,y-2*u,paint)}
        }
        paint.style=Paint.Style.FILL;paint.strokeCap=Paint.Cap.BUTT
        return true
    }
    // getLocationInWindow returns physical coordinates, independent of layout direction.
    @SuppressLint("RtlHardcoded")
    private fun showPreview() {
        if (!preview || action.length != 1) return
        val tv = TextView(context).apply {
            text = label; textSize = 32f; gravity = Gravity.CENTER
            setTextColor(if (dark) Color.WHITE else Color.BLACK)
            background = GradientDrawable().apply { setColor(if(dark) Color.rgb(110,110,115) else Color.WHITE); cornerRadius=8*density }
            elevation = 6*density
        }
        val location = IntArray(2); getLocationInWindow(location)
        popup = PopupWindow(tv, (width*1.5).toInt(), (70*density).toInt(), false).apply {
            isClippingEnabled = true
            showAtLocation(this@KeyView, Gravity.TOP or Gravity.LEFT, location[0]-width/4, location[1]-(65*density).toInt())
        }
    }
    private fun dismissPreview() { popup?.dismiss(); popup = null }
    fun cancelTouch() { handler.removeCallbacksAndMessages(null); isPressed=false; holding=false; consumed=true; dismissPreview(); invalidate() }
    override fun onDetachedFromWindow() { cancelTouch(); super.onDetachedFromWindow() }
    override fun performClick(): Boolean { super.performClick(); callback(action); return true }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; lastX = downX; consumed=false; holding=false; isPressed=true
                feedback(); showPreview(); invalidate()
                if (action == "DELETE") { callback(action); consumed=true; handler.postDelayed(repeat,380) }
                else handler.postDelayed(longPress,420)
            }
            MotionEvent.ACTION_MOVE -> {
                if (action == "SPACE" && (holding || kotlin.math.abs(event.x-downX)>18*density)) {
                    holding=true; consumed=true; handler.removeCallbacks(longPress)
                    val steps=((event.x-lastX)/(12*density)).toInt()
                    if (steps != 0) { cursor(steps.coerceIn(-8,8)); lastX+=steps*12*density }; invalidate()
                } else if (event.x < -20*density || event.x > width+20*density || event.y < -40*density || event.y > height+30*density) cancelTouch()
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacksAndMessages(null); dismissPreview()
                if (isPressed && !consumed && !holding) performClick()
                isPressed=false; holding=false; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> cancelTouch()
        }
        return true
    }
}
