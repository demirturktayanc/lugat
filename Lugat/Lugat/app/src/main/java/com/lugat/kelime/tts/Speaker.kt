package com.lugat.kelime.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.lugat.kelime.data.prefs.Accent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Cihazın metin okuma motoru (TTS) ile İngilizce telaffuz. İnternet gerektirmez (motor yüklüyse). */
@Singleton
class Speaker @Inject constructor(@ApplicationContext private val context: Context) {

    private var tts: TextToSpeech? = null
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private var accent: Accent = Accent.US
    private var rate: Float = 0.9f
    private var pending: String? = null

    private fun ensure() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyVoice()
                _ready.value = true
                pending?.let { speakNow(it) }
                pending = null
            }
        }
    }

    fun configure(accent: Accent, rate: Float) {
        this.accent = accent
        this.rate = rate
        if (_ready.value) applyVoice()
    }

    private fun applyVoice() {
        val engine = tts ?: return
        val locale = if (accent == Accent.UK) Locale.UK else Locale.US
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.ENGLISH)
        }
        engine.setSpeechRate(rate)
    }

    fun speak(text: String) {
        ensure()
        if (_ready.value) speakNow(text) else pending = text
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lugat-${text.hashCode()}")
    }

    fun warmUp() = ensure()
}
