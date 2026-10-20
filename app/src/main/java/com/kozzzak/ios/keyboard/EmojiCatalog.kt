package com.kozzzak.ios.keyboard

import android.content.Context

data class Emoji(val text: String, val group: String, val name: String)
class EmojiCatalog(context: Context) {
    val all: List<Emoji> = context.assets.open("emoji.tsv").bufferedReader().useLines { lines ->
        lines.mapNotNull { line -> val p=line.split('\t'); if(p.size==3) Emoji(p[0],p[1],p[2]) else null }.toList()
    }
    val groups = all.map { it.group }.distinct()
    fun visible(group: String, query: String, recent: List<String>): List<Emoji> = when {
        query.isNotBlank() -> {
            val aliases=mapOf("сердце" to "heart", "улыбка" to "smile", "кот" to "cat", "собака" to "dog", "корабль" to "ship", "смех" to "laugh", "огонь" to "fire", "флаг" to "flag", "лицо" to "face")
            val search=aliases[query.lowercase()] ?: query.lowercase()
            all.filter { it.name.contains(search,true) || it.text == query }.take(80)
        }
        group == "Недавние" -> recent.mapNotNull { s -> all.find { it.text==s } }
        else -> all.filter { it.group==group && !it.name.contains("skin tone") }
    }
    private fun withoutTone(value: String): String {
        val points=value.codePoints().filter { it !in 0x1F3FB..0x1F3FF && it != 0xFE0F }.toArray()
        return String(points,0,points.size)
    }
    private val toneGroups=all.filter { it.name.contains("skin tone") }.groupBy { withoutTone(it.text) }
    fun tones(emoji: Emoji): List<Emoji> = toneGroups[withoutTone(emoji.text)].orEmpty().take(5)
}
