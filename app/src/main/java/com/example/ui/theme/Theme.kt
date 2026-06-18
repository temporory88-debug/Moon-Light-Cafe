package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldLight,
    background = DeepBlack,
    surface = CardDark,
    onPrimary = DeepBlack,
    onSecondary = DeepBlack,
    onBackground = CreamText,
    onSurface = CreamText,
    tertiary = CreamText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We always default to the beautiful dark premium cafe theme
    dynamicColor: Boolean = false, // We force our intentional gold theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
