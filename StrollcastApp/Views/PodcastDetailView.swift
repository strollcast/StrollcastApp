import SwiftUI

struct PodcastDetailView: View {
    let podcast: Podcast

    @EnvironmentObject var downloadManager: DownloadManager
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var showPlayer = false
    @State private var notes: String = ""
    @State private var showTranscriptNotes = false
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

                playSection

                Divider()

                transcriptNotesSection
            }
            .padding()
        }
        .navigationTitle("Episode")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                downloadMenu
            }
        }
        .sheet(isPresented: $showPlayer) {
            PlayerView(podcast: podcast)
        }
        .onAppear {
            notes = ListeningHistoryService.shared.readNotes(for: podcast)
        }
        .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
            if let podcastId = notification.object as? String, podcastId == podcast.id {
                notes = ListeningHistoryService.shared.readNotes(for: podcast)
            }
        }
    }

    @ViewBuilder
    private var downloadMenu: some View {
        let state = downloadManager.downloadState(for: podcast)

        Menu {
            switch state {
            case .notDownloaded:
                Button {
                    downloadManager.download(podcast)
                } label: {
                    Label("Download", systemImage: "arrow.down.circle")
                }

            case .downloading:
                Button(role: .destructive) {
                    downloadManager.cancelDownload(podcast)
                } label: {
                    Label("Cancel Download", systemImage: "xmark.circle")
                }

            case .downloaded:
                Button(role: .destructive) {
                    downloadManager.deleteDownload(podcast)
                } label: {
                    Label("Delete Download", systemImage: "trash")
                }

            case .failed:
                Button {
                    downloadManager.download(podcast)
                } label: {
                    Label("Retry Download", systemImage: "arrow.clockwise")
                }
            }
        } label: {
            switch state {
            case .notDownloaded:
                Image(systemName: "ellipsis.circle")
            case .downloading(let progress):
                ZStack {
                    Circle()
                        .stroke(Color.gray.opacity(0.3), lineWidth: 2)
                        .frame(width: 22, height: 22)
                    Circle()
                        .trim(from: 0, to: progress)
                        .stroke(Color.blue, lineWidth: 2)
                        .frame(width: 22, height: 22)
                        .rotationEffect(.degrees(-90))
                }
            case .downloaded:
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            case .failed:
                Image(systemName: "exclamationmark.circle.fill")
                    .foregroundColor(.red)
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
    private var transcriptNotesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button {
                withAnimation {
                    showTranscriptNotes.toggle()
                }
                if showTranscriptNotes && transcript.isEmpty && !isLoadingTranscript {
                    loadTranscript()
                }
            } label: {
                HStack {
                    Text("Transcript & Notes")
                        .font(.headline)
                        .foregroundColor(.primary)
                    if !transcript.isEmpty || !notes.isEmpty {
                        Image(systemName: "text.quote")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                    Spacer()
                    Image(systemName: showTranscriptNotes ? "chevron.up" : "chevron.down")
                        .foregroundColor(.secondary)
                }
            }

            if showTranscriptNotes {
                if isLoadingTranscript {
                    HStack {
                        ProgressView()
                        Text("Loading transcript...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
                } else {
                    DetailTranscriptNotesView(
                        transcript: transcript,
                        notes: $notes,
                        podcast: podcast
                    )
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
}

struct DetailTranscriptNotesView: View {
    let transcript: [TranscriptCue]
    @Binding var notes: String
    let podcast: Podcast

    @State private var editingCueId: UUID? = nil
    @State private var newComment: String = ""
    @FocusState private var isCommentFocused: Bool

    private var timestampedComments: [TimeInterval: String] {
        let comments = ListeningHistoryService.shared.parseTimestampedComments(from: notes)
        var dict: [TimeInterval: String] = [:]
        for comment in comments {
            dict[comment.timestamp] = comment.text
        }
        return dict
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            if transcript.isEmpty {
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
                ForEach(transcript) { cue in
                    DetailCueWithNotesView(
                        cue: cue,
                        existingComment: findComment(for: cue.startTime),
                        isEditing: editingCueId == cue.id,
                        newComment: editingCueId == cue.id ? $newComment : .constant(""),
                        isCommentFocused: _isCommentFocused,
                        onTapAdd: {
                            editingCueId = cue.id
                            newComment = ""
                            isCommentFocused = true
                        },
                        onSubmit: {
                            if !newComment.trimmingCharacters(in: .whitespaces).isEmpty {
                                ListeningHistoryService.shared.addTimestampedComment(
                                    newComment,
                                    at: cue.startTime,
                                    for: podcast
                                )
                            }
                            editingCueId = nil
                            newComment = ""
                        },
                        onCancel: {
                            editingCueId = nil
                            newComment = ""
                        }
                    )
                }
            }
        }
        .padding(12)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    private func findComment(for timestamp: TimeInterval) -> String? {
        for (time, comment) in timestampedComments {
            if abs(time - timestamp) < 2.0 {
                return comment
            }
        }
        return nil
    }
}

struct DetailCueWithNotesView: View {
    let cue: TranscriptCue
    let existingComment: String?
    let isEditing: Bool
    @Binding var newComment: String
    @FocusState var isCommentFocused: Bool
    let onTapAdd: () -> Void
    let onSubmit: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .top, spacing: 8) {
                Text(formatTime(cue.startTime))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(width: 45, alignment: .leading)

                VStack(alignment: .leading, spacing: 2) {
                    if let speaker = cue.speaker {
                        Text(speaker)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.blue)
                    }
                    Text(cue.text)
                        .font(.body)
                        .foregroundColor(.primary)
                }

                Spacer()

                if existingComment == nil && !isEditing {
                    Button(action: onTapAdd) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                    }
                    .buttonStyle(.plain)
                    .frame(width: 44, height: 44)
                }
            }
            .padding(.vertical, 4)

            if let comment = existingComment {
                HStack(spacing: 6) {
                    Image(systemName: "note.text")
                        .font(.caption2)
                        .foregroundColor(.orange)
                    Text(comment)
                        .font(.callout)
                        .foregroundColor(.primary)
                }
                .padding(.leading, 53)
                .padding(.vertical, 4)
                .padding(.trailing, 8)
                .background(Color.orange.opacity(0.15))
                .cornerRadius(6)
            }

            if isEditing {
                HStack(spacing: 8) {
                    TextField("Add a note...", text: $newComment)
                        .font(.callout)
                        .textFieldStyle(.roundedBorder)
                        .focused($isCommentFocused)
                        .onSubmit(onSubmit)

                    Button(action: onSubmit) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.green)
                    }
                    .buttonStyle(.plain)
                    .disabled(newComment.trimmingCharacters(in: .whitespaces).isEmpty)

                    Button(action: onCancel) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.leading, 53)
                .padding(.trailing, 8)
                .padding(.vertical, 4)
            }
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
