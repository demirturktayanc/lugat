package com.lugat.kelime.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lugat.kelime.domain.Deck
import com.lugat.kelime.ui.theme.Eyebrow
import com.lugat.kelime.ui.theme.LocalDisplayFont
import com.lugat.kelime.ui.theme.deckColor

/** Dairesel ilerleme halkası (günlük hedef). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    stroke: Dp = 10.dp,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val d = size.minDimension - w
            val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(w))
            drawArc(color, -90f, 360f * animated, false, topLeft, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** İnce, yuvarlak uçlu ilerleme çubuğu. */
@Composable
fun ThinBar(progress: Float, color: Color, track: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (animated > 0f) drawRoundRect(color, size = Size(size.width * animated, size.height), cornerRadius = r)
    }
}

@Composable
fun EyebrowText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(text.uppercase(java.util.Locale.forLanguageTag("tr")), style = Eyebrow, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        action?.invoke(this)
    }
}

/** Seslendirme düğmesi */
@Composable
fun SpeakButton(onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "Dinle", tint = tint)
    }
}

/** Küçük yuvarlatılmış etiket (tür, seviye vb.) */
@Composable
fun Tag(text: String, background: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        color = content,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Deste kutucuğu: renkli zemin, köşede büyük filigran rozet (A1, B2, TIP…), başlık ve ilerleme.
 */
@Composable
fun DeckTile(
    deck: Deck,
    learned: Int,
    total: Int,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onClick: () -> Unit,
) {
    val c = deckColor(deck.colorIndex)
    val display = LocalDisplayFont.current
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(c.base)
            .then(
                if (selected == true) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .heightIn(min = 150.dp),
    ) {
        Text(
            deck.badge,
            fontFamily = display,
            fontSize = 88.sp,
            color = c.on.copy(alpha = 0.16f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp),
            maxLines = 1,
        )
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(deck.badge, c.on.copy(alpha = 0.18f), c.on)
                Spacer(Modifier.weight(1f))
                if (selected != null) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape)
                            .background(if (selected) c.on else c.on.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Text("✓", color = c.base, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(deck.title, fontFamily = display, fontSize = 22.sp, lineHeight = 26.sp, color = c.on)
            Text(
                deck.subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = c.on.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
            Spacer(Modifier.height(14.dp))
            ThinBar(
                progress = if (total == 0) 0f else learned / total.toFloat(),
                color = c.on,
                track = c.on.copy(alpha = 0.22f),
                modifier = Modifier.width(120.dp),
            )
            Text(
                "$learned / $total öğrenildi",
                fontSize = 11.sp,
                color = c.on.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Büyük, tam genişlikte eylem düğmesi */
@Composable
fun BigAction(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

/** Haftalık çubuk grafik: son 7 gün cevap sayıları. */
@Composable
fun WeekBars(values: List<Int>, labels: List<String>, goal: Int, color: Color, modifier: Modifier = Modifier) {
    val max = maxOf(values.maxOrNull() ?: 0, goal, 1)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val goalColor = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size.coerceAtLeast(1)
            val gap = 10.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val x = i * (bw + gap)
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(10f, 10f))
                val h = size.height * v / max
                if (h > 0) drawRoundRect(color, Offset(x, size.height - h), Size(bw, h), CornerRadius(10f, 10f))
            }
            val gy = size.height - size.height * goal / max
            drawLine(goalColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach {
                Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}
