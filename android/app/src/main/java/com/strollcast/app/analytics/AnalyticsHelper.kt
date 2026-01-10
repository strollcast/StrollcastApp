package com.strollcast.app.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsHelper @Inject constructor() {

    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    private val crashlytics by lazy {
        Firebase.crashlytics
    }

    // Analytics Events
    fun logPodcastPlayed(podcastId: String, title: String) {
        val bundle = Bundle().apply {
            putString("podcast_id", podcastId)
            putString("podcast_title", title)
        }
        analytics.logEvent("podcast_played", bundle)
    }

    fun logPodcastDownloaded(podcastId: String) {
        val bundle = Bundle().apply {
            putString("podcast_id", podcastId)
        }
        analytics.logEvent("podcast_downloaded", bundle)
    }

    fun logZoteroSync(success: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("success", success)
        }
        analytics.logEvent("zotero_sync", bundle)
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // Crashlytics
    fun logError(message: String, throwable: Throwable? = null) {
        crashlytics.log(message)
        throwable?.let { crashlytics.recordException(it) }
    }

    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
        analytics.setUserId(userId)
    }

    fun setUserProperty(key: String, value: String) {
        analytics.setUserProperty(key, value)
    }
}
