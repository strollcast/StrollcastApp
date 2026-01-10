package com.strollcast.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "downloaded_episodes")
data class DownloadedEpisode(
    @PrimaryKey
    val episodeId: String,

    val localAudioPath: String,

    val localTranscriptPath: String?,

    val fileSize: Long, // in bytes

    val downloadedAt: Date
)
