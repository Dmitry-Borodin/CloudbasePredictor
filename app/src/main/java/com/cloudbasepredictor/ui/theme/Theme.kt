package com.cloudbasepredictor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = ColorWhite,
    primaryContainer = Color(0xFFD8E9FA),
    onPrimaryContainer = Color(0xFF071F34),
    secondary = SkyBlue,
    onSecondary = Color(0xFF052238),
    secondaryContainer = Color(0xFFD6EAFB),
    onSecondaryContainer = Color(0xFF092235),
    tertiary = SunAmber,
    onTertiary = Color(0xFF2E2112),
    surface = ColorWhite,
    onSurface = Color(0xFF10202E),
    onSurfaceVariant = Color(0xFF4A5B66),
    surfaceContainer = MintGray,
    surfaceContainerHigh = Color(0xFFF1F6FA),
    background = ColorWhite,
    onBackground = Color(0xFF10202E),
    outline = Color(0xFF6E7F8A),
    outlineVariant = Color(0xFFC4D1D9),
)

private val DarkColors = darkColorScheme(
    primary = IceBlue,
    onPrimary = NightBlue,
    primaryContainer = Color(0xFF1E4E73),
    onPrimaryContainer = Color(0xFFD4EAFF),
    secondary = SkyBlue,
    onSecondary = NightBlue,
    secondaryContainer = Color(0xFF264B68),
    onSecondaryContainer = Color(0xFFD8ECFF),
    tertiary = Sand,
    onTertiary = Color(0xFF2E2112),
    background = NightBlue,
    onBackground = Color(0xFFEAF3FA),
    surface = NightBlue,
    onSurface = Color(0xFFEAF3FA),
    onSurfaceVariant = Color(0xFFB6C7D4),
    surfaceContainer = Color(0xFF12344D),
    surfaceContainerHigh = Color(0xFF18425F),
    outline = Color(0xFF8294A2),
    outlineVariant = Color(0xFF31495C),
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

@Preview(showBackground = true)
@Composable
private fun CloudbasePredictorThemePreview() {
    CloudbasePredictorTheme {
        Surface {
            Text(text = "Cloudbase theme preview")
        }
    }
}
