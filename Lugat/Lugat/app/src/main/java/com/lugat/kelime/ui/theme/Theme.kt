package com.lugat.kelime.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lugat.kelime.data.prefs.ThemeMode

// ---- Lugat paleti: mürekkep, kâğıt ve canlı vurgu renkleri ----
val Ink = Color(0xFF14162B)
val InkSoft = Color(0xFF2A2D4A)
val Paper = Color(0xFFF5F1E8)
val PaperCard = Color(0xFFFFFCF6)
val Tangerine = Color(0xFFFF6B3D)
val Mint = Color(0xFF14B48A)
val Lavender = Color(0xFF7B6CF6)
val Mustard = Color(0xFFF2B42A)
val Rose = Color(0xFFE8517F)
val Sky = Color(0xFF2E8BEA)
val Olive = Color(0xFF6E9A2E)
val Coral = Color(0xFFEE5A46)
val Success = Color(0xFF17A673)
val Danger = Color(0xFFE5484D)

@Immutable
data class DeckColor(val base: Color, val on: Color, val soft: Color)

/** Her desteye özel renk; content.json'daki "color" alanı bu listenin sırasıdır. */
val DeckColors = listOf(
    DeckColor(Tangerine, Color.White, Color(0xFFFFE3D8)),
    DeckColor(Mint, Color.White, Color(0xFFD5F4EA)),
    DeckColor(Lavender, Color.White, Color(0xFFE6E2FF)),
    DeckColor(Mustard, Ink, Color(0xFFFFF0C9)),
    DeckColor(Rose, Color.White, Color(0xFFFFDDE8)),
    DeckColor(Sky, Color.White, Color(0xFFDCEBFF)),
    DeckColor(Olive, Color.White, Color(0xFFE6F0D5)),
    DeckColor(Coral, Color.White, Color(0xFFFFE0DB)),
)

fun deckColor(index: Int): DeckColor = DeckColors[Math.floorMod(index, DeckColors.size)]

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E2F0),
    onPrimaryContainer = Ink,
    secondary = Tangerine,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3D8),
    onSecondaryContainer = Color(0xFF5A1D08),
    tertiary = Lavender,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE5DA),
    onSurfaceVariant = Color(0xFF5E5B66),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PaperCard,
    surfaceContainer = Color(0xFFFFFAF1),
    surfaceContainerHigh = Color(0xFFEFEADF),
    surfaceContainerHighest = Color(0xFFE7E1D4),
    outline = Color(0xFF8C8894),
    outlineVariant = Color(0xFFDCD6C9),
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFECE8FF),
    onPrimary = Ink,
    primaryContainer = InkSoft,
    onPrimaryContainer = Color(0xFFECE8FF),
    secondary = Color(0xFFFF8A63),
    onSecondary = Color(0xFF3A1204),
    secondaryContainer = Color(0xFF5A2512),
    onSecondaryContainer = Color(0xFFFFDCCF),
    tertiary = Color(0xFFA99FFF),
    background = Color(0xFF0E0F1C),
    onBackground = Color(0xFFF2EEE6),
    surface = Color(0xFF0E0F1C),
    onSurface = Color(0xFFF2EEE6),
    surfaceVariant = Color(0xFF24263B),
    onSurfaceVariant = Color(0xFFB9B6C6),
    surfaceContainerLowest = Color(0xFF090A14),
    surfaceContainerLow = Color(0xFF15172A),
    surfaceContainer = Color(0xFF1A1C31),
    surfaceContainerHigh = Color(0xFF22243B),
    surfaceContainerHighest = Color(0xFF2B2E47),
    outline = Color(0xFF7D7A8C),
    outlineVariant = Color(0xFF34374F),
    error = Color(0xFFFF7A7E),
)

/**
 * Başlık ve kelime fontu: DM Serif Display (SIL OFL). assets/fonts altında yoksa sistem serif fontu kullanılır.
 * Dosya CI'da scripts/fetch_fonts.py ile indirilir.
 */
object Fonts {
    const val DISPLAY_FILE = "DMSerifDisplay-Regular.ttf"
    const val DISPLAY_ITALIC_FILE = "DMSerifDisplay-Italic.ttf"

    @Volatile private var display: FontFamily? = null

    @OptIn(ExperimentalTextApi::class)
    fun display(context: Context): FontFamily = display ?: run {
        val files = runCatching { context.assets.list("fonts")?.toSet() }.getOrNull().orEmpty()
        val family = if (DISPLAY_FILE in files) {
            val fonts = mutableListOf(Font(path = "fonts/$DISPLAY_FILE", assetManager = context.assets))
            if (DISPLAY_ITALIC_FILE in files) {
                fonts += Font(
                    path = "fonts/$DISPLAY_ITALIC_FILE",
                    assetManager = context.assets,
                    style = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
            FontFamily(fonts)
        } else FontFamily.Serif
        display = family
        family
    }
}

val LocalDisplayFont = staticCompositionLocalOf<FontFamily> { FontFamily.Serif }

private fun typography(display: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = display),
        displayMedium = base.displayMedium.copy(fontFamily = display),
        displaySmall = base.displaySmall.copy(fontFamily = display),
        headlineLarge = base.headlineLarge.copy(fontFamily = display),
        headlineMedium = base.headlineMedium.copy(fontFamily = display),
        headlineSmall = base.headlineSmall.copy(fontFamily = display),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

/** Küçük, harf aralıklı "üst başlık" stili (ör. GÜNÜN KELİMESİ). */
val Eyebrow = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)

@Composable
fun LugatTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val display = Fonts.display(context)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalDisplayFont provides display) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography(display),
            content = content,
        )
    }
}
