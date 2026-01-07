package com.strollcast.app.repository

import com.strollcast.app.data.PodcastDao
import com.strollcast.app.data.PlaybackHistoryDao
import com.strollcast.app.data.DownloadDao
import com.strollcast.app.models.Podcast
import com.strollcast.app.models.PlaybackHistoryEntry
import com.strollcast.app.models.DownloadedEpisode
import com.strollcast.app.network.StrollcastApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val api: StrollcastApi,
    private val podcastDao: PodcastDao,
    private val historyDao: PlaybackHistoryDao,
    private val downloadDao: DownloadDao
) {
    // Podcasts
    val podcasts: Flow<List<Podcast>> = podcastDao.getAllPodcasts()

    suspend fun refreshPodcasts(): Result<Unit> {
        return try {
            val response = api.getEpisodes()
            podcastDao.insertAll(response.episodes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPodcastById(id: String): Podcast? {
        return podcastDao.getPodcastById(id)
    }

    // Playback History
    val playbackHistory: Flow<List<PlaybackHistoryEntry>> = historyDao.getHistory()

    suspend fun savePlaybackPosition(podcastId: String, position: Long) {
        historyDao.insert(
            PlaybackHistoryEntry(
                podcastId = podcastId,
                position = position,
                timestamp = Date()
            )
        )
        // Keep only last 30 days of history
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        historyDao.deleteOlderThan(cutoffTime)
    }

    suspend fun getLastPosition(podcastId: String): Long? {
        return historyDao.getLastPosition(podcastId)?.position
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun clearPlaybackHistory() {
        historyDao.clearHistory()
    }

    // Downloads
    val downloads: Flow<List<DownloadedEpisode>> = downloadDao.getAllDownloads()

    fun getAllDownloads(): Flow<List<DownloadedEpisode>> {
        return downloadDao.getAllDownloads()
    }

    suspend fun getDownload(episodeId: String): DownloadedEpisode? {
        return downloadDao.getDownload(episodeId)
    }

    suspend fun saveDownload(download: DownloadedEpisode) {
        downloadDao.insert(download)
    }

    suspend fun deleteDownload(download: DownloadedEpisode) {
        downloadDao.delete(download)
    }

    suspend fun deleteDownload(episodeId: String) {
        val download = downloadDao.getDownload(episodeId)
        if (download != null) {
            downloadDao.delete(download)
        }
    }

    suspend fun deleteAllDownloads() {
        downloadDao.deleteAll()
    }
}
