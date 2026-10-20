package com.kozzzak.ios.keyboard

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class EmojiCatalogTest {
    private val catalog=EmojiCatalog(RuntimeEnvironment.getApplication())
    @Test fun fullCatalogAndSearch() {
        assertEquals(3944,catalog.all.size)
        assertEquals(catalog.all.size,catalog.all.map {it.text}.distinct().size)
        assertTrue(catalog.visible("","сердце",emptyList()).any {it.name.contains("heart")})
        assertEquals("😀",catalog.visible("Недавние","",listOf("😀")).single().text)
    }
    @Test fun skinToneVariantsKeepTheOriginalSequence() {
        val waving=catalog.all.first {it.text=="👋"}
        assertEquals(5,catalog.tones(waving).size)
        assertTrue(catalog.tones(waving).all {it.text.startsWith("👋")})
        val redHair=catalog.all.first {it.text=="🧑‍🦰"}
        assertTrue(catalog.tones(redHair).isNotEmpty())
        assertTrue(catalog.tones(redHair).joinToString { it.name + "=" + it.text.codePoints().toArray().joinToString() },catalog.tones(redHair).all {it.text.endsWith("‍🦰")})
        assertEquals(5,catalog.tones(catalog.all.first {it.name=="victory hand"}).size)
        assertEquals(5,catalog.tones(catalog.all.first {it.name=="index pointing up"}).size)
        assertTrue(catalog.tones(catalog.all.first {it.text=="😀"}).isEmpty())
    }
}
