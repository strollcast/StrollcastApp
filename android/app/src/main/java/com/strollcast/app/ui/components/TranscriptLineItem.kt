package com.strollcast.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.utils.MarkdownLinkParser

/**
 * Transcript line item with tap-to-seek, long-press for notes, and link support
 *
 * @param cue Transcript cue to display
 * @param isHighlighted Whether this line is currently playing
 * @param noteCount Number of notes attached to this line
 * @param onClick Called when line is tapped (seek to timestamp)
 * @param onLongClick Called when line is long-pressed (add note)
 * @param onLinkClick Called when a reference link is tapped (navigate to episode)
 * @param modifier Optional modifier
 */
@Composable
fun TranscriptLineItem(
    cue: TranscriptCue,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    noteCount: Int = 0,
    onLongClick: (() -> Unit)? = null,
    onLinkClick: ((String) -> Unit)? = null
) {
    // Use appropriate link color based on highlight state for good contrast
    val linkColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }

    // Parse markdown links and build annotated string
    val annotatedText = remember(cue.text, linkColor, isHighlighted) {
        MarkdownLinkParser.buildAnnotatedString(
            text = cue.text,
            linkColor = linkColor
        )
    }

    // Store text layout result for detecting link clicks
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isHighlighted) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                cue.speaker?.let { speaker ->
                    Text(
                        text = speaker,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                BasicText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isHighlighted)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    onTextLayout = { layoutResult.value = it },
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                layoutResult.value?.let { layout ->
                                    val position = layout.getOffsetForPosition(offset)
                                    // Check if clicked on a link
                                    val annotation = annotatedText.getStringAnnotations(
                                        tag = "URL",
                                        start = position,
                                        end = position
                                    ).firstOrNull()

                                    if (annotation != null) {
                                        // Clicked on a link
                                        onLinkClick?.invoke(annotation.item)
                                    } else {
                                        // Clicked on normal text - seek to timestamp
                                        onClick()
                                    }
                                } ?: onClick()
                            },
                            onLongPress = {
                                // Long press - add note
                                onLongClick?.invoke()
                            }
                        )
                    }
                )
            }

            // Note indicator
            if (noteCount > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.StickyNote2,
                            contentDescription = "Notes",
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = noteCount.toString(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
