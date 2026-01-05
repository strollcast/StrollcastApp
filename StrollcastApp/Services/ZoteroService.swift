import Foundation
import Security

class ZoteroService: ObservableObject {
    static let shared = ZoteroService()

    private let userIdKey = "zotero_user_id"
    private let apiKeyService = "com.strollcast.zotero-api-key"

    @Published var userId: String {
        didSet {
            UserDefaults.standard.set(userId, forKey: userIdKey)
        }
    }

    @Published var apiKey: String {
        didSet {
            saveApiKeyToKeychain(apiKey)
        }
    }

    @Published var isValidating = false
    @Published var validationError: String?
    @Published var isConfigured: Bool = false
    @Published var lastSyncError: String?
    @Published var lastSyncSuccess: String?

    private init() {
        self.userId = UserDefaults.standard.string(forKey: userIdKey) ?? ""
        self.apiKey = Self.loadApiKeyFromKeychain(service: apiKeyService) ?? ""
        self.isConfigured = !userId.isEmpty && !apiKey.isEmpty
    }

    var hasCredentials: Bool {
        !apiKey.isEmpty
    }

    func validateCredentials() async -> Bool {
        let trimmedApiKey = apiKey.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedApiKey.isEmpty else {
            await MainActor.run {
                validationError = "Please enter your API Key"
            }
            return false
        }

        await MainActor.run {
            if apiKey != trimmedApiKey { apiKey = trimmedApiKey }
            isValidating = true
            validationError = nil
        }

        defer {
            Task { @MainActor in
                isValidating = false
            }
        }

        // Use the /keys endpoint to validate and get user ID
        guard let url = URL(string: "https://api.zotero.org/keys/\(trimmedApiKey)") else {
            await MainActor.run {
                validationError = "Invalid API Key format"
            }
            return false
        }

        var request = URLRequest(url: url)
        request.setValue("3", forHTTPHeaderField: "Zotero-API-Version")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                await MainActor.run {
                    validationError = "Invalid response"
                }
                return false
            }

