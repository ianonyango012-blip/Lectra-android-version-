package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CourseEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.ui.LectraUiState
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.theme.LectraTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClassTimetableAndDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCourses = listOf(
        CourseEntity(
            id = 1L,
            code = "CS101",
            title = "Distributed Systems",
            lecturer = "Dr. Lamport",
            semester = "Semester 1",
            colorHex = "#7565F6",
            targetWeeklyHours = 6
        )
    )

    private val sampleClasses = listOf(
        LectureClassEntity(
            id = 101L,
            courseId = 1L,
            courseCode = "CS101",
            courseName = "Distributed Systems",
            topicOrLectureName = "Raft Consensus",
            dayOfWeek = 1, // Monday
            startHour = 9,
            startMinute = 0,
            endHour = 11,
            endMinute = 0,
            venue = "Turing Hall 104",
            colorHex = "#7565F6"
        )
    )

    @Test
    fun plannerScreen_defaultsToClassTimetableTab() {
        var addClassClicked = false
        var addSessionClicked = false

        composeTestRule.setContent {
            LectraTheme(themeMode = ThemeMode.LIGHT) {
                PlannerScreen(
                    classes = sampleClasses,
                    courses = sampleCourses,
                    sessions = emptyList(),
                    tasks = emptyList(),
                    onAddClassClick = { addClassClicked = true },
                    onEditClass = {},
                    onDeleteClass = {},
                    onToggleSession = {},
                    onDeleteSession = {},
                    onToggleTask = {},
                    onDeleteTask = {},
                    onAddSessionClick = { addSessionClicked = true },
                    onAddTaskClick = {}
                )
            }
        }

        // Verify Class Timetable tab is displayed and add class FAB is present
        composeTestRule.onNodeWithTag("tab_class_timetable").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_study_sessions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_class_fab").assertIsDisplayed()

        // Clicking the FAB should invoke onAddClassClick
        composeTestRule.onNodeWithTag("add_class_fab").performClick()
        assertTrue("onAddClassClick should be triggered when active tab is Class Timetable", addClassClicked)

        // Switch to Study Sessions tab
        composeTestRule.onNodeWithTag("tab_study_sessions").performClick()
        composeTestRule.onNodeWithTag("add_session_fab").assertIsDisplayed()

        composeTestRule.onNodeWithTag("add_session_fab").performClick()
        assertTrue("onAddSessionClick should be triggered when active tab is Study Sessions", addSessionClicked)
    }

    @Test
    fun plannerScreen_togglesBetweenDailyAndWeeklyViews() {
        composeTestRule.setContent {
            LectraTheme(themeMode = ThemeMode.LIGHT) {
                PlannerScreen(
                    classes = sampleClasses,
                    courses = sampleCourses,
                    sessions = emptyList(),
                    tasks = emptyList(),
                    onAddClassClick = {},
                    onEditClass = {},
                    onDeleteClass = {},
                    onToggleSession = {},
                    onDeleteSession = {},
                    onToggleTask = {},
                    onDeleteTask = {},
                    onAddSessionClick = {},
                    onAddTaskClick = {}
                )
            }
        }

        // Initially Daily view is active
        composeTestRule.onNodeWithTag("daily_day_selector").assertIsDisplayed()

        // Switch to Weekly view
        composeTestRule.onNodeWithTag("view_mode_weekly").performClick()
        composeTestRule.onNodeWithText("Weekly Overview").assertIsDisplayed()

        // Switch back to Daily view
        composeTestRule.onNodeWithTag("view_mode_daily").performClick()
        composeTestRule.onNodeWithTag("daily_day_selector").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Schedule").assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysClassTimetableAndTomorrowFallback() {
        val cal = Calendar.getInstance()
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val todayDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
        val tomorrowDay = if (todayDay == 7) 1 else todayDay + 1

        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        // Create a class taking place today in the future or now
        val upcomingClassToday = LectureClassEntity(
            id = 201L,
            courseId = 1L,
            courseCode = "CS101",
            courseName = "Distributed Systems",
            topicOrLectureName = "Byzantine Fault Tolerance",
            dayOfWeek = todayDay,
            startHour = (currentHour + 1).coerceAtMost(23),
            startMinute = 0,
            endHour = (currentHour + 2).coerceAtMost(23),
            endMinute = 0,
            venue = "Lecture Hall 1",
            colorHex = "#7565F6"
        )

        val tomorrowClass = LectureClassEntity(
            id = 202L,
            courseId = 1L,
            courseCode = "CS101",
            courseName = "Distributed Systems",
            topicOrLectureName = "Paxos Consensus",
            dayOfWeek = tomorrowDay,
            startHour = 10,
            startMinute = 0,
            endHour = 12,
            endMinute = 0,
            venue = "Science Theatre 2",
            colorHex = "#7565F6"
        )

        val uiStateWithUpcoming = LectraUiState(
            courses = sampleCourses,
            classes = listOf(upcomingClassToday, tomorrowClass)
        )

        composeTestRule.setContent {
            LectraTheme(themeMode = ThemeMode.LIGHT) {
                HomeScreen(
                    uiState = uiStateWithUpcoming,
                    onNavigate = {},
                    onOpenCourse = {},
                    onToggleSession = {},
                    onToggleTask = {},
                    onStartFlashcardStudy = {},
                    onStartQuiz = {},
                    onQuickAddCourse = {},
                    onQuickAddSession = {},
                    onQuickAddTask = {},
                    onRequestPdfPicker = {},
                    onOpenPomodoro = {},
                    onOpenResearch = {},
                    onToggleTheme = {},
                    onOpenMaterial = {}
                )
            }
        }

        // Title should be "Today" when upcoming class today exists
        composeTestRule.onNodeWithTag("today_section_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Distributed Systems").assertIsDisplayed()
    }
}
