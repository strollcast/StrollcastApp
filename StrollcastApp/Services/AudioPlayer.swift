import Foundation
import AVFoundation
import MediaPlayer
import SwiftUI

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

    func load(podcast: Podcast, from url: URL) {
        stop()

        currentPodcast = podcast
        isLoading = true
        errorMessage = nil

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
            }
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

        var nowPlayingInfo: [String: Any] = [
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
}

import Combine
