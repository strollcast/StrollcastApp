import SwiftUI

struct PodcastDetailView: View {
    let podcast: Podcast

    @EnvironmentObject var downloadManager: DownloadManager
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var notes: String = ""
    @State private var transcript: [TranscriptCue] = []
    @State private var isLoadingTranscript = true

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Header with year, duration, paper link
                    headerSection

                    // Title and authors
                    VStack(alignment: .leading, spacing: 8) {
                        Text(podcast.title)
                            .font(.title2)
                            .fontWeight(.bold)

                        Text(podcast.authors)
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }

                    // Description
                    Text(podcast.description)
                        .font(.body)
                        .foregroundColor(.secondary)

                    Divider()

                    // Transcript & Notes (always visible)
                    transcriptNotesSection
                }
                .padding()
                .padding(.bottom, 80) // Space for player bar
            }

            // Fixed player bar at bottom
            playerBar
        }
        .navigationTitle("Episode")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                downloadMenu
            }
        }
        .onAppear {
            notes = ListeningHistoryService.shared.readNotes(for: podcast)
            loadTranscript()
            loadAudioIfNeeded()
        }
        .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
            if let podcastId = notification.object as? String, podcastId == podcast.id {
                notes = ListeningHistoryService.shared.readNotes(for: podcast)
            }
        }
    }

    @ViewBuilder
    private var headerSection: some View {
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

            Spacer()

            if let paperURL = podcast.paperURL {
                Link(destination: paperURL) {
                    Label("Paper", systemImage: "doc.text")
                        .font(.subheadline)
                }
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
    private var playerBar: some View {
        let isCurrentPodcast = audioPlayer.currentPodcast?.id == podcast.id

        VStack(spacing: 8) {
            // Progress bar
            if isCurrentPodcast && audioPlayer.duration > 0 {
                VStack(spacing: 2) {
                    Slider(
                        value: Binding(
                            get: { audioPlayer.currentTime },
                            set: { audioPlayer.seek(to: $0) }
                        ),
                        in: 0...max(audioPlayer.duration, 1)
                    )

                    HStack {
                        Text(audioPlayer.formattedTime(audioPlayer.currentTime))
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .monospacedDigit()
                        Spacer()
                        Text(audioPlayer.formattedTime(audioPlayer.duration))
                            .font(.caption2)
                            .foregroundColor(.secondary)
                            .monospacedDigit()
                    }
                }
                .padding(.horizontal)
            }

            // Playback controls
            HStack(spacing: 40) {
                Button {
                    audioPlayer.skipBackward()
                } label: {
                    Image(systemName: "gobackward.15")
                        .font(.title2)
                }
                .disabled(!isCurrentPodcast)

                Button {
                    if isCurrentPodcast {
                        audioPlayer.togglePlayPause()
                    } else {
                        startPlayback()
                    }
                } label: {
                    Image(systemName: isCurrentPodcast && audioPlayer.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 50))
                }

                Button {
                    audioPlayer.skipForward()
                } label: {
                    Image(systemName: "goforward.15")
                        .font(.title2)
                }
                .disabled(!isCurrentPodcast)
            }
            .padding(.bottom, 8)
        }
        .padding(.top, 8)
        .background(Color(.systemBackground))
        .shadow(color: .black.opacity(0.1), radius: 4, y: -2)
    }

    @ViewBuilder
    private var transcriptNotesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Transcript & Notes")
                .font(.headline)

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
                    podcast: podcast,
                    currentTime: audioPlayer.currentPodcast?.id == podcast.id ? audioPlayer.currentTime : 0,
                    isPlaying: audioPlayer.currentPodcast?.id == podcast.id && audioPlayer.isPlaying,
                    onPausePlayback: { audioPlayer.pause() },
                    onResumePlayback: { audioPlayer.play() },
                    onSeekTo: { time in audioPlayer.seek(to: time) }
                )
            }
        }
    }

    private func loadTranscript() {
        Task { @MainActor in
            let cues = await TranscriptService.shared.getTranscript(for: podcast)
            transcript = cues ?? []
            isLoadingTranscript = false
        }
    }

    private func loadAudioIfNeeded() {
        // If this podcast is not currently loaded, prepare it
        if audioPlayer.currentPodcast?.id != podcast.id {
            let state = downloadManager.downloadState(for: podcast)
            if case .downloaded(let url) = state {
                audioPlayer.load(podcast: podcast, from: url)
            } else {
                audioPlayer.load(podcast: podcast, from: podcast.audioURL)
            }
        }
    }

    private func startPlayback() {
        let state = downloadManager.downloadState(for: podcast)
        if case .downloaded(let url) = state {
            audioPlayer.load(podcast: podcast, from: url)
        } else {
            audioPlayer.load(podcast: podcast, from: podcast.audioURL)
        }
        audioPlayer.play()
    }
}

struct DetailTranscriptNotesView: View {
    let transcript: [TranscriptCue]
    @Binding var notes: String
    let podcast: Podcast
    let currentTime: TimeInterval
    let isPlaying: Bool
    let onPausePlayback: () -> Void
    let onResumePlayback: () -> Void
    let onSeekTo: (TimeInterval) -> Void

