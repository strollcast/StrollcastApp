package com.strollcast.app.repository

import android.util.Log
import com.strollcast.app.data.TranscriptDao
import com.strollcast.app.models.TranscriptCue
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity
import com.strollcast.app.util.VttParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom exceptions for transcript operations
 */
sealed class TranscriptException(message: String) : Exception(message) {
    class NoUrlProvided(message: String) : TranscriptException(message)
    class NotFound(message: String) : TranscriptException(message)
    class NetworkError(message: String) : TranscriptException(message)
    class ParseError(message: String) : TranscriptException(message)
    class Timeout(message: String) : TranscriptException(message)
}

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "TranscriptRepository"
        private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
        private const val MAX_CACHE_SIZE_MB = 10
        private const val NETWORK_TIMEOUT_MS = 30000L
    }

    // Memory cache for active transcript
    private val memoryCache = mutableMapOf<String, List<TranscriptCue>>()

    /**
     * Get transcript with 3-tier caching: memory → Room → network
     *
     * @param episodeId Episode ID
     * @param vttUrl Optional VTT file URL (required if not cached)
     * @return Result containing list of TranscriptCue or error
     */
    suspend fun getTranscript(episodeId: String, vttUrl: String?): Result<List<TranscriptCue>> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Check memory cache
                memoryCache[episodeId]?.let {
                    Log.d(TAG, "Transcript found in memory cache for episode: $episodeId")
                    return@withContext Result.success(it)
                }

                // 2. Check Room cache
                val cachedEntity = transcriptDao.getTranscript(episodeId)
                if (cachedEntity != null) {
                    Log.d(TAG, "Transcript found in Room cache for episode: $episodeId")
                    try {
                        val cues = VttParser.parseVTT(cachedEntity.vttContent)
                        memoryCache[episodeId] = cues
                        return@withContext Result.success(cues)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse cached VTT content, will re-download", e)
                        // Continue to network download if cached content is malformed
                    }
                }

                // 3. Download from network
                if (vttUrl.isNullOrBlank()) {
                    Log.w(TAG, "No transcript URL provided for episode: $episodeId")
                    return@withContext Result.failure(TranscriptException.NoUrlProvided("Transcript URL not available"))
                }

                Log.d(TAG, "Downloading transcript from: $vttUrl")
                try {
                    val vttContent = downloadVTT(vttUrl)
                    val cues = parseAndCache(episodeId, vttContent)
                    Result.success(cues)
                } catch (e: IOException) {
                    Log.e(TAG, "Network error downloading transcript, checking for stale cache", e)
                    // Fall back to stale cached data if network fails
                    cachedEntity?.let {
                        try {
                            val cues = VttParser.parseVTT(it.vttContent)
                            memoryCache[episodeId] = cues
                            Log.d(TAG, "Using stale cached transcript for episode: $episodeId")
                            return@withContext Result.success(cues)
                        } catch (parseError: Exception) {
                            Log.e(TAG, "Stale cache is also malformed", parseError)
                        }
                    }
                    // No cache available, return network error
                    Result.failure(TranscriptException.NetworkError("Failed to download transcript: ${e.message}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transcript for episode: $episodeId", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Download VTT file from URL
     *
     * @param url VTT file URL
     * @return VTT file content as string
     * @throws TranscriptException.NotFound if 404 response
     * @throws TranscriptException.Timeout if request times out
     * @throws TranscriptException.NetworkError for other network errors
     */
    private suspend fun downloadVTT(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = okHttpClient.newBuilder()
                    .readTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .connectTimeout(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                when (response.code) {
                    404 -> throw TranscriptException.NotFound("Transcript not found at URL: $url")
                    in 400..499 -> throw TranscriptException.NetworkError("Client error ${response.code}: ${response.message}")
                    in 500..599 -> throw TranscriptException.NetworkError("Server error ${response.code}: ${response.message}")
                }

                if (!response.isSuccessful) {
                    throw TranscriptException.NetworkError("HTTP ${response.code}: ${response.message}")
                }

                response.body?.string() ?: throw TranscriptException.NetworkError("Empty response body")
            } catch (e: java.net.SocketTimeoutException) {
                throw TranscriptException.Timeout("Request timed out after ${NETWORK_TIMEOUT_MS}ms")
            } catch (e: TranscriptException) {
                throw e // Re-throw our custom exceptions
            } catch (e: IOException) {
                throw TranscriptException.NetworkError("Network error: ${e.message}")
            }
        }
    }

    /**
     * Parse VTT content and cache in Room database
     *
     * @param episodeId Episode ID
     * @param vttContent Raw VTT file content
     * @return List of parsed TranscriptCue objects
     * @throws TranscriptException.ParseError if VTT content is malformed
     */
    private suspend fun parseAndCache(episodeId: String, vttContent: String): List<TranscriptCue> {
        return withContext(Dispatchers.IO) {
            // Parse VTT content
            val cues = try {
                VttParser.parseVTT(vttContent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse VTT content for episode: $episodeId", e)
                throw TranscriptException.ParseError("Invalid VTT format: ${e.message}")
            }

            if (cues.isEmpty()) {
                Log.w(TAG, "Parsed VTT has no cues for episode: $episodeId")
                throw TranscriptException.ParseError("Transcript contains no content")
            }

            // Check cache size and evict if necessary
            checkAndEvictCache()

            // Cache in Room
            val transcriptEntity = TranscriptEntity(
                id = episodeId,
                episodeId = episodeId,
                vttContent = vttContent,
                cachedAt = System.currentTimeMillis()
            )
            transcriptDao.insertTranscript(transcriptEntity)

            // Cache transcript lines
            val lineEntities = cues.mapIndexed { index, cue ->
                TranscriptLineEntity(
                    transcriptId = episodeId,
                    startMs = cue.startTime,
                    endMs = cue.endTime,
                    speaker = cue.speaker,
                    text = cue.text,
                    lineNumber = index
                )
            }
            transcriptDao.insertTranscriptLines(lineEntities)

            // Cache in memory
            memoryCache[episodeId] = cues

            Log.d(TAG, "Cached transcript for episode: $episodeId (${cues.size} cues)")

            cues
        }
    }

    /**
     * Check total cache size and evict oldest transcripts if exceeding MAX_CACHE_SIZE_MB
     * Uses LRU (Least Recently Used) eviction strategy based on cachedAt timestamp
     */
    private suspend fun checkAndEvictCache() {
        return withContext(Dispatchers.IO) {
            val allTranscripts = transcriptDao.getAllTranscripts()
            val totalSizeBytes = allTranscripts.sumOf { it.vttContent.toByteArray().size.toLong() }
            val totalSizeMB = totalSizeBytes / (1024 * 1024)

            if (totalSizeMB > MAX_CACHE_SIZE_MB) {
                Log.d(TAG, "Cache size ${totalSizeMB}MB exceeds limit ${MAX_CACHE_SIZE_MB}MB, evicting oldest")

                // Sort by cachedAt (oldest first) and evict until under limit
                val sortedByAge = allTranscripts.sortedBy { it.cachedAt }
                var currentSize = totalSizeMB
                var evicted = 0

                for (transcript in sortedByAge) {
                    if (currentSize <= MAX_CACHE_SIZE_MB * 0.8) break // Leave 20% buffer

                    val transcriptSize = transcript.vttContent.toByteArray().size / (1024 * 1024)
                    transcriptDao.deleteTranscript(transcript.episodeId)
                    currentSize -= transcriptSize
                    evicted++

                    // Remove from memory cache too
                    memoryCache.remove(transcript.episodeId)
                }

                Log.d(TAG, "Evicted $evicted transcripts, cache size now ${currentSize}MB")
            }
        }
    }

    /**
     * Get transcript lines from Room database
     *
     * @param episodeId Episode ID
     * @return List of TranscriptLineEntity or null if not cached
     */
    suspend fun getTranscriptLines(episodeId: String): List<TranscriptLineEntity>? {
        return withContext(Dispatchers.IO) {
            transcriptDao.getTranscriptLines(episodeId).takeIf { it.isNotEmpty() }
        }
    }

    /**
     * Get current transcript line at given playback position
     *
     * @param episodeId Episode ID
     * @param positionMs Playback position in milliseconds
     * @return TranscriptLineEntity at current position or null
     */
    suspend fun getCurrentLine(episodeId: String, positionMs: Long): TranscriptLineEntity? {
        return withContext(Dispatchers.IO) {
            transcriptDao.getCurrentLine(episodeId, positionMs)
        }
    }

    /**
     * Clear old transcripts from cache (older than CACHE_TTL_MS)
     */
    suspend fun clearOldTranscripts() {
        return withContext(Dispatchers.IO) {
            val cutoffTime = System.currentTimeMillis() - CACHE_TTL_MS
            val deleted = transcriptDao.deleteOldTranscripts(cutoffTime)
            Log.d(TAG, "Cleared $deleted old transcripts from cache")
        }
    }

    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        Log.d(TAG, "Cleared memory cache")
    }

    /**
     * Get transcript line by episode ID and line number
     *
     * @param episodeId Episode ID
     * @param lineNumber Line number (0-indexed)
     * @return TranscriptLineEntity or null if not found
     */
    suspend fun getTranscriptLineByNumber(episodeId: String, lineNumber: Int): TranscriptLineEntity? {
        return withContext(Dispatchers.IO) {
            transcriptDao.getTranscriptLineByNumber(episodeId, lineNumber)
        }
    }
}
