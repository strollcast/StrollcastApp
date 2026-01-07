import SwiftUI

struct PlayedListView: View {
    @EnvironmentObject var podcastService: PodcastService
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var navigationPath = NavigationPath()

    var playedPodcasts: [Podcast] {
        podcastService.podcasts.filter { podcast in
            ListeningHistoryService.shared.isCompleted(podcast: podcast)
        }
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                VStack(spacing: 0) {
                    if podcastService.isLoading {
                        ProgressView("Loading podcasts...")
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if playedPodcasts.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "checkmark.circle")
                                .font(.system(size: 48))
                                .foregroundColor(.secondary)
                            Text("No Played Podcasts")
                                .font(.headline)
                            Text("Podcasts you've finished will appear here")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List(playedPodcasts) { podcast in
                            NavigationLink(value: podcast) {
                                PodcastRowView(podcast: podcast)
                            }
                        }
                        .listStyle(.plain)
                    }

                    if audioPlayer.currentPodcast != nil {
                        MiniPlayerView()
                    }
                }
            }
            .navigationTitle("Played")
            .navigationDestination(for: Podcast.self) { podcast in
                PodcastDetailView(podcast: podcast)
            }
        }
    }
}

#Preview {
    PlayedListView()
        .environmentObject(PodcastService())
        .environmentObject(DownloadManager.shared)
        .environmentObject(AudioPlayer.shared)
}