            switch httpResponse.statusCode {
            case 200:
                // Parse response to get user ID
                if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let keyUserId = json["userID"] as? Int {
                    await MainActor.run {
                        userId = String(keyUserId)
                        isConfigured = true
                        validationError = nil
                    }
                    return true
                }
                await MainActor.run {
                    validationError = "Could not parse user ID from response"
                }
                return false
            case 403:
                await MainActor.run {
                    validationError = "Invalid API Key"
                }
                return false
            case 404:
                await MainActor.run {
                    validationError = "API Key not found"
                }
                return false
            default:
                await MainActor.run {
                    validationError = "Error: HTTP \(httpResponse.statusCode)"
                }
                return false
            }
        } catch {
            await MainActor.run {
                validationError = "Network error: \(error.localizedDescription)"
            }
            return false
        }
    }

    func clearCredentials() {
        userId = ""
        apiKey = ""
        isConfigured = false
        deleteApiKeyFromKeychain()
    }

    // MARK: - Zotero Item Management

    private func zoteroItemKey(for podcastId: String) -> String? {
        UserDefaults.standard.string(forKey: "zotero_item_\(podcastId)")
    }

    private func saveZoteroItemKey(_ key: String, for podcastId: String) {
        UserDefaults.standard.set(key, forKey: "zotero_item_\(podcastId)")
    }

    private func zoteroNoteKey(for podcastId: String) -> String? {
        UserDefaults.standard.string(forKey: "zotero_note_\(podcastId)")
    }

    private func saveZoteroNoteKey(_ key: String, for podcastId: String) {
        UserDefaults.standard.set(key, forKey: "zotero_note_\(podcastId)")
    }

    private func zoteroNoteVersion(for podcastId: String) -> Int {
        UserDefaults.standard.integer(forKey: "zotero_note_version_\(podcastId)")
    }

    private func saveZoteroNoteVersion(_ version: Int, for podcastId: String) {
        UserDefaults.standard.set(version, forKey: "zotero_note_version_\(podcastId)")
    }

    func addPodcastToZotero(_ podcast: Podcast) async -> Bool {

        guard isConfigured else {
            return false
        }
        print("Zotero: addPodcastToZotero called for \(podcast.id)")

        // Check if already added
        if let existingKey = zoteroItemKey(for: podcast.id) {
            print("Zotero: Already added with key \(existingKey)")
            return true
        }

        // Parse authors into creator objects
        let creators = parseAuthors(podcast.authors)

        // Build the item
        var item: [String: Any] = [
            "itemType": "journalArticle",
            "title": podcast.title,
            "creators": creators,
            "date": String(podcast.year),
            "abstractNote": podcast.description,
            "tags": [
                ["tag": "strollcast"],
                ["tag": "podcast"]
            ]
        ]

        if let paperUrl = podcast.paperUrl {
            item["url"] = paperUrl
        }

        guard let url = URL(string: "https://api.zotero.org/users/\(userId)/items") else {
            return false
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "Zotero-API-Key")
        request.setValue("3", forHTTPHeaderField: "Zotero-API-Version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(String(UUID().uuidString.prefix(32)).replacingOccurrences(of: "-", with: ""), forHTTPHeaderField: "Zotero-Write-Token")

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: [item])
            request.httpBody = jsonData

            if let jsonStr = String(data: jsonData, encoding: .utf8) {
                print("Zotero request body: \(jsonStr)")
            }

            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                print("Zotero: No HTTP response")
                return false
            }

            print("Zotero addPodcast response: \(httpResponse.statusCode)")

            if httpResponse.statusCode != 200 {
                let errorMsg = String(data: data, encoding: .utf8) ?? "HTTP \(httpResponse.statusCode)"
                print("Zotero error response: \(errorMsg)")
                await MainActor.run {
                    lastSyncError = "Failed to add: HTTP \(httpResponse.statusCode)"
                }
                return false
            }

            // Parse response to get item key
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                print("Zotero response JSON: \(json)")
                if let successful = json["successful"] as? [String: Any],
                   let firstItem = successful["0"] as? [String: Any],
                   let itemKey = firstItem["key"] as? String {
                    print("Zotero: Successfully added item with key: \(itemKey)")
                    saveZoteroItemKey(itemKey, for: podcast.id)
                    await MainActor.run {
                        lastSyncSuccess = "Added: \(podcast.title)"
                        lastSyncError = nil
                    }
                    return true
                }
                if let failed = json["failed"] as? [String: Any] {
                    print("Zotero failed items: \(failed)")
                    await MainActor.run {
                        lastSyncError = "Failed: \(failed)"
                    }
                }
            }

            return false
        } catch {
            print("Error adding to Zotero: \(error)")
            await MainActor.run {
                lastSyncError = "Error: \(error.localizedDescription)"
            }
            return false
        }
    }

    func syncNotesToZotero(_ notes: String, for podcast: Podcast) async -> Bool {
        guard isConfigured else { return false }

        // Get or create the parent item
        if zoteroItemKey(for: podcast.id) == nil {
            let added = await addPodcastToZotero(podcast)
            if !added { return false }
        }

        guard let parentKey = zoteroItemKey(for: podcast.id) else { return false }

        // Convert notes to HTML
        let htmlNotes = convertNotesToHTML(notes, podcast: podcast)

        if let noteKey = zoteroNoteKey(for: podcast.id) {
            // Update existing note
            return await updateZoteroNote(noteKey, content: htmlNotes, for: podcast)
        } else {
            // Create new note
            return await createZoteroNote(parentKey: parentKey, content: htmlNotes, for: podcast)
        }
    }

    private func createZoteroNote(parentKey: String, content: String, for podcast: Podcast) async -> Bool {
        let note: [String: Any] = [
            "itemType": "note",
            "parentItem": parentKey,
            "note": content,
            "tags": [["tag": "strollcast-notes"]]
        ]

        guard let url = URL(string: "https://api.zotero.org/users/\(userId)/items") else {
            return false
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "Zotero-API-Key")
        request.setValue("3", forHTTPHeaderField: "Zotero-API-Version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(String(UUID().uuidString.prefix(32)).replacingOccurrences(of: "-", with: ""), forHTTPHeaderField: "Zotero-Write-Token")

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: [note])
            request.httpBody = jsonData

            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                return false
            }

            // Parse response to get note key and version
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let successful = json["successful"] as? [String: Any],
               let firstItem = successful["0"] as? [String: Any],
               let noteKey = firstItem["key"] as? String,
               let noteData = firstItem["data"] as? [String: Any],
               let version = noteData["version"] as? Int {
                saveZoteroNoteKey(noteKey, for: podcast.id)
                saveZoteroNoteVersion(version, for: podcast.id)
                return true
            }

            return false
        } catch {
            print("Error creating Zotero note: \(error)")
            return false
        }
    }

    private func updateZoteroNote(_ noteKey: String, content: String, for podcast: Podcast) async -> Bool {
        let version = zoteroNoteVersion(for: podcast.id)

        guard let url = URL(string: "https://api.zotero.org/users/\(userId)/items/\(noteKey)") else {
            return false
        }

        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue(apiKey, forHTTPHeaderField: "Zotero-API-Key")
        request.setValue("3", forHTTPHeaderField: "Zotero-API-Version")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(String(version), forHTTPHeaderField: "If-Unmodified-Since-Version")

        let update: [String: Any] = ["note": content]

        do {
            let jsonData = try JSONSerialization.data(withJSONObject: update)
            request.httpBody = jsonData

            let (_, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                return false
            }

            if httpResponse.statusCode == 204 {
                // Get new version from header
                if let newVersionStr = httpResponse.value(forHTTPHeaderField: "Last-Modified-Version"),
                   let newVersion = Int(newVersionStr) {
                    saveZoteroNoteVersion(newVersion, for: podcast.id)
                }
                return true
            } else if httpResponse.statusCode == 412 {
                // Version conflict - fetch current version and retry
                return await refetchAndUpdateNote(noteKey, content: content, for: podcast)
            }

            return false
        } catch {
            print("Error updating Zotero note: \(error)")
            return false
        }
    }

    private func refetchAndUpdateNote(_ noteKey: String, content: String, for podcast: Podcast) async -> Bool {
        // Fetch current note to get latest version
        guard let url = URL(string: "https://api.zotero.org/users/\(userId)/items/\(noteKey)") else {
            return false
        }

        var request = URLRequest(url: url)
        request.setValue(apiKey, forHTTPHeaderField: "Zotero-API-Key")
        request.setValue("3", forHTTPHeaderField: "Zotero-API-Version")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse,
                  httpResponse.statusCode == 200 else {
                return false
            }

            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let version = json["version"] as? Int {
                saveZoteroNoteVersion(version, for: podcast.id)
                return await updateZoteroNote(noteKey, content: content, for: podcast)
            }

            return false
        } catch {
            return false
        }
    }

    private func parseAuthors(_ authorsString: String) -> [[String: String]] {
        // Handle "et al." pattern
        let cleaned = authorsString.replacingOccurrences(of: " et al.", with: "")
            .replacingOccurrences(of: " et al", with: "")

        // Split by comma or "and"
        let authorNames = cleaned
            .replacingOccurrences(of: " and ", with: ", ")
            .components(separatedBy: ", ")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }

        return authorNames.map { name in
            // Simple heuristic: last word is lastName
            let parts = name.components(separatedBy: " ")
            if parts.count > 1 {
                let lastName = parts.last ?? name
                let firstName = parts.dropLast().joined(separator: " ")
                return [
                    "creatorType": "author",
                    "firstName": firstName,
                    "lastName": lastName
                ]
            } else {
                // Single name - use as lastName with empty firstName
                return [
                    "creatorType": "author",
                    "firstName": "",
                    "lastName": name
                ]
            }
        }
    }

    private func convertNotesToHTML(_ notes: String, podcast: Podcast) -> String {
        var lines = notes.components(separatedBy: "\n")
        var html = "<h1>Strollcast Notes: \(escapeHTML(podcast.title))</h1>\n"

        // Skip YAML frontmatter
        if lines.first?.starts(with: "---") == true {
            if let endIndex = lines.dropFirst().firstIndex(where: { $0.starts(with: "---") }) {
                lines = Array(lines.dropFirst(endIndex + 2))
            }
        }

        for line in lines {
            let trimmed = line.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty {
                continue
            } else if trimmed.starts(with: "## ") {
                let heading = String(trimmed.dropFirst(3))
                html += "<h2>\(escapeHTML(heading))</h2>\n"
            } else if trimmed.starts(with: "[") && trimmed.contains("]") {
                // Timestamped comment: [MM:SS] text
                html += "<p>\(escapeHTML(trimmed))</p>\n"
            } else {
                html += "<p>\(escapeHTML(trimmed))</p>\n"
            }
        }

        return html
    }

    private func escapeHTML(_ text: String) -> String {
        text.replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
    }

    // MARK: - Keychain Helpers

    private func saveApiKeyToKeychain(_ key: String) {
        deleteApiKeyFromKeychain()

        guard !key.isEmpty else { return }

        let data = key.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: apiKeyService,
            kSecValueData as String: data
        ]

        SecItemAdd(query as CFDictionary, nil)
    }

    private static func loadApiKeyFromKeychain(service: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess,
              let data = result as? Data,
              let key = String(data: data, encoding: .utf8) else {
            return nil
        }

        return key
    }

    private func deleteApiKeyFromKeychain() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: apiKeyService
        ]

        SecItemDelete(query as CFDictionary)
    }
}
