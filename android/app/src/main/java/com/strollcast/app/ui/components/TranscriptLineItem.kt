package com.strollcast.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.utils.MarkdownLinkParser

/**
 * Transcript line item with tap-to-seek and long-press for notes
 *
 * @param cue Transcript cue to display
 * @param isHighlighted Whether this line is currently playing
 * @param noteCount Number of notes attached to this line
 * @param onClick Called when line is tapped (seek to timestamp)
 * @param onLongClick Called when line is long-pressed (add note)
 * @param onLinkClick Called when a reference link is tapped (navigate to episode)
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalFoundationApi::class)
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
    // Parse markdown links and build annotated string
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(cue.text, linkColor) {
        MarkdownLinkParser.buildAnnotatedString(
            text = cue.text,
            linkColor = linkColor
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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

                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isHighlighted)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        // Check if clicked on a link
                        annotatedText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            // Clicked on a link - call onLinkClick with the URL
                            onLinkClick?.invoke(annotation.item)
                        } ?: run {
                            // Clicked on normal text - call regular onClick (seek)
                            onClick()
                        }
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
