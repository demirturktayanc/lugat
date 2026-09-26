@file:OptIn(ExperimentalMaterial3Api::class)

package com.lugat.kelime.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lugat.kelime.data.prefs.SettingsStore
import com.lugat.kelime.domain.Content
import com.lugat.kelime.ui.components.BigAction
import com.lugat.kelime.ui.components.DeckGrid
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.theme.Ink
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Mint
import com.lugat.kelime.ui.theme.Mustard
import com.lugat.kelime.ui.theme.PaperCard
import com.lugat.kelime.ui.theme.Tangerine
import com.lugat.kelime.ui.theme.Lavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val content: Content,
    private val settings: SettingsStore,
) : ViewModel() {
    fun finish(decks: Set<String>, goal: Int, notify: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { settings.completeOnboarding(decks, goal, notify, hour, minute) }
    }
}

private data class GoalOption(val value: Int, val title: String, val note: String, val color: Color)

private val GOALS = listOf(
    GoalOption(10, "Rahat", "Günde ~5 dakika", Mint),
    GoalOption(20, "Düzenli", "Günde ~10 dakika", Tangerine),
    GoalOption(30, "Ciddi", "Günde ~15 dakika", Lavender),
    GoalOption(50, "Yoğun", "Sınav modu: ~25 dakika", Mustard),
)

@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var decks by rememberSaveable { mutableStateOf(setOf<String>()) }
    var goal by rememberSaveable { mutableIntStateOf(20) }
    var notify by rememberSaveable { mutableStateOf(true) }
    var hour by rememberSaveable { mutableIntStateOf(9) }
    var minute by rememberSaveable { mutableIntStateOf(0) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.finish(decks, goal, notify && granted, hour, minute)
    }
    val finish = {
        if (notify && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else vm.finish(decks, goal, notify, hour, minute)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        if (step > 0) StepDots(step, 3, Modifier.padding(top = 16.dp, start = 24.dp))
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            when (s) {
                0 -> Welcome()
                1 -> DeckGrid(
                    content = vm.content,
                    learnedOf = { 0 },
                    selected = decks,
                    onClick = { id -> decks = if (id in decks) decks - id else decks + id },
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    header = {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StepTitle("Neyi öğrenmek istiyorsun?", "Birden fazla deste seçebilirsin. Sonra Ayarlar'dan değiştirebilirsin.")
                        }
                    },
                )
                2 -> GoalStep(goal) { goal = it }
                else -> NotifyStep(notify, hour, minute, onNotify = { notify = it }) { h, m -> hour = h; minute = m }
            }
        }
        Box(Modifier.padding(20.dp)) {
            when (step) {
                0 -> BigAction("Başlayalım", Tangerine, Color.White) { step = 1 }
                1 -> BigAction(
                    if (decks.isEmpty()) "En az bir deste seç" else "Devam (${decks.size} deste)",
                    MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary,
                    enabled = decks.isNotEmpty(),
                ) { step = 2 }
                2 -> BigAction("Devam", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) { step = 3 }
                else -> BigAction("Öğrenmeye başla", Tangerine, Color.White) { finish() }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.height(6.dp).width(if (i + 1 == step) 28.dp else 12.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (i + 1 <= step) Tangerine else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 20.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun Welcome() {
    val display = LocalDisplayFont.current
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Üst üste duran kelime kartları illüstrasyonu
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            IllustrationCard("serendipity", Lavender, Color.White, -14f, (-70).dp, 10.dp)
            IllustrationCard("merak", Mint, Color.White, 10f, 70.dp, 20.dp)
            IllustrationCard("curious", PaperCard, Ink, -3f, 0.dp, 0.dp, eyebrow = "sıfat")
        }
        Spacer(Modifier.height(24.dp))
        Text("Lugat", fontFamily = display, fontSize = 56.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "İngilizce kelimeleri kartlarla, oyunlarla ve günde birkaç dakikayla öğren.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(20.dp))
        listOf(
            "İlkokuldan YDS'ye, tıptan hukuka 14 deste",
            "Unutmadan önce hatırlatan akıllı tekrar",
            "Her gün bildirimle gelen günün kelimesi",
        ).forEach {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Tangerine))
                Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun IllustrationCard(
    word: String, bg: Color, fg: Color, angle: Float,
    dx: androidx.compose.ui.unit.Dp, dy: androidx.compose.ui.unit.Dp, eyebrow: String? = null,
) {
    val display = LocalDisplayFont.current
    Box(
        Modifier.offset(dx, dy).rotate(angle).size(width = 170.dp, height = 220.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (eyebrow != null) EyebrowText(eyebrow, fg.copy(alpha = 0.6f))
            Text(word, fontFamily = display, fontSize = 28.sp, color = fg)
        }
    }
}

@Composable
private fun GoalStep(goal: Int, onGoal: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        StepTitle("Günlük hedefin ne olsun?", "Her gün kaç kart çalışmak istediğini seç. Seri (🔥) hedefi tuttuğun günlerle büyür.")
        GOALS.forEach { g ->
            val selected = g.value == goal
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(22.dp))
                    .background(if (selected) g.color else MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, if (selected) g.color else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                    .clickable { onGoal(g.value) }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (selected) (if (g.color == Mustard) Ink else Color.White) else MaterialTheme.colorScheme.onSurface
                Text("${g.value}", fontFamily = LocalDisplayFont.current, fontSize = 40.sp, color = fg, modifier = Modifier.width(76.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleLarge, color = fg)
                    Text("kart / gün · ${g.note}", style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun NotifyStep(notify: Boolean, hour: Int, minute: Int, onNotify: (Boolean) -> Unit, onTime: (Int, Int) -> Unit) {
    var picker by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(hour, minute)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        StepTitle("Günün kelimesi", "Her gün seçtiğin saatte yeni bir kelime ve onunla ilgili ilginç bir bilgi gönderelim mi?")
        // Bildirim önizlemesi
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).shadow(8.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Tangerine))
                Text("  Lugat · $time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Günün kelimesi: tulip", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Text(
                "lale · 'Tulip' kelimesi Türkçe 'tülbent' kelimesinden gelir; lale çiçeği sarığa benzetilmiştir.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onNotify(!notify) }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Günlük bildirim", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = notify, onCheckedChange = onNotify)
        }
        if (notify) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { picker = true }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Bildirim saati", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(time, fontFamily = LocalDisplayFont.current, fontSize = 30.sp)
            }
        }
    }
    if (picker) TimeDialog(hour, minute, onDismiss = { picker = false }) { h, m -> onTime(h, m); picker = false }
}

@Composable
fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
        text = { TimePicker(state = state) },
    )
}
