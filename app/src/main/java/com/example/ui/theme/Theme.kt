package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ProstutiAppTheme(
    themeSetting: String = "system",
    content: @Composable () -> Unit
) {
    val isDark = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val prostutiColors = if (isDark) DarkProstutiColors else LightProstutiColors

    val m3Colors = if (isDark) {
        darkColorScheme(
            background = prostutiColors.paper,
            surface = prostutiColors.card,
            onBackground = prostutiColors.ink,
            onSurface = prostutiColors.ink,
            primary = prostutiColors.blue,
            onPrimary = prostutiColors.onBlueButton,
            outline = prostutiColors.rule
        )
    } else {
        lightColorScheme(
            background = prostutiColors.paper,
            surface = prostutiColors.card,
            onBackground = prostutiColors.ink,
            onSurface = prostutiColors.ink,
            primary = prostutiColors.blue,
            onPrimary = prostutiColors.onBlueButton,
            outline = prostutiColors.rule
        )
    }

    CompositionLocalProvider(LocalProstutiColors provides prostutiColors) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}
