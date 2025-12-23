import SwiftUI

struct NotesListView: View {
    @EnvironmentObject var podcastService: PodcastService

    @State private var notesFiles: [(podcast: Podcast, content: String)] = []

    var body: some View {
        NavigationStack {
            Group {
                if notesFiles.isEmpty {
                    VStack(spacing: 16) {
                        Image(systemName: "note.text")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("No Notes Yet")
                            .font(.headline)
                        Text("Play a podcast and add notes to see them here")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    List(notesFiles, id: \.podcast.id) { item in
                        NavigationLink(destination: NoteDetailView(podcast: item.podcast)) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(item.podcast.title)
                                    .font(.headline)
                                    .lineLimit(2)

                                Text(item.podcast.authors)
                                    .font(.caption)
                                    .foregroundColor(.secondary)

                                Text(previewText(from: item.content))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Notes")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        loadNotes()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
        }
        .onAppear {
            loadNotes()
        }
    }

    private func loadNotes() {
        notesFiles = podcastService.podcasts.compactMap { podcast in
            let content = ListeningHistoryService.shared.readNotes(for: podcast)
            if !content.isEmpty {
                return (podcast: podcast, content: content)
            }
            return nil
        }
    }

    private func previewText(from content: String) -> String {
        // Skip YAML frontmatter and get actual notes content
        let lines = content.components(separatedBy: "\n")
        var inFrontmatter = false
        var preview = ""

        for line in lines {
            if line.trimmingCharacters(in: .whitespaces) == "---" {
                inFrontmatter = !inFrontmatter
                continue
            }
            if !inFrontmatter && !line.trimmingCharacters(in: .whitespaces).isEmpty {
                if !line.hasPrefix("#") {
                    preview += line + " "
                    if preview.count > 100 {
                        break
                    }
                }
            }
        }

        return preview.trimmingCharacters(in: .whitespaces)
    }
}

struct NoteDetailView: View {
    let podcast: Podcast

    @State private var notes: String = ""

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text(podcast.title)
                    .font(.headline)

                HStack {
                    Text(podcast.authors)
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    Text("•")
                        .foregroundColor(.secondary)

                    Text(String(podcast.year))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color(.systemGray6))

            // Notes editor
            TextEditor(text: $notes)
                .font(.system(.body, design: .monospaced))
                .padding(8)
                .onChange(of: notes) { _, newValue in
                    ListeningHistoryService.shared.saveNotes(newValue, for: podcast)
                }
        }
        .navigationTitle("Note")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            notes = ListeningHistoryService.shared.readNotes(for: podcast)
        }
        .onReceive(NotificationCenter.default.publisher(for: .listeningHistoryUpdated)) { notification in
            if let podcastId = notification.object as? String, podcastId == podcast.id {
                notes = ListeningHistoryService.shared.readNotes(for: podcast)
            }
        }
    }
}

#Preview {
    NotesListView()
        .environmentObject(PodcastService())
}
