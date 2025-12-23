import SwiftUI

struct PlayerView: View {
    let podcast: Podcast

    @EnvironmentObject var audioPlayer: AudioPlayer
    @Environment(\.dismiss) private var dismiss

    @State private var notes: String = ""
    @State private var showNotesEditor = false

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
                    audioPlayer.pause()
                    showNotesEditor = true
                }
            }
            .fullScreenCover(isPresented: $showNotesEditor) {
                NotesEditorView(notes: $notes, podcast: podcast)
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
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var audioPlayer: AudioPlayer
    @FocusState private var isEditorFocused: Bool

    var body: some View {
        NavigationStack {
            ScrollView {
                TextField("", text: $notes, axis: .vertical)
                    .font(.system(.body, design: .monospaced))
                    .padding(12)
                    .padding(.bottom, 300)
                    .focused($isEditorFocused)
            }
            .scrollDismissesKeyboard(.interactively)
                .onChange(of: notes) { _, newValue in
                    ListeningHistoryService.shared.saveNotes(newValue, for: podcast)
                }
                .navigationTitle("Notes")
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
                }
                .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
                    if let podcastId = notification.object as? String, podcastId == podcast.id {
                        notes = ListeningHistoryService.shared.readNotes(for: podcast)
                    }
                }
        }
    }
}

#Preview {
    PlayerView(podcast: Podcast.samples[0])
        .environmentObject(AudioPlayer.shared)
}
