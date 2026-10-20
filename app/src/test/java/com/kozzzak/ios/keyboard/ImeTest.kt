package com.kozzzak.ios.keyboard

import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uses a real Android Editable, composing spans and BaseInputConnection in Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ImeTest {
    class TestIme : IosInputMethodService() {
        var connection: InputConnection?=null
        var info=EditorInfo()
        override fun getCurrentInputConnection()=connection
        override fun getCurrentInputEditorInfo()=info
    }
    private lateinit var ime: TestIme
    private lateinit var field: EditText
    private lateinit var connection: InputConnection
    @Before fun setup() {
        ime=Robolectric.buildService(TestIme::class.java).create().get()
        KeyboardPrefs(ime).apply {reset();language="en";set("auto_caps",false)}
        field=EditText(ime)
        field.setText("");Selection.setSelection(field.editableText,0)
        connection=object:BaseInputConnection(field,true) { override fun getEditable():Editable=field.editableText }
        ime.connection=connection
        start(InputType.TYPE_CLASS_TEXT)
    }
    private fun start(type:Int,options:Int=0) {
        ime.info=EditorInfo().apply {inputType=type;imeOptions=options}
        ime.onStartInput(ime.info,false)
    }
    private fun type(s:String) {s.forEach {ime.handle(it.toString())}}
    @Test fun englishAndRussianComposeAndCommit() {
        type("hello");ime.handle("SPACE");ime.handle("LANG");type("привет");ime.handle("SPACE")
        assertEquals("hello привет ",field.text.toString())
        assertEquals(-1,BaseInputConnection.getComposingSpanStart(field.text))
    }
    @Test fun backspaceDuringCompositionAndSelectionReplacement() {
        type("hello");ime.handle("DELETE");ime.handle("SPACE")
        assertEquals("hell ",field.text.toString())
        Selection.setSelection(field.editableText,0,4);type("ship");ime.handle("SPACE")
        assertEquals("ship  ",field.text.toString())
    }
    @Test fun deletesWholeEmojiGrapheme() {
        val emoji="👨‍👩‍👧‍👦"
        field.setText("hello $emoji");Selection.setSelection(field.editableText,field.length())
        ime.handle("DELETE")
        assertEquals("hello ",field.text.toString())
    }
    @Test fun passwordDoesNotReadOrComposeOrPersistEditorText() {
        var surroundingReads=0;var composeCalls=0
        ime.connection=object:BaseInputConnection(field,true) {
            override fun getEditable():Editable=field.editableText
            override fun getTextBeforeCursor(n:Int,flags:Int):CharSequence {surroundingReads++;return "secret"}
            override fun getSelectedText(flags:Int):CharSequence {surroundingReads++;return "secret"}
            override fun getCursorCapsMode(reqModes:Int):Int {surroundingReads++;return 0}
            override fun setComposingText(text:CharSequence?,newCursorPosition:Int):Boolean {composeCalls++;return super.setComposingText(text,newCursorPosition)}
        }
        KeyboardPrefs(ime).apply {set("auto_caps",true);set("autocorrect",true)}
        start(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        type("private123");ime.handle("SPACE");ime.handle("DELETE")
        assertEquals(0,surroundingReads);assertEquals(0,composeCalls)
        assertTrue(KeyboardPrefs(ime).words("en").isEmpty())
        assertTrue(KeyboardPrefs(ime).recentEmoji().isEmpty())
    }
    @Test fun passwordVariantsAndEmailHaveCorrectPolicy() {
        listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD).forEach {v ->
            val p=EditorPolicy.from(EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT or v})
            assertTrue(p.sensitive);assertFalse(p.words)
        }
        val pin=EditorPolicy.from(EditorInfo().apply {inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD})
        assertTrue(pin.sensitive);assertEquals("number",pin.layout)
        val email=EditorPolicy.from(EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS})
        assertFalse(email.words)
        val incognito=EditorPolicy.from(EditorInfo().apply {inputType=InputType.TYPE_CLASS_TEXT;imeOptions=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING})
        assertTrue(incognito.sensitive)
    }
    @Test fun fieldSwitchDoesNotCarryComposingWord() {
        type("hello")
        field=EditText(ime);field.setText("")
        connection=object:BaseInputConnection(field,true) { override fun getEditable():Editable=field.editableText }
        ime.connection=connection;start(InputType.TYPE_CLASS_TEXT)
        type("world");ime.handle("SPACE")
        assertEquals("world ",field.text.toString())
    }
    @Test fun enterDispatchesEditorActionAndMultilineCommitsNewline() {
        var received=-1
        ime.connection=object:BaseInputConnection(field,true) {
            override fun getEditable():Editable=field.editableText
            override fun performEditorAction(actionCode:Int):Boolean {received=actionCode;return true}
        }
        start(InputType.TYPE_CLASS_TEXT,EditorInfo.IME_ACTION_SEND)
        type("hello");ime.handle("ENTER")
        assertEquals(EditorInfo.IME_ACTION_SEND,received)
        start(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,EditorInfo.IME_FLAG_NO_ENTER_ACTION or EditorInfo.IME_ACTION_SEND)
        ime.handle("ENTER");assertTrue(field.text.toString().endsWith("\n"))
    }
    @Test fun composingClearsWhenCursorMovesAway() {
        type("hello")
        Selection.setSelection(field.editableText,0)
        ime.onUpdateSelection(5,5,0,0,0,5)
        type("x");ime.handle("SPACE")
        assertEquals("x hello",field.text.toString())
    }
}
