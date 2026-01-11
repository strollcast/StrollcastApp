package com.strollcast.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.strollcast.app.workers.TranscriptCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class StrollcastApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Application initialization
        scheduleBackgroundTasks()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Schedule periodic background tasks using WorkManager
     */
    private fun scheduleBackgroundTasks() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Only run when battery is not low
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed for cleanup
            .build()

        // Daily transcript cleanup
        val cleanupRequest = PeriodicWorkRequestBuilder<TranscriptCleanupWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TranscriptCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            cleanupRequest
        )
    }
}
