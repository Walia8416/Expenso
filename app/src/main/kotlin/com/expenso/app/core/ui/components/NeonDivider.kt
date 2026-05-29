package com.expenso.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.expenso.app.core.ui.theme.ExpensoBrushes

@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    brush: Brush = ExpensoBrushes.neonHairline,
    vertical: Boolean = false,
) {
    if (vertical) {
        Box(
            modifier = modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(brush),
        )
    } else {
        Box(
            modifier = modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(brush),
        )
    }
}
