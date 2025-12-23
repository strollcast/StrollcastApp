import SwiftUI

struct ContentView: View {
    var body: some View {
        PodcastListView()
    }
}

#Preview {
    ContentView()
        .environmentObject(PodcastService())
        .environmentObject(DownloadManager.shared)
        .environmentObject(AudioPlayer.shared)
}
