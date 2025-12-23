import SwiftUI

@main
struct StrollcastApp: App {
    @StateObject private var podcastService = PodcastService()
    @StateObject private var downloadManager = DownloadManager.shared
    @StateObject private var audioPlayer = AudioPlayer.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(podcastService)
                .environmentObject(downloadManager)
                .environmentObject(audioPlayer)
        }
    }
}
