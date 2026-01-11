package com.strollcast.app.repository

import com.strollcast.app.data.NoteDao
import com.strollcast.app.models.NoteEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NoteRepositoryTest {
    private lateinit var repository: NoteRepository
    private lateinit var mockDao: NoteDao

    @Before
    fun setup() {
        mockDao = mockk(relaxed = true)
        repository = NoteRepository(mockDao)
    }

    @Test
    fun `getNotesForEpisode returns flow from DAO`() = runTest {
        val notes = listOf(
            NoteEntity(
                id = 1,
                transcriptLineId = 1,
                episodeId = "episode-1",
                content = "Test note",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        coEvery { mockDao.getNotesForEpisode("episode-1") } returns flowOf(notes)

        val result = repository.getNotesForEpisode("episode-1")

        // Verify flow returns correct data
        result.collect { noteList ->
            assertEquals(1, noteList.size)
            assertEquals("Test note", noteList[0].content)
        }

        coVerify { mockDao.getNotesForEpisode("episode-1") }
    }

    @Test
    fun `getNotesForLine delegates to DAO`() = runTest {
        val notes = listOf(
            NoteEntity(
                id = 1,
                transcriptLineId = 5,
                episodeId = "episode-2",
                content = "Line note",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        coEvery { mockDao.getNotesForLine(5) } returns notes

        val result = repository.getNotesForLine(5)

        assertEquals(1, result.size)
        assertEquals("Line note", result[0].content)
        coVerify { mockDao.getNotesForLine(5) }
    }

    @Test
    fun `createNote validates empty content`() = runTest {
        val result = repository.createNote(
            transcriptLineId = 1,
            episodeId = "episode-3",
            content = "   "  // Whitespace only
        )

        assertTrue(result.isFailure)
        assertEquals("Note content cannot be empty", result.exceptionOrNull()?.message)

        // DAO should not be called
        coVerify(exactly = 0) { mockDao.insertNote(any()) }
    }

    @Test
    fun `createNote validates max length`() = runTest {
        val longContent = "a".repeat(5001)  // Exceeds 5000 char limit

        val result = repository.createNote(
            transcriptLineId = 1,
            episodeId = "episode-4",
            content = longContent
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("exceeds maximum length") == true)

        // DAO should not be called
        coVerify(exactly = 0) { mockDao.insertNote(any()) }
    }

    @Test
    fun `createNote trims content and inserts`() = runTest {
        coEvery { mockDao.insertNote(any()) } returns 123L

        val result = repository.createNote(
            transcriptLineId = 1,
            episodeId = "episode-5",
            content = "  Valid content  "
        )

        assertTrue(result.isSuccess)
        assertEquals(123L, result.getOrNull())

        // Verify content was trimmed
        coVerify {
            mockDao.insertNote(
                match { note ->
                    note.content == "Valid content" &&
                    note.transcriptLineId == 1 &&
                    note.episodeId == "episode-5"
                }
            )
        }
    }

    @Test
    fun `updateNote validates empty content`() = runTest {
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-6",
            content = "",  // Empty
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val result = repository.updateNote(note)

        assertTrue(result.isFailure)
        assertEquals("Note content cannot be empty", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { mockDao.updateNote(any()) }
    }

    @Test
    fun `updateNote validates timestamps`() = runTest {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-7",
            content = "Valid content",
            createdAt = now,
            updatedAt = now - 1000  // Updated before created
        )

        val result = repository.updateNote(note)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Updated time cannot be before created time") == true)

        coVerify(exactly = 0) { mockDao.updateNote(any()) }
    }

    @Test
    fun `updateNote trims content and updates timestamp`() = runTest {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-8",
            content = "  Updated content  ",
            createdAt = now - 1000,
            updatedAt = now - 500
        )

        coEvery { mockDao.updateNote(any()) } returns Unit

        val result = repository.updateNote(note)

        assertTrue(result.isSuccess)

        // Verify content was trimmed and updatedAt was updated
        coVerify {
            mockDao.updateNote(
                match { updatedNote ->
                    updatedNote.content == "Updated content" &&
                    updatedNote.updatedAt > note.updatedAt
                }
            )
        }
    }

    @Test
    fun `deleteNote delegates to DAO`() = runTest {
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-9",
            content = "To delete",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        coEvery { mockDao.deleteNote(note) } returns Unit

        val result = repository.deleteNote(note)

        assertTrue(result.isSuccess)
        coVerify { mockDao.deleteNote(note) }
    }

    @Test
    fun `getNoteCount delegates to DAO`() = runTest {
        coEvery { mockDao.getNoteCount(5) } returns 3

        val count = repository.getNoteCount(5)

        assertEquals(3, count)
        coVerify { mockDao.getNoteCount(5) }
    }

    @Test
    fun `createNote handles DAO exceptions`() = runTest {
        coEvery { mockDao.insertNote(any()) } throws RuntimeException("Database error")

        val result = repository.createNote(
            transcriptLineId = 1,
            episodeId = "episode-10",
            content = "Valid content"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `updateNote handles DAO exceptions`() = runTest {
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-11",
            content = "Valid content",
            createdAt = System.currentTimeMillis() - 1000,
            updatedAt = System.currentTimeMillis()
        )

        coEvery { mockDao.updateNote(any()) } throws RuntimeException("Update failed")

        val result = repository.updateNote(note)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `deleteNote handles DAO exceptions`() = runTest {
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-12",
            content = "To delete",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        coEvery { mockDao.deleteNote(note) } throws RuntimeException("Delete failed")

        val result = repository.deleteNote(note)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `createNote with exactly 5000 characters succeeds`() = runTest {
        val maxContent = "a".repeat(5000)

        coEvery { mockDao.insertNote(any()) } returns 456L

        val result = repository.createNote(
            transcriptLineId = 1,
            episodeId = "episode-13",
            content = maxContent
        )

        assertTrue(result.isSuccess)
        coVerify { mockDao.insertNote(any()) }
    }

    @Test
    fun `updateNote with exactly 5000 characters succeeds`() = runTest {
        val maxContent = "b".repeat(5000)
        val note = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-14",
            content = maxContent,
            createdAt = System.currentTimeMillis() - 1000,
            updatedAt = System.currentTimeMillis()
        )

        coEvery { mockDao.updateNote(any()) } returns Unit

        val result = repository.updateNote(note)

        assertTrue(result.isSuccess)
        coVerify { mockDao.updateNote(any()) }
    }
}
