package com.kozzzak.ios.keyboard

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.BreakIterator
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.kozzzak.ios.settings.SettingsActivity
import java.util.Locale
import kotlin.math.abs

open class IosInputMethodService : InputMethodService() {
    private lateinit var prefs: KeyboardPrefs
    private lateinit var engine: WordEngine
    private lateinit var catalog: EmojiCatalog
    private var root: KeyboardSurface? = null
    private var suggestions: LinearLayout? = null
    private var policy=EditorPolicy(true,false,"letters")
    private var composing=""
    private var shift=false
    private var caps=false
    private var lastShift=0L
    private var mode="letters"
    private var symbolsSecond=false
    private var emojiMode=false
    private var emojiSearch=false
    private var query=""
    private var group="Smileys & Emotion"
    private var dark=false
    private var emojiResults: LinearLayout? = null
    private var searchLabel: TextView? = null
    private val letterKeys=mutableListOf<Pair<KeyView,String>>()
    private val d get()=resources.displayMetrics.density
    private val fg get()=if(dark) Color.WHITE else Color.BLACK
    private val bg get()=if(dark) Color.rgb(40,40,43) else Color.rgb(210,213,219)
    private val accent=Color.rgb(0,122,255)

    override fun onCreate() {
        super.onCreate()
        prefs=KeyboardPrefs(this)
        engine=WordEngine(listOf("en","ru").associateWith { lang -> assets.open("$lang.txt").bufferedReader().useLines { it.filter(String::isNotBlank).distinct().toList() } })
        catalog=EmojiCatalog(this)
    }
    override fun onEvaluateFullscreenMode()=false
    override fun onCreateInputView(): View { return buildSurface() }
    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute,restarting)
        composing=""; emojiMode=false; emojiSearch=false; query=""; caps=false; lastShift=0
        policy=EditorPolicy.from(attribute); mode=policy.layout; symbolsSecond=false
        listOf("en","ru").forEach { engine.setPersonal(it,prefs.words(it)) }
        updateAutomaticShift()
    }
    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info,restarting)
        setInputView(buildSurface())
    }
    override fun onFinishInputView(finishingInput: Boolean) {
        finishWord(false); root?.let(::cancelKeys)
        super.onFinishInputView(finishingInput)
    }
    override fun onFinishInput() {
        composing=""; query=""; root?.let(::cancelKeys)
        super.onFinishInput()
    }
    override fun onDestroy() { root?.let(::cancelKeys); root=null; super.onDestroy() }
    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd)
        if (composing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composing=""; currentInputConnection?.finishComposingText(); renderSuggestions()
        }
        updateAutomaticShift(); updateLabels()
    }
    private fun cancelKeys(view: View) {
        if(view is KeyView) view.cancelTouch()
        if(view is ViewGroup) for(i in 0 until view.childCount) cancelKeys(view.getChildAt(i))
    }
    private fun theme() {
        dark=when(prefs.theme) { "dark" -> true; "light" -> false; else -> resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES }
    }
    private fun text(value: String, size: Float=16f)=TextView(this).apply {
        text=value; textSize=size; setTextColor(fg); gravity=Gravity.CENTER
        importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    private fun buildSurface(): View {
        theme(); letterKeys.clear(); emojiResults=null; searchLabel=null
        val surface=KeyboardSurface(this,{ prefs.enabled("swipe_hide",false) && !emojiMode },{ hideKeyboard() })
        surface.setBackgroundColor(bg); surface.setPadding((2*d).toInt(),(4*d).toInt(),(2*d).toInt(),(6*d).toInt())
        root=surface
        suggestions=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        if(emojiMode && !emojiSearch) {
            buildEmojiPanel(surface)
        } else {
            if(emojiSearch) buildSearchHeader(surface) else {
                surface.addView(suggestions,LinearLayout.LayoutParams(-1,if(policy.words && prefs.enabled("suggestions")) (38*d).toInt() else 0))
                renderSuggestions()
            }
            if(mode=="number" || mode=="phone") buildNumber(surface) else buildLetters(surface)
        }
        return surface
    }
    private fun refresh() { root?.let(::cancelKeys); setInputView(buildSurface()) }
    private fun row(parent: LinearLayout, height: Int=prefs.height): LinearLayout = LinearLayout(this).also {
        it.orientation=LinearLayout.HORIZONTAL; it.gravity=Gravity.CENTER
        parent.addView(it,LinearLayout.LayoutParams(-1,(height*d).toInt()))
    }
    private fun key(row: LinearLayout, label: String, action: String=label, weight: Float=1f, special: Boolean=false, alternate: (() -> Unit)?=null) {
        val k=KeyView(this,label,action,dark,special,prefs.enabled("preview") && !policy.sensitive,
            ::handle,::feedback,::moveCursor,alternate)
        row.addView(k,LinearLayout.LayoutParams(0,-1,weight))
        if(action.length==1 && action[0].isLetter()) letterKeys.add(k to action.lowercase(Locale.ROOT))
    }
    private fun spacer(row: LinearLayout, weight: Float) { row.addView(View(this),LinearLayout.LayoutParams(0,1,weight)) }
    private fun buildLetters(parent: LinearLayout) {
        val ru=prefs.language=="ru"
        if(mode=="symbols") {
            val rows=if(symbolsSecond) listOf("[]{}#%^*+=","_\\|~<>€£¥•",".,?!'\"") else listOf("1234567890","-/:;()₽&@\"",".,?!'")
            rows.forEachIndexed { index, chars -> val r=row(parent)
                if(index==2) key(r,if(symbolsSecond) "123" else "#+=","MORE",1.5f,true)
                chars.forEach { key(r,it.toString()) }
                if(index==2) key(r,"⌫","DELETE",1.5f,true)
            }
        } else {
            val rows=if(ru) listOf("йцукенгшщзх","фывапролджэ","ячсмитьбю") else listOf("qwertyuiop","asdfghjkl","zxcvbnm")
            rows.forEachIndexed { index, chars -> val r=row(parent)
                if(index==1 && !ru) spacer(r,0.5f)
                if(index==2) key(r,if(caps) "⇪" else "⇧","SHIFT",if(ru) 1f else 1.5f,true)
                chars.forEach { c ->
                    val raw=c.toString(); val variants=when(c) {'е' -> "ё"; 'ь' -> "ъ"; 'e' -> "éèêë"; 'a' -> "áàâäæ"; 'o' -> "óòôöœ"; 'u' -> "úùûü"; 'i' -> "íìîï"; 'c' -> "ç"; 'n' -> "ñ"; else -> ""}
                    key(r,if(shift||caps) raw.uppercase(Locale.ROOT) else raw,raw,alternate=if(variants.isEmpty()) null else ({ showAlternates(r,variants.map { if(shift||caps) it.uppercaseChar().toString() else it.toString() }) }))
                }
                if(index==2) key(r,"⌫","DELETE",if(ru) 1f else 1.5f,true)
                if(index==1 && !ru) spacer(r,0.5f)
            }
        }
        val r=row(parent)
        key(r,if(mode=="symbols") "АБВ" else "123",if(mode=="symbols") "LETTERS" else "SYMBOLS",1.2f,true)
        key(r,"◎","LANG",0.9f,true) { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        key(r,"☺","EMOJI",0.9f,true)
        key(r,if(ru) "пробел" else "space","SPACE",3.5f)
        val variation=currentInputEditorInfo?.inputType?.and(InputType.TYPE_MASK_VARIATION)
        if(variation in setOf(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,InputType.TYPE_TEXT_VARIATION_URI)) key(r,"@","@",0.8f)
        else key(r,".",".",0.7f,alternate={ showAlternates(r,listOf(",","!","?",":",";","…")) })
        key(r,enterLabel(),"ENTER",1.65f,true)
        if(prefs.enabled("hide_button")) key(r,"⌄","HIDE",0.7f,true)
    }
    private fun buildNumber(parent: LinearLayout) {
        listOf("123","456","789").forEach { s -> val r=row(parent); s.forEach { key(r,it.toString()) } }
        val r=row(parent)
        val info=currentInputEditorInfo?.inputType ?: 0
        if(mode=="phone") key(r,"+","+",alternate={ showAlternates(r,listOf("*","#",",",";")) })
        else if(info and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0) key(r,".",".",alternate={ showAlternates(r,listOf(",")) })
        else if(info and InputType.TYPE_NUMBER_FLAG_SIGNED != 0) key(r,"−","-")
        else spacer(r,1f)
        key(r,"0"); key(r,"⌫","DELETE",special=true)
        val bottom=row(parent,42)
        key(bottom,"◎","LANG",special=true,alternate={ (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() })
        if(mode=="number" && info and InputType.TYPE_NUMBER_FLAG_SIGNED != 0 && info and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0) key(bottom,"−","-",special=true)
        key(bottom,enterLabel(),"ENTER",2f,true); key(bottom,"⌄","HIDE",special=true)
    }
    private fun enterLabel(): String {
        if(emojiSearch) return "Найти"
        val info=currentInputEditorInfo ?: return "↵"
        if(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return "↵"
        return when(info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "Найти"; EditorInfo.IME_ACTION_SEND -> "Отпр."; EditorInfo.IME_ACTION_GO -> "Перейти"
            EditorInfo.IME_ACTION_NEXT -> "Далее"; EditorInfo.IME_ACTION_DONE -> "Готово"; else -> "↵"
        }
    }
    private fun feedback() {
        val v=root ?: return
        if(prefs.enabled("haptics")) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if(prefs.enabled("sound",false) && !policy.sensitive) (getSystemService(AUDIO_SERVICE) as AudioManager).playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
    }
    internal fun handle(action: String) {
        when(action) {
            "SHIFT" -> { val now=SystemClock.uptimeMillis(); if(caps) {caps=false;shift=false} else if(now-lastShift<320) {caps=true;shift=true} else {caps=false;shift=!shift}; lastShift=now; updateLabels() }
            "LANG" -> { finishWord(false); prefs.language=if(prefs.language=="ru") "en" else "ru"; refresh() }
            "SYMBOLS" -> { finishWord(false); mode="symbols"; refresh() }
            "LETTERS" -> { mode="letters"; symbolsSecond=false; refresh() }
            "MORE" -> { symbolsSecond=!symbolsSecond; refresh() }
            "EMOJI" -> { finishWord(false); emojiMode=!emojiMode; emojiSearch=false;query="";refresh() }
            "HIDE" -> hideKeyboard()
            "SPACE" -> if(emojiSearch) searchText(" ") else insertSeparator(" ")
            "DELETE" -> if(emojiSearch) { query=query.dropLast(1);updateEmojiSearch() } else delete()
            "ENTER" -> if(emojiSearch) { /* Search updates live; do not send into the host editor. */ } else enter()
            else -> {
                val value=if(action.length==1 && action[0].isLetter() && (shift||caps)) action.uppercase(Locale.ROOT) else action
                if(emojiSearch) searchText(value) else insert(value)
                if(!caps && action.any(Char::isLetter)) {shift=false;updateLabels()}
            }
        }
    }
    private fun insert(value: String) {
        val ic=currentInputConnection ?: return
        if(policy.words && prefs.enabled("suggestions") && value.all(Char::isLetter) && composing.length<48) {
            composing+=value; ic.setComposingText(composing,1); renderSuggestions()
        } else { finishWord(false); ic.commitText(value,1) }
    }
    private fun finishWord(correct: Boolean) {
        val ic=currentInputConnection ?: run {composing="";return}
        val token=composing; composing=""
        if(token.isNotEmpty()) {
            val replacement=if(correct && prefs.enabled("autocorrect",false)) engine.correction(token,prefs.language) else null
            ic.commitText(replacement ?: token,1)
        }
        ic.finishComposingText(); renderSuggestions()
    }
    private fun insertSeparator(value: String) {
        val ic=currentInputConnection ?: return
        ic.beginBatchEdit()
        try { finishWord(true); ic.commitText(value,1) } finally { ic.endBatchEdit() }
        updateAutomaticShift(); updateLabels()
    }
    private fun enter() {
        finishWord(false)
        val info=currentInputEditorInfo ?: return
        val action=info.imeOptions and EditorInfo.IME_MASK_ACTION
        if(info.actionId!=0 && info.actionLabel!=null) currentInputConnection?.performEditorAction(info.actionId)
        else if(info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 && action !in setOf(EditorInfo.IME_ACTION_NONE,EditorInfo.IME_ACTION_UNSPECIFIED)) currentInputConnection?.performEditorAction(action)
        else currentInputConnection?.commitText("\n",1)
        updateAutomaticShift(); updateLabels()
    }
    private fun delete() {
        val ic=currentInputConnection ?: return
        if(composing.isNotEmpty()) {
            composing=composing.dropLast(1)
            if(composing.isEmpty()) { ic.commitText("",1); ic.finishComposingText() } else ic.setComposingText(composing,1)
            renderSuggestions();return
        }
        // Secure fields: never request surrounding text, selected text, or extracted text.
        if(policy.sensitive) { sendDelete();return }
        if(!ic.getSelectedText(0).isNullOrEmpty()) { ic.commitText("",1);return }
        val before=ic.getTextBeforeCursor(256,0)?.toString().orEmpty()
        if(before.isEmpty()) { sendDelete();return }
        val boundary=BreakIterator.getCharacterInstance(Locale.ROOT).apply {setText(before)}.preceding(before.length)
        if(boundary>=0) ic.deleteSurroundingText(before.length-boundary,0) else sendDelete()
        updateAutomaticShift();updateLabels()
    }
    private fun sendDelete() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DEL))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DEL))
    }
    private fun moveCursor(steps: Int) {
        if(emojiSearch) return
        finishWord(false)
        val code=if(steps<0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(abs(steps)) {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,code))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP,code))
        }
    }
    private fun updateAutomaticShift() {
        if(caps || !prefs.enabled("auto_caps") || policy.sensitive || !policy.words || emojiSearch) { if(!caps) shift=false;return }
        val info=currentInputEditorInfo ?: return
        shift = (currentInputConnection?.getCursorCapsMode(info.inputType or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) ?: 0) != 0
    }
    private fun updateLabels() {
        letterKeys.forEach { (view, raw) -> view.label=if(shift||caps) raw.uppercase(Locale.ROOT) else raw; view.contentDescription=view.label; view.invalidate() }
        fun visit(v: View) { if(v is KeyView && v.contentDescription=="Shift") {v.label=if(caps) "⇪" else "⇧";v.invalidate()}; if(v is ViewGroup) for(i in 0 until v.childCount) visit(v.getChildAt(i)) }
        root?.let(::visit)
    }
    private fun renderSuggestions() {
        val strip=suggestions ?: return
        strip.removeAllViews()
        if(!policy.words || !prefs.enabled("suggestions") || emojiMode) return
        val items=if(composing.isNotEmpty()) engine.candidates(composing,prefs.language) else if(prefs.language=="ru") listOf("Привет","Спасибо","Хорошо") else listOf("Hello","Thanks","Okay")
        items.forEach { item ->
            val tv=text(item).apply {setOnClickListener {
                val ic=currentInputConnection ?: return@setOnClickListener
                composing="";ic.commitText(item+" ",1);ic.finishComposingText();renderSuggestions();updateAutomaticShift();updateLabels()
            }}
            strip.addView(tv,LinearLayout.LayoutParams(0,-1,1f))
        }
    }
    private fun showAlternates(anchor: View, choices: List<String>, onChosen: (String)->Unit={handle(it)}) {
        if(choices.isEmpty()) return
        val layout=LinearLayout(this).apply {setBackgroundColor(bg);setPadding(4,4,4,4)}
        val popup=PopupWindow(layout,-2,(54*d).toInt(),true).apply {setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bg));isOutsideTouchable=true;elevation=8*d}
        choices.forEach { s -> layout.addView(text(s,24f).apply {setOnClickListener {onChosen(s);popup.dismiss()}},LinearLayout.LayoutParams((48*d).toInt(),-1)) }
        popup.showAsDropDown(anchor,0,-anchor.height-(54*d).toInt())
    }
    private fun buildEmojiPanel(parent: LinearLayout) {
        val header=row(parent,42)
        header.addView(text("Эмодзи · системный рисунок",14f),LinearLayout.LayoutParams(0,-1,1f))
        header.addView(text("Поиск",15f).apply {setTextColor(accent);setOnClickListener {emojiSearch=true;mode="letters";refresh()}},LinearLayout.LayoutParams((72*d).toInt(),-1))
        val categories=LinearLayout(this)
        val categoryNames=listOf("Недавние")+catalog.groups
        categoryNames.forEach { name -> categories.addView(text(categoryLabel(name),14f).apply {
            setPadding((12*d).toInt(),0,(12*d).toInt(),0);setTextColor(if(group==name) accent else fg)
            contentDescription=name;setOnClickListener {group=name;refresh()}
        },LinearLayout.LayoutParams(-2,(38*d).toInt())) }
        parent.addView(HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;addView(categories)},LinearLayout.LayoutParams(-1,(38*d).toInt()))
        val emojis=catalog.visible(group,"",if(policy.sensitive || !prefs.enabled("emoji_recent")) emptyList() else prefs.recentEmoji())
        if(emojis.isEmpty()) parent.addView(text("Пока пусто",16f),LinearLayout.LayoutParams(-1,(prefs.height*3*d).toInt()))
        else {
            val grid=GridView(this).apply {
                numColumns=8; stretchMode=GridView.STRETCH_COLUMN_WIDTH
                adapter=object:BaseAdapter() {
                    override fun getCount()=emojis.size
                    override fun getItem(position:Int)=emojis[position]
                    override fun getItemId(position:Int)=position.toLong()
                    override fun getView(position:Int,convertView:View?,container:ViewGroup):View = emojiCell(emojis[position]).apply {
                        layoutParams=AbsListView.LayoutParams(-1,(48*d).toInt())
                    }
                }
            }
            parent.addView(grid,LinearLayout.LayoutParams(-1,(prefs.height*3*d).toInt()))
        }
        val bottom=row(parent,46)
        key(bottom,"АБВ","EMOJI",special=true);key(bottom,"⌫","DELETE",special=true);key(bottom,"⌄","HIDE",special=true)
    }
    private fun emojiCell(emoji: Emoji): TextView = text(emoji.text,27f).apply {
        contentDescription=emoji.name
        setOnClickListener {insertEmoji(emoji.text)}
        setOnLongClickListener {val tones=catalog.tones(emoji);showAlternates(this,tones.map {it.text},::insertEmoji);tones.isNotEmpty()}
    }
    private fun insertEmoji(emoji: String) {
        currentInputConnection?.commitText(emoji,1)
        if(!policy.sensitive && prefs.enabled("emoji_recent")) prefs.rememberEmoji(emoji)
    }
    private fun categoryLabel(name: String)=when(name) {
        "Недавние" -> "◷"; "Smileys & Emotion" -> "☺"; "People & Body" -> "Люди"; "Animals & Nature" -> "Природа"
        "Food & Drink" -> "Еда"; "Travel & Places" -> "Места"; "Activities" -> "Занятия"; "Objects" -> "Предметы"; "Symbols" -> "Символы"; "Flags" -> "Флаги";else -> name
    }
    private fun buildSearchHeader(parent: LinearLayout) {
        val header=row(parent,34)
        searchLabel=text("Поиск эмодзи",14f)
        header.addView(searchLabel,LinearLayout.LayoutParams(0,-1,1f))
        header.addView(text("Отмена",14f).apply {setTextColor(accent);setOnClickListener {query="";emojiSearch=false;refresh()}},LinearLayout.LayoutParams((80*d).toInt(),-1))
        emojiResults=LinearLayout(this)
        parent.addView(HorizontalScrollView(this).apply {addView(emojiResults)},LinearLayout.LayoutParams(-1,(48*d).toInt()))
        updateEmojiSearch()
    }
    private fun searchText(value: String) {if(query.length<64) query+=value;updateEmojiSearch()}
    private fun updateEmojiSearch() {
        searchLabel?.text=if(query.isEmpty()) "Поиск: heart / сердце" else query
        emojiResults?.let { results ->
            results.removeAllViews()
            val items=catalog.visible(group,query,emptyList()).take(60)
            if(items.isEmpty()) results.addView(text("Нет результатов",14f))
            items.forEach {results.addView(emojiCell(it),LinearLayout.LayoutParams((48*d).toInt(),-1))}
        }
    }
    private fun hideKeyboard() {finishWord(false);query="";root?.let(::cancelKeys);requestHideSelf(0)}
}
