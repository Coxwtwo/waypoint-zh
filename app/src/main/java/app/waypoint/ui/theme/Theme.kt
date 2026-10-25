package app.waypoint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.waypoint.R
import app.waypoint.data.ThemeMode
import app.waypoint.domain.Method

/** The night sky with gold stars; by day, a pale dawn with deeper gold. */
@Immutable
data class Palette(
    /** Sky at the bottom of the screen, and the general background. */
    val bg: Color,
    /** Sky at the top: the glow of the Milky Way at night, the horizon at dawn. */
    val bgTop: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    /** Gold starlight. */
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    /** Background stars. */
    val star: Color,
    val good: Color,
    val danger: Color,
    /** Star colour of each method, in [Method] order. */
    val hues: List<Color>,
    val dark: Boolean,
) {
    fun hue(m: Method): Color = hues[m.ordinal]
}

val NightPalette = Palette(
    bg = Color(0xFF0B1130),
    bgTop = Color(0xFF1B2766),
    surface = Color(0xFF121A43),
    surface2 = Color(0xFF18214F),
    line = Color(0xFF283271),
    ink = Color(0xFFE7E9F6),
    muted = Color(0xFF9097C2),
    faint = Color(0xFF5E6698),
    accent = Color(0xFFE8CD82),
    accentSoft = Color(0xFF2D2E45),
    onAccent = Color(0xFF0B1130),
    star = Color(0xFFDCE0FF),
    good = Color(0xFF86E3B5),
    danger = Color(0xFFFF8080),
    hues = listOf(
        Color(0xFFE8CD82), // mandala
        Color(0xFF7FD3E8), // okr
        Color(0xFFB6A2FF), // 12 weeks
        Color(0xFFFF9F80), // 4dx
        Color(0xFF86E3B5), // kaizen
        Color(0xFFF5A0C6), // pact
        Color(0xFF8FB6FF), // ladder
        Color(0xFFD9DCF0), // simple
        Color(0xFF8ED5BE), // tracker
        Color(0xFFA9C4FF), // smart
        Color(0xFFFFC27A), // woop
        Color(0xFF9BE39B), // grow
        Color(0xFFE3B3FF), // whys
        Color(0xFFFF8A8A), // premortem
        Color(0xFF7FE0D0), // wheel
        Color(0xFFC9A7FF), // odyssey
        Color(0xFFF2D06B), // buffett
        Color(0xFFFFB0D0), // dreamline
    ),
    dark = true,
)

val DawnPalette = Palette(
    bg = Color(0xFFF2F1FA),
    bgTop = Color(0xFFFBE8DE),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF7F6FC),
    line = Color(0xFFE2E0F0),
    ink = Color(0xFF1A1F45),
    muted = Color(0xFF5D6290),
    faint = Color(0xFF9CA0C2),
    accent = Color(0xFFB0841A),
    accentSoft = Color(0xFFF5EBCF),
    onAccent = Color(0xFFFFFFFF),
    star = Color(0xFFB9B7D8),
    good = Color(0xFF2E9E6A),
    danger = Color(0xFFD9443F),
    hues = listOf(
        Color(0xFFB0841A),
        Color(0xFF1F8FAD),
        Color(0xFF6E55D6),
        Color(0xFFD9603A),
        Color(0xFF2E9E6A),
        Color(0xFFC94F86),
        Color(0xFF3D6FD1),
        Color(0xFF5D6290),
        Color(0xFF2E9E8F),
        Color(0xFF4D6FC4),
        Color(0xFFC77A1E),
        Color(0xFF3E9A3E),
        Color(0xFF9550C4),
        Color(0xFFD14848),
        Color(0xFF1E9A8A),
        Color(0xFF7D55C9),
        Color(0xFFA88412),
        Color(0xFFC9578A),
    ),
    dark = false,
)

val LocalPalette = staticCompositionLocalOf { NightPalette }

/** Body text: a clear, warm grotesque. Static cuts: variable-font weights aren't applied on every Android version. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight(400)),
    Font(R.font.manrope_medium, FontWeight(500)),
    Font(R.font.manrope_semibold, FontWeight(600)),
    Font(R.font.manrope_bold, FontWeight(700)),
    Font(R.font.manrope_extrabold, FontWeight(800)),
)

/** Display: wide and round, for titles and big numbers. */
val Unbounded = FontFamily(
    Font(R.font.unbounded_light, FontWeight(300)),
    Font(R.font.unbounded_regular, FontWeight(400)),
    Font(R.font.unbounded_medium, FontWeight(500)),
    Font(R.font.unbounded_semibold, FontWeight(600)),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(28.dp)
    val RLg = RoundedCornerShape(22.dp)
    val RMd = RoundedCornerShape(18.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(400), fontSize = 28.sp, letterSpacing = 0.01.em, lineHeight = 36.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(400), fontSize = 22.sp, letterSpacing = 0.01.em, lineHeight = 30.sp, color = U.c.ink)
    val Heading: TextStyle @Composable get() = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 22.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 0.12.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(500), fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(600), fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(500), fontSize = 22.sp, color = U.c.ink, fontFeatureSettings = "tnum")
    val Big: TextStyle @Composable get() = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight(300), fontSize = 44.sp, color = U.c.ink, fontFeatureSettings = "tnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight(700), fontSize = 15.sp)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun WaypointTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) NightPalette else DawnPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
