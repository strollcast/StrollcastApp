import Foundation
import AVFoundation
import MediaPlayer
import SwiftUI
import AudioToolbox

// MARK: - Playback History

struct PlaybackHistoryEntry: Codable, Equatable {
    let podcast: Podcast
    let position: TimeInterval
    let timestamp: Date
}

class PlaybackHistoryService {
    static let shared = PlaybackHistoryService()

    private let maxHistorySize = 4
    private let historyKey = "playbackHistory"
    private var history: [PlaybackHistoryEntry] = []
    private var currentIndex: Int = -1

    private init() {
        loadHistory()
    }

    // MARK: - History Management

    func addToHistory(podcast: Podcast, position: TimeInterval) {
        let entry = PlaybackHistoryEntry(
            podcast: podcast,
            position: position,
            timestamp: Date()
        )

        // If we're navigating backward and then play a new podcast,
        // remove all entries after current index
        if currentIndex >= 0 && currentIndex < history.count - 1 {
            history.removeSubrange((currentIndex + 1)...)
        }

        // Add new entry
        history.append(entry)
        currentIndex = history.count - 1

        // Keep only last maxHistorySize entries
        if history.count > maxHistorySize {
            history.removeFirst(history.count - maxHistorySize)
            currentIndex = history.count - 1
        }

        saveHistory()
    }

    func canGoBack() -> Bool {
        return currentIndex > 0
    }

    func goBack() -> (podcast: Podcast, position: TimeInterval)? {
        guard canGoBack() else { return nil }

        currentIndex -= 1
        let entry = history[currentIndex]

        return (entry.podcast, entry.position)
    }

    func getCurrentEntry() -> PlaybackHistoryEntry? {
        guard currentIndex >= 0 && currentIndex < history.count else {
            return nil
        }
        return history[currentIndex]
    }

    func getHistory() -> [PlaybackHistoryEntry] {
        return history
    }

    func clearHistory() {
        history.removeAll()
        currentIndex = -1
        saveHistory()
    }

    // MARK: - Persistence

    private func loadHistory() {
        guard let data = UserDefaults.standard.data(forKey: historyKey),
              let decoded = try? JSONDecoder().decode([PlaybackHistoryEntry].self, from: data) else {
            return
        }

        history = decoded
        currentIndex = history.count - 1
    }

    private func saveHistory() {
        guard let encoded = try? JSONEncoder().encode(history) else { return }
        UserDefaults.standard.set(encoded, forKey: historyKey)
    }
}

// MARK: - Audio Player

@MainActor
class AudioPlayer: ObservableObject {
    static let shared = AudioPlayer()

    @Published var currentPodcast: Podcast?
    @Published var isPlaying = false
    @Published var currentTime: TimeInterval = 0
    @Published var duration: TimeInterval = 0
    @Published var isLoading = false
    @Published var errorMessage: String?

    private var player: AVPlayer?
    private var timeObserver: Any?
    private var cuedSegments: Set<String> = [] // Track segments that have been cued
    private var lastCheckedCueIndex: Int?

