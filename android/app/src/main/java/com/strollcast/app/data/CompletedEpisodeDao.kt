package com.strollcast.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.strollcast.app.models.CompletedEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedEpisodeDao {
    @Query("SELECT * FROM completed_episodes ORDER BY completed_at DESC")
    fun getAllCompletedEpisodes(): Flow<List<CompletedEpisodeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM completed_episodes WHERE id = :episodeId)")
    suspend fun isEpisodeCompleted(episodeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedEpisode(episode: CompletedEpisodeEntity)

    @Query("DELETE FROM completed_episodes WHERE id = :episodeId")
    suspend fun removeCompletedEpisode(episodeId: String)
}
