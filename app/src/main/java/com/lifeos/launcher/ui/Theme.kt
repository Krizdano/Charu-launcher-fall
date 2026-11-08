package com.lifeos.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

/** Cozy Fall design tokens. Everything visual should come from here. */
object C {
    // core palette
    val Cream = Color(0xFFF4E3D1)        // background
    val Cream2 = Color(0xFFF9EADB)       // secondary background
    val Ink = Color(0xFF38241E)          // primary text (deep coffee)
    val Brown = Color(0xFF876B5B)        // secondary text
    val Cherry = Color(0xFF793B38)       // cherry cola (primary accent)
    val CherrySoft = Color(0xFF9B5548)
    val Olive = Color(0xFF65704A)
    val OliveSoft = Color(0xFF879064)
    val Honey = Color(0xFFD99B45)
    val Peach = Color(0xFFE9B18B)
    val Terracotta = Color(0xFFC9694A)

    // soft category tints (pills, markers)
    val HoneyTint = Color(0xFFF1D6A6)
    val OliveTint = Color(0xFFD8DCBC)
    val TerraTint = Color(0xFFEEC3AA)

    // surfaces
    val GlassTint = Color(0xFFFFF8F0)    // warm white; used at 45-80% alpha
    val Hairline = Color(0x38876B5B)     // subtle warm brown border / divider
    val ShadowBase = Color(0xFF795040)   // warm brown shadow (never black)
    val Cream90 = Color(0xE6F9EADB)

    // bottom nav
    val NavTop = Color(0xFF7C403A)
    val NavBottom = Color(0xFF5C2D29)
    val NavActive = Color(0x66260F0C)
    val NavIcon = Color(0xFFF6E7D6)

    val ProjectColors = listOf(Cherry, Honey, Olive, CherrySoft, Terracotta)
}

private val cozyScheme = lightColorScheme(
    primary = C.Cherry,
    onPrimary = C.Cream2,
    secondary = C.Olive,
    tertiary = C.Honey,
    background = C.Cream,
    onBackground = C.Ink,
    surface = C.Cream2,
    onSurface = C.Ink,
    surfaceVariant = C.Cream,
    onSurfaceVariant = C.Brown,
    outline = C.Hairline,
)

@Composable
fun LifeOsTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    remember { Fonts.init(ctx); true }
    MaterialTheme(colorScheme = cozyScheme) {
        ProvideTextStyle(TextStyle(fontFamily = Fonts.sans, color = C.Ink)) { content() }
    }
}
