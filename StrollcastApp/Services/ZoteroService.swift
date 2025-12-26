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
