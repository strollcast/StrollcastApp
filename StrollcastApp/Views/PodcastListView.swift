import SwiftUI

struct PodcastListView: View {
    @EnvironmentObject var podcastService: PodcastService
    @EnvironmentObject var audioPlayer: AudioPlayer

    var body: some View {
        NavigationStack {
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
                        List(podcastService.podcasts) { podcast in
                            NavigationLink(destination: PodcastDetailView(podcast: podcast)) {
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
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await podcastService.fetchPodcasts()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
        .task {
            if podcastService.podcasts.isEmpty {
                await podcastService.fetchPodcasts()
            }
        }
    }
}

struct MiniPlayerView: View {
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var showFullPlayer = false

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

                    Button {
                        audioPlayer.togglePlayPause()
                    } label: {
                        Image(systemName: audioPlayer.isPlaying ? "pause.fill" : "play.fill")
                            .font(.title2)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 10)
                .background(Color(.systemBackground))
                .contentShape(Rectangle())
                .onTapGesture {
                    showFullPlayer = true
                }
            }
            .sheet(isPresented: $showFullPlayer) {
                PlayerView(podcast: podcast)
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
