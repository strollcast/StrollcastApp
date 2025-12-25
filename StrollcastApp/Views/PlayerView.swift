import SwiftUI

struct PlayerView: View {
    let podcast: Podcast

    @EnvironmentObject var audioPlayer: AudioPlayer
    @Environment(\.dismiss) private var dismiss

    @State private var notes: String = ""
    @State private var showNotesEditor = false
    @State private var pausedAtTime: TimeInterval = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Compact player controls
                VStack(spacing: 12) {
                    // Title
                    Text(podcast.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                        .padding(.horizontal)

                    if audioPlayer.isLoading {
                        ProgressView()
                            .frame(height: 44)
                    } else if audioPlayer.errorMessage != nil {
                        Image(systemName: "exclamationmark.circle")
                            .foregroundColor(.red)
                            .frame(height: 44)
                    } else {
                        // Progress bar
                        VStack(spacing: 2) {
                            Slider(
                                value: Binding(
                                    get: { audioPlayer.currentTime },
                                    set: { audioPlayer.seek(to: $0) }
                                ),
                                in: 0...max(audioPlayer.duration, 1)
                            )
                            .disabled(audioPlayer.duration == 0)

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

                        // Playback controls
                        HStack(spacing: 32) {
                            Button {
                                audioPlayer.skipBackward()
                            } label: {
                                Image(systemName: "gobackward.15")
                                    .font(.title2)
                            }

                            Button {
                                audioPlayer.togglePlayPause()
                            } label: {
                                Image(systemName: audioPlayer.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                    .font(.system(size: 50))
                            }

                            Button {
                                audioPlayer.skipForward()
                            } label: {
                                Image(systemName: "goforward.15")
                                    .font(.title2)
                            }
                        }
                    }
                }
                .padding(.vertical, 12)
                .background(Color(.systemBackground))

                Divider()

                // Notes preview (tap to edit)
                ScrollView {
                    Text(notes.isEmpty ? "Tap to add notes..." : notes)
                        .font(.system(.body, design: .monospaced))
                        .foregroundColor(notes.isEmpty ? .secondary : .primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                }
                .background(Color(.systemBackground))
                .onTapGesture {
                    pausedAtTime = audioPlayer.currentTime
                    audioPlayer.pause()
                    showNotesEditor = true
                }
            }
            .fullScreenCover(isPresented: $showNotesEditor) {
                NotesEditorView(notes: $notes, podcast: podcast, currentTime: pausedAtTime)
            }
            .navigationTitle("Now Playing")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") {
                        audioPlayer.pause()
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        audioPlayer.stop()
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
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
    }
}

struct NotesEditorView: View {
    @Binding var notes: String
    let podcast: Podcast
    let currentTime: TimeInterval
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var audioPlayer: AudioPlayer
    @State private var transcript: [TranscriptCue] = []
    @State private var isLoadingTranscript = true

    var body: some View {
        NavigationStack {
            CombinedTranscriptNotesView(
                transcript: transcript,
                notes: $notes,
                podcast: podcast,
                currentTime: currentTime,
                isLoading: isLoadingTranscript
            )
            .navigationTitle("Transcript & Notes")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Continue") {
                        audioPlayer.play()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .onAppear {
                loadTranscript()
            }
            .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
                if let podcastId = notification.object as? String, podcastId == podcast.id {
                    notes = ListeningHistoryService.shared.readNotes(for: podcast)
                }
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
}

struct CombinedTranscriptNotesView: View {
    let transcript: [TranscriptCue]
    @Binding var notes: String
    let podcast: Podcast
    let currentTime: TimeInterval
    let isLoading: Bool

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
        ScrollViewReader { proxy in
            ScrollView {
                if isLoading {
                    ProgressView("Loading transcript...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .padding(.top, 100)
                } else if transcript.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "text.quote")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Transcript Available")
                            .font(.headline)
                        Text("Add general notes below")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 100)
                } else {
                    LazyVStack(alignment: .leading, spacing: 4) {
                        ForEach(Array(transcript.enumerated()), id: \.element.id) { index, cue in
                            TranscriptCueWithNotesView(
                                cue: cue,
                                isActive: currentTime >= cue.startTime && currentTime <= cue.endTime,
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
                            .id(index)
                        }
                    }
                    .padding(12)
                    .padding(.bottom, 100)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .onAppear {
                scrollToCurrentTime(proxy: proxy)
            }
        }
    }

    private func findComment(for timestamp: TimeInterval) -> String? {
        // Find comment within 2 second tolerance
        for (time, comment) in timestampedComments {
            if abs(time - timestamp) < 2.0 {
                return comment
            }
        }
        return nil
    }

    private func scrollToCurrentTime(proxy: ScrollViewProxy) {
        if let index = TranscriptService.shared.findCueIndex(for: currentTime, in: transcript) {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                withAnimation {
                    proxy.scrollTo(index, anchor: .center)
                }
            }
        }
    }
}

struct TranscriptCueWithNotesView: View {
    let cue: TranscriptCue
    let isActive: Bool
    let existingComment: String?
    let isEditing: Bool
    @Binding var newComment: String
    @FocusState var isCommentFocused: Bool
    let onTapAdd: () -> Void
    let onSubmit: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Transcript cue row
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
                            .foregroundColor(isActive ? .blue : .secondary)
                    }
                    Text(cue.text)
                        .font(.body)
                        .foregroundColor(isActive ? .primary : .secondary)
                        .fontWeight(isActive ? .medium : .regular)
                }

                Spacer()

                // Add note button (only show if no existing comment and not editing)
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
            .padding(.vertical, 6)
            .padding(.horizontal, 8)
            .background(isActive ? Color.blue.opacity(0.1) : Color.clear)
            .cornerRadius(8)

            // Existing comment display
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
                .background(Color.orange.opacity(0.1))
                .cornerRadius(6)
            }

            // Inline comment editor
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
    PlayerView(podcast: Podcast.samples[0])
        .environmentObject(AudioPlayer.shared)
}
