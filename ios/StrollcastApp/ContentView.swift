import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            TabView {
                PodcastListView()
                    .tabItem {
                        Label("Strolls", systemImage: "headphones")
                    }

                PlayedListView()
                    .tabItem {
                        Label("Played", systemImage: "checkmark.circle")
                    }

                NotesListView()
                    .tabItem {
                        Label("Notes", systemImage: "note.text")
                    }

                SettingsView()
                    .tabItem {
                        Image(systemName: "gear")
                    }
            }

            // Voice command overlay
            VoiceCommandOverlay()
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(PodcastService())
        .environmentObject(DownloadManager.shared)
        .environmentObject(AudioPlayer.shared)
}
