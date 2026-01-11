package com.strollcast.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strollcast.app.models.NoteEntity
import com.strollcast.app.ui.components.NoteDialog
import com.strollcast.app.viewmodels.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen displaying all notes across episodes
 *
 * @param episodeId Optional episode ID to filter notes (null = show all notes)
 * @param viewModel Note view model
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    episodeId: String? = null,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Load notes when screen opens
    LaunchedEffect(episodeId) {
        episodeId?.let { viewModel.loadNotesForEpisode(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (episodeId != null) "Episode Notes" else "All Notes")
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error Loading Notes",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = {
                            viewModel.clearError()
                            episodeId?.let { viewModel.loadNotesForEpisode(it) }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            uiState.notes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No notes yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Long-press any transcript line to add a note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.notes,
                        key = { it.id }
                    ) { note ->
                        NoteCard(
                            note = note,
                            onEdit = {
                                noteToEdit = note
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteNote(note)
                            }
                        )
                    }
                }
            }
        }

        // Edit dialog
        NoteDialog(
            isOpen = showEditDialog,
            existingNote = noteToEdit,
            onSave = { content ->
                noteToEdit?.let { note ->
                    viewModel.updateNote(note.copy(content = content))
                }
                showEditDialog = false
                noteToEdit = null
            },
            onCancel = {
                showEditDialog = false
                noteToEdit = null
            }
        )
    }
}

/**
 * Card displaying a single note with edit/delete actions
 */
@Composable
private fun NoteCard(
    note: NoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note content
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTimestamp(note.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    if (note.updatedAt > note.createdAt) {
                        Text(
                            text = "Edited ${formatTimestamp(note.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Format timestamp to relative or absolute time
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
