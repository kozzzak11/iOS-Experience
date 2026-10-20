package com.kozzzak.ios

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.kozzzak.ios.keyboard.KeyboardPrefs
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires an emulator/device; unlike the JVM tests this exercises the system-bound IME window. */
@RunWith(AndroidJUnit4::class)
class KeyboardIntegrationTest {
    @Test fun typeSwitchLanguageAndHide() {
        val inst=InstrumentationRegistry.getInstrumentation()
        val context=inst.targetContext
        KeyboardPrefs(context).apply {reset();language="en";set("auto_caps",false)}
        val device=UiDevice.getInstance(inst)
        val previous=device.executeShellCommand("settings get secure default_input_method").trim()
        val ime="com.kozzzak.ios/.keyboard.IosInputMethodService"
        try {
            device.executeShellCommand("ime enable $ime");device.executeShellCommand("ime set $ime")
            context.startActivity(Intent(context,InputProbeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            assertTrue(device.wait(Until.hasObject(By.res(context.packageName,"probe_text")),10000))
            device.findObject(By.res(context.packageName,"probe_text")).click()
            assertTrue(device.wait(Until.hasObject(By.desc("h")),10000))
            "hello".forEach {device.findObject(By.desc(it.toString())).click()}
            device.findObject(By.desc("Пробел; удерживайте для курсора")).click()
            assertEquals("hello ",device.findObject(By.res(context.packageName,"probe_text")).text)
            device.findObject(By.desc("Сменить язык; удерживайте для выбора клавиатуры")).click()
            assertTrue(device.wait(Until.hasObject(By.desc("п")),5000))
            "привет".forEach {device.findObject(By.desc(it.toString())).click()}
            device.findObject(By.desc("Пробел; удерживайте для курсора")).click()
            assertEquals("hello привет ",device.findObject(By.res(context.packageName,"probe_text")).text)
            device.findObject(By.desc("Скрыть клавиатуру")).click()
            assertTrue(device.wait(Until.gone(By.desc("Пробел; удерживайте для курсора")),5000))
        } finally {
            if(previous.contains('/')) device.executeShellCommand("ime set $previous")
            device.executeShellCommand("ime disable $ime")
        }
    }
}
