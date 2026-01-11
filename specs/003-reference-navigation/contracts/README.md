# API Contracts

## No New Contracts Required

This feature uses **existing API endpoints** without modifications:

### GET /episodes

**Endpoint**: `https://api.strollcast.com/episodes`

**Purpose**: Fetch episode metadata when user taps reference link.

**Usage**: `PodcastRepository.getPodcastById(episodeId)` calls this endpoint internally.

**Response format**: Already defined and unchanged.

**Example**:
```json
{
  "version": "2.0",
  "updated": "2026-01-11T18:42:16.293Z",
  "episodes": [
    {
      "id": "dao-2023-flashattention_2_fa",
      "title": "FlashAttention-2: Faster Attention with Better Parallelism...",
      "authors": "Tri Dao",
      "audioUrl": "https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.mp3",
      "transcriptUrl": "https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.vtt",
      ...
    }
  ]
}
```

## Why No Contract Changes?

- Episode metadata is already available via existing API
- Transcript VTT files already contain markdown links (no backend changes needed)
- Reference URLs already follow consistent pattern (enforced at content creation time)
- No new endpoints, parameters, or response fields required

## Reference URL Format (Content Contract)

While not a REST API contract, the reference URL format is a contract between content creators (who write episode scripts) and the mobile apps:

**Pattern**: `https://released.strollcast.com/episodes/{episode-id}/{episode-id}.(mp3|m4a)`

**Examples**:
- `https://released.strollcast.com/episodes/dao-2023-flashattention_2_fa/dao-2023-flashattention_2_fa.mp3`
- `https://released.strollcast.com/episodes/zheng-2022-alpa_automating_int/zheng-2022-alpa_automating_int.m4a`

**Validation**: Enforced by `EpisodeUrlParser.extractEpisodeId()` in mobile apps.

**Content creators must ensure**:
- Episode ID matches directory name and filename
- Episode ID exists in the API response
- URLs use `https://released.strollcast.com` domain
