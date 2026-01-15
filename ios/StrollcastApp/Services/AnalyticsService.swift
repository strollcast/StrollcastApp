import Foundation
import FirebaseAnalytics
import FirebaseCrashlytics

/// Centralized analytics and crash reporting service
final class AnalyticsService {
    static let shared = AnalyticsService()

    private init() {}

    // MARK: - User Properties

    func setUserProperty(_ value: String?, forName name: String) {
        Analytics.setUserProperty(value, forName: name)
    }

    // MARK: - Podcast Events

    func logPodcastPlay(podcastId: String, title: String, authors: String) {
        Analytics.logEvent("podcast_play", parameters: [
            "podcast_id": podcastId,
            "title": title,
            "authors": authors
        ])
        Crashlytics.crashlytics().setCustomValue(podcastId, forKey: "last_podcast_id")
    }

    func logPodcastPause(podcastId: String, currentTime: Double, duration: Double) {
        let progress = duration > 0 ? (currentTime / duration) * 100 : 0
        Analytics.logEvent("podcast_pause", parameters: [
            "podcast_id": podcastId,
            "current_time": currentTime,
            "progress_percent": progress
        ])
    }

    func logPodcastComplete(podcastId: String, title: String) {
        Analytics.logEvent("podcast_complete", parameters: [
            "podcast_id": podcastId,
            "title": title
        ])
    }

    func logPodcastSeek(podcastId: String, fromTime: Double, toTime: Double) {
        Analytics.logEvent("podcast_seek", parameters: [
            "podcast_id": podcastId,
            "from_time": fromTime,
            "to_time": toTime
        ])
    }

    // MARK: - Download Events

    func logDownloadStart(podcastId: String) {
        Analytics.logEvent("download_start", parameters: [
            "podcast_id": podcastId
        ])
    }

    func logDownloadComplete(podcastId: String, fileSize: Int64) {
        Analytics.logEvent("download_complete", parameters: [
            "podcast_id": podcastId,
            "file_size_bytes": fileSize
        ])
    }

    func logDownloadError(podcastId: String, error: String) {
        Analytics.logEvent("download_error", parameters: [
            "podcast_id": podcastId,
            "error": error
        ])
        Crashlytics.crashlytics().log("Download failed for \(podcastId): \(error)")
    }

    // MARK: - Note Events

    func logNoteAdded(podcastId: String, timestamp: Double) {
        Analytics.logEvent("note_added", parameters: [
            "podcast_id": podcastId,
            "timestamp": timestamp
        ])
    }

    // MARK: - Navigation Events

    func logScreenView(screenName: String, screenClass: String) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenClass
        ])
    }

    func logEpisodeView(podcastId: String, title: String) {
        Analytics.logEvent("episode_view", parameters: [
            "podcast_id": podcastId,
            "title": title
        ])
    }

    // MARK: - Search Events

    func logSearch(query: String, resultCount: Int) {
        Analytics.logEvent(AnalyticsEventSearch, parameters: [
            AnalyticsParameterSearchTerm: query,
            "result_count": resultCount
        ])
    }

    // MARK: - Voice Command Events

    func logVoiceCommand(command: String, success: Bool) {
        Analytics.logEvent("voice_command", parameters: [
            "command": command,
            "success": success
        ])
    }

    // MARK: - Zotero Events

    func logZoteroSync(itemCount: Int) {
        Analytics.logEvent("zotero_sync", parameters: [
            "item_count": itemCount
        ])
    }

    // MARK: - Error Logging

    func logError(_ error: Error, context: String) {
        Crashlytics.crashlytics().log("\(context): \(error.localizedDescription)")
        Crashlytics.crashlytics().record(error: error)
    }

    func logMessage(_ message: String) {
        Crashlytics.crashlytics().log(message)
    }

    // MARK: - User Identification

    func setUserId(_ userId: String?) {
        Analytics.setUserID(userId)
        if let userId = userId {
            Crashlytics.crashlytics().setUserID(userId)
        }
    }
}