    @State private var editingCueId: UUID? = nil
    @State private var newComment: String = ""
    @State private var wasPlayingBeforeEdit = false
    @State private var lastScrolledCueIndex: Int? = nil
    @FocusState private var isCommentFocused: Bool

    private var timestampedComments: [TimeInterval: String] {
        let comments = ListeningHistoryService.shared.parseTimestampedComments(from: notes)
        var dict: [TimeInterval: String] = [:]
        for comment in comments {
            dict[comment.timestamp] = comment.text
        }
        return dict
    }

    private var currentCueIndex: Int? {
        TranscriptService.shared.findCueIndex(for: currentTime, in: transcript)
    }

    var body: some View {
        ScrollViewReader { proxy in
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
                    ForEach(Array(transcript.enumerated()), id: \.element.id) { index, cue in
                        let existingComment = findComment(for: cue.startTime)
                        DetailCueWithNotesView(
                            cue: cue,
                            isActive: currentTime >= cue.startTime && currentTime <= cue.endTime,
                            existingComment: existingComment,
                            isEditing: editingCueId == cue.id,
                            newComment: editingCueId == cue.id ? $newComment : .constant(""),
                            isCommentFocused: _isCommentFocused,
                            onTapCue: {
                                onSeekTo(cue.startTime)
                            },
                            onTapAdd: {
                                wasPlayingBeforeEdit = isPlaying
                                if isPlaying {
                                    onPausePlayback()
                                }
                                editingCueId = cue.id
                                newComment = existingComment ?? ""
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
                                if wasPlayingBeforeEdit {
                                    onResumePlayback()
                                }
                            },
                            onCancel: {
                                editingCueId = nil
                                newComment = ""
                                if wasPlayingBeforeEdit {
                                    onResumePlayback()
                                }
                            }
                        )
                        .id(index)
                    }
                }
            }
            .padding(12)
            .background(Color(.systemGray6))
            .cornerRadius(8)
            .onChange(of: currentCueIndex) { _, newIndex in
                if let index = newIndex, editingCueId == nil, index != lastScrolledCueIndex {
                    lastScrolledCueIndex = index
                    withAnimation(.easeOut(duration: 0.15)) {
                        proxy.scrollTo(index, anchor: .center)
                    }
                }
            }
            .onChange(of: editingCueId) { _, newEditingId in
                if let editingId = newEditingId,
                   let index = transcript.firstIndex(where: { $0.id == editingId }) {
                    withAnimation(.easeOut(duration: 0.15)) {
                        proxy.scrollTo(index, anchor: .top)
                    }
                }
            }
        }
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
    let isActive: Bool
    let existingComment: String?
    let isEditing: Bool
    @Binding var newComment: String
    @FocusState var isCommentFocused: Bool
    let onTapCue: () -> Void
    let onTapAdd: () -> Void
    let onSubmit: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Transcript cue
            HStack(alignment: .top, spacing: 8) {
                Text(formatTime(cue.startTime))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .frame(width: 40, alignment: .leading)

                VStack(alignment: .leading, spacing: 2) {
                    if let speaker = cue.speaker {
                        Text(speaker)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(isActive ? .blue : .secondary)
                    }
                    Text(cue.text)
                        .font(.body)
                        .foregroundColor(isActive ? .primary : .secondary)
                        .fontWeight(isActive ? .medium : .regular)
                }
            }
            .padding(.vertical, 6)
            .padding(.leading, 4)
            .background(isActive ? Color.blue.opacity(0.1) : Color.clear)
            .cornerRadius(8)
            .contentShape(Rectangle())
            .onTapGesture {
                onTapCue()
            }

            // Note section (always visible)
            if isEditing {
                // Editable multi-line text field
                VStack(alignment: .leading, spacing: 8) {
                    TextField("Add note...", text: $newComment, axis: .vertical)
                        .font(.callout)
                        .lineLimit(3...6)
                        .textFieldStyle(.roundedBorder)
                        .focused($isCommentFocused)

                    HStack {
                        Spacer()
                        Button("Cancel") {
                            onCancel()
                        }
                        .foregroundColor(.secondary)

                        Button("Save") {
                            onSubmit()
                        }
                        .fontWeight(.semibold)
                        .disabled(newComment.trimmingCharacters(in: .whitespaces).isEmpty)
                    }
                    .font(.callout)
                }
                .padding(.leading, 48)
                .padding(.vertical, 4)
            } else if let comment = existingComment {
                // Show existing comment (tap to edit)
                Text(comment)
                    .font(.callout)
                    .foregroundColor(.primary)
                    .padding(.leading, 48)
                    .padding(.vertical, 4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(6)
                    .onTapGesture {
                        onTapAdd()
                    }
            } else {
                // Placeholder to add note
                Text("Add note")
                    .font(.callout)
                    .foregroundColor(.secondary)
                    .padding(.leading, 48)
                    .padding(.vertical, 4)
                    .onTapGesture {
                        onTapAdd()
                    }
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
