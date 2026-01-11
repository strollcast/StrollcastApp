package com.strollcast.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.strollcast.app.models.NoteEntity

/**
 * Dialog for creating or editing a note
 *
 * @param isOpen Whether the dialog is visible
 * @param existingNote Existing note to edit, null for new note
 * @param transcriptText The transcript line text for context
 * @param onSave Called when user saves the note with the note content
 * @param onCancel Called when user cancels or dismisses the dialog
 * @param modifier Optional modifier
 */
@Composable
fun NoteDialog(
    isOpen: Boolean,
    existingNote: NoteEntity? = null,
    transcriptText: String = "",
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var noteContent by remember(existingNote) {
        mutableStateOf(existingNote?.content ?: "")
    }

    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (existingNote != null) "Edit Note" else "Add Note",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Context - show transcript line text
                if (transcriptText.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Transcript Line:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = transcriptText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Note text field
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = {
                        noteContent = it
                        showError = false
                    },
                    label = { Text("Note") },
                    placeholder = { Text("Enter your note here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 240.dp),
                    maxLines = 10,
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                text = "Note cannot be empty",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${noteContent.length} / 5000 characters",
                                color = if (noteContent.length > 5000)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val trimmed = noteContent.trim()
                            if (trimmed.isEmpty()) {
                                showError = true
                            } else if (trimmed.length <= 5000) {
                                onSave(trimmed)
                            }
                        },
                        enabled = noteContent.isNotBlank() && noteContent.length <= 5000
                    ) {
                        Text(if (existingNote != null) "Save" else "Add")
                    }
                }
            }
        }
    }
}
