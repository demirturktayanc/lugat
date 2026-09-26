@file:OptIn(ExperimentalMaterial3Api::class)

package com.lugat.kelime.ui.decks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.DeckStats
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Word
import com.lugat.kelime.domain.WordProgress
import com.lugat.kelime.tts.Speaker
import com.lugat.kelime.ui.components.DeckGrid
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.components.SpeakButton
import com.lugat.kelime.ui.components.ThinBar
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Mustard
import com.lugat.kelime.ui.theme.deckColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ------------------------------------------------------------------ Tüm desteler

@HiltViewModel
class DecksViewModel @Inject constructor(val repo: LearningRepository) : ViewModel() {
    val learned: StateFlow<Map<String, Int>> = repo.progress.map { progress ->
        val now = System.currentTimeMillis()
        repo.content.decks.associate { it.id to repo.deckStats(it, progress, now).learned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

@Composable
fun DecksScreen(onOpenDeck: (String) -> Unit, vm: DecksViewModel = hiltViewModel()) {
    val learned by vm.learned.collectAsStateWithLifecycle()
    DeckGrid(
        content = vm.repo.content,
        learnedOf = { learned[it] ?: 0 },
        selected = null,
        onClick = onOpenDeck,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        header = {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 20.dp, start = 4.dp)) {
                    EyebrowText("${vm.repo.content.wordsById.size} kelime · ${vm.repo.content.decks.size} deste")
                    Text("Desteler", style = MaterialTheme.typography.displaySmall)
                }
            }
        },
    )
}

// ------------------------------------------------------------------ Deste ayrıntısı

data class DeckDetail(
    val deck: Deck,
    val stats: DeckStats,
    val progress: Map<String, WordProgress>,
)

@HiltViewModel
class DeckViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: LearningRepository,
    val speaker: Speaker,
) : ViewModel() {
    val deck: Deck? = savedState.get<String>("id")?.let { repo.content.deck(it) }

    val detail: StateFlow<DeckDetail?> = repo.progress.map { progress ->
        deck?.let { DeckDetail(it, repo.deckStats(it, progress, System.currentTimeMillis()), progress) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val blankCount: Int = deck?.words?.count(repo::isBlankable) ?: 0

    fun toggleFavorite(word: Word) = viewModelScope.launch { repo.toggleFavorite(word.id) }
}

@Composable
fun DeckScreen(
    onBack: () -> Unit,
    onStudy: (mode: String, source: String) -> Unit,
    vm: DeckViewModel = hiltViewModel(),
) {
    val detail by vm.detail.collectAsStateWithLifecycle()
    val d = detail ?: return
    val c = deckColor(d.deck.colorIndex)
    val display = LocalDisplayFont.current

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        // ---- Renkli başlık
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(c.base).statusBarsPadding(),
            ) {
                Text(
                    d.deck.badge, fontFamily = display, fontSize = 150.sp, color = c.on.copy(alpha = 0.13f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp), maxLines = 1,
                )
                Column(Modifier.padding(start = 8.dp, end = 20.dp, bottom = 24.dp)) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = c.on) }
                    Column(Modifier.padding(start = 12.dp)) {
                        EyebrowText(d.deck.group, c.on.copy(alpha = 0.8f))
                        Text(d.deck.title, fontFamily = display, fontSize = 38.sp, lineHeight = 42.sp, color = c.on)
                        Text(d.deck.subtitle, color = c.on.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                            HeaderStat("${d.stats.total}", "kelime", c.on)
                            HeaderStat("${d.stats.learned}", "öğrenildi", c.on)
                            HeaderStat("${d.stats.due}", "tekrar", c.on)
                        }
                        Spacer(Modifier.height(14.dp))
                        ThinBar(d.stats.learnedRatio, c.on, c.on.copy(alpha = 0.25f), height = 8.dp)
                    }
                }
            }
        }
        // ---- Çalışma modları
        item {
            Text("Nasıl çalışmak istersin?", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp))
        }
        val modes = StudyMode.entries.filter { it != StudyMode.BLANK || vm.blankCount >= 4 }
        items(modes.chunked(2)) { pair ->
            Row(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { mode ->
                    ModeCard(mode, c.soft, Modifier.weight(1f)) { onStudy(mode.key, d.deck.id) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        // ---- Kelime listesi
        item {
            Text("Kelimeler", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 4.dp))
        }
        items(d.deck.words, key = { it.id }) { w ->
            WordRow(w, d.progress[w.id], c.base, onSpeak = { vm.speaker.speak(w.en) }, onFavorite = { vm.toggleFavorite(w) })
            HorizontalDivider(Modifier.padding(start = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun HeaderStat(value: String, label: String, color: Color) {
    Column {
        Text(value, fontFamily = LocalDisplayFont.current, fontSize = 28.sp, color = color)
        Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.8f))
    }
}

private fun modeGlyph(mode: StudyMode) = when (mode) {
    StudyMode.CARDS -> "⇄"
    StudyMode.QUIZ -> "?"
    StudyMode.WRITE -> "Aa"
    StudyMode.LISTEN -> "♪"
    StudyMode.MATCH -> "⧉"
    StudyMode.BLANK -> "_"
}

@Composable
private fun ModeCard(mode: StudyMode, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) {
            Text(modeGlyph(mode), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF14162B))
        }
        Text(mode.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WordRow(w: Word, p: WordProgress?, color: Color, onSpeak: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        // Leitner kutusu göstergesi: 5 nokta
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { i ->
                val filled = (p?.box ?: 0) >= 5 - i
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (filled) color else MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(w.en, fontFamily = LocalDisplayFont.current, fontSize = 20.sp)
            Text("${w.pos.label} · ${w.tr}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SpeakButton(onSpeak, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = onFavorite) {
            val fav = p?.favorite == true
            Icon(
                if (fav) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = if (fav) "Favoriden çıkar" else "Favorilere ekle",
                tint = if (fav) Mustard else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
