package com.kozzzak.ios.keyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardRenderingTest {
    @Test fun lightAndDarkKeyboardRenderAtPhoneWidth() {
        listOf("light","dark").forEach { theme ->
            val controller=Robolectric.buildService(IosInputMethodService::class.java).create()
            val ime=controller.get()
            KeyboardPrefs(ime).apply {reset();language="ru";this.theme=theme}
            ime.onStartInput(EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT},false)
            val view=ime.onCreateInputView()
            val width=ime.resources.displayMetrics.widthPixels
            view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
            view.layout(0,0,view.measuredWidth,view.measuredHeight)
            assertEquals(width,view.measuredWidth)
            assertTrue(view.measuredHeight>0)
            val bitmap=Bitmap.createBitmap(view.measuredWidth,view.measuredHeight,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output=File("build/reports/visual/keyboard-$theme.png");output.parentFile?.mkdirs()
            output.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle();controller.destroy()
        }
    }
}
