package com.expenso.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Emerald600,
    onPrimary = Cream50,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,
    secondary = Coral500,
    onSecondary = Cream50,
    tertiary = Amber400,
    background = Cream50,
    onBackground = Ink900,
    surface = Cream50,
    onSurface = Ink900,
    surfaceVariant = Cream100,
    onSurfaceVariant = Ink500,
    outline = Cream200,
    error = DangerRed,
    onError = Cream50,
)

private val DarkColors = darkColorScheme(
    primary = Emerald400,
    onPrimary = Ink900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald50,
    secondary = Coral500,
    onSecondary = Ink900,
    tertiary = Amber400,
    background = Ink900,
    onBackground = Cream50,
    surface = Ink900,
    onSurface = Cream50,
    surfaceVariant = Ink700,
    onSurfaceVariant = Ink300,
    outline = Ink700,
    error = DangerRed,
    onError = Cream50,
)

@Composable
fun ExpensoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ExpensoTypography,
        content = content,
    )
}
