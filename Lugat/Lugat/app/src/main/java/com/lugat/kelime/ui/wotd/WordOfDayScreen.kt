package com.lugat.kelime.ui.wotd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.lugat.kelime.data.LearningRepository
import com.lugat.kelime.tts.Speaker
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.theme.Ink
import com.lugat.kelime.ui.theme.InkSoft
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Tangerine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WordOfDayViewModel @Inject constructor(
    val repo: LearningRepository,
    val speaker: Speaker,
) : ViewModel()

@Composable
fun WordOfDayScreen(onBack: () -> Unit, vm: WordOfDayViewModel = hiltViewModel()) {
    val today = remember { vm.repo.today() }
    var day by remember { mutableLongStateOf(today) }
    val fact = vm.repo.wordOfDay(day) ?: return
    val display = LocalDisplayFont.current
    val tr = Locale.forLanguageTag("tr")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ---- Kahraman alanı
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(Brush.verticalGradient(listOf(Ink, InkSoft, Color(0xFF3B2F7A))))
                .statusBarsPadding(),
        ) {
            Text(
                "“", fontFamily = display, fontSize = 260.sp, color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp),
            )
            Column(Modifier.padding(start = 8.dp, end = 24.dp, bottom = 32.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri", tint = Color.White) }
                Column(Modifier.padding(start = 16.dp)) {
                    EyebrowText(
                        if (day == today) "Günün kelimesi" else LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM", tr)),
                        Color(0xFFFFB199),
                    )
                    Text(fact.en, fontFamily = display, fontSize = 54.sp, lineHeight = 58.sp, color = Color.White,
                        modifier = Modifier.padding(top = 8.dp))
                    Text(fact.pos.label, fontStyle = FontStyle.Italic, fontFamily = display, fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.7f))
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(Tangerine).clickable { vm.speaker.speak(fact.en) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Dinle", tint = Color.White) }
                        Text(fact.tr, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }

        // ---- İlginç bilgi
        Column(Modifier.padding(20.dp)) {
            EyebrowText("Biliyor muydun?", Tangerine)
            Text(fact.fact, style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp, lineHeight = 27.sp,
                modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
            ) {
                EyebrowText("Örnek cümle")
                Text(fact.exampleEn, fontFamily = display, fontSize = 22.sp, lineHeight = 28.sp, modifier = Modifier.padding(top = 8.dp))
                Text(fact.exampleTr, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }

            // ---- Önceki günler
            Text("Önceki günler", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            (1..6).map { today - it }.forEach { d ->
                val f = vm.repo.wordOfDay(d) ?: return@forEach
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (d == day) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { day = d }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(
                        LocalDate.ofEpochDay(d).format(DateTimeFormatter.ofPattern("d MMM", tr)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp),
                    )
                    Text(f.en, fontFamily = display, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Text(f.tr, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            if (day != today) {
                Text("← Bugünün kelimesine dön", color = Tangerine, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp).clickable { day = today }.padding(8.dp))
            }
        }
    }
}
