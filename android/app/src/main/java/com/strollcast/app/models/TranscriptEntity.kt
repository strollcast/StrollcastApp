package com.strollcast.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcripts",
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["episodeId"])]
)
data class TranscriptEntity(
    @PrimaryKey
    val id: String,

    val episodeId: String,

    @ColumnInfo(name = "vtt_content")
    val vttContent: String,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long
)
