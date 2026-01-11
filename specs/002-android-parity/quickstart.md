# Quickstart: Android Feature Parity Development

**Last Updated**: 2026-01-11
**Target Audience**: Android developers implementing transcript, notes, and history features

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 36 (compileSdk)
- Minimum SDK 26 (Android 8.0)
- Existing Strollcast Android app codebase

## Project Structure Overview

```
android/app/src/main/java/com/strollcast/app/
├── data/           # Room database layer (add new DAOs here)
├── di/             # Hilt dependency injection (add new modules here)
├── models/         # Data models (add new entities here)
├── repository/     # Data repositories (add new repos here)
├── ui/             # Jetpack Compose screens (add new screens here)
├── viewmodels/     # MVVM ViewModels (add new VMs here)
└── util/           # Utility classes (add VTT parser here)
```

## Development Workflow

### 1. Database Changes (Room Entities)

**Step 1: Create Entity Class**

File: `models/TranscriptEntity.kt`

```kotlin
@Entity(
    tableName = "transcripts",
    foreignKeys = [ForeignKey(/*...*/)],
    indices = [Index(/*...*/)]
)
data class TranscriptEntity(
    @PrimaryKey val id: String,
    val episodeId: String,
    @ColumnInfo(name = "vtt_content") val vttContent: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)
```

**Step 2: Create DAO Interface**

File: `data/TranscriptDao.kt`

```kotlin
@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts WHERE id = :episodeId")
    suspend fun getTranscript(episodeId: String): TranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    // Add other queries...
}
```

**Step 3: Update Database Class**

File: `data/StrollcastDatabase.kt`

```kotlin
@Database(
    entities = [
        PodcastEntity::class,
        // ... existing entities
        TranscriptEntity::class  // Add new entity
    ],
    version = 2,  // Increment version
    exportSchema = true
)
abstract class StrollcastDatabase : RoomDatabase() {
    abstract fun transcriptDao(): TranscriptDao  // Add DAO getter

    companion object {
        // Add migration
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS transcripts (...)")
                // See data-model.md for full migration SQL
            }
        }

        fun getDatabase(context: Context): StrollcastDatabase {
            return Room.databaseBuilder(/*...*/)
                .addMigrations(MIGRATION_1_2)  // Register migration
                .build()
        }
    }
}
```

**Step 4: Test Migration**

Run app on emulator/device. Database will auto-migrate. Check Logcat for migration logs.

To test migration with existing data:
```kotlin
// In test class
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StrollcastDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        val db = helper.createDatabase(DB_NAME, 1)
        // Insert test data in version 1 schema
        db.close()

        helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)
    }
}
```

---

### 2. Repository Pattern

**Step 1: Create Repository Interface**

File: `repository/TranscriptRepository.kt`

```kotlin
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val okHttpClient: OkHttpClient,
    private val context: Context
) {
    private val memoryCache = mutableMapOf<String, List<TranscriptCue>>()

    suspend fun getTranscript(episodeId: String, vttUrl: String?): Result<List<TranscriptCue>> {
        // 1. Check memory cache
        memoryCache[episodeId]?.let { return Result.success(it) }

        // 2. Check Room cache
        transcriptDao.getTranscript(episodeId)?.let { entity ->
            val cues = parseVTT(entity.vttContent)
            memoryCache[episodeId] = cues
            return Result.success(cues)
        }

        // 3. Download from network
        vttUrl ?: return Result.failure(Exception("No transcript URL"))

        return withContext(Dispatchers.IO) {
            try {
                val vttContent = downloadVTT(vttUrl)
                val cues = parseVTT(vttContent)

                // Cache in Room
                transcriptDao.insertTranscript(
                    TranscriptEntity(
                        id = episodeId,
                        episodeId = episodeId,
                        vttContent = vttContent,
                        cachedAt = System.currentTimeMillis()
                    )
                )

                memoryCache[episodeId] = cues
                Result.success(cues)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun downloadVTT(url: String): String {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        return response.body!!.string()
    }

    private fun parseVTT(vttContent: String): List<TranscriptCue> {
        // TODO: Implement VTT parser (see VttParser.kt)
        return emptyList()
    }
}
```

**Step 2: Provide via Hilt Module**

File: `di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideTranscriptRepository(
        database: StrollcastDatabase,
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): TranscriptRepository {
        return TranscriptRepository(
            transcriptDao = database.transcriptDao(),
            okHttpClient = okHttpClient,
            context = context
        )
    }
}
```

---

### 3. ViewModel (MVVM)

**Step 1: Define UI State**

```kotlin
data class TranscriptUiState(
    val transcript: List<TranscriptCue> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLineIndex: Int? = null  // For highlighting
)
```

**Step 2: Create ViewModel**

File: `viewmodels/TranscriptViewModel.kt`

```kotlin
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscriptUiState())
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    fun loadTranscript(episodeId: String, vttUrl: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            transcriptRepository.getTranscript(episodeId, vttUrl)
                .onSuccess { cues ->
                    _uiState.update {
                        it.copy(
                            transcript = cues,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Unknown error"
                        )
                    }
                }
        }
    }

    fun updateCurrentPosition(positionMs: Long) {
        val transcript = _uiState.value.transcript
        val index = transcript.indexOfFirst {
            positionMs in it.startTime..it.endTime
        }
        _uiState.update { it.copy(currentLineIndex = index.takeIf { it >= 0 }) }
    }
}
```

