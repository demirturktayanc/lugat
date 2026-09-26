package com.lugat.kelime.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lugat.kelime.domain.CardDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val label: String) { SYSTEM("Sistem"), LIGHT("Açık"), DARK("Koyu") }
enum class Accent(val label: String) { US("Amerikan"), UK("İngiliz") }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val selectedDecks: Set<String> = emptySet(),
    val dailyGoal: Int = 20,
    val sessionSize: Int = 10,
    val notificationsEnabled: Boolean = true,
    val notifyHour: Int = 9,
    val notifyMinute: Int = 0,
    val accent: Accent = Accent.US,
    val speechRate: Float = 0.9f,
    val autoSpeak: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val direction: CardDirection = CardDirection.TR_TO_EN,
)

@Singleton
class SettingsStore @Inject constructor(private val store: DataStore<Preferences>) {

    private object K {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val DECKS = stringSetPreferencesKey("decks")
        val GOAL = intPreferencesKey("daily_goal")
        val SESSION = intPreferencesKey("session_size")
        val NOTIFY = booleanPreferencesKey("notify")
        val HOUR = intPreferencesKey("notify_hour")
        val MINUTE = intPreferencesKey("notify_minute")
        val ACCENT = stringPreferencesKey("accent")
        val RATE = floatPreferencesKey("speech_rate")
        val AUTOSPEAK = booleanPreferencesKey("auto_speak")
        val THEME = stringPreferencesKey("theme")
        val DIRECTION = stringPreferencesKey("direction")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            onboardingDone = p[K.ONBOARDED] ?: false,
            selectedDecks = p[K.DECKS] ?: d.selectedDecks,
            dailyGoal = p[K.GOAL] ?: d.dailyGoal,
            sessionSize = p[K.SESSION] ?: d.sessionSize,
            notificationsEnabled = p[K.NOTIFY] ?: d.notificationsEnabled,
            notifyHour = p[K.HOUR] ?: d.notifyHour,
            notifyMinute = p[K.MINUTE] ?: d.notifyMinute,
            accent = enumOr(p[K.ACCENT], d.accent),
            speechRate = p[K.RATE] ?: d.speechRate,
            autoSpeak = p[K.AUTOSPEAK] ?: d.autoSpeak,
            theme = enumOr(p[K.THEME], d.theme),
            direction = enumOr(p[K.DIRECTION], d.direction),
        )
    }.distinctUntilChanged()

    suspend fun completeOnboarding(decks: Set<String>, goal: Int, notify: Boolean, hour: Int, minute: Int) {
        store.edit {
            it[K.DECKS] = decks
            it[K.GOAL] = goal
            it[K.NOTIFY] = notify
            it[K.HOUR] = hour
            it[K.MINUTE] = minute
            it[K.ONBOARDED] = true
        }
    }

    suspend fun setDecks(decks: Set<String>) { store.edit { it[K.DECKS] = decks } }
    suspend fun setDailyGoal(v: Int) { store.edit { it[K.GOAL] = v } }
    suspend fun setSessionSize(v: Int) { store.edit { it[K.SESSION] = v } }
    suspend fun setNotifications(enabled: Boolean) { store.edit { it[K.NOTIFY] = enabled } }
    suspend fun setNotifyTime(hour: Int, minute: Int) { store.edit { it[K.HOUR] = hour; it[K.MINUTE] = minute } }
    suspend fun setAccent(v: Accent) { store.edit { it[K.ACCENT] = v.name } }
    suspend fun setSpeechRate(v: Float) { store.edit { it[K.RATE] = v } }
    suspend fun setAutoSpeak(v: Boolean) { store.edit { it[K.AUTOSPEAK] = v } }
    suspend fun setTheme(v: ThemeMode) { store.edit { it[K.THEME] = v.name } }
    suspend fun setDirection(v: CardDirection) { store.edit { it[K.DIRECTION] = v.name } }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
