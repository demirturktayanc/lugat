package com.lugat.kelime.ui.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.prefs.AppSettings
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.notify.DailyWordNotifications
import com.lugat.kelime.tts.Speaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    app: Application,
    settingsStore: SettingsStore,
    speaker: Speaker,
) : AndroidViewModel(app) {

    /** null: ayarlar henüz okunmadı (ilk karede yanlış ekran/tema göstermemek için) */
    val settings: StateFlow<AppSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Telaffuz ayarları
        settings.filterNotNull()
            .map { it.accent to it.speechRate }
            .distinctUntilChanged()
            .onEach { (accent, rate) -> speaker.configure(accent, rate) }
            .launchIn(viewModelScope)

        // Günün kelimesi bildirimi planı
        settings.filterNotNull()
            .map { Triple(it.onboardingDone && it.notificationsEnabled, it.notifyHour, it.notifyMinute) }
            .distinctUntilChanged()
            .onEach { (enabled, h, m) -> DailyWordNotifications.schedule(getApplication(), enabled, h, m) }
            .launchIn(viewModelScope)
    }
}
