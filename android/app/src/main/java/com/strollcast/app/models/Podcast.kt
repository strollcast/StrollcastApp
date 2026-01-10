package com.strollcast.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.Date

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey
    val id: String,

    val title: String?,

    val authors: String?,

    val year: Int?,

    val duration: String?,

    @SerializedName("duration_seconds")
    val durationSeconds: Int?,

    val description: String?,

    @SerializedName("audio_url")
    val audioUrl: String?,

    @SerializedName("transcript_url")
    val transcriptUrl: String?,

    @SerializedName("paper_url")
    val paperUrl: String?,

    val published: Boolean?,

    @SerializedName("created_at")
    val createdAt: Date?
)

data class EpisodesResponse(
    val episodes: List<Podcast>
)

data class TranscriptCue(
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val speaker: String?,
    val text: String
)
