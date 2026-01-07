import Foundation
import SwiftUI

@MainActor
class PodcastService: ObservableObject {
    @Published var podcasts: [Podcast] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let apiURL = "https://api.strollcast.com/episodes"
    private let searchURL = "https://api.strollcast.com/search"

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

    func searchPodcasts(query: String) async {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            await fetchPodcasts()
            return
        }

        isLoading = true
        errorMessage = nil

        do {
            guard var urlComponents = URLComponents(string: searchURL) else {
                throw URLError(.badURL)
            }

            urlComponents.queryItems = [URLQueryItem(name: "q", value: query)]

            guard let url = urlComponents.url else {
                throw URLError(.badURL)
            }

            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(SearchResponse.self, from: data)
            podcasts = response.episodes
        } catch {
            errorMessage = "Failed to search podcasts: \(error.localizedDescription)"
        }

        isLoading = false
    }
}
