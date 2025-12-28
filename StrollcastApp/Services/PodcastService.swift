import Foundation
import SwiftUI

@MainActor
class PodcastService: ObservableObject {
    @Published var podcasts: [Podcast] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiURL = "https://api.strollcast.com/episodes"

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
        } catch {
            errorMessage = "Failed to fetch podcasts: \(error.localizedDescription)"
        }

        isLoading = false
    }
}
