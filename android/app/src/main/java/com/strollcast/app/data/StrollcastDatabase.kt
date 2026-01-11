package com.strollcast.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.strollcast.app.models.CompletedEpisodeEntity
import com.strollcast.app.models.DownloadedEpisode
import com.strollcast.app.models.NoteEntity
import com.strollcast.app.models.Podcast
import com.strollcast.app.models.PlaybackHistoryEntry
import com.strollcast.app.models.TranscriptEntity
import com.strollcast.app.models.TranscriptLineEntity

@Database(
    entities = [
        Podcast::class,
        PlaybackHistoryEntry::class,
        DownloadedEpisode::class,
        TranscriptEntity::class,
        TranscriptLineEntity::class,
        NoteEntity::class,
        CompletedEpisodeEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StrollcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun noteDao(): NoteDao
    abstract fun completedEpisodeDao(): CompletedEpisodeDao
}
