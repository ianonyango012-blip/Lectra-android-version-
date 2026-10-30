package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LectraDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.PdfAnnotationEntity
import com.example.data.repository.LectraRepository
import com.example.data.storage.PdfStorageManager
import com.example.ui.LectraViewModel
import com.example.ui.navigation.SubScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfStudyWorkspaceTest {

    private lateinit var app: Application
    private lateinit var db: LectraDatabase
    private lateinit var repository: LectraRepository

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, LectraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LectraRepository(db, app)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testPdfSampleGenerationAndResolution() {
        val testFile = File(app.filesDir, "test_lecture.pdf")
        if (testFile.exists()) testFile.delete()

        // Generate sample PDF document using native Android PdfDocument
        PdfStorageManager.generateSampleLecturePdf(testFile, "Distributed Computing Invariants")

        assertTrue("Generated PDF file must exist", testFile.exists())
        assertTrue("Generated PDF file size must be > 0 bytes", testFile.length() > 0)

        // Resolve bundled URI
        val resolved = PdfStorageManager.resolveFile(app, "lectra://bundled/cs301_raft_consensus.pdf", "Raft Consensus")
        assertNotNull(resolved)
        assertTrue(resolved!!.exists())
        assertTrue(resolved.length() > 0)
    }

    @Test
    fun testPdfAnnotationPersistenceAndCascade() = runTest {
        val courseId = repository.insertCourse(
            CourseEntity(
                code = "CS 301",
                title = "Distributed Systems",
                colorHex = "#7565F6",
                lecturer = "Prof. Rostova",
                semester = "Fall 2026"
            )
        )

        val materialId = repository.insertMaterial(
            LectureMaterialEntity(
                courseId = courseId,
                title = "Raft Consensus Slides.pdf",
                fileUriString = "/mock/path/raft.pdf",
                fileSizeBytes = 2048,
                lastReadPage = 1,
                totalPages = 24
            )
        )

        // Insert Highlight
        val annId1 = repository.insertAnnotation(
            PdfAnnotationEntity(
                materialId = materialId,
                pageIndex = 0,
                type = "HIGHLIGHT",
                colorHex = "#FFEB3B",
                dataJson = "0.1,0.2;0.8,0.2"
            )
        )
        assertTrue(annId1 > 0)

        // Insert Sticky Note
        val annId2 = repository.insertAnnotation(
            PdfAnnotationEntity(
                materialId = materialId,
                pageIndex = 2,
                type = "NOTE",
                colorHex = "#7565F6",
                content = "Exam review: explain split-vote mitigation timeout",
                relativeX = 0.35f,
                relativeY = 0.45f
            )
        )
        assertTrue(annId2 > 0)

        // Insert Bookmark
        val annId3 = repository.insertAnnotation(
            PdfAnnotationEntity(
                materialId = materialId,
                pageIndex = 4,
                type = "BOOKMARK",
                colorHex = "#F59E0B",
                content = "Page 5 Bookmark"
            )
        )
        assertTrue(annId3 > 0)

        // Query annotations for material
        val annotations = repository.getAnnotationsForMaterial(materialId).first()
        assertEquals(3, annotations.size)

        // Query bookmarks specifically
        val bookmarks = repository.getBookmarksForMaterial(materialId).first()
        assertEquals(1, bookmarks.size)
        assertEquals(4, bookmarks[0].pageIndex)

        // Test Reading progress update
        repository.updateReadingProgress(materialId, lastReadPage = 5, totalPages = 24)
        val updatedMaterial = repository.getAllMaterials().first().first { it.id == materialId }
        assertEquals(5, updatedMaterial.lastReadPage)

        // Test bookmark removal
        repository.removeBookmark(materialId, pageIndex = 4)
        val remainingBookmarks = repository.getBookmarksForMaterial(materialId).first()
        assertEquals(0, remainingBookmarks.size)

        // Delete annotation by ID
        repository.deleteAnnotationById(annId1)
        val annotationsAfterDelete = repository.getAnnotationsForMaterial(materialId).first()
        assertEquals(1, annotationsAfterDelete.size)
        assertEquals("NOTE", annotationsAfterDelete[0].type)

        // Test cascade deletion on material delete
        repository.deleteMaterial(updatedMaterial)
        val annotationsAfterMaterialDelete = repository.getAnnotationsForMaterial(materialId).first()
        assertEquals(0, annotationsAfterMaterialDelete.size)
    }

    @Test
    fun testViewModelPdfViewerInteraction() = runTest {
        val viewModel = LectraViewModel(app)

        val material = LectureMaterialEntity(
            id = 99L,
            courseId = 1L,
            title = "Molecular Genetics Notes.pdf",
            fileUriString = "lectra://bundled/bio210_replication.pdf",
            lastReadPage = 3,
            totalPages = 18
        )

        viewModel.openPdfViewer(material)

        val uiState = viewModel.uiState.value
        assertEquals(SubScreen.PDF_VIEWER, uiState.currentSubScreen)
        assertEquals(material.id, uiState.activeReadingMaterial?.id)

        // Test closing subscreen
        val closed = viewModel.closeSubScreen()
        assertTrue(closed)
        assertEquals(SubScreen.NONE, viewModel.uiState.value.currentSubScreen)
    }
}
