package com.lugat.kelime.domain

/** Kelime türü; kısa kodlar content.json'daki "pos" alanıdır. */
enum class PartOfSpeech(val code: String, val label: String) {
    NOUN("n", "isim"),
    VERB("v", "fiil"),
    ADJECTIVE("adj", "sıfat"),
    ADVERB("adv", "zarf"),
    PHRASAL("phr", "öbek fiil"),
    PREPOSITION("prep", "edat"),
    CONJUNCTION("conj", "bağlaç"),
    PRONOUN("pron", "zamir"),
    EXPRESSION("exp", "kalıp ifade");

    companion object {
        fun of(code: String): PartOfSpeech = entries.firstOrNull { it.code == code } ?: EXPRESSION
    }
}

data class Word(
    val id: String,
    val deckId: String,
    val en: String,
    val tr: String,
    val pos: PartOfSpeech,
    val exampleEn: String,
    val exampleTr: String,
)

data class Deck(
    val id: String,
    val title: String,
    val subtitle: String,
    val group: String,
    val badge: String,
    val colorIndex: Int,
    val words: List<Word>,
)

/** "Günün kelimesi" girdisi: kelime + ilginç bilgi (köken ya da yalancı eş). */
data class DailyFact(
    val en: String,
    val pos: PartOfSpeech,
    val tr: String,
    val fact: String,
    val exampleEn: String,
    val exampleTr: String,
)

class Content(val decks: List<Deck>, val facts: List<DailyFact>) {
    val wordsById: Map<String, Word> = decks.flatMap { it.words }.associateBy { it.id }
    private val decksById = decks.associateBy { it.id }
    fun deck(id: String): Deck? = decksById[id]
    val groups: List<String> = decks.map { it.group }.distinct()
}

/** Bir kelimenin öğrenme durumu (Leitner kutusu). box = 0 → henüz öğrenilmedi. */
data class WordProgress(
    val wordId: String,
    val box: Int = 0,
    val dueAt: Long = 0L,
    val correct: Int = 0,
    val wrong: Int = 0,
    val lastSeen: Long = 0L,
    val favorite: Boolean = false,
) {
    val isLearned: Boolean get() = box >= Srs.LEARNED_BOX
    val isMastered: Boolean get() = box >= Srs.MAX_BOX
    val isHard: Boolean get() = wrong >= 2 && wrong > correct / 2
}

enum class StudyMode(val key: String, val title: String, val subtitle: String) {
    CARDS("cards", "Kartlar", "Çevir, kaydır, öğren"),
    QUIZ("quiz", "Test", "Dört seçenekten doğruyu bul"),
    WRITE("write", "Yazma", "Türkçesini gör, İngilizcesini yaz"),
    LISTEN("listen", "Dinle ve Yaz", "Duyduğun kelimeyi yaz"),
    MATCH("match", "Eşleştir", "Zamana karşı çiftleri bul"),
    BLANK("blank", "Boşluk Doldur", "Cümledeki eksik kelimeyi seç");

    companion object {
        fun of(key: String): StudyMode = entries.firstOrNull { it.key == key } ?: CARDS
    }
}

enum class CardDirection(val label: String) {
    TR_TO_EN("Önde Türkçe"),
    EN_TO_TR("Önde İngilizce"),
    MIXED("Karışık"),
}

/** Özel çalışma kaynakları (deste yerine). */
object SpecialDeck {
    const val REVIEW = "_review"
    const val FAVORITES = "_favorites"
    const val HARD = "_hard"
    const val DAILY = "_daily"
}
