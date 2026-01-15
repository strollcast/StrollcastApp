import SwiftUI
import FirebaseAnalytics

@main
struct StrollcastApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    @StateObject private var podcastService = PodcastService()
    @StateObject private var downloadManager = DownloadManager.shared
    @StateObject private var audioPlayer = AudioPlayer.shared
    @StateObject private var voiceCommandService = VoiceCommandService.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(podcastService)
                .environmentObject(downloadManager)
                .environmentObject(audioPlayer)
                .environmentObject(voiceCommandService)
                .onChange(of: scenePhase) { _, newPhase in
                    if newPhase == .active {
                        Task {
                            await podcastService.fetchPodcasts()
                        }
                        // Log app activation event
                        Analytics.logEvent("app_opened", parameters: nil)
                    }
                }
        }
    }
}
