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

        // Add to Zotero if configured
        Task {
            await ZoteroService.shared.addPodcastToZotero(podcast)
        }
    }

    func logPause(podcast: Podcast, position: TimeInterval) {
        // Save position for resume
        saveLastPosition(position, for: podcast)
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

    func saveLastActivePodcast(_ podcast: Podcast) {
        UserDefaults.standard.set(podcast.id, forKey: "last_active_podcast_id")
    }

    func getLastActivePodcastId() -> String? {
        UserDefaults.standard.string(forKey: "last_active_podcast_id")
    }

    func isCompleted(podcast: Podcast) -> Bool {
        // A podcast is completed if it has notes but no saved position
        // (position is cleared when podcast finishes playing)
        let hasHistory = hasNotes(for: podcast)
        let position = getLastPosition(for: podcast)
        return hasHistory && position == 0
    }

    func isInProgress(podcast: Podcast) -> Bool {
        // A podcast is in progress if it has a saved position
        return getLastPosition(for: podcast) > 0
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

    // MARK: - Timestamped Comments

    struct TimestampedComment {
        let timestamp: TimeInterval
        let text: String
    }

    func parseTimestampedComments(from notes: String) -> [TimestampedComment] {
        var comments: [TimestampedComment] = []
        let pattern = #"\[(\d+):(\d{2})\]\s*(.+)"#

        guard let regex = try? NSRegularExpression(pattern: pattern, options: .anchorsMatchLines) else {
            return comments
        }

        let range = NSRange(notes.startIndex..<notes.endIndex, in: notes)
        let matches = regex.matches(in: notes, options: [], range: range)

        for match in matches {
            guard match.numberOfRanges == 4,
                  let minutesRange = Range(match.range(at: 1), in: notes),
                  let secondsRange = Range(match.range(at: 2), in: notes),
                  let textRange = Range(match.range(at: 3), in: notes) else {
                continue
            }

            let minutes = Double(notes[minutesRange]) ?? 0
            let seconds = Double(notes[secondsRange]) ?? 0
            let timestamp = minutes * 60 + seconds
            let text = String(notes[textRange]).trimmingCharacters(in: .whitespaces)

            if !text.isEmpty {
                comments.append(TimestampedComment(timestamp: timestamp, text: text))
            }
        }

        return comments
    }

    func addTimestampedComment(_ comment: String, at time: TimeInterval, for podcast: Podcast) {
        var notes = readNotes(for: podcast)
        let formattedTime = formatTime(time)
        let newComment = "[\(formattedTime)] \(comment)"

        // First, remove any existing comment at this timestamp (within tolerance)
        notes = removeExistingComment(at: time, from: notes)

        // Find the ## Notes section and add after it
        if let notesRange = notes.range(of: "## Notes") {
            // Find the end of the ## Notes line
            if let lineEnd = notes[notesRange.upperBound...].firstIndex(of: "\n") {
                let insertIndex = notes.index(after: lineEnd)
                notes.insert(contentsOf: "\n\(newComment)", at: insertIndex)
            } else {
                notes.append("\n\n\(newComment)")
            }
        } else {
            // No ## Notes section, just append
            notes.append("\n\(newComment)")
        }

        saveNotes(notes, for: podcast)

        DispatchQueue.main.async {
            NotificationCenter.default.post(name: .listeningHistoryUpdated, object: podcast.id)
        }

        // Sync to Zotero if configured
        Task {
            await ZoteroService.shared.syncNotesToZotero(notes, for: podcast)
        }
    }

    private func removeExistingComment(at time: TimeInterval, from notes: String, tolerance: TimeInterval = 2.0) -> String {
        let lines = notes.components(separatedBy: "\n")
        let pattern = #"^\[(\d+):(\d{2})\]"#

        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return notes
        }

        let filteredLines = lines.filter { line in
            let range = NSRange(line.startIndex..<line.endIndex, in: line)
            guard let match = regex.firstMatch(in: line, options: [], range: range),
                  match.numberOfRanges == 3,
                  let minutesRange = Range(match.range(at: 1), in: line),
                  let secondsRange = Range(match.range(at: 2), in: line) else {
                return true // Keep non-timestamp lines
            }

            let minutes = Double(line[minutesRange]) ?? 0
            let seconds = Double(line[secondsRange]) ?? 0
            let lineTimestamp = minutes * 60 + seconds

            // Remove if within tolerance of the target time
            return abs(lineTimestamp - time) >= tolerance
        }

        return filteredLines.joined(separator: "\n")
    }

    func getComment(for timestamp: TimeInterval, in notes: String, tolerance: TimeInterval = 2.0) -> String? {
        let comments = parseTimestampedComments(from: notes)
        return comments.first { abs($0.timestamp - timestamp) < tolerance }?.text
    }

    private func createNewFile(at url: URL, podcast: Podcast, position: TimeInterval) {
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
