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
    @FocusState private var isEditorFocused: Bool
    @State private var selectedTab = 1  // Start on Notes tab
    @State private var transcript: [TranscriptCue] = []
    @State private var isLoadingTranscript = true

    var body: some View {
        NavigationStack {
            TabView(selection: $selectedTab) {
                TranscriptView(
                    transcript: transcript,
                    currentTime: currentTime,
                    isLoading: isLoadingTranscript
                )
                .tag(0)

                ScrollView {
                    TextField("", text: $notes, axis: .vertical)
                        .font(.system(.body, design: .monospaced))
                        .padding(12)
                        .padding(.bottom, 300)
                        .focused($isEditorFocused)
                }
                .scrollDismissesKeyboard(.interactively)
                .tag(1)
            }
            .tabViewStyle(.page(indexDisplayMode: .always))
            .indexViewStyle(.page(backgroundDisplayMode: .always))
            .onChange(of: notes) { _, newValue in
                ListeningHistoryService.shared.saveNotes(newValue, for: podcast)
            }
            .navigationTitle(selectedTab == 0 ? "Transcript" : "Notes")
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
                isEditorFocused = true
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

struct TranscriptView: View {
    let transcript: [TranscriptCue]
    let currentTime: TimeInterval
    let isLoading: Bool

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
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 100)
                } else {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(Array(transcript.enumerated()), id: \.element.id) { index, cue in
                            TranscriptCueView(
                                cue: cue,
                                isActive: currentTime >= cue.startTime && currentTime <= cue.endTime
                            )
                            .id(index)
                        }
                    }
                    .padding(12)
                    .padding(.bottom, 100)
                }
            }
            .onAppear {
                scrollToCurrentTime(proxy: proxy)
            }
        }
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

struct TranscriptCueView: View {
    let cue: TranscriptCue
    let isActive: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(formatTime(cue.startTime))
                .font(.caption)
                .foregroundColor(.secondary)
                .frame(width: 50, alignment: .leading)

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
        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .background(isActive ? Color.blue.opacity(0.1) : Color.clear)
        .cornerRadius(8)
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
