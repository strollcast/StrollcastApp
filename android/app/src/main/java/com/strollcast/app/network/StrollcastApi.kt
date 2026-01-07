package com.strollcast.app.network

import com.strollcast.app.models.EpisodesResponse
import retrofit2.http.GET

interface StrollcastApi {
    @GET("episodes")
    suspend fun getEpisodes(): EpisodesResponse

    companion object {
        const val BASE_URL = "https://api.strollcast.com/"
    }
}
