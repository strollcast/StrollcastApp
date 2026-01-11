# Data Model: Android Feature Parity

**Date**: 2026-01-11
**Database**: Room SQLite (Android local persistence)
**Current Version**: 1
**Target Version**: 2

## Overview

This document defines the database schema extensions required for Android feature parity. Four new entities are introduced to support transcripts, notes, and playback history tracking.

## Entity Relationship Diagram

```
┌─────────────┐
│  Podcast    │ (existing)
│─────────────│
│ id (PK)     │◄──────────┐
│ title       │           │
│ ...         │           │
└─────────────┘           │
                         │ FK
                         │
┌──────────────────────┐  │
│ TranscriptEntity     │  │
│──────────────────────│  │
│ id (PK)              │◄─┘
│ episodeId (FK)       │
│ vttContent           │
│ cachedAt             │
└──────────────────────┘
         │
         │ 1:N
         │
         ▼
┌──────────────────────┐
│ TranscriptLineEntity │
│──────────────────────│
│ id (PK)              │
│ transcriptId (FK)    │◄────┐
│ startMs              │     │
│ endMs                │     │ FK
│ speaker              │     │
│ text                 │     │
│ lineNumber           │     │
└──────────────────────┘     │
         │                   │
         │ 1:N               │
         │                   │
         ▼                   │
┌──────────────────────┐     │
│ NoteEntity           │     │
│──────────────────────│     │
│ id (PK)              │     │
│ transcriptLineId(FK) │─────┘
│ episodeId (denorm)   │
│ content              │
│ createdAt            │
│ updatedAt            │
└──────────────────────┘

┌──────────────────────┐
│ CompletedEpisode     │
│──────────────────────│
│ id (PK)              │
│ episodeId (FK)       │◄──── References Podcast
│ completionPercent    │
│ completedAt          │
│ totalDurationMs      │
└──────────────────────┘
```

## Entities

### 1. TranscriptEntity

**Purpose**: Cache downloaded VTT transcript files for offline access

**Table Name**: `transcripts`

**Kotlin Entity**:
```kotlin
@Entity(
    tableName = "transcripts",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["episodeId"])]
)
data class TranscriptEntity(
    @PrimaryKey
    val id: String,              // Episode ID (same as episodeId)

    val episodeId: String,       // Foreign key to podcasts table

    @ColumnInfo(name = "vtt_content")
    val vttContent: String,      // Raw VTT file content (10KB-100KB typical)

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long           // Timestamp for LRU eviction (System.currentTimeMillis())
)
```

**Fields**:
- `id`: Primary key, same value as `episodeId` (1:1 relationship)
- `episodeId`: References `podcasts.id`, CASCADE delete when episode removed
- `vttContent`: Full VTT file as TEXT column (SQLite handles up to ~1MB efficiently)
- `cachedAt`: Unix timestamp (milliseconds) for cache TTL and LRU eviction

**Indexes**:
- `episodeId`: Speeds up lookup by episode (most common query)

**Validation Rules**:
- `vttContent` must start with "WEBVTT" header (validated in repository layer)
- `cachedAt` must be > 0 (current timestamp)

**Cache Eviction Rules**:
- Delete transcripts where `cachedAt < (now - 30 days)`
- If total size > 10MB, delete oldest by `cachedAt ASC` until under limit

---

### 2. TranscriptLineEntity

**Purpose**: Store parsed transcript cues for tap-to-seek and note attachment

**Table Name**: `transcript_lines`

**Kotlin Entity**:
```kotlin
@Entity(
    tableName = "transcript_lines",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcriptId"]),
        Index(value = ["transcriptId", "lineNumber"])
    ]
)
data class TranscriptLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transcript_id")
    val transcriptId: String,    // Foreign key to transcripts table

    @ColumnInfo(name = "start_ms")
    val startMs: Long,           // Start timestamp in milliseconds

    @ColumnInfo(name = "end_ms")
    val endMs: Long,             // End timestamp in milliseconds

    val speaker: String?,        // Speaker name from <v> tag (nullable)

    val text: String,            // Cue text content

    @ColumnInfo(name = "line_number")
    val lineNumber: Int          // Sequential line number (0-based) for ordering
)
```

**Fields**:
- `id`: Auto-generated primary key
- `transcriptId`: References `transcripts.id`, CASCADE delete when transcript removed
- `startMs`: Milliseconds from start (0 to episode duration), used for seeking
- `endMs`: Milliseconds from start, used for highlighting current line
- `speaker`: Optional speaker name extracted from `<v Speaker>` VTT tag
- `text`: Transcript line text content (after removing VTT tags)
- `lineNumber`: Sequential ordering (0, 1, 2...) for display and navigation

**Indexes**:
- `transcriptId`: Fast lookup of all lines for an episode
- Composite `(transcriptId, lineNumber)`: Supports ordered queries

**Validation Rules**:
- `startMs < endMs` (enforced in repository)
- `lineNumber >= 0` and unique within transcript
- `text` not empty after trimming

