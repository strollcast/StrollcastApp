package com.strollcast.app.utils

import android.net.Uri

/**
 * Utility for parsing and validating Strollcast episode and paper reference URLs
 */
object EpisodeUrlParser {

    // Pattern matches: https://released.strollcast.com/episodes/{id}/{id}.mp3 or .m4a
    // The {id} must appear in both the directory name and filename
    private val EPISODE_URL_PATTERN =
        "https://released\\.strollcast\\.com/episodes/([^/]+)/\\1\\.(mp3|m4a)".toRegex()

    /**
     * Extract episode ID from a Strollcast episode URL
     * @param url The URL to parse
     * @return Episode ID if valid, null otherwise
     *
     * Example:
     * Input: "https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.mp3"
     * Output: "dao-2023-flashattention_2_fa"
     */
    fun extractEpisodeId(url: String): String? {
        return EPISODE_URL_PATTERN.matchEntire(url)?.groupValues?.get(1)
    }

    /**
     * Check if a URL is a valid Strollcast episode URL
     * @param url The URL to validate
     * @return true if valid episode URL, false otherwise
     */
    fun isEpisodeUrl(url: String): Boolean {
        return EPISODE_URL_PATTERN.matches(url)
    }

    /**
     * Extract arXiv ID from a Strollcast paper reference URL
     * @param url The URL to parse
     * @return arXiv ID if valid, null otherwise
     *
     * Example:
     * Input: "https://strollcast.com/paper/arxiv/2106.09685"
     * Output: "2106.09685"
     */
    fun extractArxivId(url: String): String? {
        val uri = Uri.parse(url) ?: return null

        // Check host is strollcast.com
        if (uri.host != "strollcast.com") return null

        val pathSegments = uri.pathSegments
        // Expected: ["paper", "arxiv", "2106.09685"]
        if (pathSegments.size < 3) return null
        if (pathSegments[0] != "paper") return null
        if (pathSegments[1] != "arxiv") return null

        // Join remaining segments (handles old arXiv format with slashes)
        return pathSegments.drop(2).joinToString("/")
    }

    /**
     * Check if a URL is a Strollcast paper reference URL
     * @param url The URL to validate
     * @return true if valid paper reference URL, false otherwise
     */
    fun isPaperReferenceUrl(url: String): Boolean {
        return extractArxivId(url) != null
    }
}
