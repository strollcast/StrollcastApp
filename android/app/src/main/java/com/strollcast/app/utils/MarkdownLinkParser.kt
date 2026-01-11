package com.strollcast.app.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Utility for parsing markdown-style links from plain text
 * Format: [link text](url)
 */
object MarkdownLinkParser {

    private val MARKDOWN_LINK_REGEX = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex()

    /**
     * Represents a parsed markdown link
     */
    data class ParsedLink(
        val text: String,       // Link display text (e.g., "FlashAttention-2")
        val url: String,        // Full URL
        val startIndex: Int,    // Character offset where link starts in original text
        val endIndex: Int       // Character offset where link ends in original text
    )

    /**
     * Parse all markdown links from text
     * @param text The text to parse
     * @return List of parsed links
     */
    fun parseLinks(text: String): List<ParsedLink> {
        return MARKDOWN_LINK_REGEX.findAll(text).map { match ->
            ParsedLink(
                text = match.groupValues[1],
                url = match.groupValues[2],
                startIndex = match.range.first,
                endIndex = match.range.last + 1
            )
        }.toList()
    }

    /**
     * Build an AnnotatedString with styled clickable links
     * @param text The text containing markdown links
     * @param linkColor Color for link text
     * @return AnnotatedString with styled links
     */
    fun buildAnnotatedString(
        text: String,
        linkColor: Color
    ): AnnotatedString {
        val links = parseLinks(text)

        return buildAnnotatedString {
            if (links.isEmpty()) {
                append(text)
                return@buildAnnotatedString
            }

            var lastIndex = 0
            links.forEach { link ->
                // Add text before link
                if (lastIndex < link.startIndex) {
                    append(text.substring(lastIndex, link.startIndex))
                }

                // Add styled link with URL annotation
                withStyle(SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                )) {
                    pushStringAnnotation(tag = "URL", annotation = link.url)
                    append(link.text)
                    pop()
                }

                lastIndex = link.endIndex
            }

            // Add remaining text after last link
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
