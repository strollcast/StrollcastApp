import SwiftUI

struct PodcastDetailView: View {
    let podcast: Podcast

    @EnvironmentObject var downloadManager: DownloadManager
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var showPlayer = false
    @State private var notes: String = ""
    @State private var showNotes = false
    @State private var showTranscript = false
    @State private var transcript: [TranscriptCue] = []
    @State private var isLoadingTranscript = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text(String(podcast.year))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.blue.opacity(0.1))
                            .foregroundColor(.blue)
                            .cornerRadius(6)

                        Text(podcast.duration)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    Text(podcast.title)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(podcast.authors)
                        .font(.headline)
                        .foregroundColor(.secondary)
                }

                Text(podcast.description)
                    .font(.body)
                    .foregroundColor(.secondary)

                Divider()

                downloadSection

                Divider()

                playSection

                Divider()

                notesSection

                Divider()

                transcriptSection
            }
            .padding()
        }
        .navigationTitle("Episode")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showPlayer) {
            PlayerView(podcast: podcast)
        }
        .onAppear {
            notes = ListeningHistoryService.shared.readNotes(for: podcast)
            showNotes = !notes.isEmpty
        }
        .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
            if let podcastId = notification.object as? String, podcastId == podcast.id {
                notes = ListeningHistoryService.shared.readNotes(for: podcast)
            }
        }
    }

    @ViewBuilder
    private var downloadSection: some View {
        let state = downloadManager.downloadState(for: podcast)

        VStack(alignment: .leading, spacing: 12) {
            Text("Download")
                .font(.headline)

            switch state {
            case .notDownloaded:
                Button {
                    downloadManager.download(podcast)
                } label: {
                    Label("Download Episode", systemImage: "arrow.down.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

            case .downloading(let progress):
                VStack(spacing: 8) {
                    ProgressView(value: progress)
                    HStack {
                        Text("Downloading... \(Int(progress * 100))%")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button("Cancel") {
                            downloadManager.cancelDownload(podcast)
                        }
                        .font(.caption)
                        .foregroundColor(.red)
                    }
                }

            case .downloaded:
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("Downloaded")
                        .foregroundColor(.secondary)
                    Spacer()
                    Button("Delete") {
                        downloadManager.deleteDownload(podcast)
                    }
                    .foregroundColor(.red)
                }

            case .failed(let error):
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "exclamationmark.circle.fill")
                            .foregroundColor(.red)
                        Text("Download failed")
                            .foregroundColor(.red)
                    }
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Button {
                        downloadManager.download(podcast)
                    } label: {
                        Label("Retry", systemImage: "arrow.clockwise")
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }

    @ViewBuilder
    private var playSection: some View {
        let state = downloadManager.downloadState(for: podcast)
        let isCurrentlyPlaying = audioPlayer.currentPodcast?.id == podcast.id

        VStack(alignment: .leading, spacing: 12) {
            Text("Playback")
                .font(.headline)

            if case .downloaded(let url) = state {
                if isCurrentlyPlaying {
                    Button {
                        showPlayer = true
                    } label: {
                        Label("Open Player", systemImage: "music.note")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                } else {
                    Button {
                        audioPlayer.load(podcast: podcast, from: url)
                        audioPlayer.play()
                        showPlayer = true
                    } label: {
                        Label("Play Episode", systemImage: "play.fill")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                Text("Download the episode first to play it offline")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Button {
                    audioPlayer.load(podcast: podcast, from: podcast.audioURL)
                    audioPlayer.play()
                    showPlayer = true
                } label: {
                    Label("Stream Episode", systemImage: "antenna.radiowaves.left.and.right")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    @ViewBuilder
    private var notesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                withAnimation {
                    showNotes.toggle()
                }
            } label: {
                HStack {
                    Text("Notes")
                        .font(.headline)
                        .foregroundColor(.primary)
                    if !notes.isEmpty {
                        Image(systemName: "note.text")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                    Spacer()
                    Image(systemName: showNotes ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
            }

            if showNotes {
                TextEditor(text: $notes)
                    .font(.system(.body, design: .monospaced))
                    .frame(minHeight: 200)
                    .padding(8)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    .onChange(of: notes) { _, newValue in
                        ListeningHistoryService.shared.saveNotes(newValue, for: podcast)
                    }
            }
        }
    }

    @ViewBuilder
    private var transcriptSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                withAnimation {
                    showTranscript.toggle()
                }
                if showTranscript && transcript.isEmpty && !isLoadingTranscript {
                    loadTranscript()
                }
            } label: {
                HStack {
                    Text("Transcript")
                        .font(.headline)
                        .foregroundColor(.primary)
                    if !transcript.isEmpty {
                        Image(systemName: "text.quote")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                    Spacer()
                    Image(systemName: showTranscript ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
            }

            if showTranscript {
                if isLoadingTranscript {
                    HStack {
                        ProgressView()
                        Text("Loading transcript...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
                } else if transcript.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "text.quote")
                            .font(.title)
                            .foregroundColor(.secondary)
                        Text("No transcript available")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                } else {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(transcript) { cue in
                            VStack(alignment: .leading, spacing: 2) {
                                HStack {
                                    Text(formatTime(cue.startTime))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                    if let speaker = cue.speaker {
                                        Text(speaker)
                                            .font(.caption)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.blue)
                                    }
                                }
                                Text(cue.text)
                                    .font(.body)
                                    .foregroundColor(.primary)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .padding(12)
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                }
            }
        }
    }

    private func loadTranscript() {
        isLoadingTranscript = true
        Task { @MainActor in
            let cues = await TranscriptService.shared.getTranscript(for: podcast)
            transcript = cues ?? []
            isLoadingTranscript = false
        }
    }

    private func formatTime(_ time: TimeInterval) -> String {
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

#Preview {
    NavigationStack {
        PodcastDetailView(podcast: Podcast.samples[0])
    }
    .environmentObject(DownloadManager.shared)
    .environmentObject(AudioPlayer.shared)
}
