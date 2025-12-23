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

        // Only create the file if it doesn't exist - don't update existing files
        if !fileManager.fileExists(atPath: url.path) {
            createNewFile(at: url, podcast: podcast)
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

    private func createNewFile(at url: URL, podcast: Podcast) {
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

        # \(podcast.title)

        ## Notes


        """

        try? content.write(to: url, atomically: true, encoding: .utf8)
    }
}
