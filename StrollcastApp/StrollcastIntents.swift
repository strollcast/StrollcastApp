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

// MARK: - Play Previous Intent

struct PlayPreviousIntent: AppIntent {
    static var title: LocalizedStringResource = "Play Previous in Strollcast"
    static var description = IntentDescription("Plays the previous podcast from playback history")

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard PlaybackHistoryService.shared.canGoBack() else {
            return .result(dialog: "No previous podcast in history.")
        }

        let success = AudioPlayer.shared.loadPreviousFromHistory()

        if success {
            return .result(dialog: "Playing previous podcast.")
        } else {
            return .result(dialog: "Could not load previous podcast.")
        }
    }
}

// MARK: - Go to Reference Intent

struct GoToReferenceIntent: AppIntent {
    static var title: LocalizedStringResource = "Go to Reference in Strollcast"
    static var description = IntentDescription("Plays the referenced episode if there's a link in the current or previous transcript segment")

    static var openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard let podcast = AudioPlayer.shared.currentPodcast else {
            return .result(dialog: "No podcast is currently playing.")
        }

        // Get transcript
        guard let cues = await TranscriptService.shared.getTranscript(for: podcast) else {
            return .result(dialog: "No transcript available for this episode.")
        }

        // Find current cue
        let currentTime = AudioPlayer.shared.currentTime
        guard let currentIndex = TranscriptService.shared.findCueIndex(for: currentTime, in: cues) else {
            return .result(dialog: "Could not find current position in transcript.")
        }

        // Check current and previous cues for mp3 links
        let cuesToCheck = [currentIndex, max(0, currentIndex - 1)]

        for index in cuesToCheck {
            guard index < cues.count else { continue }
            let cue = cues[index]

            // Look for mp3 links in markdown format: [text](url.mp3)
            if let mp3URL = extractMP3Link(from: cue.text) {
                // Found an mp3 link, try to play it
                // Extract episode ID from URL
                // Expected format: https://released.strollcast.com/episodes/{episode_id}/{episode_id}.mp3
                let pathComponents = mp3URL.pathComponents
                guard pathComponents.count >= 3,
                      pathComponents[pathComponents.count - 3] == "episodes" else {
                    return .result(dialog: "Invalid reference URL format.")
                }

                let episodeId = pathComponents[pathComponents.count - 2]

                // Fetch the episode from the API
                if let episode = await fetchEpisode(id: episodeId) {
                    // Load and play the referenced episode
                    // Check if downloaded locally first, like the main app does
                    let downloadState = DownloadManager.shared.downloadState(for: episode)
                    if case .downloaded(let url) = downloadState {
                        AudioPlayer.shared.load(podcast: episode, from: url)
                    } else {
                        AudioPlayer.shared.load(podcast: episode, from: episode.audioURL)
                    }
                    AudioPlayer.shared.play()
                    return .result(dialog: "Now playing \(episode.title).")
                } else {
                    return .result(dialog: "Referenced episode not found.")
                }
            }
        }

        return .result(dialog: "No reference found in the current or previous segment.")
    }

    private func extractMP3Link(from text: String) -> URL? {
        // Regex pattern to match [text](url.mp3) or [text](url.m4a)
        let pattern = "\\[([^\\]]+)\\]\\((https?://[^\\)]+\\.(mp3|m4a))\\)"

        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else {
            return nil
        }

        let nsString = text as NSString
        let matches = regex.matches(in: text, options: [], range: NSRange(location: 0, length: nsString.length))

        if let match = matches.first, match.numberOfRanges >= 3 {
            let urlRange = match.range(at: 2)
            let urlString = nsString.substring(with: urlRange)
            return URL(string: urlString)
        }

        return nil
    }

    private func fetchEpisode(id: String) async -> Podcast? {
        guard let url = URL(string: "https://api.strollcast.com/episodes") else {
            return nil
        }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(EpisodesResponse.self, from: data)
            return response.episodes.first(where: { $0.id == id })
        } catch {
            print("Error fetching episode \(id): \(error)")
            return nil
        }
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
            intent: PlayPreviousIntent(),
            phrases: [
                "Play previous in \(.applicationName)",
                "\(.applicationName) play previous",
                "Previous podcast in \(.applicationName)",
                "\(.applicationName) previous"
            ],
            shortTitle: "Play Previous",
            systemImageName: "arrow.uturn.backward.circle"
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

        AppShortcut(
            intent: GoToReferenceIntent(),
            phrases: [
                "Go to reference in \(.applicationName)",
                "\(.applicationName) go to reference",
                "Play reference in \(.applicationName)",
                "\(.applicationName) play reference"
            ],
            shortTitle: "Go to Reference",
            systemImageName: "link.circle"
        )
    }
}
