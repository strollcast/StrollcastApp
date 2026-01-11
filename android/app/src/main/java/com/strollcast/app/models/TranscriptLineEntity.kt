package com.strollcast.app.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_lines",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcriptId"]),
        Index(value = ["transcriptId", "lineNumber"])
    ]
)
data class TranscriptLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transcript_id")
    val transcriptId: String,

    @ColumnInfo(name = "start_ms")
    val startMs: Long,

    @ColumnInfo(name = "end_ms")
    val endMs: Long,

    val speaker: String?,

    val text: String,

    @ColumnInfo(name = "line_number")
    val lineNumber: Int
)
