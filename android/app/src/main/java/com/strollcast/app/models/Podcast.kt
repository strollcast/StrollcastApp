package com.strollcast.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import java.util.Date

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey
    val id: String,

    val title: String?,

    val authors: String?,

    val year: Int?,

    val duration: String?,

    val durationSeconds: Int?,

    val description: String?,

    val audioUrl: String?,

    val transcriptUrl: String?,

    val paperUrl: String?,

    val published: Boolean?,

    val createdAt: Date?
)

data class EpisodesResponse(
    val episodes: List<Podcast>
)

class EpisodesResponseDeserializer : JsonDeserializer<EpisodesResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): EpisodesResponse {
        val jsonObject = json.asJsonObject
        val episodesArray = jsonObject.getAsJsonArray("episodes")
        val episodes = episodesArray.map { element ->
            context.deserialize<Podcast>(element, Podcast::class.java)
        }
        return EpisodesResponse(episodes)
    }
}

data class TranscriptCue(
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val speaker: String?,
    val text: String
)
