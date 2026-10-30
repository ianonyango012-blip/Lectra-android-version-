package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import java.util.Calendar
import java.util.Locale

enum class TimetableSection {
    CLASS_TIMETABLE,
    STUDY_SESSIONS
}

enum class TimetableViewMode {
    DAILY,
    WEEKLY
}

@Composable
fun PlannerScreen(
    classes: List<LectureClassEntity>,
    courses: List<CourseEntity>,
    sessions: List<StudySessionEntity>,
    tasks: List<StudyTaskEntity>,
    onAddClassClick: () -> Unit,
    onEditClass: (LectureClassEntity) -> Unit,
    onDeleteClass: (LectureClassEntity) -> Unit,
    onToggleSession: (StudySessionEntity) -> Unit,
    onDeleteSession: (StudySessionEntity) -> Unit,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onDeleteTask: (StudyTaskEntity) -> Unit,
    onAddSessionClick: () -> Unit,
    onAddTaskClick: () -> Unit
) {
    // Current day of week in Java Calendar: Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6, Sat=7
    val cal = Calendar.getInstance()
    val calDay = cal.get(Calendar.DAY_OF_WEEK)
    val todayNum = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = cal.get(Calendar.MINUTE)
    val currentTimeInMinutes = currentHour * 60 + currentMinute

    // Default to Class Timetable tab as required!
    var activeSection by remember { mutableStateOf(TimetableSection.CLASS_TIMETABLE) }
    var viewMode by remember { mutableStateOf(TimetableViewMode.DAILY) }
    var selectedDay by remember { mutableIntStateOf(todayNum) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val accentViolet = Color(0xFF7565F6)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeSection == TimetableSection.CLASS_TIMETABLE) {
                        onAddClassClick()
                    } else {
                        onAddSessionClick()
                    }
                },
                containerColor = accentViolet,
                contentColor = Color.White,
                modifier = Modifier.testTag(
                    if (activeSection == TimetableSection.CLASS_TIMETABLE) "add_class_fab" else "add_session_fab"
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (activeSection == TimetableSection.CLASS_TIMETABLE) "Add Class" else "Add Study Session"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("planner_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. TOP TIMETABLE NAVIGATION TABS
            // ==========================================
            item(key = "timetable_navigation_tabs") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        TabRow(
                            selectedTabIndex = if (activeSection == TimetableSection.CLASS_TIMETABLE) 0 else 1,
                            containerColor = Color.Transparent,
                            contentColor = accentViolet,
                            divider = {}
                        ) {
                            Tab(
                                selected = activeSection == TimetableSection.CLASS_TIMETABLE,
                                onClick = { activeSection = TimetableSection.CLASS_TIMETABLE },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.School,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Class Timetable",
                                            fontWeight = if (activeSection == TimetableSection.CLASS_TIMETABLE) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("tab_class_timetable")
                            )

                            Tab(
                                selected = activeSection == TimetableSection.STUDY_SESSIONS,
                                onClick = { activeSection = TimetableSection.STUDY_SESSIONS },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Study Sessions",
                                            fontWeight = if (activeSection == TimetableSection.STUDY_SESSIONS) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("tab_study_sessions")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. CLASS TIMETABLE SECTION (DEFAULT)
            // ==========================================
            if (activeSection == TimetableSection.CLASS_TIMETABLE) {
                // View Mode Switcher (Daily vs Weekly)
                item(key = "class_view_mode_selector") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewMode == TimetableViewMode.DAILY) "Daily Schedule" else "Weekly Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == TimetableViewMode.DAILY) accentViolet else Color.Transparent)
                                    .clickable { viewMode = TimetableViewMode.DAILY }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("view_mode_daily")
                            ) {
                                Text(
                                    text = "Daily",
                                    fontSize = 12.sp,
                                    fontWeight = if (viewMode == TimetableViewMode.DAILY) FontWeight.Bold else FontWeight.Normal,
                                    color = if (viewMode == TimetableViewMode.DAILY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == TimetableViewMode.WEEKLY) accentViolet else Color.Transparent)
                                    .clickable { viewMode = TimetableViewMode.WEEKLY }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("view_mode_weekly")
                            ) {
                                Text(
                                    text = "Weekly",
                                    fontSize = 12.sp,
                                    fontWeight = if (viewMode == TimetableViewMode.WEEKLY) FontWeight.Bold else FontWeight.Normal,
                                    color = if (viewMode == TimetableViewMode.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Daily View: Day Selector & Classes for Selected Day
                if (viewMode == TimetableViewMode.DAILY) {
                    item(key = "class_daily_days_row") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_day_selector"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${fullDayNames[selectedDay - 1]} Classes",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val count = classes.count { it.dayOfWeek == selectedDay }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$count ${if (count == 1) "Class" else "Classes"}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    daysOfWeek.forEachIndexed { index, dayName ->
                                        val dayNum = index + 1
                                        val isSelected = selectedDay == dayNum
                                        val classCount = classes.count { it.dayOfWeek == dayNum }
                                        val isToday = dayNum == todayNum

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> accentViolet
                                                        isToday -> accentViolet.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                    }
                                                )
                                                .clickable { selectedDay = dayNum }
                                                .padding(vertical = 10.dp)
                                                .testTag("day_button_$dayNum"),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (classCount > 0) {
                                                            if (isSelected) Color.White else accentViolet
                                                        } else Color.Transparent
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Classes for selected day
                    val dayClasses = classes
                        .filter { it.dayOfWeek == selectedDay }
                        .sortedBy { it.startHour * 60 + it.startMinute }

                    if (dayClasses.isEmpty()) {
                        item(key = "empty_day_classes") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No university classes scheduled for ${fullDayNames[selectedDay - 1]}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = onAddClassClick,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("empty_add_class_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Class")
                                    }
                                }
                            }
                        }
                    } else {
                        items(dayClasses, key = { "class_${it.id}" }) { lectureClass ->
                            val isClassToday = selectedDay == todayNum
                            val startMinutes = lectureClass.startHour * 60 + lectureClass.startMinute
                            val endMinutes = lectureClass.endHour * 60 + lectureClass.endMinute
                            val isInProgress = isClassToday && currentTimeInMinutes in startMinutes..endMinutes
                            val isCompleted = isClassToday && currentTimeInMinutes > endMinutes

                            ClassTimetableCard(
                                lectureClass = lectureClass,
                                courses = courses,
                                isInProgress = isInProgress,
                                isCompleted = isCompleted,
                                isToday = isClassToday,
                                onEdit = { onEditClass(lectureClass) },
                                onDelete = { onDeleteClass(lectureClass) }
                            )
                        }
                    }
                } else {
                    // Weekly Overview: Display all 7 days in order
                    item(key = "weekly_timetable_schedule_marker") {
                        Box(modifier = Modifier.testTag("weekly_timetable_schedule"))
                    }
                    items(daysOfWeek.indices.toList(), key = { "week_day_$it" }) { dayIdx ->
                        val dayNum = dayIdx + 1
                        val dayClasses = classes
                            .filter { it.dayOfWeek == dayNum }
                            .sortedBy { it.startHour * 60 + it.startMinute }
                        val isToday = dayNum == todayNum

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isToday) accentViolet else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = fullDayNames[dayIdx],
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isToday) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = accentViolet,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "TODAY",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${dayClasses.size} ${if (dayClasses.size == 1) "class" else "classes"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (dayClasses.isEmpty()) {
                                    Text(
                                        text = "No classes scheduled",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        dayClasses.forEach { lectureClass ->
                                            WeeklyClassRowItem(
                                                lectureClass = lectureClass,
                                                onEdit = { onEditClass(lectureClass) },
                                                onDelete = { onDeleteClass(lectureClass) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. STUDY SESSIONS SECTION (PRESERVED)
            // ==========================================
            if (activeSection == TimetableSection.STUDY_SESSIONS) {
                // Days of the Week Selector Row for Study Sessions
                item(key = "planner_days_selector") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Weekly Study Sessions",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${sessions.count { it.isCompleted }}/${sessions.size} Done",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                daysOfWeek.forEachIndexed { index, dayName ->
                                    val dayNum = index + 1
                                    val isSelected = selectedDay == dayNum
                                    val count = sessions.count { it.dayOfWeek == dayNum }
                                    val isToday = dayNum == todayNum

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .clickable { selectedDay = dayNum }
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = dayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (count > 0) {
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                    } else Color.Transparent
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Day Sessions Header
                item(key = "planner_schedule_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${fullDayNames[selectedDay - 1]} Study Plan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${sessions.count { it.dayOfWeek == selectedDay }} Sessions scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val selectedDaySessions = sessions.filter { it.dayOfWeek == selectedDay }
                if (selectedDaySessions.isEmpty()) {
                    item(key = "empty_sessions_state") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No study sessions scheduled for ${fullDayNames[selectedDay - 1]}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(selectedDaySessions, key = { "session_${it.id}" }) { session ->
                        StudySessionCard(
                            session = session,
                            onToggle = { onToggleSession(session) },
                            onDelete = { onDeleteSession(session) }
                        )
                    }
                }

                // Action Tasks Section
                item(key = "tasks_header") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Assignments & Deadlines",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onAddTaskClick) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Task",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item(key = "empty_tasks_state") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No pending tasks or deadlines.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(tasks, key = { "task_${it.id}" }) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleTask(task) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${task.courseCode} • Priority: ${task.priority}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteTask(task) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Task",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Class Timetable Card (Daily View)
// ---------------------------------------------------------------------------
@Composable
private fun ClassTimetableCard(
    lectureClass: LectureClassEntity,
    courses: List<CourseEntity>,
    isInProgress: Boolean,
    isCompleted: Boolean,
    isToday: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val course = courses.find { it.id == lectureClass.courseId }
    val courseColor = try {
        Color(android.graphics.Color.parseColor(lectureClass.colorHex.ifBlank { course?.colorHex ?: "#7565F6" }))
    } catch (_: Exception) {
        Color(0xFF7565F6)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .testTag("class_card_${lectureClass.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isInProgress) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Course Code badge, status indicator, action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(courseColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = lectureClass.courseCode.ifBlank { course?.code ?: "COURSE" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = courseColor
                        )
                    }

                    if (isToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        when {
                            isInProgress -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "In Progress",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22C55E)
                                        )
                                    }
                                }
                            }
                            isCompleted -> {
                                Text(
                                    text = "Completed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = "Upcoming",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7565F6)
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp).testTag("edit_class_button_${lectureClass.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Class",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_class_button_${lectureClass.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Class",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Class / Topic Title
            Text(
                text = lectureClass.topicOrLectureName.ifBlank { lectureClass.courseName.ifBlank { course?.title ?: "University Lecture" } },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (lectureClass.courseName.isNotBlank() && lectureClass.courseName != lectureClass.topicOrLectureName) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lectureClass.courseName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timing & Venue Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF7565F6),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val timeRange = String.format(
                        Locale.getDefault(),
                        "%02d:%02d – %02d:%02d",
                        lectureClass.startHour,
                        lectureClass.startMinute,
                        lectureClass.endHour,
                        lectureClass.endMinute
                    )
                    Text(
                        text = timeRange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Venue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lectureClass.venue.ifBlank { "Main Campus" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Weekly Class Row Item
// ---------------------------------------------------------------------------
@Composable
private fun WeeklyClassRowItem(
    lectureClass: LectureClassEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onEdit() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lectureClass.courseCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7565F6)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            "%02d:%02d – %02d:%02d",
                            lectureClass.startHour,
                            lectureClass.startMinute,
                            lectureClass.endHour,
                            lectureClass.endMinute
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lectureClass.topicOrLectureName.ifBlank { lectureClass.courseName },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lectureClass.venue.isNotBlank()) {
                    Text(
                        text = "📍 ${lectureClass.venue}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Study Session Card (Preserved from original PlannerScreen)
// ---------------------------------------------------------------------------
@Composable
private fun StudySessionCard(
    session: StudySessionEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (session.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = if (session.isCompleted) "Mark Incomplete" else "Mark Complete",
                        tint = if (session.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = session.topicTitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (session.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${session.courseCode} • ${String.format("%02d:%02d", session.startHour, session.startMinute)} (${session.durationMinutes}m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (session.targetGoal.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🎯 ${session.targetGoal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
