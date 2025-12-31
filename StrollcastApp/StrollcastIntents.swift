import AppIntents
import SwiftUI

// MARK: - Pause Strollcast Intent

struct PauseStrollcastIntent: AppIntent {
    static var title: LocalizedStringResource = "Pause Strollcast"
    static var description = IntentDescription("Pauses Strollcast and enters voice command mode")

    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        // Pause the audio player
        AudioPlayer.shared.pause()

        // Start listening for voice commands
        await VoiceCommandService.shared.startListeningForCommands()

        return .result(dialog: "Strollcast paused. Say 'Record note' or 'Go back'.")
    }
}

// MARK: - Go Back Intent (Direct Siri command)

struct GoBackStrollcastIntent: AppIntent {
    static var title: LocalizedStringResource = "Go Back in Strollcast"
    static var description = IntentDescription("Goes back 30 seconds in the current podcast")

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard AudioPlayer.shared.currentPodcast != nil else {
            return .result(dialog: "No podcast is currently playing.")
        }

        AudioPlayer.shared.skipBackward(seconds: 30)

        return .result(dialog: "Went back 30 seconds.")
    }
}

// MARK: - Record Note Intent (Direct Siri command)

struct RecordNoteStrollcastIntent: AppIntent {
    static var title: LocalizedStringResource = "Record Strollcast Note"
    static var description = IntentDescription("Records a 15 second voice note for the current podcast")

    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard AudioPlayer.shared.currentPodcast != nil else {
            return .result(dialog: "No podcast is currently playing.")
        }

        // Pause and start recording
        AudioPlayer.shared.pause()

        // The VoiceCommandService will handle the recording flow
        await VoiceCommandService.shared.startListeningForCommands()

        return .result(dialog: "Recording note. Speak now.")
    }
}

// MARK: - Resume Strollcast Intent

struct ResumeStrollcastIntent: AppIntent {
    static var title: LocalizedStringResource = "Resume Strollcast"
    static var description = IntentDescription("Resumes playback of the current podcast")

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard AudioPlayer.shared.currentPodcast != nil else {
            return .result(dialog: "No podcast is currently loaded.")
        }

        AudioPlayer.shared.play()

        return .result(dialog: "Resuming playback.")
    }
}

// MARK: - App Shortcuts Provider

struct StrollcastShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: PauseStrollcastIntent(),
            phrases: [
                "Pause \(.applicationName)",
                "Pause \(.applicationName) podcast",
                "Stop \(.applicationName)",
                "\(.applicationName) pause"
            ],
            shortTitle: "Pause Strollcast",
            systemImageName: "pause.circle"
        )

        AppShortcut(
            intent: GoBackStrollcastIntent(),
            phrases: [
                "Go back in \(.applicationName)",
                "Rewind \(.applicationName)",
                "\(.applicationName) go back"
            ],
            shortTitle: "Go Back",
            systemImageName: "gobackward.30"
        )

        AppShortcut(
            intent: RecordNoteStrollcastIntent(),
            phrases: [
                "Record note in \(.applicationName)",
                "\(.applicationName) record note",
                "Take a note in \(.applicationName)"
            ],
            shortTitle: "Record Note",
            systemImageName: "mic.circle"
        )

        AppShortcut(
            intent: ResumeStrollcastIntent(),
            phrases: [
                "Resume \(.applicationName)",
                "Play \(.applicationName)",
                "Continue \(.applicationName)"
            ],
            shortTitle: "Resume",
            systemImageName: "play.circle"
        )
    }
}
