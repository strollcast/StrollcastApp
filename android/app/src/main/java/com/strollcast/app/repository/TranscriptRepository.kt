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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "TranscriptRepository"
        private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
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
                transcriptDao.getTranscript(episodeId)?.let { entity ->
                    Log.d(TAG, "Transcript found in Room cache for episode: $episodeId")
                    val cues = VttParser.parseVTT(entity.vttContent)
                    memoryCache[episodeId] = cues
                    return@withContext Result.success(cues)
                }

                // 3. Download from network
                if (vttUrl.isNullOrBlank()) {
                    Log.w(TAG, "No transcript URL provided for episode: $episodeId")
                    return@withContext Result.failure(Exception("No transcript URL available"))
                }

                Log.d(TAG, "Downloading transcript from: $vttUrl")
                val vttContent = downloadVTT(vttUrl)
                val cues = parseAndCache(episodeId, vttContent)

                Result.success(cues)
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
     * @throws IOException if download fails
     */
    private suspend fun downloadVTT(url: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: ${response.message}")
            }

            response.body?.string() ?: throw IOException("Empty response body")
        }
    }

    /**
     * Parse VTT content and cache in Room database
     *
     * @param episodeId Episode ID
     * @param vttContent Raw VTT file content
     * @return List of parsed TranscriptCue objects
     */
    private suspend fun parseAndCache(episodeId: String, vttContent: String): List<TranscriptCue> {
        return withContext(Dispatchers.IO) {
            // Parse VTT content
            val cues = VttParser.parseVTT(vttContent)

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
}
