package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ProstutiColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val rule: Color,
    val blue: Color,
    val blueSoft: Color,
    val onBlueButton: Color,
    val vermilion: Color,
    val vermilionSoft: Color,
    val moss: Color,
    val mossSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val isDark: Boolean
)

val LightProstutiColors = ProstutiColors(
    paper = Color(0xFFF3EEE3),
    card = Color(0xFFFBF8F1),
    ink = Color(0xFF1A2230),
    ink2 = Color(0xFF4A5463),
    ink3 = Color(0xFF7A8290),
    rule = Color(0xFFD8CFBC),
    blue = Color(0xFF1F3A5F),
    blueSoft = Color(0xFFDCE4F0),
    onBlueButton = Color(0xFFFBF8F1),
    vermilion = Color(0xFFD9482B),
    vermilionSoft = Color(0xFFF7DDD5),
    moss = Color(0xFF3F7A4A),
    mossSoft = Color(0xFFDCEBDD),
    amber = Color(0xFFB8791A),
    amberSoft = Color(0xFFF6E7C3),
    isDark = false
)

val DarkProstutiColors = ProstutiColors(
    paper = Color(0xFF0F1317),
    card = Color(0xFF171D23),
    ink = Color(0xFFECE7DB),
    ink2 = Color(0xFFB3B9C2),
    ink3 = Color(0xFF7F8893),
    rule = Color(0xFF28313A),
    blue = Color(0xFF9DB9E3),
    blueSoft = Color(0xFF1E2A3B),
    onBlueButton = Color(0xFF0F1317),
    vermilion = Color(0xFFFF7F5F),
    vermilionSoft = Color(0xFF3A211B),
    moss = Color(0xFF8CC19A),
    mossSoft = Color(0xFF1B2B20),
    amber = Color(0xFFE8B04A),
    amberSoft = Color(0xFF33291A),
    isDark = true
)

data class SubjectPigment(val name: String, val light: Color, val dark: Color)

val SUBJECT_PIGMENTS = listOf(
    SubjectPigment("Indigo", Color(0xFF3B5BA5), Color(0xFF8FA8E6)),
    SubjectPigment("Teal", Color(0xFF1F8077), Color(0xFF6CC4B9)),
    SubjectPigment("Ochre", Color(0xFFB8791A), Color(0xFFE3B15A)),
    SubjectPigment("Brick", Color(0xFFB04032), Color(0xFFF08C7A)),
    SubjectPigment("Plum", Color(0xFF7A4B7D), Color(0xFFC79BCB)),
    SubjectPigment("Olive", Color(0xFF6F7F2E), Color(0xFFB5C46A)),
    SubjectPigment("Slate", Color(0xFF4F6275), Color(0xFFA3B6C8)),
    SubjectPigment("Rose", Color(0xFFB85A7E), Color(0xFFE69DB9))
)

fun getSubjectColor(colorIndex: Int, isDark: Boolean): Color {
    val idx = (colorIndex % SUBJECT_PIGMENTS.size + SUBJECT_PIGMENTS.size) % SUBJECT_PIGMENTS.size
    val pigment = SUBJECT_PIGMENTS[idx]
    return if (isDark) pigment.dark else pigment.light
}

val LocalProstutiColors = staticCompositionLocalOf { LightProstutiColors }

object ProstutiTheme {
    val colors: ProstutiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalProstutiColors.current
}
