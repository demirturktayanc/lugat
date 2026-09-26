package com.lugat.kelime.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lugat.kelime.ui.theme.Danger
import com.lugat.kelime.ui.theme.Success
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Kartın kaydırma durumu; alttaki düğmeler de kartı aynı animasyonla kaydırabilsin diye dışarıda tutulur. */
@Stable
class SwipeState {
    val offsetX = Animatable(0f)
    var widthPx: Float = 1000f
    var busy: Boolean = false

    /** Kartı ekrandan dışarı fırlatır; animasyon bitince [onDone] çağrılır. */
    suspend fun swipeAway(right: Boolean, onDone: (Boolean) -> Unit) {
        if (busy) return
        busy = true
        offsetX.animateTo(if (right) widthPx * 1.4f else -widthPx * 1.4f, tween(260))
        onDone(right)
    }
}

/**
 * 3D çevrilen ve sağa/sola kaydırılabilen öğrenme kartı.
 * Dokununca döner; sağa kaydırma = biliyorum, sola kaydırma = tekrar.
 */
@Composable
fun FlipCard(
    flipped: Boolean,
    onFlip: () -> Unit,
    swipe: SwipeState,
    onSwiped: (right: Boolean) -> Unit,
    frontColor: Color,
    backColor: Color,
    modifier: Modifier = Modifier,
    front: @Composable BoxScope.() -> Unit,
    back: @Composable BoxScope.() -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "flip",
    )
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(32.dp)

    BoxWithConstraints(modifier) {
        swipe.widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val fraction = (swipe.offsetX.value / swipe.widthPx).coerceIn(-1f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = swipe.offsetX.value
                    rotationZ = fraction * 10f
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (abs(swipe.offsetX.value) > swipe.widthPx * 0.28f) {
                                    swipe.swipeAway(swipe.offsetX.value > 0, onSwiped)
                                } else {
                                    swipe.offsetX.animateTo(0f, tween(220))
                                }
                            }
                        },
                        onDragCancel = { scope.launch { swipe.offsetX.animateTo(0f) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { swipe.offsetX.snapTo(swipe.offsetX.value + drag) }
                    }
                },
        ) {
            // Kartın kendisi (Y ekseninde döner)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 16f * density
                    }
                    .shadow(elevation = 18.dp, shape = shape, clip = false)
                    .clip(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFlip,
                    ),
            ) {
                if (rotation <= 90f) {
                    CardFace(frontColor, front)
                } else {
                    Box(Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }) {
                        CardFace(backColor, back)
                    }
                }
            }
            // Kaydırma damgası
            if (abs(fraction) > 0.05f) {
                val right = fraction > 0
                val color = if (right) Success else Danger
                Text(
                    text = if (right) "BİLİYORUM" else "TEKRAR",
                    color = color,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .align(if (right) Alignment.TopStart else Alignment.TopEnd)
                        .padding(28.dp)
                        .graphicsLayer {
                            alpha = (abs(fraction) * 3f).coerceAtMost(1f)
                            rotationZ = if (right) -12f else 12f
                        }
                        .border(3.dp, color, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CardFace(color: Color, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(color), content = content)
}
