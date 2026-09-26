package com.lugat.kelime.ui.study

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lugat.kelime.domain.AnswerResult
import com.lugat.kelime.domain.Blank
import com.lugat.kelime.domain.StudyMode
import com.lugat.kelime.domain.Word
import com.lugat.kelime.ui.components.BigAction
import com.lugat.kelime.ui.components.EyebrowText
import com.lugat.kelime.ui.components.FlipCard
import com.lugat.kelime.ui.components.SpeakButton
import com.lugat.kelime.ui.components.SwipeState
import com.lugat.kelime.ui.theme.Danger
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.Success
import com.lugat.kelime.ui.theme.Tangerine
import com.lugat.kelime.ui.theme.deckColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ================================================================== KARTLAR

@Composable
fun CardsMode(s: StudyUi, vm: StudyViewModel) {
    val word = s.current ?: return
    key(word.id, s.index) {
        var flipped by remember { mutableStateOf(false) }
        val swipe = remember { SwipeState() }
        val scope = rememberCoroutineScope()
        val englishFirst = s.englishFirst[word.id] == true
        val c = deckColor(vm.colorIndex(word.deckId))
        val onSwiped: (Boolean) -> Unit = { right -> vm.answer(word, right); vm.next() }

        // İngilizce yüz görününce otomatik seslendir
        LaunchedEffect(flipped) {
            if (s.autoSpeak && flipped != englishFirst) vm.speak(word.en)
        }

        Column(Modifier.fillMaxSize()) {
          Box(Modifier.weight(1f).fillMaxWidth()) {
            // Arkadaki bir sonraki kart (deste hissi)
            if (s.index < s.total - 1) {
                Box(
                    Modifier.matchParentSize().padding(horizontal = 30.dp, vertical = 22.dp)
                        .graphicsLayer { rotationZ = 4f }
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
            FlipCard(
                flipped = flipped,
                onFlip = { flipped = !flipped },
                swipe = swipe,
                onSwiped = onSwiped,
                frontColor = if (englishFirst) c.base else MaterialTheme.colorScheme.surfaceContainerLowest,
                backColor = if (englishFirst) MaterialTheme.colorScheme.surfaceContainerLowest else c.base,
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
                front = {
                    if (englishFirst) EnglishFace(word, c.on, vm) else TurkishFace(word, MaterialTheme.colorScheme.onSurface, showExample = false)
                },
                back = {
                    if (englishFirst) TurkishFace(word, MaterialTheme.colorScheme.onSurface, showExample = true)
                    else EnglishFace(word, c.on, vm)
                },
            )
          }
            Text(
                if (flipped) "Sağa kaydır: biliyorum · Sola: tekrar" else "Çevirmek için karta dokun",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnswerButton("Tekrar", Danger, Modifier.weight(1f)) {
                    scope.launch { swipe.swipeAway(false, onSwiped) }
                }
                AnswerButton("Biliyorum", Success, Modifier.weight(1f)) {
                    scope.launch { swipe.swipeAway(true, onSwiped) }
                }
            }
        }
    }
}

@Composable
private fun AnswerButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 56.dp).clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.12f))
            .border(2.dp, color, RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun BoxScope.TurkishFace(word: Word, color: Color, showExample: Boolean) {
    Column(
        Modifier.align(Alignment.Center).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EyebrowText("Türkçe · ${word.pos.label}", color.copy(alpha = 0.55f))
        Text(
            word.tr, fontFamily = LocalDisplayFont.current, fontSize = 40.sp, lineHeight = 46.sp,
            color = color, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp),
        )
        if (showExample) {
            Text(
                word.exampleTr, color = color.copy(alpha = 0.7f), textAlign = TextAlign.Center,
                fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.EnglishFace(word: Word, color: Color, vm: StudyViewModel) {
    Column(
        Modifier.align(Alignment.Center).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EyebrowText("İngilizce · ${word.pos.label}", color.copy(alpha = 0.7f))
        Text(
            word.en, fontFamily = LocalDisplayFont.current, fontSize = 44.sp, lineHeight = 50.sp,
            color = color, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp),
        )
        SpeakButton({ vm.speak(word.en) }, tint = color)
        Text(
            "“${word.exampleEn}”", color = color.copy(alpha = 0.9f), textAlign = TextAlign.Center,
            fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ================================================================== TEST ve BOŞLUK DOLDURMA

@Composable
fun QuizMode(s: StudyUi, vm: StudyViewModel) {
    val word = s.current ?: return
    val q = s.questions[word.id] ?: return
    val blank = s.mode == StudyMode.BLANK
    key(word.id, s.index) {
        var chosen by remember { mutableStateOf<String?>(null) }
        val c = deckColor(vm.colorIndex(word.deckId))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            // Soru kartı
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.base).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EyebrowText(
                    when {
                        blank -> "Boşluğu doldur"
                        q.promptIsEnglish -> "Türkçesi hangisi?"
                        else -> "İngilizcesi hangisi?"
                    },
                    c.on.copy(alpha = 0.75f),
                )
                if (blank) {
                    Text(
                        blankSentence(q.prompt, if (chosen != null) q.answer else null, c.on),
                        color = c.on, fontSize = 22.sp, lineHeight = 30.sp, textAlign = TextAlign.Center,
                        fontFamily = LocalDisplayFont.current, modifier = Modifier.padding(top = 14.dp),
                    )
                } else {
                    Text(
                        q.prompt, color = c.on, fontSize = 38.sp, lineHeight = 44.sp, textAlign = TextAlign.Center,
                        fontFamily = LocalDisplayFont.current, modifier = Modifier.padding(top = 10.dp),
                    )
                    if (q.promptIsEnglish) SpeakButton({ vm.speak(word.en) }, tint = c.on)
                }
            }
            Spacer(Modifier.height(18.dp))
            q.options.forEach { opt ->
                val isAnswer = opt == q.answer
                val state = when {
                    chosen == null -> OptionState.IDLE
                    isAnswer -> OptionState.CORRECT
                    opt == chosen -> OptionState.WRONG
                    else -> OptionState.DIM
                }
                OptionButton(opt, state, englishFont = !q.promptIsEnglish || blank) {
                    if (chosen == null) {
                        chosen = opt
                        vm.answer(word, isAnswer)
                        if (s.autoSpeak) vm.speak(word.en)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (chosen != null) {
                val correct = chosen == q.answer
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background((if (correct) Success else Danger).copy(alpha = 0.12f)).padding(16.dp),
                ) {
                    Text(
                        if (correct) "Doğru!" else "Doğrusu: ${q.answer}",
                        color = if (correct) Success else Danger, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    )
                    Text(
                        if (blank) q.hint ?: "" else "${word.en} — ${word.tr}\n${word.exampleEn}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                BigAction("Devam", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) { vm.next() }
            }
        }
    }
}

private fun blankSentence(prompt: String, filled: String?, fillColor: Color) = buildAnnotatedString {
    val i = prompt.indexOf(Blank.GAP)
    if (i < 0) { append(prompt); return@buildAnnotatedString }
    append(prompt.substring(0, i))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = fillColor)) {
        append(filled ?: "_____")
    }
    append(prompt.substring(i + Blank.GAP.length))
}

private enum class OptionState { IDLE, CORRECT, WRONG, DIM }

@Composable
private fun OptionButton(text: String, state: OptionState, englishFont: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        when (state) {
            OptionState.CORRECT -> Success
            OptionState.WRONG -> Danger
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "opt",
    )
    val fg = when (state) {
        OptionState.CORRECT, OptionState.WRONG -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).alpha(if (state == OptionState.DIM) 0.5f else 1f)
            .clip(RoundedCornerShape(18.dp)).background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text, color = fg,
            fontFamily = if (englishFont) LocalDisplayFont.current else null,
            fontSize = if (englishFont) 22.sp else 17.sp,
        )
    }
}

// ================================================================== YAZMA ve DİNLE-YAZ

@Composable
fun TypeMode(s: StudyUi, vm: StudyViewModel) {
    val word = s.current ?: return
    val listen = s.mode == StudyMode.LISTEN
    key(word.id, s.index) {
        var text by remember { mutableStateOf("") }
        var result by remember { mutableStateOf<AnswerResult?>(null) }
        val focus = remember { FocusRequester() }
        val c = deckColor(vm.colorIndex(word.deckId))
        val submit: () -> Unit = {
            if (result == null && text.isNotBlank()) {
                result = vm.check(word, text)
                vm.speak(word.en)
            } else if (result != null) vm.next()
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(c.base).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (listen) {
                    EyebrowText("Duyduğunu yaz", c.on.copy(alpha = 0.75f))
                    Box(
                        Modifier.padding(top = 16.dp).size(96.dp).clip(CircleShape).background(c.on.copy(alpha = 0.18f))
                            .clickable { vm.speak(word.en) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Tekrar dinle", tint = c.on, modifier = Modifier.size(44.dp))
                    }
                    Text(
                        if (result == null) "Tekrar dinlemek için dokun" else "${word.en} — ${word.tr}",
                        color = c.on.copy(alpha = 0.85f), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center,
                    )
                } else {
                    EyebrowText("İngilizcesini yaz · ${word.pos.label}", c.on.copy(alpha = 0.75f))
                    Text(
                        word.tr, fontFamily = LocalDisplayFont.current, fontSize = 38.sp, lineHeight = 44.sp,
                        color = c.on, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        "İpucu: ${word.en.first()}… (${word.en.length} harf)",
                        color = c.on.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            val borderColor = when (result) {
                AnswerResult.CORRECT -> Success
                AnswerResult.ALMOST -> Tangerine
                AnswerResult.WRONG -> Danger
                null -> MaterialTheme.colorScheme.outline
            }
            OutlinedTextField(
                value = text,
                onValueChange = { if (result == null) text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                placeholder = { Text("İngilizce yaz…") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            result?.let { r ->
                Column(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(borderColor.copy(alpha = 0.12f)).padding(16.dp),
                ) {
                    Text(
                        when (r) {
                            AnswerResult.CORRECT -> "Doğru!"
                            AnswerResult.ALMOST -> "Neredeyse! Doğru yazımı: ${word.en}"
                            AnswerResult.WRONG -> "Doğrusu: ${word.en}"
                        },
                        color = borderColor, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    )
                    Text(word.exampleEn, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    Text(word.exampleTr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            BigAction(
                if (result == null) "Kontrol et" else "Devam",
                if (result == null) MaterialTheme.colorScheme.primary else Tangerine,
                if (result == null) MaterialTheme.colorScheme.onPrimary else Color.White,
                enabled = result != null || text.isNotBlank(),
            ) { submit() }
            if (result == null) {
                Text(
                    "Bilmiyorum, göster",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clickable {
                        result = vm.check(word, "")
                        vm.speak(word.en)
                    },
                )
            }
        }
    }
}

// ================================================================== EŞLEŞTİRME

private data class Tile(val wordId: String, val text: String, val english: Boolean)

@Composable
fun MatchMode(s: StudyUi, vm: StudyViewModel) {
    val rounds = remember(s.queue) {
        val chunks = s.queue.chunked(5).toMutableList()
        // Son tur tek kelimeyse önceki tura kat
        if (chunks.size > 1 && chunks.last().size < 2) {
            val last = chunks.removeAt(chunks.lastIndex)
            chunks[chunks.lastIndex] = chunks.last() + last
        }
        chunks.toList()
    }
    var round by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); seconds++ } }

    val words = rounds.getOrNull(round) ?: return
    key(round) {
        val tiles = remember { (words.map { Tile(it.id, it.en, true) } + words.map { Tile(it.id, it.tr, false) }).shuffled() }
        var selected by remember { mutableStateOf<Tile?>(null) }
        var matched by remember { mutableStateOf(setOf<String>()) }
        var missed by remember { mutableStateOf(setOf<String>()) }
        var wrong by remember { mutableStateOf<Pair<Tile, Tile>?>(null) }

        LaunchedEffect(wrong) { if (wrong != null) { delay(450); wrong = null } }
        LaunchedEffect(matched.size) {
            if (matched.size == words.size) {
                delay(350)
                if (round + 1 < rounds.size) round++ else vm.finishMatch()
            }
        }

        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    EyebrowText("Tur ${round + 1} / ${rounds.size}")
                    Text("Çiftleri eşleştir", style = MaterialTheme.typography.headlineSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%d:%02d".format(seconds / 60, seconds % 60), fontFamily = LocalDisplayFont.current, fontSize = 28.sp)
                    Text("$mistakes hata", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            tiles.chunked(2).forEach { row ->
                Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { tile ->
                        val isMatched = tile.wordId in matched
                        val isWrong = wrong?.let { it.first == tile || it.second == tile } == true
                        MatchTile(
                            tile = tile,
                            selected = selected == tile,
                            matched = isMatched,
                            wrong = isWrong,
                            color = deckColor(vm.colorIndex(tile.wordId.substringBefore(':'))).base,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isMatched || wrong != null) return@MatchTile
                            val sel = selected
                            when {
                                sel == null || sel.english == tile.english -> selected = tile
                                sel.wordId == tile.wordId -> {
                                    matched = matched + tile.wordId
                                    selected = null
                                    val w = words.first { it.id == tile.wordId }
                                    vm.answer(w, tile.wordId !in missed)
                                    if (s.autoSpeak) vm.speak(w.en)
                                }
                                else -> {
                                    wrong = sel to tile
                                    missed = missed + sel.wordId + tile.wordId
                                    mistakes++
                                    selected = null
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MatchTile(
    tile: Tile,
    selected: Boolean,
    matched: Boolean,
    wrong: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val alpha by animateFloatAsState(if (matched) 0f else 1f, label = "matched")
    val bg by animateColorAsState(
        when {
            wrong -> Danger
            selected -> color
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "tile",
    )
    val fg = if (wrong || selected) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier.height(76.dp).alpha(alpha).clip(RoundedCornerShape(18.dp)).background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(enabled = !matched, onClick = onClick).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            tile.text, color = fg, textAlign = TextAlign.Center, maxLines = 2,
            fontFamily = if (tile.english) LocalDisplayFont.current else null,
            fontSize = if (tile.english) 19.sp else 15.sp,
            lineHeight = 19.sp,
        )
    }
}
