package com.strollcast.app.di

import com.strollcast.app.data.CompletedEpisodeDao
import com.strollcast.app.data.NoteDao
import com.strollcast.app.data.TranscriptDao
import com.strollcast.app.repository.HistoryRepository
import com.strollcast.app.repository.NoteRepository
import com.strollcast.app.repository.TranscriptRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTranscriptRepository(
        transcriptDao: TranscriptDao,
        okHttpClient: OkHttpClient
    ): TranscriptRepository {
        return TranscriptRepository(transcriptDao, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteDao: NoteDao
    ): NoteRepository {
        return NoteRepository(noteDao)
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(
        completedEpisodeDao: CompletedEpisodeDao
    ): HistoryRepository {
        return HistoryRepository(completedEpisodeDao)
    }
}
