import Foundation

extension Notification.Name {
    static let listeningHistoryUpdated = Notification.Name("listeningHistoryUpdated")
}

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
        // Use local storage for now
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

        if !fileManager.fileExists(atPath: url.path) {
            createNewFile(at: url, podcast: podcast, position: position)
        }
    }

    func logPause(podcast: Podcast, position: TimeInterval) {
        let url = fileURL(for: podcast)
        guard fileManager.fileExists(atPath: url.path),
              var content = try? String(contentsOf: url, encoding: .utf8) else { return }

        let formattedPosition = formatTime(position)
        let newEntry = "Paused at \(formattedPosition)"

        var lines = content.components(separatedBy: "\n")

        // Remove trailing empty lines
        while let last = lines.last, last.trimmingCharacters(in: .whitespaces).isEmpty {
            lines.removeLast()
        }

        // Only remove "Paused at" if it's the last line (user hasn't added content after it)
        if let lastLine = lines.last, lastLine.hasPrefix("Paused at") {
            lines.removeLast()
        }

        lines.append(newEntry)
        content = lines.joined(separator: "\n")

        try? content.write(to: url, atomically: true, encoding: .utf8)

        // Save position for resume
        saveLastPosition(position, for: podcast)

        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .listeningHistoryUpdated, object: podcast.id)
        }
    }

    func saveLastPosition(_ position: TimeInterval, for podcast: Podcast) {
        UserDefaults.standard.set(position, forKey: "playback_position_\(podcast.id)")
    }

    func getLastPosition(for podcast: Podcast) -> TimeInterval {
        UserDefaults.standard.double(forKey: "playback_position_\(podcast.id)")
    }

    func clearLastPosition(for podcast: Podcast) {
        UserDefaults.standard.removeObject(forKey: "playback_position_\(podcast.id)")
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

    private func createNewFile(at url: URL, podcast: Podcast, position: TimeInterval) {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "EEE MMM d yyyy 'at' ha"
        let dateString = dateFormatter.string(from: Date())
        let formattedPosition = formatTime(position)

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

        Started on \(dateString), playback time \(formattedPosition)

        ## Notes


        """

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
