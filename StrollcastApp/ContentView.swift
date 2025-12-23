import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            PodcastListView()
                .tabItem {
                    Label("Podcasts", systemImage: "headphones")
                }

            NotesListView()
                .tabItem {
                    Label("Notes", systemImage: "note.text")
                }
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(PodcastService())
        .environmentObject(DownloadManager.shared)
        .environmentObject(AudioPlayer.shared)
}
