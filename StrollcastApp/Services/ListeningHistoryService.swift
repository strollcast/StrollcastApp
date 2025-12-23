import Foundation

class ListeningHistoryService {
    static let shared = ListeningHistoryService()

    private let folderName = "Strollcast"
    private let fileManager = FileManager.default

    private init() {
        createStrollcastFolderIfNeeded()
    }

    private var iCloudDocumentsURL: URL? {
        fileManager.url(forUbiquityContainerIdentifier: nil)?.appendingPathComponent("Documents")
    }

    private var strollcastFolderURL: URL? {
        iCloudDocumentsURL?.appendingPathComponent(folderName)
    }

    private var localFallbackURL: URL {
        fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(folderName)
    }

    private var activeFolderURL: URL {
        if let iCloudURL = strollcastFolderURL, fileManager.fileExists(atPath: iCloudURL.path) {
            return iCloudURL
        }
        return localFallbackURL
    }

    private func createStrollcastFolderIfNeeded() {
        // Try iCloud first
        if let iCloudURL = strollcastFolderURL {
            if !fileManager.fileExists(atPath: iCloudURL.path) {
                try? fileManager.createDirectory(at: iCloudURL, withIntermediateDirectories: true)
            }
        }

        // Always create local fallback
        if !fileManager.fileExists(atPath: localFallbackURL.path) {
            try? fileManager.createDirectory(at: localFallbackURL, withIntermediateDirectories: true)
        }
    }

    private func fileURL(for podcast: Podcast) -> URL {
        let sanitizedTitle = podcast.id
            .replacingOccurrences(of: "/", with: "-")
            .replacingOccurrences(of: ":", with: "-")
        return activeFolderURL.appendingPathComponent("\(sanitizedTitle).md")
    }

    func logPlayback(podcast: Podcast, position: TimeInterval) {
        let url = fileURL(for: podcast)
        let timestamp = ISO8601DateFormatter().string(from: Date())
        let formattedPosition = formatTime(position)

        if fileManager.fileExists(atPath: url.path) {
            // Append new entry
            appendEntry(to: url, timestamp: timestamp, position: formattedPosition)
        } else {
            // Create new file with header
            createNewFile(at: url, podcast: podcast, timestamp: timestamp, position: formattedPosition)
        }
    }

    func readNotes(for podcast: Podcast) -> String {
        let url = fileURL(for: podcast)
        if fileManager.fileExists(atPath: url.path) {
            return (try? String(contentsOf: url, encoding: .utf8)) ?? ""
        }
        return ""
    }

    func saveNotes(_ content: String, for podcast: Podcast) {
        let url = fileURL(for: podcast)
        try? content.write(to: url, atomically: true, encoding: .utf8)
    }

    func hasNotes(for podcast: Podcast) -> Bool {
        let url = fileURL(for: podcast)
        return fileManager.fileExists(atPath: url.path)
    }

    private func createNewFile(at url: URL, podcast: Podcast, timestamp: String, position: String) {
        let content = """
        ---
        id: \(podcast.id)
        title: \(podcast.title)
        authors: \(podcast.authors)
        year: \(podcast.year)
        duration: \(podcast.duration)
        audioPath: \(podcast.audioPath)
        paperUrl: \(podcast.paperUrl ?? "")
        ---

        # Listening History

        ## \(podcast.title)

        | Date | Position |
        |------|----------|
        | \(timestamp) | \(position) |
        """

        try? content.write(to: url, atomically: true, encoding: .utf8)
    }

    private func appendEntry(to url: URL, timestamp: String, position: String) {
        guard var content = try? String(contentsOf: url, encoding: .utf8) else { return }

        let newEntry = "| \(timestamp) | \(position) |"
        content += "\n\(newEntry)"

        try? content.write(to: url, atomically: true, encoding: .utf8)
    }

    private func formatTime(_ time: TimeInterval) -> String {
        guard time.isFinite && !time.isNaN else { return "0:00" }
        let hours = Int(time) / 3600
        let minutes = (Int(time) % 3600) / 60
        let seconds = Int(time) % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }
}
