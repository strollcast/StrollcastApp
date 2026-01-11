package com.strollcast.app.utils

/**
 * Utility for parsing and validating Strollcast episode URLs
 * Format: https://released.strollcast.com/episodes/{id}/{id}.(mp3|m4a)
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
}
