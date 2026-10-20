package com.kozzzak.ios.keyboard

import org.junit.Assert.*
import org.junit.Test

class WordEngineTest {
    private val engine=WordEngine(mapOf("en" to listOf("hello","help","world","ship","work"),"ru" to listOf("привет","погода","судно")))
    @Test fun transpositionAndReplacement() {
        assertEquals(1,WordEngine.distance("hlelo","hello"))
        assertEquals(1,WordEngine.distance("погрда","погода"))
        assertEquals("Hello",engine.correction("Hlelo","en"))
        assertEquals("погода",engine.correction("погрда","ru"))
    }
    @Test fun ambiguousAndKnownWordsAreNotCorrected() {
        val e=WordEngine(mapOf("en" to listOf("cart","cast")))
        assertNull(e.correction("cant","en"))
        assertNull(engine.correction("ship","en"))
        assertNull(engine.correction("HHLO","en"))
        assertNull(engine.correction("abc1","en"))
    }
    @Test fun explicitPersonalWordsTakePartWithoutLearningText() {
        engine.setPersonal("ru",setOf("Момбаса"))
        assertTrue(engine.candidates("мом","ru").contains("момбаса"))
        assertFalse(engine.candidates("мом","en").contains("момбаса"))
        assertEquals(listOf("xyzzy"),engine.candidates("xyzzy","en"))
    }
    @Test fun candidateCaseAndLimits() {
        assertTrue(engine.candidates("He","en").contains("Hello"))
        assertTrue(engine.candidates("HEL","en").contains("HELLO"))
        assertTrue(engine.candidates("h","en").size<=3)
        assertTrue(engine.candidates("a".repeat(100),"en").isEmpty())
    }
}
