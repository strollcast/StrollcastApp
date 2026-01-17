import Foundation
import SwiftUI

/// Utility for parsing markdown-style links from plain text
/// Format: [link text](url)
struct MarkdownLinkParser {

    /// Represents a parsed markdown link
    struct ParsedLink {
        let text: String        // Link display text
        let url: String         // Full URL
        let startIndex: Int     // Character offset where link starts
        let endIndex: Int       // Character offset where link ends
    }

    /// Parse all markdown links from text
    static func parseLinks(from text: String) -> [ParsedLink] {
        let pattern = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)"

        guard let regex = try? NSRegularExpression(pattern: pattern, options: []) else {
            return []
        }

        let nsString = text as NSString
        let matches = regex.matches(in: text, options: [], range: NSRange(location: 0, length: nsString.length))

        return matches.compactMap { match in
            guard match.numberOfRanges >= 3 else { return nil }

            let textRange = match.range(at: 1)
            let urlRange = match.range(at: 2)

            return ParsedLink(
                text: nsString.substring(with: textRange),
                url: nsString.substring(with: urlRange),
                startIndex: match.range.location,
                endIndex: match.range.location + match.range.length
            )
        }
    }

    /// Build an AttributedString with styled clickable links
    static func buildAttributedString(from text: String, linkColor: Color = .blue) -> AttributedString {
        let links = parseLinks(from: text)

        guard !links.isEmpty else {
            return AttributedString(text)
        }

        var result = AttributedString()
        var lastIndex = 0

        for link in links {
            // Add text before link
            if lastIndex < link.startIndex {
                let startIdx = text.index(text.startIndex, offsetBy: lastIndex)
                let endIdx = text.index(text.startIndex, offsetBy: link.startIndex)
                result += AttributedString(String(text[startIdx..<endIdx]))
            }

            // Add styled link
            var linkString = AttributedString(link.text)
            linkString.foregroundColor = linkColor
            linkString.underlineStyle = .single
            linkString.link = URL(string: link.url)
            result += linkString

            lastIndex = link.endIndex
        }

        // Add remaining text after last link
        if lastIndex < text.count {
            let startIdx = text.index(text.startIndex, offsetBy: lastIndex)
            result += AttributedString(String(text[startIdx...]))
        }

        return result
    }

    /// Extract episode ID from a Strollcast audio URL
    /// Expected format: https://released.strollcast.com/episodes/{episode_id}/{episode_id}.mp3
    static func extractEpisodeId(from urlString: String) -> String? {
        guard let url = URL(string: urlString) else { return nil }

        let pathComponents = url.pathComponents
        guard pathComponents.count >= 3,
              pathComponents[pathComponents.count - 3] == "episodes" else {
            return nil
        }

        return pathComponents[pathComponents.count - 2]
    }

    /// Represents a paper reference extracted from a URL
    struct PaperReference {
        let type: String      // e.g., "arxiv"
        let id: String        // e.g., "1706.03762"
    }

    /// Extract paper reference from a Strollcast paper URL
    /// Expected format: https://strollcast.com/paper/arxiv/{arxiv_id}
    static func extractPaperReference(from urlString: String) -> PaperReference? {
        guard let url = URL(string: urlString) else { return nil }

        // Check host is strollcast.com
        guard url.host == "strollcast.com" else { return nil }

        let pathComponents = url.pathComponents
        // Expected: ["", "paper", "arxiv", "1706.03762"]
        guard pathComponents.count >= 4,
              pathComponents[1] == "paper" else {
            return nil
        }

        let type = pathComponents[2]  // "arxiv"
        let id = pathComponents[3...].joined(separator: "/")  // Handle IDs with slashes

        return PaperReference(type: type, id: id)
    }

    /// Check if a URL is a Strollcast paper reference URL
    static func isPaperReferenceUrl(_ urlString: String) -> Bool {
        return extractPaperReference(from: urlString) != nil
    }
}
