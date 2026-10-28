package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.CourseEntity
import com.example.data.model.LectureMaterialEntity
import com.example.ui.screens.CourseListScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CourseListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows empty state when no courses exist`() {
        var addCourseClicked = false

        composeTestRule.setContent {
            CourseListScreen(
                courses = emptyList(),
                materials = emptyList(),
                onCourseClick = {},
                onAddCourseClick = { addCourseClicked = true },
                onDeleteCourse = {}
            )
        }

        composeTestRule.onNodeWithTag("course_list_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your courses start here").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_course_fab").assertIsDisplayed()

        // Clicking empty state add button triggers callback
        composeTestRule.onNodeWithTag("empty_add_course_button").performClick()
        assertTrue(addCourseClicked)
    }

    @Test
    fun `shows course list with cards and triggers details`() {
        val testCourses = listOf(
            CourseEntity(
                id = 101L,
                code = "CS301",
                title = "Algorithms & Data Structures",
                colorHex = "#F59E0B",
                lecturer = "Dr. Alan Turing",
                semester = "Year 2 Sem 1",
                targetWeeklyHours = 8
            ),
            CourseEntity(
                id = 102L,
                code = "MATH204",
                title = "Linear Algebra",
                colorHex = "#3B82F6",
                lecturer = "Prof. Euler",
                semester = "Year 2 Sem 1",
                targetWeeklyHours = 6
            )
        )

        val testMaterials = listOf(
            LectureMaterialEntity(
                id = 1L,
                courseId = 101L,
                title = "Week 1 - Graph Theory.pdf",
                fileUriString = "content://dummy/path.pdf"
            )
        )

        var clickedCourseId: Long? = null
        var addCourseClicked = false

        composeTestRule.setContent {
            CourseListScreen(
                courses = testCourses,
                materials = testMaterials,
                onCourseClick = { clickedCourseId = it },
                onAddCourseClick = { addCourseClicked = true },
                onDeleteCourse = {}
            )
        }

        // Verify grid and header are displayed
        composeTestRule.onNodeWithTag("course_grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("My Courses").assertIsDisplayed()

        // Verify first card
        composeTestRule.onNodeWithTag("course_card_CS301").assertIsDisplayed()
        composeTestRule.onNodeWithText("CS301").assertIsDisplayed()
        composeTestRule.onNodeWithText("Algorithms & Data Structures").assertIsDisplayed()

        // Verify clicking the card invokes onCourseClick
        composeTestRule.onNodeWithTag("course_card_CS301").performClick()
        assertEquals(101L, clickedCourseId)

        // Verify FAB click
        composeTestRule.onNodeWithTag("add_course_fab").performClick()
        assertTrue(addCourseClicked)
    }
}
