import Foundation

struct TranscriptCue: Identifiable {
    let id = UUID()
    let startTime: TimeInterval
    let endTime: TimeInterval
    let speaker: String?
    let text: String
}

class TranscriptService {
    static let shared = TranscriptService()

    private let fileManager = FileManager.default
    private var cache: [String: [TranscriptCue]] = [:]

    private init() {}

    private var transcriptsFolderURL: URL {
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let folderURL = documentsURL.appendingPathComponent("Transcripts")
        if !fileManager.fileExists(atPath: folderURL.path) {
            try? fileManager.createDirectory(at: folderURL, withIntermediateDirectories: true)
        }
        return folderURL
    }

    private func localURL(for podcast: Podcast) -> URL {
        let sanitizedId = podcast.id
            .replacingOccurrences(of: "/", with: "-")
            .replacingOccurrences(of: ":", with: "-")
        return transcriptsFolderURL.appendingPathComponent("\(sanitizedId).vtt")
    }

    private func remoteURL(for podcast: Podcast) -> URL? {
        // Use the transcriptUrl from the API response
        return podcast.transcriptURL
    }

    func getTranscript(for podcast: Podcast) async -> [TranscriptCue]? {
        // Check memory cache
        if let cached = cache[podcast.id] {
            return cached
        }

        // Check local file
        let localFile = localURL(for: podcast)
        if fileManager.fileExists(atPath: localFile.path) {
            if let content = try? String(contentsOf: localFile, encoding: .utf8) {
                let cues = parseVTT(content)
                if cues.isEmpty {
                    // Local file is corrupted or empty, delete it and re-download
                    try? fileManager.removeItem(at: localFile)
                } else {
                    cache[podcast.id] = cues
                    return cues
                }
            }
        }

        // Download from server
        guard let remoteURL = remoteURL(for: podcast) else {
            return nil
        }

        do {
            let (data, response) = try await URLSession.shared.data(from: remoteURL)

            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode != 200 {
                return nil
            }

            guard let content = String(data: data, encoding: .utf8) else {
                return nil
            }

            // Save locally
            try? content.write(to: localFile, atomically: true, encoding: .utf8)

            let cues = parseVTT(content)
            cache[podcast.id] = cues
            return cues
        } catch {
            return nil
        }
    }

    private func parseVTT(_ content: String) -> [TranscriptCue] {
        var cues: [TranscriptCue] = []
        let lines = content.components(separatedBy: .newlines)

        var i = 0
        while i < lines.count {
            let line = lines[i].trimmingCharacters(in: .whitespaces)

            // Look for timestamp line (e.g., "00:00:00.000 --> 00:00:05.000")
            if line.contains("-->") {
                let times = line.components(separatedBy: "-->")
                if times.count == 2 {
                    let startTime = parseTimestamp(times[0].trimmingCharacters(in: .whitespaces))
                    let endTime = parseTimestamp(times[1].trimmingCharacters(in: .whitespaces))

                    // Collect text lines until empty line or next timestamp
                    var textLines: [String] = []
                    i += 1
                    while i < lines.count {
                        let textLine = lines[i]
                        if textLine.trimmingCharacters(in: .whitespaces).isEmpty {
                            break
                        }
                        if textLine.contains("-->") {
                            i -= 1
                            break
                        }
                        textLines.append(textLine)
                        i += 1
                    }

                    let rawText = textLines.joined(separator: " ").trimmingCharacters(in: .whitespaces)
                    if !rawText.isEmpty {
                        let (speaker, text) = extractSpeaker(from: rawText)
                        cues.append(TranscriptCue(startTime: startTime, endTime: endTime, speaker: speaker, text: text))
                    }
                }
            }
            i += 1
        }

        return cues
    }

    private func parseTimestamp(_ timestamp: String) -> TimeInterval {
        // Format: HH:MM:SS.mmm or MM:SS.mmm
        let parts = timestamp.components(separatedBy: ":")
        var seconds: TimeInterval = 0

        if parts.count == 3 {
            // HH:MM:SS.mmm
            seconds += (Double(parts[0]) ?? 0) * 3600
            seconds += (Double(parts[1]) ?? 0) * 60
            seconds += Double(parts[2]) ?? 0
        } else if parts.count == 2 {
            // MM:SS.mmm
            seconds += (Double(parts[0]) ?? 0) * 60
            seconds += Double(parts[1]) ?? 0
        }

        return seconds
    }

    private func extractSpeaker(from text: String) -> (speaker: String?, text: String) {
        // Handle VTT voice tags like "<v Eric>Hello world"
        if text.hasPrefix("<v ") {
            if let endIndex = text.firstIndex(of: ">") {
                let speakerStart = text.index(text.startIndex, offsetBy: 3)
                let speaker = String(text[speakerStart..<endIndex])
                let remainingText = String(text[text.index(after: endIndex)...]).trimmingCharacters(in: .whitespaces)
                return (speaker, remainingText)
            }
        }
        return (nil, text)
    }

    func findCueIndex(for time: TimeInterval, in cues: [TranscriptCue]) -> Int? {
        for (index, cue) in cues.enumerated() {
            if time >= cue.startTime && time <= cue.endTime {
                return index
            }
            if time < cue.startTime {
                return max(0, index - 1)
            }
        }
        return cues.isEmpty ? nil : cues.count - 1
    }
}
