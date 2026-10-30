package com.example.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LectraPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicPreferencesScreen(
    preferences: LectraPreferences,
    onBackClick: () -> Unit
) {
    val primaryViolet = Color(0xFF7565F6)

    // State for notifications
    var notifClasses by remember { mutableStateOf(preferences.notifyClasses) }
    var notifAssignments by remember { mutableStateOf(preferences.notifyAssignments) }
    var notifSessions by remember { mutableStateOf(preferences.notifySessions) }
    var notifRevision by remember { mutableStateOf(preferences.notifyRevision) }
    var notifLeadTime by remember { mutableIntStateOf(preferences.notificationLeadTimeMinutes) }

    // State for timetable
    var firstDayOfWeek by remember { mutableIntStateOf(preferences.timetableFirstDayOfWeek) }
    var classReminderTime by remember { mutableIntStateOf(preferences.timetableClassReminderMinutes) }
    var defaultTimetableView by remember { mutableStateOf(preferences.timetableDefaultViewMode) }

    // State for reading
    var pdfOpeningMode by remember { mutableStateOf(preferences.pdfOpeningMode) }
    var pdfNightMode by remember { mutableStateOf(preferences.pdfNightReadingMode) }
    var pdfPageDisplay by remember { mutableStateOf(preferences.pdfDefaultPageDisplay) }
    var pdfAutoSave by remember { mutableStateOf(preferences.pdfAutoSaveAnnotations) }

    // State for revision
    var flashcardOrder by remember { mutableStateOf(preferences.flashcardReviewOrder) }
    var quizDifficulty by remember { mutableStateOf(preferences.quizDifficultyFilter) }
    var revisionDuration by remember { mutableIntStateOf(preferences.revisionDurationMinutes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Academic Preferences",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("academic_prefs_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("academic_preferences_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. NOTIFICATIONS SECTION
            // ==========================================
            item(key = "academic_notifications_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Notifications & Reminders",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configure proactive study and deadline alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggles
                        PreferenceToggleRow(
                            title = "Upcoming Classes",
                            subtitle = "Alert before university lecture and lab sessions",
                            checked = notifClasses,
                            testTag = "toggle_notif_classes",
                            onCheckedChange = {
                                notifClasses = it
                                preferences.notifyClasses = it
                            }
                        )

                        PreferenceToggleRow(
                            title = "Assignment Deadlines",
                            subtitle = "Reminder before coursework and project submission dates",
                            checked = notifAssignments,
                            testTag = "toggle_notif_assignments",
                            onCheckedChange = {
                                notifAssignments = it
                                preferences.notifyAssignments = it
                            }
                        )

                        PreferenceToggleRow(
                            title = "Study Session Reminders",
                            subtitle = "Notification when scheduled personal study time arrives",
                            checked = notifSessions,
                            testTag = "toggle_notif_sessions",
                            onCheckedChange = {
                                notifSessions = it
                                preferences.notifySessions = it
                            }
                        )

                        PreferenceToggleRow(
                            title = "Revision & Active Recall",
                            subtitle = "Spaced repetition reminders for saved flashcard decks",
                            checked = notifRevision,
                            testTag = "toggle_notif_revision",
                            onCheckedChange = {
                                notifRevision = it
                                preferences.notifyRevision = it
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reminder timing
                        Text(
                            text = "Default Reminder Timing:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 15, 30, 60).forEach { mins ->
                                val label = if (mins >= 60) "1 hour before" else "$mins min before"
                                FilterChip(
                                    selected = notifLeadTime == mins,
                                    onClick = {
                                        notifLeadTime = mins
                                        preferences.notificationLeadTimeMinutes = mins
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Android 13+ requires notification permissions for system alert banners. Reminders operate entirely on-device without network telemetry.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. TIMETABLE PREFERENCES SECTION
            // ==========================================
            item(key = "academic_timetable_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Timetable Preferences",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Class Timetable schedule and viewing options",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // First day of week
                        Text(
                            text = "First Day of the Week:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = firstDayOfWeek == 1,
                                onClick = {
                                    firstDayOfWeek = 1
                                    preferences.timetableFirstDayOfWeek = 1
                                },
                                label = { Text("Monday (Default)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = firstDayOfWeek == 7,
                                onClick = {
                                    firstDayOfWeek = 7
                                    preferences.timetableFirstDayOfWeek = 7
                                },
                                label = { Text("Sunday", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Default Timetable View
                        Text(
                            text = "Default Timetable Layout:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = defaultTimetableView == "DAILY",
                                onClick = {
                                    defaultTimetableView = "DAILY"
                                    preferences.timetableDefaultViewMode = "DAILY"
                                },
                                label = { Text("Daily Schedule", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = defaultTimetableView == "WEEKLY",
                                onClick = {
                                    defaultTimetableView = "WEEKLY"
                                    preferences.timetableDefaultViewMode = "WEEKLY"
                                },
                                label = { Text("Weekly Overview", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Timetable preferences apply directly to your Class Timetable without modifying class records. Class Timetable and Study Sessions remain organized separately.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 3. READING PREFERENCES SECTION
            // ==========================================
            item(key = "academic_reading_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PDF Reading Preferences",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Viewing mode & annotation defaults",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default PDF Reader
                        PreferenceToggleRow(
                            title = "Built-in PDF Study Workspace",
                            subtitle = "Open lecture PDFs directly inside LECTRA reader with note-taking tools",
                            checked = pdfOpeningMode == "built_in",
                            testTag = "toggle_pdf_builtin",
                            onCheckedChange = {
                                pdfOpeningMode = if (it) "built_in" else "external"
                                preferences.pdfOpeningMode = pdfOpeningMode
                            }
                        )

                        PreferenceToggleRow(
                            title = "Auto-Save Annotations",
                            subtitle = "Automatically save highlights and sticky notes while reading",
                            checked = pdfAutoSave,
                            testTag = "toggle_pdf_autosave",
                            onCheckedChange = {
                                pdfAutoSave = it
                                preferences.pdfAutoSaveAnnotations = it
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Night reading preference
                        Text(
                            text = "PDF Night Reading Palette:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "STANDARD" to "Standard",
                                "INVERTED" to "Dark Invert",
                                "SEPIA" to "Warm Sepia"
                            ).forEach { (modeKey, modeTitle) ->
                                FilterChip(
                                    selected = pdfNightMode == modeKey,
                                    onClick = {
                                        pdfNightMode = modeKey
                                        preferences.pdfNightReadingMode = modeKey
                                    },
                                    label = { Text(modeTitle, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Original formatting, formulas, vector graphics, and lecturer layout in imported PDFs are preserved unchanged.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ==========================================
            // 4. REVISION PREFERENCES SECTION
            // ==========================================
            item(key = "academic_revision_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Style,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Revision & Active Recall",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Flashcard deck & quiz session defaults",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Flashcard Review Order
                        Text(
                            text = "Flashcard Review Order:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "SHUFFLE" to "Smart Shuffle",
                                "CHRONOLOGICAL" to "Sequential",
                                "PRIORITY" to "Needs Practice"
                            ).forEach { (orderKey, orderTitle) ->
                                FilterChip(
                                    selected = flashcardOrder == orderKey,
                                    onClick = {
                                        flashcardOrder = orderKey
                                        preferences.flashcardReviewOrder = orderKey
                                    },
                                    label = { Text(orderTitle, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quiz Question Difficulty
                        Text(
                            text = "Practice Question Scope:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "ALL" to "All Questions",
                                "MODERATE" to "Moderate & High",
                                "CHALLENGING" to "Challenging Only"
                            ).forEach { (diffKey, diffTitle) ->
                                FilterChip(
                                    selected = quizDifficulty == diffKey,
                                    onClick = {
                                        quizDifficulty = diffKey
                                        preferences.quizDifficultyFilter = diffKey
                                    },
                                    label = { Text(diffTitle, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Revision timer default
                        Text(
                            text = "Default Focus Session Duration:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 25, 45).forEach { mins ->
                                FilterChip(
                                    selected = revisionDuration == mins,
                                    onClick = {
                                        revisionDuration = mins
                                        preferences.revisionDurationMinutes = mins
                                    },
                                    label = { Text("$mins Minutes", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryViolet,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val primaryViolet = Color(0xFF7565F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = primaryViolet,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
