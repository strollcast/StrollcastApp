import Foundation
import SwiftUI

enum DownloadState: Equatable {
    case notDownloaded
    case downloading(progress: Double)
    case downloaded(localURL: URL)
    case failed(error: String)
}

@MainActor
class DownloadManager: NSObject, ObservableObject {
    static let shared = DownloadManager()

    @Published var downloadStates: [String: DownloadState] = [:]

    private var downloadTasks: [String: URLSessionDownloadTask] = [:]
    private var progressObservers: [String: NSKeyValueObservation] = [:]

    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.default
        return URLSession(configuration: config, delegate: self, delegateQueue: .main)
    }()

    private override init() {
        super.init()
        loadDownloadedFiles()
    }

    private func loadDownloadedFiles() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }

        do {
            let files = try fileManager.contentsOfDirectory(at: documentsURL, includingPropertiesForKeys: nil)
            for file in files where file.pathExtension == "m4a" {
                let podcastId = file.deletingPathExtension().lastPathComponent
                downloadStates[podcastId] = .downloaded(localURL: file)
            }
        } catch {
            print("Error loading downloaded files: \(error)")
        }
    }

    func downloadState(for podcast: Podcast) -> DownloadState {
        return downloadStates[podcast.id] ?? .notDownloaded
    }

    func localURL(for podcast: Podcast) -> URL? {
        if case .downloaded(let url) = downloadStates[podcast.id] {
            return url
        }
        return nil
    }

    func download(_ podcast: Podcast) {
        guard downloadStates[podcast.id] == nil || downloadStates[podcast.id] == .notDownloaded else {
            return
        }

        downloadStates[podcast.id] = .downloading(progress: 0)

        let task = session.downloadTask(with: podcast.audioURL)
        task.taskDescription = podcast.id
        downloadTasks[podcast.id] = task

        let observation = task.progress.observe(\.fractionCompleted) { [weak self] progress, _ in
            Task { @MainActor in
                self?.downloadStates[podcast.id] = .downloading(progress: progress.fractionCompleted)
            }
        }
        progressObservers[podcast.id] = observation

        task.resume()
    }

    func cancelDownload(_ podcast: Podcast) {
        downloadTasks[podcast.id]?.cancel()
        downloadTasks.removeValue(forKey: podcast.id)
        progressObservers.removeValue(forKey: podcast.id)
        downloadStates[podcast.id] = .notDownloaded
    }

    func deleteDownload(_ podcast: Podcast) {
        if case .downloaded(let url) = downloadStates[podcast.id] {
            try? FileManager.default.removeItem(at: url)
        }
        downloadStates[podcast.id] = .notDownloaded
    }

    func deleteAllDownloads() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }

        do {
            let files = try fileManager.contentsOfDirectory(at: documentsURL, includingPropertiesForKeys: nil)
            for file in files where file.pathExtension == "m4a" {
                try? fileManager.removeItem(at: file)
            }
        } catch {
            print("Error deleting all files: \(error)")
        }

        // Reset all download states
        for (podcastId, state) in downloadStates {
            if case .downloaded = state {
                downloadStates[podcastId] = .notDownloaded
            }
        }
    }

    var totalDownloadedSize: Int64 {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return 0
        }

        var totalSize: Int64 = 0
        do {
            let files = try fileManager.contentsOfDirectory(at: documentsURL, includingPropertiesForKeys: [.fileSizeKey])
            for file in files where file.pathExtension == "m4a" {
                let attributes = try file.resourceValues(forKeys: [.fileSizeKey])
                totalSize += Int64(attributes.fileSize ?? 0)
            }
        } catch {
            print("Error calculating total size: \(error)")
        }
        return totalSize
    }

    var downloadedCount: Int {
        downloadStates.values.filter { if case .downloaded = $0 { return true } else { return false } }.count
    }

    private func saveFile(tempURL: URL, for podcastId: String) -> URL? {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }

        let destinationURL = documentsURL.appendingPathComponent("\(podcastId).m4a")

        try? fileManager.removeItem(at: destinationURL)

        do {
            try fileManager.moveItem(at: tempURL, to: destinationURL)
            return destinationURL
        } catch {
            print("Error saving file: \(error)")
            return nil
        }
    }
}

extension DownloadManager: URLSessionDownloadDelegate {
    nonisolated func urlSession(_ session: URLSession, downloadTask: URLSessionDownloadTask, didFinishDownloadingTo location: URL) {
        guard let podcastId = downloadTask.taskDescription else { return }

        let tempURL = location
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }

        let destinationURL = documentsURL.appendingPathComponent("\(podcastId).m4a")

        try? fileManager.removeItem(at: destinationURL)

        do {
            try fileManager.moveItem(at: tempURL, to: destinationURL)
            Task { @MainActor in
                self.downloadStates[podcastId] = .downloaded(localURL: destinationURL)
                self.downloadTasks.removeValue(forKey: podcastId)
                self.progressObservers.removeValue(forKey: podcastId)
            }
        } catch {
            Task { @MainActor in
                self.downloadStates[podcastId] = .failed(error: error.localizedDescription)
            }
        }
    }

    nonisolated func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        guard let podcastId = task.taskDescription, let error = error else { return }

        Task { @MainActor in
            if (error as NSError).code != NSURLErrorCancelled {
                self.downloadStates[podcastId] = .failed(error: error.localizedDescription)
            }
            self.downloadTasks.removeValue(forKey: podcastId)
            self.progressObservers.removeValue(forKey: podcastId)
        }
    }
}
