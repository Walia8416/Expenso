package com.expenso.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.core.domain.model.Category
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val category: Category) : EditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val cats by vm.categories.collectAsStateWithLifecycle()
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editorTarget = EditorTarget.New },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New category") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        ReorderableCategoryList(
            cats = cats,
            onReorder = vm::reorderCategories,
            onEdit = { editorTarget = EditorTarget.Edit(it) },
            onArchive = { vm.archiveCategory(it.id) },
            onUnarchive = { vm.unarchiveCategory(it.id) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        )
    }

    when (val target = editorTarget) {
        EditorTarget.New -> CategoryEditorSheet(
            initial = CategoryEditorInitial(),
            existing = null,
            onDismiss = { editorTarget = null },
            onSaveNew = { name, emoji, color, group ->
                vm.createCategory(name, emoji, color, group)
                editorTarget = null
            },
            onSaveEdit = { },
        )
        is EditorTarget.Edit -> CategoryEditorSheet(
            initial = target.category.toInitial(),
            existing = target.category,
            onDismiss = { editorTarget = null },
            onSaveNew = { _, _, _, _ -> },
            onSaveEdit = { updated ->
                vm.updateCategory(updated)
                editorTarget = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun ReorderableCategoryList(
    cats: List<Category>,
    onReorder: (List<String>) -> Unit,
    onEdit: (Category) -> Unit,
    onArchive: (Category) -> Unit,
    onUnarchive: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    var working by remember(cats) { mutableStateOf(cats) }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        working = working.toMutableList().also {
            val moved = it.removeAt(from.index)
            it.add(to.index, moved)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(working, key = { it.id }) { category ->
            ReorderableItem(reorderState, key = category.id) { isDragging ->
                CategoryRow(
                    category = category,
                    isDragging = isDragging,
                    onEdit = { onEdit(category) },
                    onArchive = { onArchive(category) },
                    onUnarchive = { onUnarchive(category) },
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        onReorder(working.map { it.id })
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isDragging: Boolean,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    dragHandle: @Composable () -> Unit,
) {
    val background = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer
        category.isArchived -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(parseColor(category.colorHex), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(category.emoji, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.lifestyleGroup.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (category.isDefault) {
                    Spacer(Modifier.size(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(100),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (category.isArchived) {
                    Spacer(Modifier.size(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                RoundedCornerShape(100),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

        IconButton(onClick = onEdit) {
            Icon(
                Icons.Rounded.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (category.isArchived) {
            IconButton(onClick = onUnarchive) {
                Icon(
                    Icons.Rounded.Unarchive,
                    contentDescription = "Unarchive",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(onClick = onArchive) {
                Icon(
                    Icons.Rounded.Archive,
                    contentDescription = "Archive",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        dragHandle()
    }
}
