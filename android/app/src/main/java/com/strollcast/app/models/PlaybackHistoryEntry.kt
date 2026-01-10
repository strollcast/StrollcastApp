package com.strollcast.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val podcastId: String,

    val position: Long, // in milliseconds

    val timestamp: Date
)
