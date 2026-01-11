package com.strollcast.app.repository

import android.util.Log
import com.strollcast.app.data.CompletedEpisodeDao
import com.strollcast.app.models.CompletedEpisodeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val completedEpisodeDao: CompletedEpisodeDao
) {
    companion object {
        private const val TAG = "HistoryRepository"
        private const val COMPLETION_THRESHOLD = 0.9 // 90%
    }

    /**
     * Get all completed episodes as Flow
     *
     * @return Flow of completed episodes sorted by completion date (newest first)
     */
    fun getAllCompletedEpisodes(): Flow<List<CompletedEpisodeEntity>> {
        return completedEpisodeDao.getAllCompletedEpisodes()
    }

    /**
     * Check if an episode is marked as completed
     *
     * @param episodeId Episode ID
     * @return True if episode is completed
     */
    suspend fun isEpisodeCompleted(episodeId: String): Boolean {
        return withContext(Dispatchers.IO) {
            completedEpisodeDao.isEpisodeCompleted(episodeId)
        }
    }

    /**
     * Mark episode as complete if playback reaches 90% threshold
     *
     * @param episodeId Episode ID
     * @param playbackPosition Current playback position in milliseconds
     * @param episodeDuration Total episode duration in milliseconds
     * @return Result indicating success or error
     */
    suspend fun markEpisodeComplete(
        episodeId: String,
        playbackPosition: Long,
        episodeDuration: Long
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (episodeDuration <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Episode duration must be positive"))
                }

                val completionPercent = ((playbackPosition.toDouble() / episodeDuration) * 100).toInt()

                // Only mark as complete if threshold is met
                if (completionPercent < (COMPLETION_THRESHOLD * 100).toInt()) {
                    Log.d(TAG, "Episode $episodeId not yet complete: $completionPercent%")
                    return@withContext Result.success(Unit)
                }

                val completedEpisode = CompletedEpisodeEntity(
                    id = episodeId,
                    episodeId = episodeId,
                    completionPercent = completionPercent.coerceIn(0, 100),
                    completedAt = System.currentTimeMillis(),
                    totalDurationMs = episodeDuration
                )

                completedEpisodeDao.insertCompletedEpisode(completedEpisode)
                Log.d(TAG, "Marked episode as complete: $episodeId ($completionPercent%)")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking episode as complete", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Remove episode from completed list
     *
     * @param episodeId Episode ID
     * @return Result indicating success or error
     */
    suspend fun removeFromCompleted(episodeId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                completedEpisodeDao.removeCompletedEpisode(episodeId)
                Log.d(TAG, "Removed episode from completed: $episodeId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing completed episode", e)
                Result.failure(e)
            }
        }
    }
}
