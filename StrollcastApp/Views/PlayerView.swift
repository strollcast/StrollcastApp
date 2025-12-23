import SwiftUI

struct PlayerView: View {
    let podcast: Podcast

    @EnvironmentObject var audioPlayer: AudioPlayer
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 32) {
                Spacer()

                VStack(spacing: 16) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 20)
                            .fill(
                                LinearGradient(
                                    colors: [.blue.opacity(0.6), .purple.opacity(0.6)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 200, height: 200)
                            .shadow(radius: 10)

                        Image(systemName: "waveform")
                            .font(.system(size: 60))
                            .foregroundColor(.white)
                    }
                }

                VStack(spacing: 8) {
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

                Spacer()
            }
            .padding()
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
        }
    }
}

#Preview {
    PlayerView(podcast: Podcast.samples[0])
        .environmentObject(AudioPlayer.shared)
}
