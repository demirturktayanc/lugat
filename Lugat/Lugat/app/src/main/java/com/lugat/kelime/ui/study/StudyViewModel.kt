package com.lugat.kelime.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.data.prefs.AppSettings
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.domain.AnswerChecker
import com.lugat.kelime.domain.AnswerResult
import com.lugat.kelime.domain.Blank
import com.lugat.kelime.domain.CardDirection
import com.lugat.kelime.domain.Quiz
import com.lugat.kelime.domain.SpecialDeck
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Word
import com.lugat.kelime.tts.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** Test ve boşluk doldurma soruları */
data class Question(
    val word: Word,
    val prompt: String,
    val promptIsEnglish: Boolean,
    val answer: String,
    val options: List<String>,
    /** Boşluk doldurmada cümlenin Türkçesi */
    val hint: String? = null,
)

data class AnswerRecord(val word: Word, val correct: Boolean)

data class StudyUi(
    val loading: Boolean = true,
    val mode: StudyMode = StudyMode.CARDS,
    val source: String = "",
    val title: String = "",
    val queue: List<Word> = emptyList(),
    val index: Int = 0,
    val questions: Map<String, Question> = emptyMap(),
    /** Kart yönü: true → önde İngilizce */
    val englishFirst: Map<String, Boolean> = emptyMap(),
    val records: List<AnswerRecord> = emptyList(),
    val requeued: Set<String> = emptySet(),
    val finished: Boolean = false,
    val autoSpeak: Boolean = true,
) {
    val current: Word? get() = queue.getOrNull(index)
    val total: Int get() = queue.size
    val correctCount: Int get() = records.count { it.correct }
    val progress: Float get() = if (queue.isEmpty()) 0f else index / queue.size.toFloat()
    val isEmpty: Boolean get() = !loading && queue.isEmpty()
}

@HiltViewModel
class StudyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: LearningRepository,
    private val settingsStore: SettingsStore,
    private val speaker: Speaker,
) : ViewModel() {

    private val mode = StudyMode.of(savedState.get<String>("mode") ?: "cards")
    private val source = savedState.get<String>("source") ?: SpecialDeck.DAILY
    private val random = Random(System.nanoTime())

    private val _ui = MutableStateFlow(StudyUi(mode = mode, source = source, title = titleOf(source)))
    val ui: StateFlow<StudyUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            val size = when (mode) {
                StudyMode.MATCH -> 10
                else -> settings.sessionSize
            }
            val words = repo.buildSession(source, mode, settings.selectedDecks, size)
            val minimum = if (mode == StudyMode.MATCH) 4 else 1
            val queue = if (words.size >= minimum) words else emptyList()
            _ui.update {
                it.copy(
                    loading = false,
                    queue = queue,
                    questions = buildQuestions(queue, settings),
                    englishFirst = queue.associate { w -> w.id to englishFirst(settings.direction) },
                    autoSpeak = settings.autoSpeak,
                )
            }
            if (mode == StudyMode.LISTEN) queue.firstOrNull()?.let { speak(it.en) }
        }
    }

    private fun englishFirst(d: CardDirection) = when (d) {
        CardDirection.TR_TO_EN -> false
        CardDirection.EN_TO_TR -> true
        CardDirection.MIXED -> random.nextBoolean()
    }

    private fun buildQuestions(words: List<Word>, settings: AppSettings): Map<String, Question> = when (mode) {
        StudyMode.QUIZ -> words.associate { w ->
            val enPrompt = englishFirst(settings.direction)
            val pool = repo.poolFor(w)
            w.id to if (enPrompt) {
                Question(w, w.en, true, w.tr, Quiz.options(w, pool, random) { it.tr })
            } else {
                Question(w, w.tr, false, w.en, Quiz.options(w, pool, random) { it.en })
            }
        }
        StudyMode.BLANK -> words.associate { w ->
            val pool = repo.poolFor(w)
            w.id to Question(
                word = w,
                prompt = Blank.make(w.exampleEn, w.en) ?: w.exampleEn,
                promptIsEnglish = true,
                answer = w.en,
                options = Quiz.options(w, pool, random) { it.en },
                hint = w.exampleTr,
            )
        }
        else -> emptyMap()
    }

    fun speak(text: String) = speaker.speak(text)

    fun colorIndex(deckId: String): Int = repo.content.deck(deckId)?.colorIndex ?: 0

    /** Test/kart/boşluk: cevabı kaydeder (ilerlemeyi [next] yapar). */
    fun answer(word: Word, correct: Boolean) {
        viewModelScope.launch { repo.recordAnswer(word, correct) }
        _ui.update { s ->
            val requeue = !correct && mode == StudyMode.CARDS && word.id !in s.requeued
            s.copy(
                records = s.records + AnswerRecord(word, correct),
                queue = if (requeue) s.queue + word else s.queue,
                requeued = if (requeue) s.requeued + word.id else s.requeued,
            )
        }
    }

    /** Yazma/dinleme: metni kontrol eder ve kaydeder. */
    fun check(word: Word, input: String): AnswerResult {
        val result = AnswerChecker.check(input, word.en)
        answer(word, result != AnswerResult.WRONG)
        return result
    }

    fun next() {
        _ui.update { s ->
            val i = s.index + 1
            if (i >= s.queue.size) s.copy(index = s.queue.size, finished = true) else s.copy(index = i)
        }
        val s = _ui.value
        if (!s.finished) {
            val w = s.current ?: return
            if (mode == StudyMode.LISTEN) speak(w.en)
        }
    }

    /** Eşleştirme oyunu bitince çağrılır. */
    fun finishMatch() { _ui.update { it.copy(index = it.queue.size, finished = true) } }

    private fun titleOf(source: String) = when (source) {
        SpecialDeck.REVIEW -> "Tekrar"
        SpecialDeck.DAILY -> "Günlük çalışma"
        SpecialDeck.FAVORITES -> "Favoriler"
        SpecialDeck.HARD -> "Zor kelimeler"
        else -> repo.content.deck(source)?.title ?: ""
    }
}
