@file:OptIn(ExperimentalMaterial3Api::class)

package com.lugat.kelime.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.data.prefs.Accent
import com.lugat.kelime.data.prefs.AppSettings
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.data.prefs.ThemeMode
import com.lugat.kelime.domain.CardDirection
import com.lugat.kelime.notify.DailyWordNotifications
import com.lugat.kelime.tts.Speaker
import com.lugat.kelime.ui.components.BigAction
import com.lugat.kelime.ui.components.DeckGrid
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.onboarding.TimeDialog
import com.lugat.kelime.ui.theme.Danger
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Tangerine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    val repo: LearningRepository,
    private val speaker: Speaker,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val learnedByDeck: StateFlow<Map<String, Int>> = repo.progress.map { progress ->
        val now = System.currentTimeMillis()
        repo.content.decks.associate { it.id to repo.deckStats(it, progress, now).learned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun toggleDeck(id: String, current: Set<String>) = viewModelScope.launch {
        val next = if (id in current) current - id else current + id
        if (next.isNotEmpty()) store.setDecks(next)
    }
    fun setGoal(v: Int) = viewModelScope.launch { store.setDailyGoal(v) }
    fun setSession(v: Int) = viewModelScope.launch { store.setSessionSize(v) }
    fun setDirection(v: CardDirection) = viewModelScope.launch { store.setDirection(v) }
    fun setAccent(v: Accent) = viewModelScope.launch { store.setAccent(v) }
    fun setRate(v: Float) = viewModelScope.launch { store.setSpeechRate(v) }
    fun setAutoSpeak(v: Boolean) = viewModelScope.launch { store.setAutoSpeak(v) }
    fun setNotifications(v: Boolean) = viewModelScope.launch { store.setNotifications(v) }
    fun setTime(h: Int, m: Int) = viewModelScope.launch { store.setNotifyTime(h, m) }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { store.setTheme(v) }
    fun reset() = viewModelScope.launch { repo.resetProgress() }
    fun testSpeech() = speaker.speak("Hello! Welcome to Lugat.")
}

@Composable
fun SettingsScreen(onChooseDecks: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val context = LocalContext.current
    var timeDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setNotifications(granted)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 32.dp)) {
        Text("Ayarlar", style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(start = 20.dp, top = 20.dp))

        Group("Öğrenme") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onChooseDecks).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Destelerim", style = MaterialTheme.typography.titleMedium)
                    Text("${s.selectedDecks.size} deste seçili", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Düzenle", color = Tangerine, fontWeight = FontWeight.SemiBold)
            }
            Label("Günlük hedef (kart)")
            Chips(listOf(10, 20, 30, 50), s.dailyGoal, { "$it" }, vm::setGoal)
            Label("Oturum uzunluğu (kelime)")
            Chips(listOf(5, 10, 15, 20), s.sessionSize, { "$it" }, vm::setSession)
            Label("Kart yönü")
            Segmented(CardDirection.entries, s.direction, { it.label }, vm::setDirection)
        }

        Group("Telaffuz") {
            Segmented(Accent.entries, s.accent, { it.label }, vm::setAccent)
            Label("Konuşma hızı")
            var rate by remember(s.speechRate) { mutableFloatStateOf(s.speechRate) }
            Slider(value = rate, onValueChange = { rate = it }, onValueChangeFinished = { vm.setRate(rate) }, valueRange = 0.5f..1.3f)
            SwitchRow("Otomatik seslendir", "İngilizce kelime göründüğünde oku", s.autoSpeak, vm::setAutoSpeak)
            TextButton(onClick = { vm.testSpeech() }) { Text("Sesi dene") }
        }

        Group("Günün kelimesi") {
            SwitchRow("Günlük bildirim", "Her gün bir kelime ve ilginç bir bilgi", s.notificationsEnabled) { on ->
                if (on && Build.VERSION.SDK_INT >= 33 && !DailyWordNotifications.hasPermission(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else vm.setNotifications(on)
            }
            if (s.notificationsEnabled) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { timeDialog = true }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Bildirim saati", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("%02d:%02d".format(s.notifyHour, s.notifyMinute), fontFamily = LocalDisplayFont.current,
                        style = MaterialTheme.typography.headlineSmall)
                }
                TextButton(onClick = { DailyWordNotifications.show(context) }) { Text("Bildirimi şimdi dene") }
            }
        }

        Group("Görünüm") {
            Segmented(ThemeMode.entries, s.theme, { it.label }, vm::setTheme)
        }

        Group("Veri") {
            Text(
                "İlerlemeyi sıfırla",
                color = Danger,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { resetDialog = true }.padding(vertical = 8.dp),
            )
            Text(
                "Lugat v1.0 · ${vm.repo.content.wordsById.size} kelime, ${vm.repo.content.decks.size} deste. " +
                    "Kelime listeleri, örnek cümleler ve bilgiler bu uygulama için özgün olarak hazırlanmıştır. " +
                    "Başlık yazı tipi: DM Serif Display (SIL Open Font License). Telaffuz cihazınızın metin okuma motoruyla yapılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (timeDialog) {
        TimeDialog(s.notifyHour, s.notifyMinute, onDismiss = { timeDialog = false }) { h, m ->
            vm.setTime(h, m); timeDialog = false
        }
    }
    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("İlerleme sıfırlansın mı?") },
            text = { Text("Öğrenilen kelimeler, seri ve istatistikler silinir. Bu işlem geri alınamaz.") },
            confirmButton = { TextButton(onClick = { vm.reset(); resetDialog = false }) { Text("Sıfırla", color = Danger) } },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
    ) {
        EyebrowText(title, Tangerine, Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
private fun <T> Chips(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            FilterChip(selected = v == selected, onClick = { onSelect(v) }, label = { Text(label(v)) })
        }
    }
}

@Composable
private fun <T> Segmented(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        values.forEachIndexed { i, v ->
            SegmentedButton(
                selected = v == selected,
                onClick = { onSelect(v) },
                shape = SegmentedButtonDefaults.itemShape(i, values.size),
            ) { Text(label(v), maxLines = 1) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ------------------------------------------------------------------ Deste seçimi

@Composable
fun ChooseDecksScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val learned by vm.learnedByDeck.collectAsStateWithLifecycle()
    val s = settings ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            Text("Destelerim", style = MaterialTheme.typography.headlineSmall)
        }
        DeckGrid(
            content = vm.repo.content,
            learnedOf = { learned[it] ?: 0 },
            selected = s.selectedDecks,
            onClick = { vm.toggleDeck(it, s.selectedDecks) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            header = {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Ana sayfada ve günlük çalışmada kullanılacak desteleri seç. En az bir deste seçili kalmalı.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            },
        )
        BigAction("Tamam", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
            Modifier.padding(20.dp), onClick = onBack)
    }
}
