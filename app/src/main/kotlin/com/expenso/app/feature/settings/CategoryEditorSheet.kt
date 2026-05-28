package com.expenso.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.LifestyleGroup

private val EmojiPalette = listOf(
    "\uD83C\uDF5B", "\uD83D\uDED2", "\uD83D\uDE95", "\uD83D\uDCC4", "\uD83D\uDECD",
    "\uD83C\uDFAC", "\uD83E\uDE7A", "\uD83C\uDFE0", "\u2708\uFE0F", "\uD83C\uDFCB\uFE0F",
    "\uD83D\uDCDA", "\uD83D\uDCF1", "\u2615", "\uD83C\uDF70", "\uD83C\uDF54",
    "\uD83C\uDF7A", "\uD83D\uDC8A", "\uD83D\uDC89", "\uD83D\uDC36", "\uD83C\uDF93",
    "\uD83D\uDC6A", "\uD83C\uDFA4", "\uD83C\uDFB5", "\uD83C\uDFAE", "\uD83D\uDCBC",
    "\uD83D\uDD27", "\uD83D\uDCA1", "\uD83D\uDCB0", "\uD83D\uDCB3", "\u26FD",
    "\uD83D\uDE97", "\uD83D\uDE8C", "\uD83D\uDE86", "\uD83E\uDDF4", "\uD83D\uDD01",
    "\uD83C\uDF81", "\uD83E\uDDFE", "\u2B50", "\uD83C\uDF3F", "\u2022",
)

private val ColorPalette = listOf(
    "#E26A4F", "#2DAE85", "#3C78D8", "#6A4FE2", "#D84FA6",
    "#F3B23C", "#4FBDE2", "#8B5E3C", "#FF8A65", "#7E57C2",
    "#26A69A", "#9A9A93",
)

data class CategoryEditorInitial(
    val id: String? = null,
    val name: String = "",
    val emoji: String = "\uD83D\uDCCC",
    val colorHex: String = ColorPalette.first(),
    val lifestyleGroup: LifestyleGroup = LifestyleGroup.OTHER,
    val isArchived: Boolean = false,
)

fun Category.toInitial() = CategoryEditorInitial(
    id = id,
    name = name,
    emoji = emoji,
    colorHex = colorHex,
    lifestyleGroup = lifestyleGroup,
    isArchived = isArchived,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    initial: CategoryEditorInitial,
    onDismiss: () -> Unit,
    onSaveNew: (name: String, emoji: String, colorHex: String, group: LifestyleGroup) -> Unit,
    onSaveEdit: (Category) -> Unit,
    existing: Category? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var emoji by rememberSaveable(initial.id) { mutableStateOf(initial.emoji) }
    var colorHex by rememberSaveable(initial.id) { mutableStateOf(initial.colorHex) }
    var group by remember(initial.id) { mutableStateOf(initial.lifestyleGroup) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                if (existing == null) "New category" else "Edit category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(parseColor(colorHex), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.size(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    placeholder = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Emoji")
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(EmojiPalette, key = { it }) { option ->
                    val selected = option == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            )
                            .clickable { emoji = option },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Color")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ColorPalette, key = { it }) { option ->
                    val selected = option.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(parseColor(option), CircleShape)
                            .border(
                                width = if (selected) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                            )
                            .clickable { colorHex = option },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Lifestyle bucket")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LifestyleGroup.values(), key = { it.name }) { g ->
                    val selected = g == group
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(100),
                            )
                            .clickable { group = g }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            g.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (existing == null) {
                            onSaveNew(name, emoji, colorHex, group)
                        } else {
                            onSaveEdit(
                                existing.copy(
                                    name = name.trim(),
                                    emoji = emoji,
                                    colorHex = colorHex,
                                    lifestyleGroup = group,
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(if (existing == null) "Create" else "Save") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun parseColor(hex: String): Color = runCatching {
    val clean = hex.removePrefix("#")
    val value = if (clean.length == 6) "FF$clean" else clean
    Color(value.toLong(16))
}.getOrDefault(Color(0xFF9A9A93))
