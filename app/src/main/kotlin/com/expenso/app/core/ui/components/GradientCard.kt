package com.expenso.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.theme.ExpensoBrushes
import com.expenso.app.core.ui.theme.GlassWhite4
import com.expenso.app.core.ui.theme.GlassWhite8

/**
 * Glass card — rounded surface with subtle hairline border. The default look
 * is a translucent fill so the app-wide [MeshBackground] shows through; pass
 * an explicit [gradient] to fill the card with a brand-colored gradient
 * instead (used for hero cards, primary CTAs, etc).
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    border: Brush? = ExpensoBrushes.neonHairline,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val surfaceFill: Brush = gradient
        ?: Brush.linearGradient(
            colors = listOf(
                if (dark) GlassWhite8 else Color(0xFFFFFFFF),
                if (dark) GlassWhite4 else Color(0xFFFAFAF6),
            )
        )

    val outerModifier = modifier
        .clip(shape)
        .background(surfaceFill, shape)
        .let { base ->
            if (border != null) base.border(1.dp, border, shape) else base
        }
        .padding(contentPadding)

    Column(modifier = outerModifier, content = content)
}

/** Returns the right onSurface color for text drawn on top of a [GradientCard]. */
@Composable
fun gradientCardContentColor(): Color = MaterialTheme.colorScheme.onSurface
