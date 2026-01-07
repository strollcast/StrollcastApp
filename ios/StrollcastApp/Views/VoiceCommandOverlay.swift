import SwiftUI

struct VoiceCommandOverlay: View {
    @ObservedObject var voiceService = VoiceCommandService.shared

    var body: some View {
        Group {
            if voiceService.isListening || voiceService.isRecordingNote || !voiceService.statusMessage.isEmpty {
                overlayContent
            }
        }
    }

    private var overlayContent: some View {
        VStack(spacing: 20) {
            Spacer()

            VStack(spacing: 16) {
                // Animated microphone icon
                if voiceService.isListening || voiceService.isRecordingNote {
                    MicrophoneAnimationView(isRecording: voiceService.isRecordingNote)
                        .frame(width: 80, height: 80)
                }

                // Status message
                Text(voiceService.statusMessage)
                    .font(.headline)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)

                // Recognized text (while listening for commands)
                if voiceService.isListening && !voiceService.recognizedText.isEmpty {
                    Text(voiceService.recognizedText)
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.8))
                        .padding(.horizontal)
                        .multilineTextAlignment(.center)
                }

                // Recording countdown
                if voiceService.isRecordingNote {
                    Text("\(voiceService.noteRecordingTimeRemaining)s")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundColor(.white)

                    Button("Cancel") {
                        voiceService.cancelNoteRecording()
                    }
                    .buttonStyle(.bordered)
                    .tint(.white)
                }

                // Command hints
                if voiceService.isListening && !voiceService.isRecordingNote {
                    VStack(spacing: 8) {
                        Text("Say a command:")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))

                        HStack(spacing: 12) {
                            CommandHintPill(text: "Record note", icon: "mic")
                            CommandHintPill(text: "Go back", icon: "gobackward.30")
                        }

                        HStack(spacing: 12) {
                            CommandHintPill(text: "Resume", icon: "play")
                        }
                    }
                    .padding(.top, 8)
                }
            }
            .padding(32)
            .background(
                RoundedRectangle(cornerRadius: 24)
                    .fill(.ultraThinMaterial)
                    .overlay(
                        RoundedRectangle(cornerRadius: 24)
                            .stroke(Color.white.opacity(0.2), lineWidth: 1)
                    )
            )
            .padding(.horizontal, 24)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.6))
        .ignoresSafeArea()
    }
}

struct MicrophoneAnimationView: View {
    let isRecording: Bool
    @State private var scale: CGFloat = 1.0
    @State private var opacity: Double = 0.5

    var body: some View {
        ZStack {
            // Pulsing circles
            ForEach(0..<3) { index in
                Circle()
                    .stroke(isRecording ? Color.red : Color.blue, lineWidth: 2)
                    .scaleEffect(scale + CGFloat(index) * 0.2)
                    .opacity(opacity - Double(index) * 0.15)
            }

            // Microphone icon
            Image(systemName: isRecording ? "mic.fill" : "waveform")
                .font(.system(size: 32))
                .foregroundColor(isRecording ? .red : .blue)
        }
        .onAppear {
            withAnimation(
                Animation.easeInOut(duration: 1.0)
                    .repeatForever(autoreverses: true)
            ) {
                scale = 1.3
                opacity = 0.2
            }
        }
    }
}

struct CommandHintPill: View {
    let text: String
    let icon: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
            Text(text)
                .font(.caption)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.2))
        .cornerRadius(16)
        .foregroundColor(.white)
    }
}

#Preview {
    VoiceCommandOverlay()
}
