package com.anestesia.app.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── ASTM Anesthesia Color System ─────────────────────────────────────────────
object AstmColors {
    val Relajante = Color(0xFFE53935)        // Rojo ASTM
    val Opioide = Color(0xFF1565C0)           // Azul ASTM
    val Hipnotico = Color(0xFFF9A825)         // Amarillo ASTM (oscurecido para contraste)
    val Benzodiacepina = Color(0xFFE65100)    // Naranja ASTM
    val Otro = Color(0xFF455A64)

    val Warning = Color(0xFFF57F17)
    val Critical = Color(0xFFB71C1C)
    val Safe = Color(0xFF2E7D32)
    val PanicRed = Color(0xFFD32F2F)

    val Background = Color(0xFFF9F9F9)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF0D1B2A)         // Azul marino profundo
    val OnSurfaceVariant = Color(0xFF37474F)
    val Outline = Color(0xFFB0BEC5)
    val CardBorder = Color(0xFFE0E0E0)
}

fun drugCategoryColor(category: String): Color = when (category.uppercase()) {
    "RELAJANTE" -> AstmColors.Relajante
    "OPIOIDE" -> AstmColors.Opioide
    "HIPNOTICO" -> AstmColors.Hipnotico
    "BENZODIACEPINA" -> AstmColors.Benzodiacepina
    else -> AstmColors.Otro
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D1B2A),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = Color(0xFF0D1B2A),
    tertiary = Color(0xFF1B5E20),
    error = Color(0xFFB71C1C),
    onError = Color.White,
    background = AstmColors.Background,
    onBackground = AstmColors.OnSurface,
    surface = AstmColors.Surface,
    onSurface = AstmColors.OnSurface,
    onSurfaceVariant = AstmColors.OnSurfaceVariant,
    outline = AstmColors.Outline,
    surfaceVariant = Color(0xFFF5F5F5),
)

@Composable
fun AnestesiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography(),
        content = content
    )
}