**Query Patterns**:
```sql
-- Get all lines for episode (ordered)
SELECT * FROM transcript_lines WHERE transcript_id = ? ORDER BY line_number ASC

-- Find line at timestamp (for highlighting)
SELECT * FROM transcript_lines
WHERE transcript_id = ? AND start_ms <= ? AND end_ms > ?
LIMIT 1
```

---

### 3. NoteEntity

**Purpose**: Store user-created notes attached to specific transcript lines

**Table Name**: `notes`

**Kotlin Entity**:
```kotlin
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptLineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transcriptLineId"]),
        Index(value = ["episodeId", "createdAt"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "transcript_line_id")
    val transcriptLineId: Int,   // Foreign key to transcript_lines table

    @ColumnInfo(name = "episode_id")
    val episodeId: String,       // Denormalized for "all notes for episode" query

    val content: String,         // Note text (user-entered)

    @ColumnInfo(name = "created_at")
    val createdAt: Long,         // Unix timestamp (milliseconds)

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long          // Unix timestamp (milliseconds), updated on edit
)
```

**Fields**:
- `id`: Auto-generated primary key
- `transcriptLineId`: References `transcript_lines.id`, CASCADE delete when line removed
- `episodeId`: Denormalized episode ID for efficient "all notes for episode" queries (avoids JOIN)
- `content`: User note text, max 5000 characters (enforced in UI)
- `createdAt`: Timestamp when note created (immutable)
- `updatedAt`: Timestamp when note last modified (updated on every edit)

**Indexes**:
- `transcriptLineId`: Lookup notes for specific line (show note indicator)
- Composite `(episodeId, createdAt)`: "All notes for episode" sorted by creation time

**Validation Rules**:
- `content` not empty after trimming
- `content.length <= 5000` characters
- `createdAt <= updatedAt` (enforced on insert/update)

**Query Patterns**:
```sql
-- Get all notes for episode (for NotesScreen)
SELECT * FROM notes WHERE episode_id = ? ORDER BY created_at DESC

-- Check if line has notes (for indicator)
SELECT COUNT(*) FROM notes WHERE transcript_line_id = ?

-- Get notes for specific line (for detail view)
SELECT * FROM notes WHERE transcript_line_id = ? ORDER BY created_at ASC
```

---

### 4. CompletedEpisodeEntity

**Purpose**: Track episodes user has finished listening to (90%+ played)

**Table Name**: `completed_episodes`

**Kotlin Entity**:
```kotlin
@Entity(
    tableName = "completed_episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["completed_at"], orders = [Index.Order.DESC])
    ]
)
data class CompletedEpisodeEntity(
    @PrimaryKey
    val id: String,              // Episode ID (same as episodeId, 1:1 relationship)

    @ColumnInfo(name = "episode_id")
    val episodeId: String,       // Foreign key to podcasts table

    @ColumnInfo(name = "completion_percent")
    val completionPercent: Int,  // 0-100, typically 90-100 for "completed"

    @ColumnInfo(name = "completed_at")
    val completedAt: Long,       // Unix timestamp when marked complete

    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long    // Episode duration for display (copied from episode)
)
```

**Fields**:
- `id`: Primary key, same value as `episodeId` (1:1 relationship)
- `episodeId`: References `podcasts.id`, CASCADE delete
- `completionPercent`: 0-100, inserted when reaches 90%, updated if user continues listening
- `completedAt`: Timestamp when first marked as complete (90%+ threshold crossed)
- `totalDurationMs`: Denormalized episode duration for display without JOIN

**Indexes**:
- `completed_at DESC`: Sorted "Played" list (most recent first)

**Validation Rules**:
- `completionPercent >= 90 && completionPercent <= 100`
- `completedAt > 0`
- `totalDurationMs > 0`

**Completion Logic** (in repository/ViewModel):
```kotlin
// Mark as complete when:
if (playbackPosition >= (episodeDuration * 0.9)) {
    // Insert or update CompletedEpisodeEntity
}
```

**Query Patterns**:
```sql
-- Get played episodes list (for PlayedListScreen)
SELECT ce.*, p.title, p.authors
FROM completed_episodes ce
JOIN podcasts p ON ce.episode_id = p.id
ORDER BY ce.completed_at DESC

-- Check if episode is complete (for badge display)
SELECT EXISTS(SELECT 1 FROM completed_episodes WHERE episode_id = ?)
```

---

## Database Migration

### Migration: Version 1 → Version 2

