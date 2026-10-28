package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.graphics.vector.ImageVector

enum class LectraDestination(
    val title: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    COURSES("Notes", "Notes", Icons.Filled.Description, Icons.Outlined.Description),
    PLANNER("Timetable", "Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    REVISION("Flashcards", "Flashcards", Icons.Filled.Style, Icons.Outlined.Style),
    SEARCH("Google Search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

enum class SubScreen {
    NONE,
    COURSE_DETAIL,
    FLASHCARD_STUDY,
    QUIZ_STUDY,
    POMODORO_TIMER,
    GLOBAL_SEARCH,
    RESEARCH
}
