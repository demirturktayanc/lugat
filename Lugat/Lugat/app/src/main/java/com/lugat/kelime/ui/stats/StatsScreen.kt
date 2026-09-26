package com.lugat.kelime.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.DeckStats
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.domain.SpecialDeck
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Streak
import com.lugat.kelime.domain.Word
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.components.ThinBar
import com.lugat.kelime.ui.components.WeekBars
import com.lugat.kelime.ui.theme.Ink
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Lavender
import com.lugat.kelime.ui.theme.Mint
import com.lugat.kelime.ui.theme.Mustard
import com.lugat.kelime.ui.theme.Rose
import com.lugat.kelime.ui.theme.Tangerine
import com.lugat.kelime.ui.theme.deckColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class StatsState(
    val loaded: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val learned: Int = 0,
    val mastered: Int = 0,
    val totalAnswers: Int = 0,
    val accuracy: Int = 0,
    val week: List<Int> = List(7) { 0 },
    val weekLabels: List<String> = List(7) { "" },
    val goal: Int = 20,
    val favorites: List<Word> = emptyList(),
    val hard: List<Word> = emptyList(),
    val decks: List<Pair<Deck, DeckStats>> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repo: LearningRepository,
    settings: SettingsStore,
) : ViewModel() {
    val state: StateFlow<StatsState> = combine(repo.progress, repo.activity, settings.settings) { progress, activity, s ->
        val today = repo.today()
        val days = activity.filter { it.answers > 0 }.map { it.day }.toSet()
        val byDay = activity.associateBy { it.day }
        val weekDays = (6 downTo 0).map { today - it }
        val tr = Locale.forLanguageTag("tr")
        val answers = activity.sumOf { it.answers }
        val correct = activity.sumOf { it.correct }
        val now = System.currentTimeMillis()
        StatsState(
            loaded = true,
            streak = Streak.current(days, today),
            bestStreak = Streak.best(days),
            learned = progress.values.count { it.isLearned },
            mastered = progress.values.count { it.isMastered },
            totalAnswers = answers,
            accuracy = if (answers == 0) 0 else correct * 100 / answers,
            week = weekDays.map { byDay[it]?.answers ?: 0 },
            weekLabels = weekDays.map { LocalDate.ofEpochDay(it).dayOfWeek.getDisplayName(TextStyle.SHORT, tr) },
            goal = s.dailyGoal,
            favorites = progress.values.filter { it.favorite }.mapNotNull { repo.content.wordsById[it.wordId] },
            hard = progress.values.filter { it.isHard }.mapNotNull { repo.content.wordsById[it.wordId] },
            decks = repo.content.decks.map { it to repo.deckStats(it, progress, now) }.filter { it.second.seen > 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}

@Composable
fun StatsScreen(onStudy: (mode: String, source: String) -> Unit, vm: StatsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    if (!s.loaded) return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
        Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
            EyebrowText("${s.totalAnswers} cevap · %${s.accuracy} doğruluk")
            Text("İlerleme", style = MaterialTheme.typography.displaySmall)
        }
        // ---- Özet kutuları
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("🔥 ${s.streak}", "gün seri", Tangerine, Color.White, Modifier.weight(1f))
            StatBox("${s.learned}", "öğrenildi", Mint, Color.White, Modifier.weight(1f))
            StatBox("${s.mastered}", "ustalaşıldı", Lavender, Color.White, Modifier.weight(1f))
        }
        Text(
            "En uzun seri: ${s.bestStreak} gün",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        // ---- Haftalık grafik
        Column(
            Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
        ) {
            Text("Son 7 gün", style = MaterialTheme.typography.titleLarge)
            Text("Kesikli çizgi günlük hedefin (${s.goal} kart)", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 14.dp))
            WeekBars(s.week, s.weekLabels, s.goal, Tangerine)
        }
        // ---- Özel listeler
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ListCard("Zor kelimeler", "${s.hard.size} kelime", Rose, Color.White, Modifier.weight(1f), enabled = s.hard.isNotEmpty()) {
                onStudy(StudyMode.CARDS.key, SpecialDeck.HARD)
            }
            ListCard("Favoriler", "${s.favorites.size} kelime", Mustard, Ink, Modifier.weight(1f), enabled = s.favorites.isNotEmpty()) {
                onStudy(StudyMode.CARDS.key, SpecialDeck.FAVORITES)
            }
        }
        // ---- Deste ilerlemesi
        if (s.decks.isNotEmpty()) {
            Text("Deste ilerlemesi", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp))
            s.decks.forEach { (deck, st) ->
                val c = deckColor(deck.colorIndex)
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(deck.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${st.learned}/${st.total}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    ThinBar(st.learnedRatio, c.base, MaterialTheme.colorScheme.surfaceVariant, height = 8.dp)
                }
            }
        } else {
            Text(
                "Çalışmaya başladığında destelerindeki ilerlemen burada görünecek.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, bg: Color, fg: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(bg).padding(16.dp)) {
        Text(value, fontFamily = LocalDisplayFont.current, fontSize = 30.sp, color = fg)
        Text(label, fontSize = 12.sp, color = fg.copy(alpha = 0.85f))
    }
}

@Composable
private fun ListCard(title: String, subtitle: String, bg: Color, fg: Color, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick).padding(16.dp),
    ) {
        Text(title, fontFamily = LocalDisplayFont.current, fontSize = 22.sp, color = fg)
        Text(if (enabled) "$subtitle · çalış →" else "Henüz yok", fontSize = 12.sp, color = fg.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold)
    }
}
