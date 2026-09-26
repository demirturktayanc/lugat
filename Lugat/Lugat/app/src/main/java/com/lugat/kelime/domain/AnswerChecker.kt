package com.lugat.kelime.domain

import java.text.Normalizer
import java.util.Locale

enum class AnswerResult { CORRECT, ALMOST, WRONG }

/** Yazılı cevap kontrolü: büyük/küçük harf, fazla boşluk ve noktalama önemsenmez; küçük yazım hatası tolere edilir. */
object AnswerChecker {
    private val MARKS = Regex("\\p{M}+")
    private val PUNCT = Regex("[^\\p{L}\\p{N}' -]")
    private val SPACES = Regex("\\s+")

    fun normalize(s: String): String {
        val lower = s.lowercase(Locale.ROOT).replace('’', '\'').replace('‘', '\'')
        val noMarks = MARKS.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
        return SPACES.replace(PUNCT.replace(noMarks, " ").replace('-', ' '), " ").trim()
    }

    fun check(input: String, expected: String): AnswerResult {
        val a = normalize(input)
        val e = normalize(expected)
        if (a.isEmpty()) return AnswerResult.WRONG
        if (a == e) return AnswerResult.CORRECT
        // "to" ile yazılmış fiiller de kabul: "to run" = "run"
        if (a.removePrefix("to ") == e) return AnswerResult.CORRECT
        val tolerance = when {
            e.length >= 8 -> 2
            e.length >= 4 -> 1
            else -> 0
        }
        return if (tolerance > 0 && levenshtein(a, e) <= tolerance) AnswerResult.ALMOST else AnswerResult.WRONG
    }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }
}
