package com.strollcast.app.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.strollcast.app.models.NoteEntity
import org.junit.Rule
import org.junit.Test

class NoteDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noteDialog_notOpen_doesNotDisplay() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = false,
                onSave = {},
                onCancel = {}
            )
        }

        // Dialog should not be visible
        composeTestRule.onNodeWithText("Add Note").assertDoesNotExist()
    }

    @Test
    fun noteDialog_open_displaysCorrectly() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Verify dialog elements are present
        composeTestRule.onNodeWithText("Add Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Note").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertIsDisplayed()
    }

    @Test
    fun noteDialog_withExistingNote_showsEditMode() {
        val existingNote = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-1",
            content = "Existing note content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                existingNote = existingNote,
                onSave = {},
                onCancel = {}
            )
        }

        // Should show "Edit Note" title
        composeTestRule.onNodeWithText("Edit Note").assertIsDisplayed()

        // Should show "Save" button instead of "Add"
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").assertDoesNotExist()

        // Should pre-populate with existing content
        composeTestRule.onNodeWithText("Existing note content").assertIsDisplayed()
    }

    @Test
    fun noteDialog_withTranscriptText_showsContext() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                transcriptText = "This is the transcript line text for context",
                onSave = {},
                onCancel = {}
            )
        }

        // Should show transcript context
        composeTestRule.onNodeWithText("Transcript Line:").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is the transcript line text for context").assertIsDisplayed()
    }

    @Test
    fun noteDialog_cancel_triggersCallback() {
        var cancelCalled = false

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = {},
                onCancel = { cancelCalled = true }
            )
        }

        // Click cancel button
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Verify callback was called
        assert(cancelCalled) { "Cancel callback was not called" }
    }

    @Test
    fun noteDialog_save_withValidContent_triggersCallback() {
        var savedContent: String? = null

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = { content -> savedContent = content },
                onCancel = {}
            )
        }

        // Enter note content
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("My test note")

        // Click save button
        composeTestRule.onNodeWithText("Add").performClick()

        // Verify callback was called with correct content
        assert(savedContent == "My test note") {
            "Expected 'My test note', got '$savedContent'"
        }
    }

    @Test
    fun noteDialog_save_withEmptyContent_showsError() {
        var saveCalled = false

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = { saveCalled = true },
                onCancel = {}
            )
        }

        // Try to save without entering content
        composeTestRule.onNodeWithText("Add").performClick()

        // Should show error
        composeTestRule.onNodeWithText("Note cannot be empty").assertIsDisplayed()

        // Save callback should not be called
        assert(!saveCalled) { "Save callback should not be called with empty content" }
    }

    @Test
    fun noteDialog_save_withWhitespaceOnly_showsError() {
        var saveCalled = false

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = { saveCalled = true },
                onCancel = {}
            )
        }

        // Enter only whitespace
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("   ")

        // Try to save
        composeTestRule.onNodeWithText("Add").performClick()

        // Should show error
        composeTestRule.onNodeWithText("Note cannot be empty").assertIsDisplayed()

        // Save callback should not be called
        assert(!saveCalled) { "Save callback should not be called with whitespace content" }
    }

    @Test
    fun noteDialog_characterCount_updates() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Initially shows 0 characters
        composeTestRule.onNodeWithText("0 / 5000 characters").assertIsDisplayed()

        // Enter text
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("Hello")

        // Character count should update
        composeTestRule.onNodeWithText("5 / 5000 characters").assertIsDisplayed()
    }

    @Test
    fun noteDialog_exceedsMaxLength_disablesSave() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Enter text exceeding 5000 characters
        val longText = "a".repeat(5001)
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput(longText)

        // Save button should be disabled
        composeTestRule.onNodeWithText("Add").assertIsNotEnabled()

        // Error should be shown
        composeTestRule.onNodeWithText("5001 / 5000 characters").assertIsDisplayed()
    }

    @Test
    fun noteDialog_exactly5000Characters_allowsSave() {
        var savedContent: String? = null

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = { content -> savedContent = content },
                onCancel = {}
            )
        }

        // Enter exactly 5000 characters
        val maxText = "b".repeat(5000)
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput(maxText)

        // Character count should show max
        composeTestRule.onNodeWithText("5000 / 5000 characters").assertIsDisplayed()

        // Save button should be enabled
        composeTestRule.onNodeWithText("Add").assertIsEnabled()

        // Should allow save
        composeTestRule.onNodeWithText("Add").performClick()
        assert(savedContent == maxText) {
            "Expected max length text, got ${savedContent?.length} characters"
        }
    }

    @Test
    fun noteDialog_updateExistingNote_preservesContent() {
        val existingNote = NoteEntity(
            id = 1,
            transcriptLineId = 1,
            episodeId = "episode-1",
            content = "Original content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        var savedContent: String? = null

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                existingNote = existingNote,
                onSave = { content -> savedContent = content },
                onCancel = {}
            )
        }

        // Original content should be displayed
        composeTestRule.onNodeWithText("Original content").assertIsDisplayed()

        // Modify the content
        composeTestRule.onNodeWithText("Original content").performTextClearance()
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("Modified content")

        // Save
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify new content was saved
        assert(savedContent == "Modified content") {
            "Expected 'Modified content', got '$savedContent'"
        }
    }

    @Test
    fun noteDialog_errorClears_whenTyping() {
        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = {},
                onCancel = {}
            )
        }

        // Try to save with empty content to trigger error
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Note cannot be empty").assertIsDisplayed()

        // Start typing
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("A")

        // Error should disappear
        composeTestRule.onNodeWithText("Note cannot be empty").assertDoesNotExist()
    }

    @Test
    fun noteDialog_trimsWhitespace_beforeSaving() {
        var savedContent: String? = null

        composeTestRule.setContent {
            NoteDialog(
                isOpen = true,
                onSave = { content -> savedContent = content },
                onCancel = {}
            )
        }

        // Enter content with leading/trailing whitespace
        composeTestRule.onNodeWithText("Enter your note here...").performTextInput("  Trimmed note  ")

        // Save
        composeTestRule.onNodeWithText("Add").performClick()

        // Verify content was trimmed
        assert(savedContent == "Trimmed note") {
            "Expected 'Trimmed note', got '$savedContent'"
        }
    }
}
