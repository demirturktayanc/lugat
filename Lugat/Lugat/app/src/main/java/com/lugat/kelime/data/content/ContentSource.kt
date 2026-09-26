package com.lugat.kelime.data.content

import android.content.Context
import com.lugat.kelime.domain.Content
import com.lugat.kelime.domain.DailyFact
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.domain.PartOfSpeech
import com.lugat.kelime.domain.Word
import org.json.JSONObject

/**
 * Kelime içeriği APK içindeki assets/content.json dosyasından bir kez okunur ve bellekte tutulur
 * (~760 kelime, birkaç yüz KB). Hem uygulama hem de bildirim işçisi (WorkManager) bunu kullanır.
 */
object ContentSource {
    private const val ASSET = "content.json"

    @Volatile
    private var cached: Content? = null

    fun get(context: Context): Content =
        cached ?: synchronized(this) {
            cached ?: parse(context.applicationContext.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })
                .also { cached = it }
        }

    fun parse(json: String): Content {
        val root = JSONObject(json)
        val decksJson = root.getJSONArray("decks")
        val decks = (0 until decksJson.length()).map { i ->
            val d = decksJson.getJSONObject(i)
            val id = d.getString("id")
            val wordsJson = d.getJSONArray("words")
            val words = (0 until wordsJson.length()).map { j ->
                val w = wordsJson.getJSONObject(j)
                Word(
                    id = w.getString("id"),
                    deckId = id,
                    en = w.getString("en"),
                    tr = w.getString("tr"),
                    pos = PartOfSpeech.of(w.getString("pos")),
                    exampleEn = w.getString("exEn"),
                    exampleTr = w.getString("exTr"),
                )
            }
            Deck(
                id = id,
                title = d.getString("title"),
                subtitle = d.getString("subtitle"),
                group = d.getString("group"),
                badge = d.getString("badge"),
                colorIndex = d.getInt("color"),
                words = words,
            )
        }
        val factsJson = root.getJSONArray("facts")
        val facts = (0 until factsJson.length()).map { i ->
            val f = factsJson.getJSONObject(i)
            DailyFact(
                en = f.getString("en"),
                pos = PartOfSpeech.of(f.getString("pos")),
                tr = f.getString("tr"),
                fact = f.getString("fact"),
                exampleEn = f.getString("exEn"),
                exampleTr = f.getString("exTr"),
            )
        }
        return Content(decks, facts)
    }
}
