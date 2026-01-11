package com.strollcast.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcript_line_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcript_line_id"]),
        Index(value = ["episode_id", "created_at"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transcript_line_id")
    val transcriptLineId: Int,

    @ColumnInfo(name = "episode_id")
    val episodeId: String,

    val content: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
