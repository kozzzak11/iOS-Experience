package com.kozzzak.ios.keyboard

import java.util.Locale

/** Small deterministic offline baseline. No learning, telemetry, context history or network. */
class WordEngine(private val baseWords: Map<String, List<String>>) {
    private val personal = mutableMapOf<String, Set<String>>()
    fun setPersonal(language: String, words: Set<String>) { personal[language] = words.map { it.lowercase(Locale.ROOT) }.toSet() }
    fun candidates(token: String, language: String): List<String> {
        if (token.isEmpty() || token.length > 48) return emptyList()
        val lower = token.lowercase(Locale.ROOT)
        val words = (personal[language].orEmpty() + baseWords[language].orEmpty()).distinct()
        val prefix = words.filter { it != lower && it.startsWith(lower) }.take(3)
        val corrections = if (lower.length >= 3) words.asSequence().filter { it != lower && distance(lower, it) == 1 }.take(3).toList() else emptyList()
        return (listOf(token) + (corrections + prefix).distinct().map { matchCase(token, it) }).distinct().take(3)
    }
    fun correction(token: String, language: String): String? {
        val lower = token.lowercase(Locale.ROOT)
        if (token.length < 4 || token.any { !it.isLetter() } || token.all { it.isUpperCase() }) return null
        val words = personal[language].orEmpty() + baseWords[language].orEmpty()
        if (lower in words) return null
        // Auto replacement only when the one-edit answer is unambiguous. Never learn automatically.
        val close = words.distinct().filter { distance(lower, it) == 1 }
        return close.singleOrNull()?.let { matchCase(token, it) }
    }
    companion object {
        fun matchCase(source: String, target: String): String = when {
            source.all(Char::isUpperCase) -> target.uppercase(Locale.ROOT)
            source.firstOrNull()?.isUpperCase() == true -> target.replaceFirstChar { it.titlecase(Locale.ROOT) }
            else -> target
        }
        /** Optimal string alignment, including adjacent transposition. */
        fun distance(a: String, b: String): Int {
            if (kotlin.math.abs(a.length - b.length) > 1) return 2
            val d = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) d[i][0] = i
            for (j in 0..b.length) d[0][j] = j
            for (i in 1..a.length) for (j in 1..b.length) {
                d[i][j] = minOf(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]+if (a[i-1] == b[j-1]) 0 else 1)
                if (i > 1 && j > 1 && a[i-1] == b[j-2] && a[i-2] == b[j-1]) d[i][j] = minOf(d[i][j], d[i-2][j-2]+1)
            }
            return d[a.length][b.length]
        }
    }
}
