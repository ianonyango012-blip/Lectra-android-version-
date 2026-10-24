package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class LectraDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    COURSES("My Courses", Icons.Filled.Book, Icons.Outlined.Book),
    PLANNER("Planner", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    REVISION("Revision", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

enum class SubScreen {
    NONE,
    COURSE_DETAIL,
    FLASHCARD_STUDY,
    QUIZ_STUDY
}
