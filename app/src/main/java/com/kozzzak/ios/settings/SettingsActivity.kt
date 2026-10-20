package com.kozzzak.ios.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kozzzak.ios.keyboard.KeyboardPrefs

class SettingsActivity : ComponentActivity() {
    private var resumed by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { SettingsScreen() }
    }
    override fun onResume() {super.onResume();resumed++}
    @Composable private fun SettingsScreen() {
        val prefs=remember {KeyboardPrefs(this)}
        var revision by remember {mutableIntStateOf(0)}
        var section by remember {mutableIntStateOf(0)}
        var dictionary by remember {mutableStateOf(false)}
        var reset by remember {mutableStateOf(false)}
        val theme=remember(revision) {prefs.theme}
        val dark=when(theme) {"dark"->true;"light"->false;else->isSystemInDarkTheme()}
        val colors=if(dark) darkColorScheme(primary=Color(0xFF0A84FF),background=Color(0xFF000000),surface=Color(0xFF1C1C1E))
            else lightColorScheme(primary=Color(0xFF007AFF),background=Color(0xFFF2F2F7),surface=Color.White)
        MaterialTheme(colorScheme=colors) {
            Surface(Modifier.fillMaxSize(),color=colors.background) {
                Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text("iOS Experience",fontSize=32.sp,fontWeight=FontWeight.Bold)
                    Text("Keyboard · 0.1.0",color=colors.onSurfaceVariant)
                    val status=remember(resumed,revision) {
                        val imm=getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        val enabled=imm.enabledInputMethodList.any {it.packageName==packageName}
                        val selected=Settings.Secure.getString(contentResolver,Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith("$packageName/")==true
                        when {selected->"Клавиатура выбрана";enabled->"Включена — выберите для ввода";else->"Включите клавиатуру в Android"}
                    }
                    Card(shape=RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text(status,fontWeight=FontWeight.SemiBold)
                            Text("Офлайн. Без рекламы и сетевого доступа. Вводимый текст не записывается.",fontSize=14.sp)
                            Button(onClick={startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))},modifier=Modifier.fillMaxWidth()) {Text("1. Включить клавиатуру")}
                            OutlinedButton(onClick={(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()},modifier=Modifier.fillMaxWidth()) {Text("2. Выбрать клавиатуру")}
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("Keyboard","Emoji","Appearance").forEachIndexed { i,label -> FilterChip(selected=section==i,onClick={section=i},label={Text(label)}) }
                    }
                    when(section) {
                        0 -> {
                            Section("Ввод") {
                                Toggle("Заглавная после точки",prefs.enabled("auto_caps")) {prefs.set("auto_caps",it);revision++}
                                Toggle("Подсказки RU / EN",prefs.enabled("suggestions")) {prefs.set("suggestions",it);revision++}
                                Toggle("Автокоррекция",prefs.enabled("autocorrect",false)) {prefs.set("autocorrect",it);revision++}
                                Text("Встроен небольшой базовый словарь. Подсказки дополняют слово; полноценной языковой модели и предсказания фраз пока нет.",fontSize=13.sp,color=colors.onSurfaceVariant)
                                OutlinedButton(onClick={dictionary=true}) {Text("Личный словарь")}
                            }
                            Section("Нажатия и скрытие") {
                                listOf("haptics" to "Тактильная отдача","sound" to "Звуки Android","preview" to "Всплывающие символы","hide_button" to "Кнопка скрытия ⌄","swipe_hide" to "Свайп вниз для скрытия").forEach { (key,label) ->
                                    Toggle(label,prefs.enabled(key,key !in setOf("sound","swipe_hide"))) {prefs.set(key,it);revision++}
                                }
                                Text("Удерживайте пробел и ведите влево/вправо для курсора. Удерживайте Backspace для удаления, е для ё, ь для ъ. Двойной Shift — Caps Lock. Удержание ◎ — выбор другой клавиатуры.",fontSize=13.sp)
                                Text("Свайп вниз по умолчанию выключен. Android также скрывает клавиатуру системным жестом «Назад».",fontSize=13.sp)
                            }
                            Section("Проверить ввод") {
                                var sample by remember {mutableStateOf("")}
                                var password by remember {mutableStateOf("")}
                                var number by remember {mutableStateOf("")}
                                OutlinedTextField(sample,{sample=it},label={Text("RU / EN, emoji, Enter")},modifier=Modifier.fillMaxWidth(),minLines=2)
                                OutlinedTextField(password,{password=it},label={Text("Тест пароля · не вводите настоящий")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth())
                                OutlinedTextField(number,{number=it},label={Text("Числа")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
                            }
                        }
                        1 -> {
                            Section("Панель эмодзи") {
                                Toggle("Недавно использованные",prefs.enabled("emoji_recent")) {prefs.set("emoji_recent",it);revision++}
                                Text("Категории, поиск по английским названиям и нескольким русским ключевым словам. Удержание эмодзи — доступные оттенки кожи.")
                                Text("В этой сборке используется системная графика Android. Apple Emoji ещё не реализованы. Вставленный символ рисует приложение-получатель своим шрифтом.")
                                Text("Обычный APK не заменяет системный emoji-шрифт Nothing OS. Root не требуется и не используется.",fontSize=13.sp)
                            }
                        }
                        2 -> {
                            Section("Оформление клавиатуры") {
                                Text("Тема")
                                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                    listOf("system" to "Авто","light" to "Светлая","dark" to "Тёмная").forEach { (key,label) -> FilterChip(selected=theme==key,onClick={prefs.theme=key;revision++},label={Text(label)}) }
                                }
                                val height=remember(revision) {prefs.height}
                                Text("Высота ряда: $height dp")
                                Slider(value=height.toFloat(),onValueChange={prefs.height=it.toInt();revision++},valueRange=44f..68f,steps=23)
                                KeyboardPreview(dark)
                                Text("Предпросмотр оформления. Рабочую клавиатуру откройте в поле «Проверить ввод».",fontSize=13.sp)
                            }
                        }
                    }
                    Section("Возврат к стандартной клавиатуре") {
                        Text("Нажмите «Выбрать клавиатуру» и выберите прежнюю. Затем при необходимости отключите iOS Keyboard в системном списке. Сброс телефона не нужен.",fontSize=14.sp)
                        OutlinedButton(onClick={reset=true}) {Text("Сбросить настройки iOS Keyboard")}
                    }
                }
            }
            if(dictionary) DictionaryDialog(prefs,onClose={dictionary=false;revision++})
            if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("Сброс настроек?")},text={Text("Будут удалены настройки, личный словарь и недавние эмодзи этого приложения.")},confirmButton={TextButton(onClick={prefs.reset();reset=false;revision++}){Text("Сбросить")}},dismissButton={TextButton(onClick={reset=false}){Text("Отмена")}})
        }
    }
}
@Composable private fun Section(title: String, content: @Composable ColumnScope.()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,fontSize=14.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
        }
    }
}
@Composable private fun Toggle(label: String,checked: Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(label,Modifier.weight(1f),fontSize=15.sp)
        Switch(checked=checked,onCheckedChange=onChange)
    }
}
@Composable private fun KeyboardPreview(dark:Boolean) {
    Column(Modifier.fillMaxWidth().background(if(dark) Color(0xFF28282B) else Color(0xFFD2D5DB),RoundedCornerShape(12.dp)).padding(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf("qwertyuiop","asdfghjkl","zxcvbnm").forEach {row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp,Alignment.CenterHorizontally)) {
                row.forEach {c -> Box(Modifier.weight(1f).height(36.dp).background(if(dark) Color(0xFF69696E) else Color.White,RoundedCornerShape(4.dp)),contentAlignment=Alignment.Center) {Text(c.toString(),color=if(dark) Color.White else Color.Black)} }
            }
        }
        Box(Modifier.fillMaxWidth(.6f).align(Alignment.CenterHorizontally).height(32.dp).background(if(dark) Color(0xFF69696E) else Color.White,RoundedCornerShape(4.dp)),contentAlignment=Alignment.Center) {Text("space",fontSize=12.sp,color=if(dark) Color.White else Color.Black)}
    }
}
@Composable private fun DictionaryDialog(prefs:KeyboardPrefs,onClose:()->Unit) {
    var language by remember {mutableStateOf("ru")}
    var raw by remember {mutableStateOf(prefs.words(language).sorted().joinToString("\n"))}
    AlertDialog(onDismissRequest=onClose,title={Text("Личный словарь")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("ru","en").forEach {lang -> FilterChip(selected=language==lang,onClick={prefs.saveWords(language,raw.lines().map(String::trim).toSet());language=lang;raw=prefs.words(lang).sorted().joinToString("\n")},label={Text(lang.uppercase())})}}
            Text("Одно слово на строку. Только добавленные вручную слова сохраняются.",fontSize=13.sp)
            OutlinedTextField(raw,{raw=it},minLines=5,maxLines=8,modifier=Modifier.fillMaxWidth())
        }
    },confirmButton={TextButton(onClick={prefs.saveWords(language,raw.lines().map(String::trim).toSet());onClose()}){Text("Сохранить")}},dismissButton={TextButton(onClick=onClose){Text("Закрыть")}})
}
