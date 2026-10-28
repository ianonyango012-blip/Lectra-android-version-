package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LectureClassEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.ui.LectraUiState
import com.example.ui.components.LectraBrandHeader
import com.example.ui.navigation.LectraDestination
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exact LECTRA Dashboard reproducing the visual reference screenshot in Light and Dark modes.
 */
@Composable
fun HomeScreen(
    uiState: LectraUiState,
    onNavigate: (LectraDestination) -> Unit,
    onOpenCourse: (Long) -> Unit,
    onToggleSession: (StudySessionEntity) -> Unit = {},
    onToggleTask: (StudyTaskEntity) -> Unit = {},
    onStartFlashcardStudy: (Long?) -> Unit = {},
    onStartQuiz: (Long?) -> Unit = {},
    onQuickAddCourse: () -> Unit = {},
    onQuickAddSession: () -> Unit = {},
    onQuickAddTask: () -> Unit = {},
    onRequestPdfPicker: () -> Unit = {},
    onOpenPomodoro: (Long?) -> Unit = {},
    onOpenResearch: (String) -> Unit = {},
    onToggleTheme: () -> Unit = {},
    onOpenMaterial: (LectureMaterialEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = when (uiState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Exact palette from specification
    val mainBackground = if (isDark) Color(0xFF0B1020) else Color(0xFFFFFFFF)
    val cardBackground = if (isDark) Color(0xFF111B36) else Color(0xFFFFFFFF)
    val secondaryCardBg = if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0B1020)
    val secondaryText = if (isDark) Color(0xFFB7C3E0) else Color(0xFF64748B)
    val primaryViolet = if (isDark) Color(0xFF806BFF) else Color(0xFF7565F6)
    val borderColor = if (isDark) Color(0xFF293966) else Color(0xFFE3E7F2)

    val prefs = remember { context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE) }
    val studentName = remember(prefs) { prefs.getString("student_name", null)?.takeIf { it.isNotBlank() } }

    val greetingPrefix = when (uiState.currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }
    val greetingText = if (studentName != null) {
        "$greetingPrefix, $studentName ☀️"
    } else {
        "$greetingPrefix ☀️"
    }

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val currentTimeInMinutes = uiState.currentHour * 60 + uiState.currentMinute
    val todayClasses = remember(uiState.classes, uiState.currentDayOfWeek) {
        uiState.classes
            .filter { it.dayOfWeek == uiState.currentDayOfWeek }
            .distinctBy { listOf(it.courseId, it.dayOfWeek, it.startHour, it.startMinute, it.endHour, it.topicOrLectureName) }
            .sortedBy { it.startHour * 60 + it.startMinute }
    }

    // Determine upcoming/in-progress classes
    val activeAndUpcomingClasses = remember(todayClasses, currentTimeInMinutes) {
        todayClasses.filter {
            val endInMinutes = it.endHour * 60 + it.endMinute
            endInMinutes >= currentTimeInMinutes
        }
    }

    // Real recent activity: user imported materials and topics
    val recentMaterials = uiState.materials.take(3)
    val recentTopics = uiState.courseTopics.take(3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBackground)
            .statusBarsPadding()
            .testTag("home_screen_dashboard"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ==========================================
        // 1. DASHBOARD HEADER
        // ==========================================
        item(key = "dashboard_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                // Top Row: Logo Monogram + "Lectra" on left, Theme Toggle on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LectraBrandHeader()

                    // Circular Theme Toggle Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF))
                            .border(1.dp, borderColor, CircleShape)
                            .clickable { onToggleTheme() }
                            .testTag("theme_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeToggleIcon(isDark = isDark, tint = primaryViolet)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Second Row: Greeting with student name & Tagline
                Text(
                    text = greetingText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.testTag("dashboard_greeting_text")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your notes. Your structure. Your success.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = secondaryText
                )
            }
        }

        // ==========================================
        // 2. TODAY'S CLASSES BANNER
        // ==========================================
        item(key = "todays_classes_banner") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigate(LectraDestination.PLANNER) }
                    .testTag("today_classes_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = secondaryCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rounded violet calendar icon box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF233566) else Color(0xFFECE8FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Today's Classes",
                            tint = primaryViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today's Classes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stay on track with your schedule",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = secondaryText
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Timetable",
                        tint = primaryViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ==========================================
        // 3. TODAY TIMETABLE CARDS ROW
        // ==========================================
        item(key = "today_timetable_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Title Row: "Today" on left, Date on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = currentDateStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryText
                    )
                }

                if (todayClasses.isEmpty()) {
                    // Empty timetable state
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No classes scheduled for today",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onNavigate(LectraDestination.PLANNER) },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_timetable_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Timetable", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // Horizontally scrollable row of class cards
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("today_classes_horizontal_row"),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(todayClasses, key = { it.id }) { lectureClass ->
                            val classStartMinutes = lectureClass.startHour * 60 + lectureClass.startMinute
                            val classEndMinutes = lectureClass.endHour * 60 + lectureClass.endMinute
                            val isInProgress = currentTimeInMinutes in classStartMinutes..classEndMinutes
                            val isCompleted = currentTimeInMinutes > classEndMinutes
                            val isNext = !isInProgress && !isCompleted && activeAndUpcomingClasses.firstOrNull()?.id == lectureClass.id

                            val course = uiState.courses.find { it.id == lectureClass.courseId }
                            val courseName = course?.title?.takeIf { it.isNotBlank() }
                                ?: course?.code
                                ?: lectureClass.topicOrLectureName.takeIf { it.isNotBlank() }
                                ?: "Lecture"

                            // Assignment associated with this course
                            val linkedTask = uiState.tasks.find { it.courseId == lectureClass.courseId && !it.isCompleted }

                            ClassCardItem(
                                courseName = courseName,
                                venue = lectureClass.venue.ifBlank { "Campus" },
                                startHour = lectureClass.startHour,
                                startMinute = lectureClass.startMinute,
                                endHour = lectureClass.endHour,
                                endMinute = lectureClass.endMinute,
                                isInProgress = isInProgress,
                                isNext = isNext,
                                isCompleted = isCompleted,
                                linkedTask = linkedTask,
                                isDark = isDark,
                                cardBackground = cardBackground,
                                borderColor = borderColor,
                                primaryViolet = primaryViolet,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { onOpenCourse(lectureClass.courseId) }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. QUICK ACTIONS SECTION
        // ==========================================
        item(key = "quick_actions_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2x2 Grid of Quick Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Import PDF & Add Notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = "Import PDF",
                            subtitle = "Add lecture notes",
                            iconBadgeType = BadgeType.PDF,
                            isDark = isDark,
                            cardBackground = cardBackground,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_import_pdf"),
                            onClick = onRequestPdfPicker
                        )

                        QuickActionTile(
                            title = "Add Notes",
                            subtitle = "Create new notes",
                            iconBadgeType = BadgeType.DOCUMENT,
                            isDark = isDark,
                            cardBackground = cardBackground,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_add_notes"),
                            onClick = onQuickAddCourse
                        )
                    }

                    // Row 2: Study Plan & Research
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionTile(
                            title = "Study Plan",
                            subtitle = "Plan your study",
                            iconBadgeType = BadgeType.CALENDAR,
                            isDark = isDark,
                            cardBackground = cardBackground,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_study_plan"),
                            onClick = { onNavigate(LectraDestination.PLANNER) }
                        )

                        QuickActionTile(
                            title = "Research",
                            subtitle = "Search the web",
                            iconBadgeType = BadgeType.RESEARCH,
                            isDark = isDark,
                            cardBackground = cardBackground,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_research"),
                            onClick = { onOpenResearch("") }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. RECENT ACTIVITY SECTION
        // ==========================================
        item(key = "recent_activity_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = "View all  ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryViolet,
                        modifier = Modifier
                            .clickable { onNavigate(LectraDestination.COURSES) }
                            .testTag("recent_activity_view_all")
                    )
                }

                val hasRecentActivity = recentMaterials.isNotEmpty() || recentTopics.isNotEmpty()

                if (!hasRecentActivity) {
                    // Clean Empty State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your created notes and imported lecture materials will appear here.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = secondaryText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestPdfPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("recent_activity_import_pdf")
                            ) {
                                Text("Import PDF", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Display lecture materials (PDFs)
                        recentMaterials.forEach { material ->
                            val course = uiState.courses.find { it.id == material.courseId }
                            val courseTitle = course?.title ?: course?.code ?: "Lecture Material"
                            val timestampStr = formatTimestamp(material.importDate)

                            RecentActivityCard(
                                title = material.title,
                                courseSubtitle = courseTitle,
                                timestamp = timestampStr,
                                isPdf = true,
                                isDark = isDark,
                                cardBackground = cardBackground,
                                borderColor = borderColor,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { onOpenMaterial(material) }
                            )
                        }

                        // Display note topics
                        recentTopics.forEach { topic ->
                            val course = uiState.courses.find { it.id == topic.courseId }
                            val courseTitle = course?.title ?: course?.code ?: "Course Notes"
                            val timestampStr = "Updated recently"

                            RecentActivityCard(
                                title = topic.title,
                                courseSubtitle = courseTitle,
                                timestamp = timestampStr,
                                isPdf = false,
                                isDark = isDark,
                                cardBackground = cardBackground,
                                borderColor = borderColor,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onClick = { onOpenCourse(topic.courseId) }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. STUDY MOTIVATION CARD
        // ==========================================
        item(key = "study_motivation_card") {
            StudyMotivationCard(
                isDark = isDark,
                borderColor = borderColor,
                primaryViolet = primaryViolet,
                primaryText = primaryText
            )
        }
    }
}

/**
 * Class card item in the horizontal scrollable timetable row.
 */
@Composable
private fun ClassCardItem(
    courseName: String,
    venue: String,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    isInProgress: Boolean,
    isNext: Boolean,
    isCompleted: Boolean,
    linkedTask: StudyTaskEntity?,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryViolet: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    val cardBorder = if (isInProgress) {
        androidx.compose.foundation.BorderStroke(1.5.dp, primaryViolet)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    }

    Card(
        modifier = Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("class_card_$courseName"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = cardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    isInProgress = isInProgress,
                    isNext = isNext,
                    isCompleted = isCompleted,
                    isDark = isDark,
                    primaryViolet = primaryViolet
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Course Name
            Text(
                text = courseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Venue Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = venue,
                    fontSize = 12.sp,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                val timeRangeStr = String.format(
                    Locale.getDefault(),
                    "%d:%02d – %d:%02d %s",
                    if (startHour > 12) startHour - 12 else if (startHour == 0) 12 else startHour,
                    startMinute,
                    if (endHour > 12) endHour - 12 else if (endHour == 0) 12 else endHour,
                    endMinute,
                    if (endHour >= 12) "PM" else "AM"
                )
                Text(
                    text = timeRangeStr,
                    fontSize = 12.sp,
                    color = secondaryText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Assignment Row Container
            val assignmentRowBg = if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(assignmentRowBg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = if (linkedTask != null) primaryViolet else secondaryText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (linkedTask != null) "Assignment due" else "No assignment",
                            fontSize = 11.sp,
                            fontWeight = if (linkedTask != null) FontWeight.Medium else FontWeight.Normal,
                            color = if (linkedTask != null) primaryViolet else secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (linkedTask != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = primaryViolet,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status badge ("In Progress", "Next", "Later", or "Completed").
 */
@Composable
private fun StatusBadge(
    isInProgress: Boolean,
    isNext: Boolean,
    isCompleted: Boolean,
    isDark: Boolean,
    primaryViolet: Color
) {
    val (label, bg, textCol, hasDot, dotCol) = when {
        isInProgress -> Quintuple(
            "In Progress",
            if (isDark) Color(0x337565F6) else Color(0xFFEEF0FE),
            primaryViolet,
            true,
            Color(0xFF22C55E) // Green indicator dot
        )
        isNext -> Quintuple(
            "Next",
            if (isDark) Color(0x2238BDF8) else Color(0xFFE8F4FD),
            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            true,
            Color(0xFF38BDF8)
        )
        isCompleted -> Quintuple(
            "Completed",
            if (isDark) Color(0x2264748B) else Color(0xFFF1F5F9),
            Color(0xFF64748B),
            false,
            Color.Transparent
        )
        else -> Quintuple(
            "Later",
            if (isDark) Color(0x2264748B) else Color(0xFFF1F5F9),
            if (isDark) Color(0xFFB7C3E0) else Color(0xFF64748B),
            false,
            Color.Transparent
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotCol)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textCol
            )
        }
    }
}

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

enum class BadgeType {
    PDF, DOCUMENT, CALENDAR, RESEARCH
}

/**
 * Quick action tile in the 2x2 grid.
 */
@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    iconBadgeType: BadgeType,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge Box
            QuickActionIconBadge(type = iconBadgeType, isDark = isDark)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Pastel icon container for quick action tiles matching the screenshot.
 */
@Composable
private fun QuickActionIconBadge(type: BadgeType, isDark: Boolean) {
    when (type) {
        BadgeType.PDF -> {
            // Reddish / Pink rounded container with "PDF" text inside
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF381C26) else Color(0xFFFFECEF)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        BadgeType.DOCUMENT -> {
            // Violet rounded container with document lines
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF261E47) else Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF9B8AFF) else Color(0xFF7565F6),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        BadgeType.CALENDAR -> {
            // Mint / Green rounded container with calendar icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF143328) else Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        BadgeType.RESEARCH -> {
            // Soft violet/blue rounded container with search icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1F2252) else Color(0xFFE0E7FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Recent Activity Card matching the screenshot.
 */
@Composable
private fun RecentActivityCard(
    title: String,
    courseSubtitle: String,
    timestamp: String,
    isPdf: Boolean,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("recent_activity_card_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            if (isPdf) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF381C26) else Color(0xFFFFECEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PDF",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E2850) else Color(0xFFE0E7FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = courseSubtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timestamp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = secondaryText
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Study Motivation Card at the bottom of the dashboard.
 */
@Composable
private fun StudyMotivationCard(
    isDark: Boolean,
    borderColor: Color,
    primaryViolet: Color,
    primaryText: Color
) {
    val cardBg = if (isDark) Color(0xFF111B36) else Color(0xFFF5F3FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("study_motivation_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle wave background Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val wavePath = Path().apply {
                    moveTo(w * 0.45f, h)
                    cubicTo(w * 0.6f, h * 0.7f, w * 0.75f, 0f, w, 0f)
                    lineTo(w, h)
                    close()
                }
                val waveColor = if (isDark) Color(0x2217244A) else Color(0x33EDE9FE)
                drawPath(wavePath, color = waveColor)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular violet graduation cap badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(primaryViolet),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Motivation",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "“Small steps every day lead to big results.”",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = primaryText,
                    modifier = Modifier.weight(1f)
                )

                // 3 Understated Carousel Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(primaryViolet)
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(primaryViolet.copy(alpha = 0.35f))
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(primaryViolet.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

/**
 * Sun / Moon icon toggle button.
 */
@Composable
private fun ThemeToggleIcon(isDark: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f

        if (!isDark) {
            // Sun icon: central circle + rays
            drawCircle(
                color = tint,
                radius = radius * 0.42f,
                center = center
            )
            val rayCount = 8
            val innerR = radius * 0.65f
            val outerR = radius * 0.95f
            for (i in 0 until rayCount) {
                val angle = (i * (360f / rayCount)) * (Math.PI / 180.0)
                val cos = Math.cos(angle).toFloat()
                val sin = Math.sin(angle).toFloat()
                drawLine(
                    color = tint,
                    start = Offset(center.x + innerR * cos, center.y + innerR * sin),
                    end = Offset(center.x + outerR * cos, center.y + outerR * sin),
                    strokeWidth = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        } else {
            // Moon icon: crescent moon
            val moonPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - radius * 0.85f,
                        center.y - radius * 0.85f,
                        center.x + radius * 0.85f,
                        center.y + radius * 0.85f
                    ),
                    startAngleDegrees = -90f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
                cubicTo(
                    center.x - radius * 0.1f, center.y + radius * 0.7f,
                    center.x - radius * 0.1f, center.y - radius * 0.7f,
                    center.x, center.y - radius * 0.85f
                )
                close()
            }
            drawPath(moonPath, color = tint)
        }
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val diff = System.currentTimeMillis() - timestampMillis
    val oneDay = 24 * 60 * 60 * 1000L
    return when {
        diff < oneDay -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Today, ${sdf.format(Date(timestampMillis))}"
        }
        diff < 2 * oneDay -> {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday, ${sdf.format(Date(timestampMillis))}"
        }
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestampMillis))
        }
    }
}
