import Foundation
import SwiftUI

@MainActor
class PodcastService: ObservableObject {
    @Published var podcasts: [Podcast] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let baseURL = "https://strollcast.com"

    func fetchPodcasts() async {
        isLoading = true
        errorMessage = nil

        do {
            guard let url = URL(string: baseURL) else {
                throw URLError(.badURL)
            }

            let (data, _) = try await URLSession.shared.data(from: url)
            guard let html = String(data: data, encoding: .utf8) else {
                throw URLError(.cannotDecodeContentData)
            }

            podcasts = parseHTML(html)

            if podcasts.isEmpty {
                podcasts = Podcast.samples
            }
        } catch {
            errorMessage = "Failed to fetch podcasts: \(error.localizedDescription)"
            podcasts = Podcast.samples
        }

        isLoading = false
    }

    private func parseHTML(_ html: String) -> [Podcast] {
        var parsedPodcasts: [Podcast] = []

        let cardPattern = #"<article[^>]*class="[^"]*podcast-card[^"]*"[^>]*>[\s\S]*?</article>"#
        let titlePattern = #"<h\d[^>]*>([^<]+)</h\d>"#
        let authorPattern = #"(?:Authors?|By):?\s*([^<\n]+)"#
        let yearPattern = #"\b(20\d{2})\b"#
        let durationPattern = #"(\d+)\s*min"#
        let descPattern = #"<p[^>]*class="[^"]*description[^"]*"[^>]*>([^<]+)</p>"#
        let audioPattern = #"href="([^"]+\.m4a)""#

        if let cardRegex = try? NSRegularExpression(pattern: cardPattern, options: []) {
            let range = NSRange(html.startIndex..., in: html)
            let matches = cardRegex.matches(in: html, options: [], range: range)

            for (index, match) in matches.enumerated() {
                if let cardRange = Range(match.range, in: html) {
                    let cardHTML = String(html[cardRange])

                    let title = extractFirst(pattern: titlePattern, from: cardHTML) ?? "Unknown Title"
                    let authors = extractFirst(pattern: authorPattern, from: cardHTML) ?? "Unknown"
                    let yearStr = extractFirst(pattern: yearPattern, from: cardHTML) ?? "2023"
                    let durationStr = extractFirst(pattern: durationPattern, from: cardHTML) ?? "0"
                    let description = extractFirst(pattern: descPattern, from: cardHTML) ?? ""
                    let audioPath = extractFirst(pattern: audioPattern, from: cardHTML) ?? ""

                    let podcast = Podcast(
                        id: "podcast-\(index)",
                        title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                        authors: authors.trimmingCharacters(in: .whitespacesAndNewlines),
                        year: Int(yearStr) ?? 2023,
                        duration: "\(durationStr) min",
                        description: description.trimmingCharacters(in: .whitespacesAndNewlines),
                        audioPath: audioPath
                    )
                    parsedPodcasts.append(podcast)
                }
            }
        }

        return parsedPodcasts
    }

    private func extractFirst(pattern: String, from text: String) -> String? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return nil
        }

        let range = NSRange(text.startIndex..., in: text)
        if let match = regex.firstMatch(in: text, options: [], range: range) {
            if match.numberOfRanges > 1, let captureRange = Range(match.range(at: 1), in: text) {
                return String(text[captureRange])
            } else if let matchRange = Range(match.range, in: text) {
                return String(text[matchRange])
            }
        }
        return nil
    }
}
