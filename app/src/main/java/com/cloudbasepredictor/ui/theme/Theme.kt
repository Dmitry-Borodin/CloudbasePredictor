package com.cloudbasepredictor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = MintGray,
    secondary = SkyBlue,
    tertiary = SunAmber,
    surface = ColorWhite,
    surfaceContainer = MintGray,
    surfaceContainerHigh = Color(0xFFF1F6FA),
    background = ColorWhite,
)

private val DarkColors = darkColorScheme(
    primary = IceBlue,
    secondary = SkyBlue,
    tertiary = Sand,
    background = NightBlue,
    surface = NightBlue,
    surfaceContainer = Color(0xFF12344D),
    surfaceContainerHigh = Color(0xFF18425F),
)

@Composable
fun CloudbasePredictorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
