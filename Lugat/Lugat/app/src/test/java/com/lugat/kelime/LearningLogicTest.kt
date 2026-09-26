package com.lugat.kelime

import com.lugat.kelime.domain.AnswerChecker
import com.lugat.kelime.domain.AnswerResult
import com.lugat.kelime.domain.Blank
import com.lugat.kelime.domain.PartOfSpeech
import com.lugat.kelime.domain.Quiz
import com.lugat.kelime.domain.Sessions
import com.lugat.kelime.domain.Srs
import com.lugat.kelime.domain.Streak
import com.lugat.kelime.domain.Word
import com.lugat.kelime.domain.WordOfDay
import com.lugat.kelime.domain.WordProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LearningLogicTest {
    private val day = 24L * 60 * 60 * 1000
    private fun w(i: Int, pos: PartOfSpeech = PartOfSpeech.NOUN) =
        Word("d:w$i", "d", "word$i", "kelime$i", pos, "This is word$i.", "Bu kelime$i.")

    @Test
    fun srsDogruCevapKutuyuArtirir() {
        val p1 = Srs.answer(null, "x", true, 0)
        assertEquals(1, p1.box); assertEquals(day, p1.dueAt)
        val p2 = Srs.answer(p1, "x", true, 10)
        assertEquals(2, p2.box); assertEquals(10 + 3 * day, p2.dueAt)
        var p = p2
        repeat(10) { p = Srs.answer(p, "x", true, 0) }
        assertEquals(Srs.MAX_BOX, p.box)
        assertTrue(p.isMastered)
    }

    @Test
    fun srsYanlisCevapBirinciKutuyaDondurur() {
        val p = Srs.answer(WordProgress("x", box = 4, correct = 5), "x", false, 100)
        assertEquals(1, p.box); assertEquals(100, p.dueAt); assertEquals(1, p.wrong)
        assertTrue(Srs.isDue(p, 100))
    }

    @Test
    fun cevapKontrolu() {
        assertEquals(AnswerResult.CORRECT, AnswerChecker.check("  Apple ", "apple"))
        assertEquals(AnswerResult.CORRECT, AnswerChecker.check("to run", "run"))
        assertEquals(AnswerResult.CORRECT, AnswerChecker.check("hard working", "hard-working"))
        assertEquals(AnswerResult.CORRECT, AnswerChecker.check("how much is it", "How much is it?"))
        assertEquals(AnswerResult.ALMOST, AnswerChecker.check("aple", "apple"))
        assertEquals(AnswerResult.ALMOST, AnswerChecker.check("enviroment", "environment"))
        assertEquals(AnswerResult.WRONG, AnswerChecker.check("cat", "car"))
        assertEquals(AnswerResult.WRONG, AnswerChecker.check("", "apple"))
        assertEquals(AnswerResult.WRONG, AnswerChecker.check("banana", "apple"))
        assertEquals(3, AnswerChecker.levenshtein("kitten", "sitting"))
    }

    @Test
    fun seriHesabi() {
        assertEquals(3, Streak.current(setOf(8L, 9L, 10L), 10))
        assertEquals(3, Streak.current(setOf(8L, 9L, 10L), 11)) // bugün henüz çalışılmadı, seri sürüyor
        assertEquals(0, Streak.current(setOf(8L, 9L, 10L), 12))
        assertEquals(0, Streak.current(emptySet(), 12))
        assertEquals(4, Streak.best(setOf(1L, 2L, 5L, 6L, 7L, 8L, 10L)))
    }

    @Test
    fun boslukDoldurma() {
        assertEquals("I _____ an apple every day.", Blank.make("I eat an apple every day.", "eat"))
        assertEquals("_____ the meeting.", Blank.make("Call off the meeting.", "call off"))
        assertNull(Blank.make("He ate it.", "eat"))
        assertNull(Blank.make("A theatre.", "eat")) // kelime içinde geçmesi sayılmaz
    }

    @Test
    fun testSecenekleriTekrarsizVeDogruyuIcerir() {
        val pool = (1..10).map { w(it) }
        val opts = Quiz.options(pool[0], pool, Random(1)) { it.en }
        assertEquals(4, opts.size)
        assertEquals(4, opts.toSet().size)
        assertTrue("word1" in opts)
        val small = Quiz.options(pool[0], pool.take(2), Random(1)) { it.en }
        assertEquals(2, small.size)
    }

    @Test
    fun oturumOnceTekrarSonraYeni() {
        val words = (1..20).map { w(it) }
        val progress = mapOf(
            "d:w5" to WordProgress("d:w5", box = 2, dueAt = 50),
            "d:w6" to WordProgress("d:w6", box = 3, dueAt = 5000),
        )
        val s = Sessions.forDeck(words, progress, now = 100, size = 10, random = Random(3))
        assertEquals(10, s.size)
        assertTrue(words[4] in s) // tekrarı gelmiş
        assertTrue(words[5] !in s) // tekrarı gelmemiş, yeni de değil
        assertEquals(10, s.toSet().size)
        assertEquals(listOf(words[4]), Sessions.review(words, progress, 100, 20))
    }

    @Test
    fun gununKelimesiHerGunFarkliVeDongusel() {
        val n = 51
        val idx = (0L until n).map { WordOfDay.index(n, 20000 + it) }
        assertEquals(n, idx.toSet().size)
        assertEquals(WordOfDay.index(n, 20000L), WordOfDay.index(n, 20000L + n))
    }
}
