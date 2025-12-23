import SwiftUI

struct PlayerView: View {
    let podcast: Podcast

    @EnvironmentObject var audioPlayer: AudioPlayer
    @Environment(\.dismiss) private var dismiss

    @State private var notes: String = ""
    @State private var showNotes = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Album art
                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(
                                LinearGradient(
                                    colors: [.blue.opacity(0.6), .purple.opacity(0.6)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 180, height: 180)
                            .shadow(radius: 10)

                        Image(systemName: "waveform")
                            .font(.system(size: 50))
                            .foregroundColor(.white)
                    }
                    .padding(.top, 20)

                    // Title and metadata
                    VStack(spacing: 6) {
                        Text(podcast.title)
                            .font(.title3)
                            .fontWeight(.bold)
                            .multilineTextAlignment(.center)
                            .lineLimit(3)

                        Text(podcast.authors)
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Text("\(podcast.year) - \(podcast.duration)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal)

                    // Player controls
                    if audioPlayer.isLoading {
                        ProgressView("Loading...")
                    } else if let error = audioPlayer.errorMessage {
                        VStack(spacing: 8) {
                            Text("Error")
                                .foregroundColor(.red)
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                    } else {
                        VStack(spacing: 16) {
                            VStack(spacing: 4) {
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
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .monospacedDigit()
                                    Spacer()
                                    Text(audioPlayer.formattedTime(audioPlayer.duration))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                        .monospacedDigit()
                                }
                            }
                            .padding(.horizontal)

                            HStack(spacing: 40) {
                                Button {
                                    audioPlayer.skipBackward()
                                } label: {
                                    Image(systemName: "gobackward.15")
                                        .font(.title)
                                }

                                Button {
                                    audioPlayer.togglePlayPause()
                                } label: {
                                    Image(systemName: audioPlayer.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                                        .font(.system(size: 64))
                                }

                                Button {
                                    audioPlayer.skipForward()
                                } label: {
                                    Image(systemName: "goforward.15")
                                        .font(.title)
                                }
                            }
                        }
                    }

                    // Notes section
                    VStack(alignment: .leading, spacing: 8) {
                        Button {
                            withAnimation {
                                showNotes.toggle()
                            }
                        } label: {
                            HStack {
                                Text("Notes")
                                    .font(.headline)
                                    .foregroundColor(.primary)
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
                    .padding(.horizontal)
                    .padding(.top, 8)
                }
                .padding(.bottom, 20)
            }
            .navigationTitle("Now Playing")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") {
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
        }
    }
}

#Preview {
    PlayerView(podcast: Podcast.samples[0])
        .environmentObject(AudioPlayer.shared)
}
