package com.lugat.kelime.domain

import kotlin.random.Random

object Streak {
    /** Bugün ya da dün biten ardışık aktif gün sayısı (günler epochDay olarak). */
    fun current(activeDays: Set<Long>, today: Long): Int {
        var day = when {
            today in activeDays -> today
            (today - 1) in activeDays -> today - 1
            else -> return 0
        }
        var count = 0
        while (day in activeDays) { count++; day-- }
        return count
    }

    fun best(activeDays: Set<Long>): Int {
        var best = 0
        for (d in activeDays) {
            if ((d - 1) in activeDays) continue // yalnızca serinin ilk gününden say
            var len = 0
            var x = d
            while (x in activeDays) { len++; x++ }
            best = maxOf(best, len)
        }
        return best
    }
}

object Blank {
    const val GAP = "_____"

    /** Örnek cümlede kelimeyi (tam kelime, büyük/küçük harf duyarsız) boşlukla değiştirir; bulunamazsa null. */
    fun make(sentence: String, word: String): String? {
        val re = Regex("(?<![A-Za-z])" + Regex.escape(word) + "(?![A-Za-z])", RegexOption.IGNORE_CASE)
        return if (re.containsMatchIn(sentence)) re.replaceFirst(sentence, GAP) else null
    }
}

object Quiz {
    /**
     * [answerOf] ile üretilen doğru cevap + aynı havuzdan (mümkünse aynı türden) çeldiriciler.
     * Sonuç karıştırılmış ve tekrarsızdır; havuz küçükse daha az seçenek dönebilir.
     */
    fun options(
        target: Word,
        pool: List<Word>,
        random: Random,
        count: Int = 4,
        answerOf: (Word) -> String,
    ): List<String> {
        val correct = answerOf(target)
        val seen = mutableSetOf(AnswerChecker.normalize(correct))
        val candidates = pool.filter { it.id != target.id }.shuffled(random)
            .sortedByDescending { it.pos == target.pos } // sortedBy kararlıdır: karışık sıra korunur
        val picks = mutableListOf<String>()
        for (w in candidates) {
            if (picks.size >= count - 1) break
            val a = answerOf(w)
            if (seen.add(AnswerChecker.normalize(a))) picks += a
        }
        return (picks + correct).shuffled(random)
    }
}

object Sessions {
    /**
     * Bir deste için çalışma oturumu: önce tekrar zamanı gelmiş kelimeler (en fazla yarısı),
     * sonra yeni kelimeler (deste sırasıyla), gerekirse en eski görülen kelimelerle doldurulur.
     */
    fun forDeck(
        words: List<Word>,
        progress: Map<String, WordProgress>,
        now: Long,
        size: Int,
        random: Random,
    ): List<Word> {
        if (words.isEmpty()) return emptyList()
        val due = words.filter { w -> progress[w.id]?.let { Srs.isDue(it, now) } == true }
            .sortedBy { progress[it.id]?.dueAt ?: 0L }
            .take(size / 2)
        val fresh = words.filter { w -> (progress[w.id]?.box ?: 0) == 0 && w !in due }
        val chosen = LinkedHashSet<Word>()
        chosen += due
        for (w in fresh) { if (chosen.size >= size) break; chosen += w }
        if (chosen.size < size) {
            val rest = words.filter { it !in chosen }.sortedBy { progress[it.id]?.lastSeen ?: 0L }
            for (w in rest) { if (chosen.size >= size) break; chosen += w }
        }
        return chosen.toList().shuffled(random)
    }

    /** Seçili destelerdeki tüm tekrarı gelmiş kelimeler (en gecikmiş önce). */
    fun review(words: List<Word>, progress: Map<String, WordProgress>, now: Long, limit: Int): List<Word> =
        words.filter { w -> progress[w.id]?.let { Srs.isDue(it, now) } == true }
            .sortedBy { progress[it.id]?.dueAt ?: 0L }
            .take(limit)
}

object WordOfDay {
    /** Her gün farklı, kendini ancak liste bitince tekrar eden, cihazdan bağımsız sabit bir seçim. */
    fun index(size: Int, epochDay: Long): Int {
        if (size <= 0) return -1
        val order = (0 until size).shuffled(Random(20260926))
        return order[Math.floorMod(epochDay, size.toLong()).toInt()]
    }
}
