import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            PodcastListView()
                .tabItem {
                    Label("Podcasts", systemImage: "headphones")
                }

            PlayedListView()
                .tabItem {
                    Label("Played", systemImage: "checkmark.circle")
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
