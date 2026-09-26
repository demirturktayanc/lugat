package com.lugat.kelime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.DeckStats
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.domain.DailyFact
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.domain.SpecialDeck
import com.lugat.kelime.domain.Srs
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Streak
import com.lugat.kelime.tts.Speaker
import com.lugat.kelime.ui.components.BigAction
import com.lugat.kelime.ui.components.DeckTile
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.components.ProgressRing
import com.lugat.kelime.ui.components.SectionHeader
import com.lugat.kelime.ui.components.SpeakButton
import com.lugat.kelime.ui.theme.Ink
import com.lugat.kelime.ui.theme.InkSoft
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Lavender
import com.lugat.kelime.ui.theme.Mustard
import com.lugat.kelime.ui.theme.Tangerine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HomeState(
    val loaded: Boolean = false,
    val streak: Int = 0,
    val todayAnswers: Int = 0,
    val goal: Int = 20,
    val due: Int = 0,
    val newAvailable: Int = 0,
    val totalLearned: Int = 0,
    val fact: DailyFact? = null,
    val decks: List<Pair<Deck, DeckStats>> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: LearningRepository,
    settings: SettingsStore,
    val speaker: Speaker,
) : ViewModel() {
    val state: StateFlow<HomeState> = combine(settings.settings, repo.progress, repo.activity) { s, progress, activity ->
        val now = System.currentTimeMillis()
        val today = repo.today()
        val selected = repo.content.decks.filter { it.id in s.selectedDecks }
        val stats = selected.map { it to repo.deckStats(it, progress, now) }
        HomeState(
            loaded = true,
            streak = Streak.current(activity.filter { it.answers > 0 }.map { it.day }.toSet(), today),
            todayAnswers = activity.firstOrNull { it.day == today }?.answers ?: 0,
            goal = s.dailyGoal,
            due = stats.sumOf { it.second.due },
            newAvailable = stats.sumOf { it.second.total - it.second.seen },
            totalLearned = progress.values.count { it.box >= Srs.LEARNED_BOX },
            fact = repo.wordOfDay(today),
            decks = stats,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

@Composable
fun HomeScreen(
    onStudy: (mode: String, source: String) -> Unit,
    onOpenDeck: (String) -> Unit,
    onOpenWordOfDay: () -> Unit,
    onChooseDecks: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    if (!s.loaded) return
    val display = LocalDisplayFont.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp),
    ) {
        // ---- Başlık: tarih + selamlama + seri
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                EyebrowText(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("tr"))))
                Text(greeting(), fontFamily = display, fontSize = 34.sp, lineHeight = 38.sp, modifier = Modifier.padding(top = 4.dp))
            }
            StreakBadge(s.streak)
        }

        // ---- Günlük hedef kartı
        Row(
            Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(
                progress = s.todayAnswers / s.goal.toFloat(),
                modifier = Modifier.size(96.dp),
                color = Tangerine,
                stroke = 11.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${s.todayAnswers}", fontFamily = display, fontSize = 30.sp)
                    Text("/ ${s.goal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(start = 20.dp).weight(1f)) {
                Text(
                    if (s.todayAnswers >= s.goal) "Bugünkü hedef tamam!" else "Günlük hedef",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (s.todayAnswers >= s.goal) "Harika gidiyorsun. İstersen devam et."
                    else "${(s.goal - s.todayAnswers).coerceAtLeast(0)} kart kaldı · ${s.totalLearned} kelime öğrenildi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // ---- Ana eylemler
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BigAction(
                text = if (s.due > 0) "Tekrar et · ${s.due} kelime hazır" else "Bugünün çalışmasını başlat",
                background = Tangerine,
                content = Color.White,
            ) {
                onStudy(StudyMode.CARDS.key, if (s.due > 0) SpecialDeck.REVIEW else SpecialDeck.DAILY)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMode("Test", "4 seçenek", Lavender, Color.White, Modifier.weight(1f)) {
                    onStudy(StudyMode.QUIZ.key, SpecialDeck.DAILY)
                }
                QuickMode("Eşleştir", "Zamana karşı", Mustard, Ink, Modifier.weight(1f)) {
                    onStudy(StudyMode.MATCH.key, SpecialDeck.DAILY)
                }
            }
        }

        // ---- Günün kelimesi
        s.fact?.let { fact -> WordOfDayCard(fact, onSpeak = { vm.speaker.speak(fact.en) }, onClick = onOpenWordOfDay) }

        // ---- Destelerim
        SectionHeader("Destelerim") {
            Text(
                "Düzenle",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onChooseDecks).padding(8.dp),
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            s.decks.forEach { (deck, st) ->
                DeckTile(deck, st.learned, st.total, Modifier.width(220.dp)) { onOpenDeck(deck.id) }
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Günaydın!"
    in 12..17 -> "İyi günler!"
    in 18..22 -> "İyi akşamlar!"
    else -> "İyi geceler!"
}

@Composable
private fun StreakBadge(streak: Int) {
    Column(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (streak > 0) Tangerine.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔥 $streak", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("gün seri", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickMode(title: String, subtitle: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(16.dp),
    ) {
        Text(title, color = fg, fontFamily = LocalDisplayFont.current, fontSize = 22.sp)
        Text(subtitle, color = fg.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun WordOfDayCard(fact: DailyFact, onSpeak: () -> Unit, onClick: () -> Unit) {
    val display = LocalDisplayFont.current
    Box(
        Modifier.padding(20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Ink, InkSoft, Color(0xFF3B2F7A))))
            .clickable(onClick = onClick)
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyebrowText("Günün kelimesi", Color(0xFFFFB199), Modifier.weight(1f))
                SpeakButton(onSpeak, tint = Color.White)
            }
            Text(fact.en, fontFamily = display, fontSize = 40.sp, color = Color.White, lineHeight = 44.sp)
            Text("${fact.pos.label} · ${fact.tr}", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                fact.fact,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Devamını oku →", color = Color(0xFFFFB199), fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp))
        }
    }
}
