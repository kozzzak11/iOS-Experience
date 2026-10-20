package com.kozzzak.ios

import android.content.Intent
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.kozzzak.ios.keyboard.KeyboardPrefs
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** System-bound IME test: real editor, real IME window and UI-injected key presses. */
@RunWith(AndroidJUnit4::class)
class KeyboardIntegrationTest {
    @Test fun typeSwitchLanguageAndHide() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        KeyboardPrefs(context).apply {reset();language="en";set("auto_caps",false)}
        val device=UiDevice.getInstance(inst)
        val previous=device.executeShellCommand("settings get secure default_input_method").trim()
        val showWithHardware=device.executeShellCommand("settings get secure show_ime_with_hard_keyboard").trim()
        val ime="com.kozzzak.ios/.keyboard.IosInputMethodService"
        try {
            device.wakeUp()
            device.executeShellCommand("wm dismiss-keyguard")
            device.executeShellCommand("settings put secure show_ime_with_hard_keyboard 1")
            device.executeShellCommand("ime enable $ime")
            device.executeShellCommand("ime set $ime")
            val activity=inst.startActivitySync(Intent(context,InputProbeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            inst.waitForIdleSync()
            var editor:EditText?=null
            inst.runOnMainSync {
                val field=checkNotNull(activity.findViewById<EditText>(R.id.probe_text)) {"Test editor was not created"}
                editor=field
                field.requestFocus()
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(field,InputMethodManager.SHOW_IMPLICIT)
            }
            assertTrue("English IME did not become visible",device.wait(Until.hasObject(By.desc("h")),15000))
            "hello".forEach {device.findObject(By.desc(it.toString())).click()}
            device.findObject(By.desc("Пробел; удерживайте для курсора")).click()
            inst.waitForIdleSync()
            var actual=""
            inst.runOnMainSync {actual=editor!!.text.toString()}
            assertEquals("hello ",actual)
            device.findObject(By.desc("Сменить язык; удерживайте для выбора клавиатуры")).click()
            assertTrue("Russian IME did not become visible",device.wait(Until.hasObject(By.desc("п")),5000))
            "привет".forEach {device.findObject(By.desc(it.toString())).click()}
            device.findObject(By.desc("Пробел; удерживайте для курсора")).click()
            inst.waitForIdleSync()
            inst.runOnMainSync {actual=editor!!.text.toString()}
            assertEquals("hello привет ",actual)
            device.findObject(By.desc("Скрыть клавиатуру")).click()
            assertTrue("Hide button left the keyboard visible",device.wait(Until.gone(By.desc("Пробел; удерживайте для курсора")),5000))
        } catch(failure:Throwable) {
            val directory=File(context.getExternalFilesDir(null),"ime-diagnostics").apply {mkdirs()}
            runCatching {device.takeScreenshot(File(directory,"failure.png"))}
            val hierarchy=runCatching {
                ByteArrayOutputStream().use {out -> device.dumpWindowHierarchy(out);out.toString("UTF-8")}
            }.getOrElse {"Hierarchy unavailable: ${it.message}"}
            File(directory,"hierarchy.xml").writeText(hierarchy)
            throw AssertionError("${failure.message}\n$hierarchy",failure)
        } finally {
            if(previous.contains('/')) device.executeShellCommand("ime set $previous")
            if(!previous.startsWith(context.packageName+"/")) device.executeShellCommand("ime disable $ime")
            if(showWithHardware=="null") device.executeShellCommand("settings delete secure show_ime_with_hard_keyboard")
            else if(showWithHardware in listOf("0","1")) device.executeShellCommand("settings put secure show_ime_with_hard_keyboard $showWithHardware")
        }
    }
}
