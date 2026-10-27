package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupOptions
import com.example.data.backup.RestoreMode
import com.example.data.local.LectraDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.TopicEntity
import com.example.data.model.UnitEntity
import com.example.data.repository.LectraRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreTest {

    private lateinit var application: Application
    private lateinit var database: LectraDatabase
    private lateinit var repository: LectraRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() = runTest {
        application = ApplicationProvider.getApplicationContext()
        val testScope = TestScope()
        database = LectraDatabase.getDatabase(application, testScope)
        repository = LectraRepository(database, application)
        backupManager = BackupManager(repository, application)
        repository.clearAllTables()
    }

    @Test
    fun `test backup generation produces valid JSON with correct counts`() = runTest {
        // Insert sample course, unit, topic
        val courseId = repository.insertCourse(
            CourseEntity(
                code = "TEST 101",
                title = "Intro to Testing",
                colorHex = "#D97706",
                lecturer = "Dr. Test",
                semester = "Term 1",
                targetWeeklyHours = 4
            )
        )

        val unitId = repository.insertUnit(
            UnitEntity(
                courseId = courseId,
                unitNumber = 1,
                title = "Unit 1: Fundamentals",
                description = "Basics"
            )
        )

        repository.insertTopic(
            TopicEntity(
                unitId = unitId,
                courseId = courseId,
                title = "Topic A",
                summaryNotes = "Important notes"
            )
        )

        repository.insertFlashcard(
            FlashcardEntity(
                courseId = courseId,
                frontPrompt = "What is testing?",
                backAnswer = "Verification of code"
            )
        )

        val result = backupManager.createBackupJson(BackupOptions())
        assertTrue(result.isSuccess)

        val (jsonString, summary) = result.getOrThrow()
        assertNotNull(jsonString)
        val json = JSONObject(jsonString)
        assertEquals("LECTRA", json.getString("app"))
        assertEquals(1, json.getInt("version"))

        assertEquals("Summary should report 1 course", 1, summary.coursesCount)
        assertEquals("Summary should report 1 unit", 1, summary.unitsCount)
        assertEquals("Summary should report 1 topic", 1, summary.topicsCount)
        assertEquals("Summary should report 1 flashcard", 1, summary.flashcardsCount)
    }

    @Test
    fun `test non-destructive merge restore preserves existing records`() = runTest {
        val course1Id = repository.insertCourse(
            CourseEntity(
                code = "EXISTING 101",
                title = "Existing Course",
                colorHex = "#2D5A46",
                lecturer = "Prof. Alpha",
                semester = "Term 1"
            )
        )

        // Generate backup data containing course 1
        val backupResult = backupManager.createBackupJson(BackupOptions())
        assertTrue(backupResult.isSuccess)
        val (backupJson, _) = backupResult.getOrThrow()

        // Add a newer course that shouldn't be lost during a MERGE
        val course2Id = repository.insertCourse(
            CourseEntity(
                code = "NEW 202",
                title = "Newly Added Course",
                colorHex = "#7165FF",
                lecturer = "Prof. Beta",
                semester = "Term 1"
            )
        )

        // Parse and execute MERGE restore
        val parseResult = backupManager.parseBackupString(backupJson)
        assertTrue(parseResult.isSuccess)
        val validationResult = parseResult.getOrThrow()
        assertTrue(validationResult.isValid)
        val backupData = validationResult.data!!
        val restoreResult = backupManager.restoreBackupData(backupData, RestoreMode.MERGE)
        assertTrue(restoreResult.isSuccess)

        // Verify both courses exist
        val initialCourses = repository.getAllCoursesList()
        assertEquals(2, initialCourses.size)
    }

    @Test
    fun `test file copy to internal storage creates valid offline path`() {
        val dummyFile = java.io.File(application.cacheDir, "test_lecture.pdf").apply {
            writeText("%PDF-1.4 Mock PDF content for testing offline persistence")
        }
        val dummyUri = android.net.Uri.fromFile(dummyFile)
        val savedPath = repository.saveFileToInternalStorage(dummyUri)
        assertNotNull(savedPath)
        assertTrue("Saved path must point to local file or valid URI", savedPath.isNotEmpty())
    }
}
