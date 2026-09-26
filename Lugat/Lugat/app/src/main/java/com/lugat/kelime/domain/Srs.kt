package com.lugat.kelime.domain

/**
 * Aralıklı tekrar (Leitner sistemi).
 * Doğru cevap kelimeyi bir üst kutuya taşır ve bir sonraki tekrarı ileri bir güne atar;
 * yanlış cevap kelimeyi 1. kutuya geri indirir ve hemen tekrar sırasına koyar.
 */
object Srs {
    const val MAX_BOX = 5
    const val LEARNED_BOX = 3
    private const val DAY = 24L * 60 * 60 * 1000

    /** Kutu → bir sonraki tekrara kadar geçecek gün sayısı */
    val INTERVAL_DAYS = intArrayOf(0, 1, 3, 7, 14, 30)

    fun answer(current: WordProgress?, wordId: String, correct: Boolean, now: Long): WordProgress {
        val p = current ?: WordProgress(wordId)
        return if (correct) {
            val box = (p.box + 1).coerceAtMost(MAX_BOX)
            p.copy(
                box = box,
                dueAt = now + INTERVAL_DAYS[box] * DAY,
                correct = p.correct + 1,
                lastSeen = now,
            )
        } else {
            p.copy(box = 1, dueAt = now, wrong = p.wrong + 1, lastSeen = now)
        }
    }

    fun isDue(p: WordProgress, now: Long): Boolean = p.box > 0 && p.dueAt <= now
}
