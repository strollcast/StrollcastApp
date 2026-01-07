import Foundation

struct Podcast: Identifiable, Codable, Equatable, Hashable {
    let id: String
    let title: String
    let authors: String
    let year: Int
    let duration: String
    let durationSeconds: Int?
    let description: String
    let audioUrl: String
    let transcriptUrl: String?
    let paperUrl: String?

    var audioURL: URL {
        URL(string: audioUrl)!
    }

    var fileName: String {
        audioUrl.components(separatedBy: "/").last ?? "\(id).m4a"
    }

    var transcriptURL: URL? {
        guard let transcriptUrl = transcriptUrl else { return nil }
        return URL(string: transcriptUrl)
    }

    var paperURL: URL? {
        guard let paperUrl = paperUrl else { return nil }
        return URL(string: paperUrl)
    }
}

struct EpisodesResponse: Codable {
    let version: String
    let updated: String
    let episodes: [Podcast]
}

struct SearchResponse: Codable {
    let version: String
    let query: String
    let count: Int
    let episodes: [Podcast]
}

#if DEBUG
extension Podcast {
    static let samples: [Podcast] = [
        Podcast(
            id: "pathways-2022",
            title: "Pathways: Asynchronous Distributed Dataflow for ML",
            authors: "Barham et al.",
            year: 2022,
            duration: "29 min",
            durationSeconds: 1740,
            description: "Google's orchestration layer for accelerators using asynchronous dataflow, enabling flexible parallelism across thousands of TPUs.",
            audioUrl: "https://strollcast.com/barham-2022-pathways/barham-2022-pathways.m4a",
            transcriptUrl: nil,
            paperUrl: "https://arxiv.org/abs/2203.12533"
        ),
        Podcast(
            id: "megatron-2021",
            title: "Efficient Large-Scale Language Model Training on GPU Clusters Using Megatron-LM",
            authors: "Narayanan et al.",
            year: 2021,
            duration: "34 min",
            durationSeconds: 2040,
            description: "NVIDIA's techniques for training trillion-parameter models across thousands of GPUs using tensor, pipeline, and data parallelism.",
            audioUrl: "https://strollcast.com/narayanan-2021-megatron-lm/narayanan-2021-megatron-lm.m4a",
            transcriptUrl: nil,
            paperUrl: "https://arxiv.org/abs/2104.04473"
        ),
        Podcast(
            id: "fsdp-2023",
            title: "PyTorch FSDP: Experiences on Scaling Fully Sharded Data Parallel",
            authors: "Zhao et al.",
            year: 2023,
            duration: "24 min",
            durationSeconds: 1440,
            description: "Meta's production experiences building fully sharded data parallel training into PyTorch.",
            audioUrl: "https://strollcast.com/zhao-2023-pytorch-fsdp/zhao-2023-pytorch-fsdp.m4a",
            transcriptUrl: nil,
            paperUrl: "https://arxiv.org/abs/2304.11277"
        ),
        Podcast(
            id: "zero-2020",
            title: "ZeRO: Memory Optimizations Toward Training Trillion Parameter Models",
            authors: "Rajbhandari et al.",
            year: 2020,
            duration: "17 min",
            durationSeconds: 1020,
            description: "Microsoft's breakthrough technique for eliminating memory redundancy in distributed training.",
            audioUrl: "https://strollcast.com/rajbhandari-2020-zero/rajbhandari-2020-zero.m4a",
            transcriptUrl: nil,
            paperUrl: "https://arxiv.org/abs/1910.02054"
        )
    ]
}
#endif
