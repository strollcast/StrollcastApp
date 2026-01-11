package com.strollcast.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completed_episodes",
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["episode_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["episode_id"]),
        Index(value = ["completed_at"], orders = [Index.Order.DESC])
    ]
)
data class CompletedEpisodeEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "episode_id")
    val episodeId: String,

    @ColumnInfo(name = "completion_percent")
    val completionPercent: Int,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long,

    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long
)