**Migration File**: `android/app/src/main/java/com/strollcast/app/data/migrations/Migration1To2.kt`

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create transcripts table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transcripts (
                id TEXT PRIMARY KEY NOT NULL,
                episodeId TEXT NOT NULL,
                vtt_content TEXT NOT NULL,
                cached_at INTEGER NOT NULL,
                FOREIGN KEY(episodeId) REFERENCES podcasts(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcripts_episodeId ON transcripts(episodeId)")

        // 2. Create transcript_lines table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transcript_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transcript_id TEXT NOT NULL,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                speaker TEXT,
                text TEXT NOT NULL,
                line_number INTEGER NOT NULL,
                FOREIGN KEY(transcript_id) REFERENCES transcripts(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcript_lines_transcriptId ON transcript_lines(transcript_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transcript_lines_transcriptId_lineNumber ON transcript_lines(transcript_id, line_number)")

        // 3. Create notes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transcript_line_id INTEGER NOT NULL,
                episode_id TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(transcript_line_id) REFERENCES transcript_lines(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_transcriptLineId ON notes(transcript_line_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_notes_episodeId_createdAt ON notes(episode_id, created_at)")

        // 4. Create completed_episodes table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS completed_episodes (
                id TEXT PRIMARY KEY NOT NULL,
                episode_id TEXT NOT NULL,
                completion_percent INTEGER NOT NULL,
                completed_at INTEGER NOT NULL,
                total_duration_ms INTEGER NOT NULL,
                FOREIGN KEY(episode_id) REFERENCES podcasts(id) ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_completed_episodes_completedAt ON completed_episodes(completed_at DESC)")
    }
}
```

**Update StrollcastDatabase.kt**:
```kotlin
@Database(
    entities = [
        PodcastEntity::class,           // Existing
        PlaybackHistoryEntry::class,    // Existing
        DownloadedEpisode::class,       // Existing
        TranscriptEntity::class,        // NEW
        TranscriptLineEntity::class,    // NEW
        NoteEntity::class,              // NEW
        CompletedEpisodeEntity::class   // NEW
    ],
    version = 2,  // Increment from 1 to 2
    exportSchema = true
)
abstract class StrollcastDatabase : RoomDatabase() {
    // Existing DAOs
    abstract fun podcastDao(): PodcastDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun downloadDao(): DownloadDao

    // New DAOs
    abstract fun transcriptDao(): TranscriptDao
    abstract fun noteDao(): NoteDao
    abstract fun completedEpisodeDao(): CompletedEpisodeDao

    companion object {
        @Volatile
        private var INSTANCE: StrollcastDatabase? = null

        fun getDatabase(context: Context): StrollcastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StrollcastDatabase::class.java,
                    "strollcast_database"
                )
                .addMigrations(MIGRATION_1_2)  // Register migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

## DAO Interfaces

### TranscriptDao
```kotlin
@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts WHERE id = :episodeId")
    suspend fun getTranscript(episodeId: String): TranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE cached_at < :cutoffTime")
    suspend fun deleteOldTranscripts(cutoffTime: Long): Int

    @Query("SELECT * FROM transcript_lines WHERE transcript_id = :transcriptId ORDER BY line_number ASC")
    suspend fun getTranscriptLines(transcriptId: String): List<TranscriptLineEntity>

    @Insert
    suspend fun insertTranscriptLines(lines: List<TranscriptLineEntity>)

    @Query("SELECT * FROM transcript_lines WHERE transcript_id = :transcriptId AND start_ms <= :positionMs AND end_ms > :positionMs LIMIT 1")
    suspend fun getCurrentLine(transcriptId: String, positionMs: Long): TranscriptLineEntity?
}
```

### NoteDao
```kotlin
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE episode_id = :episodeId ORDER BY created_at DESC")
    fun getNotesForEpisode(episodeId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE transcript_line_id = :lineId ORDER BY created_at ASC")
    suspend fun getNotesForLine(lineId: Int): List<NoteEntity>

    @Insert
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("SELECT COUNT(*) FROM notes WHERE transcript_line_id = :lineId")
    suspend fun getNoteCount(lineId: Int): Int
}
```

### CompletedEpisodeDao
```kotlin
@Dao
interface CompletedEpisodeDao {
    @Query("SELECT * FROM completed_episodes ORDER BY completed_at DESC")
    fun getAllCompletedEpisodes(): Flow<List<CompletedEpisodeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM completed_episodes WHERE id = :episodeId)")
    suspend fun isEpisodeCompleted(episodeId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedEpisode(episode: CompletedEpisodeEntity)

    @Query("DELETE FROM completed_episodes WHERE id = :episodeId")
    suspend fun removeCompletedEpisode(episodeId: String)
}
```

---

## Performance Considerations

**Estimated Storage Requirements**:
- Transcripts: 30KB average × 100 episodes = 3MB
- Transcript Lines: ~500 lines × 50 bytes × 100 episodes = 2.5MB
- Notes: 200 bytes × 500 notes = 100KB
- Completed Episodes: 100 bytes × 100 episodes = 10KB
- **Total**: ~6MB for typical usage

**Query Optimization**:
- All foreign key relationships use indexes
- Composite indexes for common query patterns
- Flow-based queries for reactive UI updates
- AUTOINCREMENT on primary keys avoids reuse

**Cleanup Strategy**:
- Transcripts: WorkManager periodic job (daily) to delete old entries
- Orphaned data: Handled by CASCADE delete on foreign keys
- No cleanup needed for notes/completed episodes (user data)

---

## Next Steps

1. Create DAO interfaces with Room annotations
2. Implement migration MIGRATION_1_2
3. Update StrollcastDatabase.kt with new entities and version
4. Create Repository classes (TranscriptRepository, NoteRepository, HistoryRepository)
5. Test migration on emulator with test data
