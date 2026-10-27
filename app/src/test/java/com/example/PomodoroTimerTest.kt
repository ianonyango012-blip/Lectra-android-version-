package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CourseEntity
import com.example.ui.LectraUiState
import com.example.ui.LectraViewModel
import com.example.ui.PomodoroMode
import com.example.ui.screens.PomodoroTimerScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w390dp-h844dp")
class PomodoroTimerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCourses = listOf(
        CourseEntity(id = 1L, code = "CS101", title = "Introduction to Computer Science", colorHex = "#7565F6", lecturer = "Dr. Turing", semester = "Year 1"),
        CourseEntity(id = 2L, code = "MATH201", title = "Linear Algebra", colorHex = "#16A34A", lecturer = "Prof. Gauss", semester = "Year 1")
    )

    @Test
    fun `PomodoroTimerScreen renders course chips, countdown dial, and control buttons`() {
        var startClicked = false
        var stopClicked = false
        var selectedCourseId = 1L

        val uiState = LectraUiState(
            courses = sampleCourses,
            selectedTimerCourseId = 1L,
            timerMode = PomodoroMode.FOCUS,
            timerTotalSeconds = 25 * 60,
            timerRemainingSeconds = 25 * 60,
            isTimerRunning = false,
            completedPomodoroCount = 2,
            timerSessionGoal = "Chapter 4 Algorithms"
        )

        composeTestRule.setContent {
            PomodoroTimerScreen(
                uiState = uiState,
                onBackClick = {},
                onSelectCourse = { selectedCourseId = it },
                onSetGoal = {},
                onSetMode = { _, _ -> },
                onStartTimer = { startClicked = true },
                onPauseTimer = {},
                onStopAndLog = { stopClicked = true },
                onResetTimer = {},
                onSkipMode = {}
            )
        }

        // Verify Header and Completed Badge
        composeTestRule.onNodeWithText("Pomodoro Timer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomodoro_streak_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 Completed").assertIsDisplayed()

        // Verify Course Chip
        composeTestRule.onNodeWithTag("pomodoro_course_chip_1").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("CS101")[0].assertIsDisplayed()

        // Verify Timer Display (25:00) exists and is displayed when scrolled
        composeTestRule.onNodeWithTag("pomodoro_timer_display").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("25:00").assertExists()

        // Verify Goal and Course
        composeTestRule.onNodeWithTag("pomodoro_goal_input").assertExists()

        // Verify Mode Chips
        composeTestRule.onNodeWithTag("pomodoro_mode_focus").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomodoro_mode_short_break").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomodoro_mode_long_break").performScrollTo().assertIsDisplayed()

        // Verify Controls
        composeTestRule.onNodeWithTag("pomodoro_play_pause_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Focus").assertExists()
        composeTestRule.onNodeWithTag("pomodoro_stop_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomodoro_reset_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("pomodoro_skip_button").performScrollTo().assertIsDisplayed()

        // Click Start Button
        composeTestRule.onNodeWithTag("pomodoro_play_pause_button").performClick()
        assertTrue("Start focus button must trigger start callback", startClicked)

        // Click Stop Button
        composeTestRule.onNodeWithTag("pomodoro_stop_button").performClick()
        assertTrue("Stop button must trigger stop callback", stopClicked)
    }

    @Test
    fun `test ViewModel Pomodoro start, pause, reset, and mode switching`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LectraViewModel(app)

        // 1. Initial State
        val initialState = viewModel.uiState.value
        assertEquals(PomodoroMode.FOCUS, initialState.timerMode)
        assertEquals(25 * 60, initialState.timerTotalSeconds)
        assertEquals(25 * 60, initialState.timerRemainingSeconds)
        assertFalse(initialState.isTimerRunning)

        // 2. Select Course
        viewModel.selectTimerCourse(100L)
        assertEquals(100L, viewModel.uiState.value.selectedTimerCourseId)

        // 3. Set Goal
        viewModel.setTimerSessionGoal("Distributed Systems Lecture 3")
        assertEquals("Distributed Systems Lecture 3", viewModel.uiState.value.timerSessionGoal)

        // 4. Switch to Short Break Mode
        viewModel.setTimerMode(PomodoroMode.SHORT_BREAK)
        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.timerMode)
        assertEquals(5 * 60, viewModel.uiState.value.timerTotalSeconds)
        assertEquals(5 * 60, viewModel.uiState.value.timerRemainingSeconds)

        // 5. Switch back to Focus Mode with custom 50m duration
        viewModel.setTimerMode(PomodoroMode.FOCUS, 50)
        assertEquals(PomodoroMode.FOCUS, viewModel.uiState.value.timerMode)
        assertEquals(50 * 60, viewModel.uiState.value.timerTotalSeconds)
        assertEquals(50 * 60, viewModel.uiState.value.timerRemainingSeconds)

        // 6. Start Timer
        viewModel.startPomodoroTimer()
        assertTrue("Timer should be marked as running", viewModel.uiState.value.isTimerRunning)

        // 7. Pause Timer
        viewModel.pausePomodoroTimer()
        assertFalse("Timer should be paused", viewModel.uiState.value.isTimerRunning)

        // 8. Reset Timer
        viewModel.resetPomodoroTimer()
        assertFalse(viewModel.uiState.value.isTimerRunning)
        assertEquals(50 * 60, viewModel.uiState.value.timerRemainingSeconds)

        // 9. Stop and log
        viewModel.stopAndLogPomodoroSession()
        assertFalse(viewModel.uiState.value.isTimerRunning)
    }
}
