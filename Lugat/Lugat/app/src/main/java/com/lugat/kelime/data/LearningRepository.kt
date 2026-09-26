package com.lugat.kelime.data

import androidx.room.withTransaction
import com.lugat.kelime.data.db.ActivityEntity
import com.lugat.kelime.data.db.LugatDatabase
import com.lugat.kelime.data.db.ProgressEntity
import com.lugat.kelime.domain.Blank
import com.lugat.kelime.domain.Content
import com.lugat.kelime.domain.DailyFact
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.domain.Sessions
import com.lugat.kelime.domain.SpecialDeck
import com.lugat.kelime.domain.Srs
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Word
import com.lugat.kelime.domain.WordOfDay
import com.lugat.kelime.domain.WordProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class DayActivity(val day: Long, val answers: Int, val correct: Int, val learned: Int)

data class DeckStats(val total: Int, val seen: Int, val learned: Int, val mastered: Int, val due: Int) {
    val learnedRatio: Float get() = if (total == 0) 0f else learned / total.toFloat()
    val seenRatio: Float get() = if (total == 0) 0f else seen / total.toFloat()
}

@Singleton
class LearningRepository @Inject constructor(
    val content: Content,
    private val db: LugatDatabase,
) {
    private val dao = db.dao()

    val progress: Flow<Map<String, WordProgress>> =
        dao.observeProgress().map { list -> list.associate { it.wordId to it.toDomain() } }

    val activity: Flow<List<DayActivity>> =
        dao.observeActivity().map { list -> list.map { DayActivity(it.day, it.answers, it.correct, it.learned) } }

    fun today(): Long = LocalDate.now().toEpochDay()

    fun wordOfDay(day: Long = today()): DailyFact? =
        content.facts.getOrNull(WordOfDay.index(content.facts.size, day))

    fun deckStats(deck: Deck, progress: Map<String, WordProgress>, now: Long): DeckStats {
        var seen = 0; var learned = 0; var mastered = 0; var due = 0
        for (w in deck.words) {
            val p = progress[w.id] ?: continue
            if (p.box > 0) seen++
            if (p.isLearned) learned++
            if (p.isMastered) mastered++
            if (Srs.isDue(p, now)) due++
        }
        return DeckStats(deck.words.size, seen, learned, mastered, due)
    }

    fun wordsOf(deckIds: Set<String>): List<Word> =
        content.decks.filter { it.id in deckIds }.flatMap { it.words }

    /** Test/boşluk doldurma çeldiricileri için havuz: kelimenin kendi destesi. */
    fun poolFor(word: Word): List<Word> = content.deck(word.deckId)?.words ?: emptyList()

    fun isBlankable(word: Word): Boolean = Blank.make(word.exampleEn, word.en) != null

    /**
     * Çalışma oturumu kelimeleri.
     * [source]: deste kimliği ya da [SpecialDeck] sabitlerinden biri.
     */
    suspend fun buildSession(source: String, mode: StudyMode, selectedDecks: Set<String>, size: Int): List<Word> {
        val progress = progress.first()
        val now = System.currentTimeMillis()
        val random = Random(now)
        fun eligible(list: List<Word>) = if (mode == StudyMode.BLANK) list.filter(::isBlankable) else list
        val selectedWords = eligible(wordsOf(selectedDecks.ifEmpty { content.decks.map { it.id }.toSet() }))
        return when (source) {
            SpecialDeck.REVIEW -> Sessions.review(selectedWords, progress, now, size * 2)
            SpecialDeck.FAVORITES -> eligible(content.wordsById.values.toList())
                .filter { progress[it.id]?.favorite == true }.shuffled(random).take(size * 2)
            SpecialDeck.HARD -> eligible(content.wordsById.values.toList())
                .filter { progress[it.id]?.isHard == true }.shuffled(random).take(size * 2)
            SpecialDeck.DAILY -> {
                // Günlük karma: önce tekrarı gelenler, sonra seçili destelerden yeni kelimeler
                val review = Sessions.review(selectedWords, progress, now, size / 2)
                val fresh = selectedWords.filter { (progress[it.id]?.box ?: 0) == 0 && it !in review }
                (review + fresh.take(size - review.size)).shuffled(random)
            }
            else -> Sessions.forDeck(eligible(content.deck(source)?.words ?: emptyList()), progress, now, size, random)
        }
    }

    /** Cevabı kaydeder; kelime yeni öğrenildiyse (box 3'e ulaştıysa) günlük "öğrenilen" sayacını artırır. */
    suspend fun recordAnswer(word: Word, correct: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            val before = dao.progress(word.id)?.toDomain()
            val after = Srs.answer(before, word.id, correct, now)
            dao.upsertProgress(ProgressEntity.from(after))
            val newlyLearned = (before?.isLearned != true) && after.isLearned
            val day = today()
            val a = dao.activity(day) ?: ActivityEntity(day, 0, 0, 0)
            dao.upsertActivity(
                a.copy(
                    answers = a.answers + 1,
                    correct = a.correct + if (correct) 1 else 0,
                    learned = a.learned + if (newlyLearned) 1 else 0,
                )
            )
        }
    }

    suspend fun toggleFavorite(wordId: String) = withContext(Dispatchers.IO) {
        val p = dao.progress(wordId)?.toDomain() ?: WordProgress(wordId)
        dao.upsertProgress(ProgressEntity.from(p.copy(favorite = !p.favorite)))
    }

    suspend fun resetProgress() = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.clearProgress()
            dao.clearActivity()
        }
    }
}
