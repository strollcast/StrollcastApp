package com.strollcast.app.data

import androidx.room.*
import com.strollcast.app.models.DownloadedEpisode
import com.strollcast.app.models.PlaybackHistoryEntry
import com.strollcast.app.models.Podcast
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts WHERE published IS NULL OR published = 1 ORDER BY createdAt DESC")
    fun getAllPodcasts(): Flow<List<Podcast>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcastById(id: String): Podcast?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(podcasts: List<Podcast>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: Podcast)

    @Delete
    suspend fun delete(podcast: Podcast)

    @Query("DELETE FROM podcasts")
    suspend fun deleteAll()
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 4")
    fun getHistory(): Flow<List<PlaybackHistoryEntry>>

    @Query("SELECT * FROM playback_history WHERE podcastId = :podcastId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPosition(podcastId: String): PlaybackHistoryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlaybackHistoryEntry)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("DELETE FROM playback_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloaded_episodes")
    fun getAllDownloads(): Flow<List<DownloadedEpisode>>

    @Query("SELECT * FROM downloaded_episodes WHERE episodeId = :episodeId")
    suspend fun getDownload(episodeId: String): DownloadedEpisode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadedEpisode)

    @Delete
    suspend fun delete(download: DownloadedEpisode)

    @Query("DELETE FROM downloaded_episodes")
    suspend fun deleteAll()
}
