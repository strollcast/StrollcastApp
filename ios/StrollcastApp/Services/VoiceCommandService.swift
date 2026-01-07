import Foundation
import Speech
import AVFoundation

@MainActor
class VoiceCommandService: ObservableObject {
    static let shared = VoiceCommandService()

    @Published var isListening = false
    @Published var recognizedText = ""
    @Published var statusMessage = ""
    @Published var isRecordingNote = false
    @Published var noteRecordingTimeRemaining: Int = 0

    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var audioEngine: AVAudioEngine?

    private var noteAudioRecorder: AVAudioRecorder?
    private var noteRecordingTimer: Timer?
    private var currentNoteURL: URL?

    private let commandTimeout: TimeInterval = 10.0
    private var commandTimer: Timer?

    enum VoiceCommand {
        case recordNote
        case goBack
        case resume
        case unknown
    }

    private init() {
        speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "en-US"))
        audioEngine = AVAudioEngine()
    }

    // MARK: - Authorization

    func requestAuthorization() async -> Bool {
        // Request speech recognition authorization
        let speechStatus = await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                continuation.resume(returning: status)
            }
        }

        guard speechStatus == .authorized else {
            statusMessage = "Speech recognition not authorized"
            return false
        }

        // Request microphone authorization
        let micStatus = await AVAudioApplication.requestRecordPermission()
        guard micStatus else {
            statusMessage = "Microphone access not authorized"
            return false
        }

        return true
    }

    // MARK: - Voice Command Listening

    func startListeningForCommands() async {
        guard await requestAuthorization() else { return }

        // Pause the podcast
        AudioPlayer.shared.pause()

        statusMessage = "Listening for command..."
        isListening = true
        recognizedText = ""

        do {
            try await startRecognition()
        } catch {
            statusMessage = "Failed to start listening: \(error.localizedDescription)"
            isListening = false
        }
    }

    private func startRecognition() async throws {
        // Cancel any previous task
        recognitionTask?.cancel()
        recognitionTask = nil

        // Configure audio session for recording
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.playAndRecord, mode: .measurement, options: [.defaultToSpeaker, .allowBluetoothHFP])
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        guard let audioEngine = audioEngine else { return }

        let inputNode = audioEngine.inputNode

        // Remove any existing tap
        inputNode.removeTap(onBus: 0)

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            throw NSError(domain: "VoiceCommand", code: 1, userInfo: [NSLocalizedDescriptionKey: "Unable to create recognition request"])
        }

        recognitionRequest.shouldReportPartialResults = true
        recognitionRequest.taskHint = .dictation

        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            Task { @MainActor in
                guard let self = self else { return }

                if let result = result {
                    self.recognizedText = result.bestTranscription.formattedString

                    // Check for commands
                    let command = self.parseCommand(from: self.recognizedText)
                    if command != .unknown {
                        self.stopListening()
                        await self.executeCommand(command)
                    }
                }

                if error != nil || (result?.isFinal ?? false) {
                    self.stopListening()
                }
            }
        }

        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { [weak self] buffer, _ in
            self?.recognitionRequest?.append(buffer)
        }

        audioEngine.prepare()
        try audioEngine.start()

        // Start command timeout
        startCommandTimeout()
    }

    private func startCommandTimeout() {
        commandTimer?.invalidate()
        commandTimer = Timer.scheduledTimer(withTimeInterval: commandTimeout, repeats: false) { [weak self] _ in
            Task { @MainActor in
                self?.stopListening()
                self?.statusMessage = "No command recognized. Resuming playback."
                // Resume playback after timeout
                try? await Task.sleep(nanoseconds: 1_500_000_000) // 1.5 seconds
                AudioPlayer.shared.play()
            }
        }
    }

    func stopListening() {
        commandTimer?.invalidate()
        commandTimer = nil

        audioEngine?.stop()
        audioEngine?.inputNode.removeTap(onBus: 0)

        recognitionRequest?.endAudio()
        recognitionRequest = nil

        recognitionTask?.cancel()
        recognitionTask = nil

        isListening = false

        // Restore audio session for playback
        Task {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try? AVAudioSession.sharedInstance().setActive(true)
        }
    }

    // MARK: - Command Parsing

    private func parseCommand(from text: String) -> VoiceCommand {
        let lowercased = text.lowercased()

        if lowercased.contains("record") && lowercased.contains("note") {
            return .recordNote
        }

        if lowercased.contains("go back") || lowercased.contains("go backwards") || lowercased.contains("rewind") {
            return .goBack
        }

        if lowercased.contains("resume") || lowercased.contains("continue") || lowercased.contains("play") {
            return .resume
        }

        return .unknown
    }

    // MARK: - Command Execution

    private func executeCommand(_ command: VoiceCommand) async {
        switch command {
        case .recordNote:
            statusMessage = "Recording note for 15 seconds..."
            await startNoteRecording()

        case .goBack:
            statusMessage = "Going back 30 seconds"
            AudioPlayer.shared.skipBackward(seconds: 30)
            try? await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
            AudioPlayer.shared.play()
            statusMessage = ""

        case .resume:
            statusMessage = "Resuming playback"
            AudioPlayer.shared.play()
            statusMessage = ""

        case .unknown:
            statusMessage = "Command not recognized"
            try? await Task.sleep(nanoseconds: 1_500_000_000)
            AudioPlayer.shared.play()
            statusMessage = ""
        }
    }

    // MARK: - Note Recording

    private func startNoteRecording() async {
        guard let podcast = AudioPlayer.shared.currentPodcast else {
            statusMessage = "No podcast playing"
            return
        }

        let timestamp = AudioPlayer.shared.currentTime

        // Create temp file for recording
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "voice_note_\(Date().timeIntervalSince1970).m4a"
        currentNoteURL = tempDir.appendingPathComponent(fileName)

        guard let noteURL = currentNoteURL else { return }

        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44100,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
        ]

        do {
            try AVAudioSession.sharedInstance().setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try AVAudioSession.sharedInstance().setActive(true)

            noteAudioRecorder = try AVAudioRecorder(url: noteURL, settings: settings)
            noteAudioRecorder?.record()

            isRecordingNote = true
            noteRecordingTimeRemaining = 15

            // Start countdown timer
            startRecordingCountdown(podcast: podcast, timestamp: timestamp)

        } catch {
            statusMessage = "Failed to start recording: \(error.localizedDescription)"
            AudioPlayer.shared.play()
        }
    }

    private func startRecordingCountdown(podcast: Podcast, timestamp: TimeInterval) {
        noteRecordingTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] timer in
            Task { @MainActor in
                guard let self = self else {
                    timer.invalidate()
                    return
                }

                self.noteRecordingTimeRemaining -= 1
                self.statusMessage = "Recording... \(self.noteRecordingTimeRemaining)s"

                if self.noteRecordingTimeRemaining <= 0 {
                    timer.invalidate()
                    await self.finishNoteRecording(podcast: podcast, timestamp: timestamp)
                }
            }
        }
    }

    private func finishNoteRecording(podcast: Podcast, timestamp: TimeInterval) async {
        noteRecordingTimer?.invalidate()
        noteRecordingTimer = nil

        noteAudioRecorder?.stop()
        noteAudioRecorder = nil

        isRecordingNote = false

        guard let noteURL = currentNoteURL else { return }

        // Transcribe the recorded note
        statusMessage = "Transcribing note..."

        do {
            let transcription = try await transcribeAudio(url: noteURL)

            if !transcription.isEmpty {
                // Save the transcribed note
                ListeningHistoryService.shared.addTimestampedComment(
                    transcription,
                    at: timestamp,
                    for: podcast
                )
                statusMessage = "Note saved!"
            } else {
                statusMessage = "No speech detected"
            }

            // Clean up temp file
            try? FileManager.default.removeItem(at: noteURL)
            currentNoteURL = nil

            // Resume playback after a short delay
            try? await Task.sleep(nanoseconds: 1_500_000_000)

            // Restore audio session and resume
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try? AVAudioSession.sharedInstance().setActive(true)

            AudioPlayer.shared.play()
            statusMessage = ""

        } catch {
            statusMessage = "Transcription failed: \(error.localizedDescription)"
            try? FileManager.default.removeItem(at: noteURL)
            currentNoteURL = nil

            try? await Task.sleep(nanoseconds: 1_500_000_000)
            AudioPlayer.shared.play()
            statusMessage = ""
        }
    }

    private func transcribeAudio(url: URL) async throws -> String {
        guard let recognizer = speechRecognizer, recognizer.isAvailable else {
            throw NSError(domain: "VoiceCommand", code: 2, userInfo: [NSLocalizedDescriptionKey: "Speech recognizer not available"])
        }

        let request = SFSpeechURLRecognitionRequest(url: url)
        request.shouldReportPartialResults = false

        return try await withCheckedThrowingContinuation { continuation in
            recognizer.recognitionTask(with: request) { result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                    return
                }

                if let result = result, result.isFinal {
                    continuation.resume(returning: result.bestTranscription.formattedString)
                }
            }
        }
    }

    // MARK: - Cancel Recording

    func cancelNoteRecording() {
        noteRecordingTimer?.invalidate()
        noteRecordingTimer = nil

        noteAudioRecorder?.stop()
        noteAudioRecorder = nil

        if let noteURL = currentNoteURL {
            try? FileManager.default.removeItem(at: noteURL)
            currentNoteURL = nil
        }

        isRecordingNote = false
        noteRecordingTimeRemaining = 0
        statusMessage = ""

        // Restore audio session and resume
        Task {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try? AVAudioSession.sharedInstance().setActive(true)
            AudioPlayer.shared.play()
        }
    }
}