---

### 4. Compose UI Screen

**Step 1: Create Screen Composable**

File: `ui/screens/TranscriptScreen.kt`

```kotlin
@Composable
fun TranscriptScreen(
    episodeId: String,
    viewModel: TranscriptViewModel = hiltViewModel(),
    onSeekTo: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(episodeId) {
        viewModel.loadTranscript(episodeId, vttUrl = null) // Get from episode
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.error != null -> Text("Error: ${uiState.error}")
            else -> {
                LazyColumn {
                    items(
                        items = uiState.transcript,
                        key = { it.startTime }  // Stable key for performance
                    ) { cue ->
                        TranscriptLineItem(
                            cue = cue,
                            isHighlighted = uiState.transcript.indexOf(cue) == uiState.currentLineIndex,
                            onClick = { onSeekTo(cue.startTime) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptLineItem(
    cue: TranscriptCue,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            cue.speaker?.let { speaker ->
                Text(
                    text = speaker,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = cue.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

**Step 2: Add to Navigation**

File: `ui/StrollcastApp.kt`

```kotlin
NavHost(navController, startDestination = "podcasts") {
    // Existing routes...

    composable(
        route = "transcript/{episodeId}",
        arguments = listOf(navArgument("episodeId") { type = NavType.StringType })
    ) { backStackEntry ->
        val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
        TranscriptScreen(
            episodeId = episodeId,
            onSeekTo = { timestampMs -> /* Seek player */ }
        )
    }
}
```

---

### 5. Testing

#### Unit Tests (Repository)

File: `test/java/repository/TranscriptRepositoryTest.kt`

```kotlin
class TranscriptRepositoryTest {
    private lateinit var repository: TranscriptRepository
    private val mockDao = mockk<TranscriptDao>()

    @Test
    fun `getTranscript returns cached data`() = runTest {
        // Arrange
        val cachedEntity = TranscriptEntity(/*...*/)
        coEvery { mockDao.getTranscript(any()) } returns cachedEntity

        // Act
        val result = repository.getTranscript("episode-123", null)

        // Assert
        assertTrue(result.isSuccess)
        verify { mockDao.getTranscript("episode-123") }
    }
}
```

#### Instrumentation Tests (Database)

File: `androidTest/java/data/TranscriptDaoTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class TranscriptDaoTest {
    private lateinit var database: StrollcastDatabase
    private lateinit var dao: TranscriptDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StrollcastDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.transcriptDao()
    }

    @Test
    fun insertAndRetrieveTranscript() = runTest {
        val transcript = TranscriptEntity(/*...*/)
        dao.insertTranscript(transcript)

        val retrieved = dao.getTranscript(transcript.id)
        assertEquals(transcript, retrieved)
    }
}
```

---

## Common Pitfalls & Solutions

### 1. Foreign Key Constraint Failures
**Problem**: `FOREIGN KEY constraint failed` on insert
**Solution**: Ensure parent row exists before inserting child (e.g., episode must exist before transcript)

### 2. Main Thread Database Access
**Problem**: `Cannot access database on the main thread`
**Solution**: All DAO calls must be in `suspend` functions or wrapped in `withContext(Dispatchers.IO)`

### 3. Compose Recomposition Issues
**Problem**: LazyColumn items flicker or recompose unnecessarily
**Solution**: Use stable `key` parameter in `items()` (e.g., `key = { it.startTime }`)

### 4. Migration Crashes
**Problem**: App crashes on launch after database schema change
**Solution**: Implement proper `Migration` object, test with `MigrationTestHelper`, or use `fallbackToDestructiveMigration()` for development

### 5. Memory Leaks in ViewModels
**Problem**: Coroutines continue after ViewModel cleared
**Solution**: Use `viewModelScope.launch` (automatically cancelled when ViewModel destroyed)

---

## Performance Tips

1. **Lazy Loading**: Use LazyColumn for large lists (handles virtualization automatically)
2. **Stable Keys**: Always provide `key` parameter in `items()` for optimal recomposition
3. **Background Parsing**: Parse VTT on `Dispatchers.IO`, not main thread
4. **Indexed Queries**: Add Room `@Index` annotations on foreign keys and frequently queried columns
5. **Flow vs LiveData**: Use `Flow` for better Compose integration (`.collectAsState()`)

---

## Debugging Tools

### View Database Contents (Android Studio)
1. **App Inspection** → **Database Inspector**
2. Browse tables, run SQL queries live
3. Export database to SQLite file

### Debug Compose Layout
1. **Layout Inspector** → Enable "Show Recomposition Counts"
2. Identify performance bottlenecks

### Network Debugging
1. Use **OkHttp Logging Interceptor** for HTTP requests:
```kotlin
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()
```

---

## Next Steps

1. Complete VTT parser implementation (`util/VttParser.kt`)
2. Implement all repositories (Transcript, Note, History)
3. Create ViewModels for each feature
4. Build Compose UI screens
5. Wire up navigation
6. Test on physical device
7. Run manual QA checklist (see spec.md acceptance scenarios)

---

## Resources

- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Jetpack Compose State](https://developer.android.com/jetpack/compose/state)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
