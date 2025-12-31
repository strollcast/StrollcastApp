import SwiftUI

struct SettingsView: View {
    @StateObject private var zoteroService = ZoteroService.shared
    @StateObject private var downloadManager = DownloadManager.shared
    private let transcriptService = TranscriptService.shared

    @State private var apiKey: String = ""
    @State private var showingApiKey = false
    @State private var showingDeleteConfirmation = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Link your Zotero account to save papers from podcasts to your library.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Link(destination: URL(string: "https://www.zotero.org/settings/keys")!) {
                            Label("Create an API Key at zotero.org", systemImage: "arrow.up.right.square")
                                .font(.subheadline)
                        }
                    }
                    .padding(.vertical, 4)
                } header: {
                    Text("Zotero Integration")
                }

                Section {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("API Key")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        HStack {
                            if showingApiKey {
                                TextField("Enter your API Key", text: $apiKey)
                                    .textContentType(.password)
                                    .autocapitalization(.none)
                                    .disableAutocorrection(true)
                            } else {
                                SecureField("Enter your API Key", text: $apiKey)
                                    .textContentType(.password)
                            }
                            Button {
                                showingApiKey.toggle()
                            } label: {
                                Image(systemName: showingApiKey ? "eye.slash" : "eye")
                                    .foregroundColor(.secondary)
                            }
                        }
                    }

                    if zoteroService.isConfigured && !zoteroService.userId.isEmpty {
                        HStack {
                            Text("User ID")
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(zoteroService.userId)
                                .foregroundColor(.primary)
                        }
                    }
                } header: {
                    Text("Credentials")
                } footer: {
                    Text("Your API Key is stored securely in the Keychain.")
                        .font(.caption)
                }

                Section {
                    Button {
                        saveAndValidate()
                    } label: {
                        HStack {
                            Text("Save & Validate")
                            Spacer()
                            if zoteroService.isValidating {
                                ProgressView()
                            } else if zoteroService.isConfigured {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                            }
                        }
                    }
                    .disabled(apiKey.isEmpty || zoteroService.isValidating)

                    if let error = zoteroService.validationError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    if zoteroService.isConfigured {
                        Button(role: .destructive) {
                            clearCredentials()
                        } label: {
                            Text("Remove Zotero Connection")
                        }
                    }
                }

                if zoteroService.isConfigured {
                    Section {
                        if let success = zoteroService.lastSyncSuccess {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundColor(.green)
                                Text(success)
                                    .font(.caption)
                            }
                        }
                        if let error = zoteroService.lastSyncError {
                            HStack {
                                Image(systemName: "exclamationmark.circle.fill")
                                    .foregroundColor(.red)
                                Text(error)
                                    .font(.caption)
                            }
                        }
                        if zoteroService.lastSyncSuccess == nil && zoteroService.lastSyncError == nil {
                            Text("Play a podcast to sync to Zotero")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    } header: {
                        Text("Sync Status")
                    }
                }

                Section {
                    HStack {
                        Text("Downloaded Episodes")
                        Spacer()
                        Text("\(downloadManager.downloadedCount)")
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Audio Storage")
                        Spacer()
                        Text(formattedSize(downloadManager.totalDownloadedSize))
                            .foregroundColor(.secondary)
                    }

                    HStack {
                        Text("Transcript Storage")
                        Spacer()
                        Text(formattedSize(transcriptService.totalTranscriptSize))
                            .foregroundColor(.secondary)
                    }

                    Button(role: .destructive) {
                        showingDeleteConfirmation = true
                    } label: {
                        HStack {
                            Image(systemName: "trash")
                            Text("Delete All Downloads")
                        }
                    }
                    .disabled(downloadManager.downloadedCount == 0 && transcriptService.totalTranscriptSize == 0)
                } header: {
                    Text("Storage")
                } footer: {
                    Text("Downloaded episodes and transcripts are stored locally for offline playback.")
                        .font(.caption)
                }
            }
            .navigationTitle("Settings")
            .confirmationDialog(
                "Delete All Downloads?",
                isPresented: $showingDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete All", role: .destructive) {
                    downloadManager.deleteAllDownloads()
                    transcriptService.deleteAllTranscripts()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will remove \(downloadManager.downloadedCount) downloaded episodes and all cached transcripts (\(formattedSize(downloadManager.totalDownloadedSize + transcriptService.totalTranscriptSize))). You can re-download them later.")
            }
            .onAppear {
                apiKey = zoteroService.apiKey
            }
        }
    }

    private func saveAndValidate() {
        zoteroService.apiKey = apiKey

        Task {
            await zoteroService.validateCredentials()
        }
    }

    private func clearCredentials() {
        zoteroService.clearCredentials()
        apiKey = ""
    }

    private func formattedSize(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useKB, .useMB, .useGB]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

#Preview {
    SettingsView()
}
