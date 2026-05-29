package com.expenso.app.core.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ExpensoBrushes {

    /** App-wide background mesh — used behind the root content in dark mode. */
    val heroMesh: Brush = Brush.linearGradient(
        colors = listOf(HeroMidnight, HeroIndigo, HeroTeal),
        start = Offset(0f, 0f),
        end = Offset.Infinite,
    )

    /** Primary CTA / active-state gradient (purple → teal). */
    val gradPrimary: Brush = Brush.linearGradient(
        listOf(GradPrimaryStart, GradPrimaryEnd),
    )

    /** Income / positive-delta gradient (pink → coral). */
    val gradWarm: Brush = Brush.linearGradient(
        listOf(GradWarmStart, GradWarmEnd),
    )

    /** Spend / chart series gradient (indigo → teal). */
    val gradCool: Brush = Brush.linearGradient(
        listOf(GradCoolStart, GradCoolEnd),
    )

    /** Subtle 1px hairline for glass-card borders. */
    val neonHairline: Brush = Brush.linearGradient(
        listOf(
            GradPrimaryStart.copy(alpha = 0.6f),
            GradPrimaryEnd.copy(alpha = 0.6f),
        ),
    )

    /** Under-curve fill for the primary chart line — fades to transparent. */
    fun underCurvePrimary(): Brush = Brush.verticalGradient(
        colors = listOf(
            GradPrimaryStart.copy(alpha = 0.4f),
            GradPrimaryStart.copy(alpha = 0f),
        ),
    )

    /** Under-curve fill for the secondary chart line. */
    fun underCurveSecondary(): Brush = Brush.verticalGradient(
        colors = listOf(
            GradCoolEnd.copy(alpha = 0.35f),
            GradCoolEnd.copy(alpha = 0f),
        ),
    )

    fun glassSurface(dark: Boolean): Color =
        if (dark) SurfaceGlassDark else SurfaceGlassLight
}
