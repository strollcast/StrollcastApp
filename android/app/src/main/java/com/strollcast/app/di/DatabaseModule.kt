package com.strollcast.app.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.strollcast.app.data.StrollcastDatabase
import com.strollcast.app.data.PodcastDao
import com.strollcast.app.data.PlaybackHistoryDao
import com.strollcast.app.data.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StrollcastDatabase {
        return Room.databaseBuilder(
            context,
            StrollcastDatabase::class.java,
            "strollcast_database"
        )
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
}
