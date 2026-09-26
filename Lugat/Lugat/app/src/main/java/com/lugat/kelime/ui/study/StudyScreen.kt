package com.lugat.kelime.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.ui.components.BigAction
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.components.ProgressRing
import com.lugat.kelime.ui.components.ThinBar
import com.lugat.kelime.ui.theme.Danger
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Success
import com.lugat.kelime.ui.theme.Tangerine

@Composable
fun StudyScreen(
    onClose: () -> Unit,
    onRestart: (mode: String, source: String) -> Unit,
    vm: StudyViewModel = hiltViewModel(),
) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        // ---- Üst çubuk
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Kapat") }
            Column(Modifier.weight(1f)) {
                EyebrowText(s.mode.title)
                Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            }
            if (!s.loading && !s.isEmpty && !s.finished && s.mode != StudyMode.MATCH) {
                Text("${(s.index + 1).coerceAtMost(s.total)} / ${s.total}", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (!s.finished && s.mode != StudyMode.MATCH) {
            ThinBar(
                s.progress, Tangerine, MaterialTheme.colorScheme.surfaceVariant,
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                s.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                s.isEmpty -> EmptySession(s.mode, onClose)
                s.finished -> ResultView(s, onClose, onRestart = { onRestart(s.mode.key, s.source) })
                else -> when (s.mode) {
                    StudyMode.CARDS -> CardsMode(s, vm)
                    StudyMode.QUIZ, StudyMode.BLANK -> QuizMode(s, vm)
                    StudyMode.WRITE, StudyMode.LISTEN -> TypeMode(s, vm)
                    StudyMode.MATCH -> MatchMode(s, vm)
                }
            }
        }
    }
}

@Composable
private fun EmptySession(mode: StudyMode, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("✨", fontSize = 56.sp)
        Text("Şimdilik çalışılacak kelime yok", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp))
        Text(
            if (mode == StudyMode.MATCH) "Eşleştirme için en az 4 kelime gerekiyor. Başka bir deste seçmeyi dene."
            else "Tekrar zamanı gelmiş kelimelerin yok. Yeni kelimeler için bir deste aç ya da Ayarlar'dan deste ekle.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        BigAction("Tamam", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onClick = onClose)
    }
}

@Composable
private fun ResultView(s: StudyUi, onClose: () -> Unit, onRestart: () -> Unit) {
    val answered = s.records.size
    val ratio = if (answered == 0) 0f else s.correctCount / answered.toFloat()
    val display = LocalDisplayFont.current
    val missed = s.records.filter { !it.correct }.map { it.word }.distinctBy { it.id }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        ProgressRing(ratio, Modifier.size(170.dp), color = if (ratio >= 0.6f) Success else Tangerine, stroke = 14.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%${(ratio * 100).toInt()}", fontFamily = display, fontSize = 44.sp)
                Text("doğruluk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            when {
                ratio >= 0.9f -> "Mükemmel!"
                ratio >= 0.7f -> "Çok iyi!"
                ratio >= 0.4f -> "İyi gidiyorsun"
                else -> "Güzel bir başlangıç"
            },
            fontFamily = display, fontSize = 34.sp, modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "${s.correctCount} doğru · ${answered - s.correctCount} yanlış",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (missed.isNotEmpty()) {
            Column(
                Modifier.padding(top = 24.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
            ) {
                EyebrowText("Tekrar listene eklendi")
                missed.forEach { w ->
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Danger))
                        Text(w.en, fontFamily = display, fontSize = 20.sp, modifier = Modifier.padding(start = 12.dp))
                        Text(" — ${w.tr}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        BigAction("Yeni tur", Tangerine, Color.White, onClick = onRestart)
        Spacer(Modifier.height(10.dp))
        BigAction("Bitir", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, onClick = onClose)
    }
}
