import SwiftUI

struct PodcastRowView: View {
    let podcast: Podcast

    @EnvironmentObject var downloadManager: DownloadManager
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var hasNotes = false
    @State private var remainingTime: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(String(podcast.year))
                    .font(.caption)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue.opacity(0.1))
                    .foregroundColor(.blue)
                    .cornerRadius(4)

                if let remaining = remainingTime {
                    Text("\(remaining) left")
                        .font(.caption)
                        .foregroundColor(.orange)
                } else {
                    Text(podcast.duration)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                if hasNotes {
                    Image(systemName: "note.text")
                        .font(.caption)
                        .foregroundColor(.orange)
                }

                Spacer()

                downloadStatusIcon
            }

            Text(podcast.title)
                .font(.headline)
                .lineLimit(2)

            Text(podcast.authors)
                .font(.subheadline)
                .foregroundColor(.secondary)

            Text(podcast.description)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
        .padding(.vertical, 8)
        .onAppear {
            hasNotes = ListeningHistoryService.shared.hasNotes(for: podcast)
            updateRemainingTime()
        }
        .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
            if let podcastId = notification.object as? String, podcastId == podcast.id {
                hasNotes = ListeningHistoryService.shared.hasNotes(for: podcast)
                updateRemainingTime()
            }
        }
    }

    private func updateRemainingTime() {
        let savedPosition = ListeningHistoryService.shared.getLastPosition(for: podcast)
        guard savedPosition > 0 else {
            remainingTime = nil
            return
        }

        let totalSeconds = parseDuration(podcast.duration)
        let remaining = totalSeconds - savedPosition
        guard remaining > 0 else {
            remainingTime = nil
            return
        }

        remainingTime = formatTime(remaining)
    }

    private func parseDuration(_ duration: String) -> TimeInterval {
        // Handle "X min" format
        if duration.contains("min") {
            let parts = duration.split(separator: " ")
            if let minutes = Int(parts.first ?? "") {
                return TimeInterval(minutes * 60)
            }
        }

        // Handle "X:XX" or "X:XX:XX" format
        let parts = duration.split(separator: ":").compactMap { Int($0) }
        switch parts.count {
        case 2:
            return TimeInterval(parts[0] * 60 + parts[1])
        case 3:
            return TimeInterval(parts[0] * 3600 + parts[1] * 60 + parts[2])
        default:
            return 0
        }
    }

    private func formatTime(_ time: TimeInterval) -> String {
        let hours = Int(time) / 3600
        let minutes = (Int(time) % 3600) / 60
        let seconds = Int(time) % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    @ViewBuilder
    private var downloadStatusIcon: some View {
        let state = downloadManager.downloadState(for: podcast)

        switch state {
        case .notDownloaded:
            EmptyView()
        case .downloading(let progress):
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.3), lineWidth: 2)
                    .frame(width: 20, height: 20)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(Color.blue, lineWidth: 2)
                    .frame(width: 20, height: 20)
                    .rotationEffect(.degrees(-90))
            }
        case .downloaded:
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.green)
        case .failed:
            Image(systemName: "exclamationmark.circle.fill")
                .foregroundColor(.red)
        }
    }

}

#Preview {
    List {
        PodcastRowView(podcast: Podcast.samples[0])
    }
    .environmentObject(DownloadManager.shared)
    .environmentObject(AudioPlayer.shared)
}
