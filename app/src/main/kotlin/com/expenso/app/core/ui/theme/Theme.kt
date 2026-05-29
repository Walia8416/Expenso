package com.expenso.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.expenso.app.core.ui.components.MeshBackground

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
    primary = GradPrimaryStart,
    onPrimary = Cream50,
    primaryContainer = HeroVioletGlow,
    onPrimaryContainer = Cream50,
    secondary = GradPrimaryEnd,
    onSecondary = HeroMidnight,
    tertiary = NeonMint,
    background = HeroMidnight,
    onBackground = Cream50,
    surface = HeroIndigo,
    onSurface = Cream50,
    surfaceVariant = HeroVioletGlow,
    onSurfaceVariant = Ink300,
    outline = GlassWhite8,
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
    ) {
        if (darkTheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
            ) {
                MeshBackground(modifier = Modifier.fillMaxSize())
                content()
            }
        } else {
            content()
        }
    }
}
