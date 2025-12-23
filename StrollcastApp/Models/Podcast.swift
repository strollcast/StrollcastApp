import Foundation

struct Podcast: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let authors: String
    let year: Int
    let duration: String
    let description: String
    let audioPath: String

    var audioURL: URL {
        URL(string: "https://strollcast.com\(audioPath)")!
    }

    var fileName: String {
        audioPath.components(separatedBy: "/").last ?? "\(id).m4a"
    }
}

extension Podcast {
    static let samples: [Podcast] = [
        Podcast(
            id: "pathways-2022",
            title: "Pathways: Asynchronous Distributed Dataflow for ML",
            authors: "Barham et al.",
            year: 2022,
            duration: "29 min",
            description: "Google's orchestration layer for accelerators using asynchronous dataflow, enabling flexible parallelism across thousands of TPUs.",
            audioPath: "/barham-2022-pathways/barham-2022-pathways.m4a"
        ),
        Podcast(
            id: "megatron-2021",
            title: "Efficient Large-Scale Language Model Training on GPU Clusters Using Megatron-LM",
            authors: "Narayanan et al.",
            year: 2021,
            duration: "34 min",
            description: "NVIDIA's techniques for training trillion-parameter models across thousands of GPUs using tensor, pipeline, and data parallelism.",
            audioPath: "/narayanan-2021-megatron-lm/narayanan-2021-megatron-lm.m4a"
        ),
        Podcast(
            id: "fsdp-2023",
            title: "PyTorch FSDP: Experiences on Scaling Fully Sharded Data Parallel",
            authors: "Zhao et al.",
            year: 2023,
            duration: "24 min",
            description: "Meta's production experiences building fully sharded data parallel training into PyTorch.",
            audioPath: "/zhao-2023-pytorch-fsdp/zhao-2023-pytorch-fsdp.m4a"
        ),
        Podcast(
            id: "zero-2020",
            title: "ZeRO: Memory Optimizations Toward Training Trillion Parameter Models",
            authors: "Rajbhandari et al.",
            year: 2020,
            duration: "17 min",
            description: "Microsoft's breakthrough technique for eliminating memory redundancy in distributed training.",
            audioPath: "/rajbhandari-2020-zero/rajbhandari-2020-zero.m4a"
        )
    ]
}
