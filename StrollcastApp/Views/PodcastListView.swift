import SwiftUI

struct PodcastListView: View {
    @EnvironmentObject var podcastService: PodcastService
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var navigationPath = NavigationPath()
    @State private var hasNavigatedToLastPodcast = false
    @State private var searchText = ""

    var unplayedPodcasts: [Podcast] {
        podcastService.podcasts.filter { podcast in
            !ListeningHistoryService.shared.isCompleted(podcast: podcast)
        }
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                VStack(spacing: 0) {
                    if podcastService.isLoading {
                        ProgressView("Loading podcasts...")
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if let error = podcastService.errorMessage {
                        VStack(spacing: 16) {
                            Image(systemName: "exclamationmark.triangle")
                                .font(.largeTitle)
                                .foregroundColor(.orange)
                            Text(error)
                                .multilineTextAlignment(.center)
                                .foregroundColor(.secondary)
                            Button("Retry") {
                                Task {
                                    await podcastService.fetchPodcasts()
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .padding()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List(unplayedPodcasts) { podcast in
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
            .navigationTitle("Strollcast")
            .navigationDestination(for: Podcast.self) { podcast in
                PodcastDetailView(podcast: podcast)
            }
            .searchable(text: $searchText, prompt: "Search episodes")
            .onChange(of: searchText) { _, newValue in
                Task {
                    if newValue.isEmpty {
                        await podcastService.fetchPodcasts()
                    } else {
                        await podcastService.searchPodcasts(query: newValue)
                    }
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            searchText = ""
                            await podcastService.fetchPodcasts()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .onChange(of: podcastService.podcasts) { _, podcasts in
                navigateToLastPodcastIfNeeded(podcasts: podcasts)
            }
        }
        .task {
            if podcastService.podcasts.isEmpty {
                await podcastService.fetchPodcasts()
            }
        }
    }

    private func navigateToLastPodcastIfNeeded(podcasts: [Podcast]) {
        guard !hasNavigatedToLastPodcast,
              !podcasts.isEmpty,
              let lastPodcastId = ListeningHistoryService.shared.getLastActivePodcastId(),
              let podcast = podcasts.first(where: { $0.id == lastPodcastId }) else {
            return
        }
        hasNavigatedToLastPodcast = true
        navigationPath.append(podcast)
    }
}

struct MiniPlayerView: View {
    @EnvironmentObject var audioPlayer: AudioPlayer

    var body: some View {
        if let podcast = audioPlayer.currentPodcast {
            VStack(spacing: 0) {
                Divider()

                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(podcast.title)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .lineLimit(1)
                        Text(podcast.authors)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }

                    Spacer()

                    HStack(spacing: 16) {
                        Button {
                            audioPlayer.skipBackward()
                        } label: {
                            Image(systemName: "gobackward.15")
                                .font(.body)
                        }

                        Button {
                            audioPlayer.togglePlayPause()
                        } label: {
                            Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                                .font(.title2)
                        }

                        Button {
                            audioPlayer.skipForward()
                        } label: {
                            Image(systemName: "goforward.15")
                                .font(.body)
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 10)
                .background(Color(.systemBackground))
            }
        }
    }
}

#Preview {
    PodcastListView()
        .environmentObject(PodcastService())
        .environmentObject(DownloadManager.shared)
        .environmentObject(AudioPlayer.shared)
}
