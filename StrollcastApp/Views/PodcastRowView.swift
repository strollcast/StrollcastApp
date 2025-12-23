import SwiftUI

struct PodcastRowView: View {
    let podcast: Podcast

    @EnvironmentObject var downloadManager: DownloadManager
    @EnvironmentObject var audioPlayer: AudioPlayer

    @State private var hasNotes = false

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

                Text(podcast.duration)
                    .font(.caption)
                    .foregroundColor(.secondary)

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
        }
    }

    @ViewBuilder
    private var downloadStatusIcon: some View {
        let state = downloadManager.downloadState(for: podcast)

        switch state {
        case .notDownloaded:
            Image(systemName: "arrow.down.circle")
                .foregroundColor(.secondary)
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
