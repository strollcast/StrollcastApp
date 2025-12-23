import Foundation
import SwiftUI

@MainActor
class PodcastService: ObservableObject {
    @Published var podcasts: [Podcast] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiURL = "https://strollcast.com/api/episodes.json"

    func fetchPodcasts() async {
        isLoading = true
        errorMessage = nil

        do {
            guard let url = URL(string: apiURL) else {
                throw URLError(.badURL)
            }

            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(EpisodesResponse.self, from: data)
            podcasts = response.episodes

            if podcasts.isEmpty {
                podcasts = Podcast.samples
            }
        } catch {
            errorMessage = "Failed to fetch podcasts: \(error.localizedDescription)"
            podcasts = Podcast.samples
        }

        isLoading = false
    }
}