    private init() {
        setupAudioSession()
        setupRemoteCommands()
    }

    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Failed to setup audio session: \(error)")
        }
    }

    private func setupRemoteCommands() {
        let commandCenter = MPRemoteCommandCenter.shared()

        commandCenter.playCommand.addTarget { [weak self] _ in
            Task { @MainActor in
                self?.play()
            }
            return .success
        }

        commandCenter.pauseCommand.addTarget { [weak self] _ in
            Task { @MainActor in
                self?.pause()
            }
            return .success
        }

        commandCenter.skipForwardCommand.preferredIntervals = [15]
        commandCenter.skipForwardCommand.addTarget { [weak self] _ in
            Task { @MainActor in
                self?.skipForward(seconds: 15)
            }
            return .success
        }

        commandCenter.skipBackwardCommand.preferredIntervals = [15]
        commandCenter.skipBackwardCommand.addTarget { [weak self] _ in
            Task { @MainActor in
                self?.skipBackward(seconds: 15)
            }
            return .success
        }

        commandCenter.changePlaybackPositionCommand.addTarget { [weak self] event in
            if let event = event as? MPChangePlaybackPositionCommandEvent {
                Task { @MainActor in
                    self?.seek(to: event.positionTime)
                }
            }
            return .success
        }
    }

    func load(podcast: Podcast, from url: URL, addToHistory: Bool = true) {
        stop()

        // Add current podcast to history before switching
        if addToHistory, let currentPodcast = currentPodcast {
            PlaybackHistoryService.shared.addToHistory(podcast: currentPodcast, position: currentTime)
        }

        currentPodcast = podcast
        isLoading = true
        errorMessage = nil

        // Clear cued segments for new podcast
        cuedSegments.removeAll()
        lastCheckedCueIndex = nil

        ListeningHistoryService.shared.saveLastActivePodcast(podcast)

        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)

        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: playerItem,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                self?.isPlaying = false
                self?.currentTime = 0
                self?.player?.seek(to: .zero)
                if let podcast = self?.currentPodcast {
                    ListeningHistoryService.shared.clearLastPosition(for: podcast)
                }
            }
        }

        playerItem.publisher(for: \.status)
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                guard let self = self else { return }

                switch status {
                case .readyToPlay:
                    self.isLoading = false
                    let assetDuration = playerItem.duration
                    if assetDuration.isNumeric {
                        self.duration = assetDuration.seconds
                    }
                    self.setupTimeObserver()
                    self.restoreLastPosition()
                    self.updateNowPlayingInfo()
                case .failed:
                    self.isLoading = false
                    self.errorMessage = playerItem.error?.localizedDescription ?? "Failed to load audio"
                default:
                    break
                }
            }
            .store(in: &cancellables)
    }

    private var cancellables = Set<AnyCancellable>()

    private func setupTimeObserver() {
        guard let player = player else { return }

        let interval = CMTime(seconds: 0.5, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            Task { @MainActor in
                self?.currentTime = time.seconds
                self?.updateNowPlayingInfo()
                await self?.checkForLinkCue()
            }
        }
    }

    private func restoreLastPosition() {
        guard let podcast = currentPodcast else { return }
        let savedPosition = ListeningHistoryService.shared.getLastPosition(for: podcast)
        if savedPosition > 0 && savedPosition < duration {
            seek(to: savedPosition)
        }
    }

    func play() {
        player?.play()
        isPlaying = true
        updateNowPlayingInfo()
        logListeningHistory()
    }

    private func logListeningHistory() {
        guard let podcast = currentPodcast else { return }
        let position = currentTime
        Task.detached {
            ListeningHistoryService.shared.logPlayback(podcast: podcast, position: position)
        }
    }

    func pause() {
        player?.pause()
        isPlaying = false
        updateNowPlayingInfo()
        logPauseHistory()
    }

    private func logPauseHistory() {
        guard let podcast = currentPodcast else { return }
        let position = currentTime
        Task.detached {
            ListeningHistoryService.shared.logPause(podcast: podcast, position: position)
        }
    }

    func togglePlayPause() {
        if isPlaying {
            pause()
        } else {
            play()
        }
    }

    func seek(to time: TimeInterval) {
        let cmTime = CMTime(seconds: time, preferredTimescale: CMTimeScale(NSEC_PER_SEC))
        player?.seek(to: cmTime)
        currentTime = time
    }

    func skipForward(seconds: Double = 15) {
        let newTime = min(currentTime + seconds, duration)
        seek(to: newTime)
    }

    func skipBackward(seconds: Double = 15) {
        let newTime = max(currentTime - seconds, 0)
        seek(to: newTime)
    }

    func loadPreviousFromHistory() -> Bool {
        guard let (podcast, position) = PlaybackHistoryService.shared.goBack() else {
            return false
        }

        // Load the podcast without adding to history (we're navigating history)
        load(podcast: podcast, from: podcast.audioURL, addToHistory: false)

        // Seek to the saved position after a short delay to let the player initialize
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.seek(to: position)
            self?.play()
        }

        return true
    }

    func stop() {
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
            timeObserver = nil
        }

        player?.pause()
        player = nil
        currentPodcast = nil
        isPlaying = false
        currentTime = 0
        duration = 0
        isLoading = false
        errorMessage = nil
        cancellables.removeAll()

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    private func updateNowPlayingInfo() {
        guard let podcast = currentPodcast else { return }

        let nowPlayingInfo: [String: Any] = [
            MPMediaItemPropertyTitle: podcast.title,
            MPMediaItemPropertyArtist: podcast.authors,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: currentTime,
            MPMediaItemPropertyPlaybackDuration: duration,
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? 1.0 : 0.0
        ]

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
    }

    func formattedTime(_ time: TimeInterval) -> String {
        guard time.isFinite && !time.isNaN else { return "0:00" }
        let minutes = Int(time) / 60
        let seconds = Int(time) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    // MARK: - Link Cue

    private func checkForLinkCue() async {
        guard let podcast = currentPodcast else { return }

        // Get transcript
        guard let cues = await TranscriptService.shared.getTranscript(for: podcast) else {
            return
        }

        // Find current cue index
        guard let currentIndex = TranscriptService.shared.findCueIndex(for: currentTime, in: cues) else {
            return
        }

        // Only process if we've moved to a new cue
        if lastCheckedCueIndex == currentIndex {
            return
        }
        lastCheckedCueIndex = currentIndex

        let cue = cues[currentIndex]

        // Check if this segment has a link (markdown format: [text](url))
        guard hasLink(in: cue.text) else {
            return
        }

        // Create unique identifier for this segment
        let segmentId = "\(podcast.id)-\(currentIndex)"

        // Check if we've already cued this segment
        guard !cuedSegments.contains(segmentId) else {
            return
        }

        // Calculate 2/3 point of the segment
        let segmentDuration = cue.endTime - cue.startTime
        let twoThirdsPoint = cue.startTime + (segmentDuration * 2.0 / 3.0)

        // Check if we're at or past the 2/3 point
        if currentTime >= twoThirdsPoint {
            cuedSegments.insert(segmentId)
            playLinkCue()
        }
    }

    private func hasLink(in text: String) -> Bool {
        // Check for markdown links: [text](url)
        let linkPattern = "\\[([^\\]]+)\\]\\([^\\)]+\\)"
        guard let regex = try? NSRegularExpression(pattern: linkPattern, options: []) else {
            return false
        }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return regex.firstMatch(in: text, options: [], range: range) != nil
    }

    private func playLinkCue() {
        // Play a subtle notification sound (1407 is a gentle "Note" sound)
        AudioServicesPlaySystemSound(1407)
    }
}

import Combine
