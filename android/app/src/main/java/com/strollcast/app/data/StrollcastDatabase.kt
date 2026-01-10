package com.strollcast.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.strollcast.app.models.DownloadedEpisode
import com.strollcast.app.models.Podcast
import com.strollcast.app.models.PlaybackHistoryEntry

@Database(
    entities = [
        Podcast::class,
        PlaybackHistoryEntry::class,
        DownloadedEpisode::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StrollcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun downloadDao(): DownloadDao
}
