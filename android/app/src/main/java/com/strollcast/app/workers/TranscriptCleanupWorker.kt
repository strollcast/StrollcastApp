package com.strollcast.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strollcast.app.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker to periodically clean up old cached transcripts
 * Runs daily to remove transcripts older than 30 days
 */
@HiltWorker
class TranscriptCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptRepository: TranscriptRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "TranscriptCleanupWorker"
        const val WORK_NAME = "transcript_cleanup"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting transcript cleanup")

            // Clear old transcripts (older than 30 days)
            transcriptRepository.clearOldTranscripts()

            // Also clear memory cache to free up RAM
            transcriptRepository.clearMemoryCache()

            Log.d(TAG, "Transcript cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during transcript cleanup", e)
            // Retry on failure
            Result.retry()
        }
    }
}
