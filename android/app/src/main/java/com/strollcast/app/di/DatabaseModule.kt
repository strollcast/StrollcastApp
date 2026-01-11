package com.strollcast.app.di

import android.content.Context
import androidx.room.Room
import com.strollcast.app.data.CompletedEpisodeDao
import com.strollcast.app.data.DownloadDao
import com.strollcast.app.data.NoteDao
import com.strollcast.app.data.PlaybackHistoryDao
import com.strollcast.app.data.PodcastDao
import com.strollcast.app.data.StrollcastDatabase
import com.strollcast.app.data.TranscriptDao
import com.strollcast.app.data.migrations.MIGRATION_5_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StrollcastDatabase {
        return Room.databaseBuilder(
            context,
            StrollcastDatabase::class.java,
            "strollcast_database"
        )
        .addMigrations(MIGRATION_5_6)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePodcastDao(database: StrollcastDatabase): PodcastDao {
        return database.podcastDao()
    }

    @Provides
    fun providePlaybackHistoryDao(database: StrollcastDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }

    @Provides
    fun provideDownloadDao(database: StrollcastDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideTranscriptDao(database: StrollcastDatabase): TranscriptDao {
        return database.transcriptDao()
    }

    @Provides
    fun provideNoteDao(database: StrollcastDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideCompletedEpisodeDao(database: StrollcastDatabase): CompletedEpisodeDao {
        return database.completedEpisodeDao()
    }
}
