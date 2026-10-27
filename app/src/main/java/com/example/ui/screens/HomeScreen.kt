package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.ui.LectraUiState
import com.example.ui.navigation.LectraDestination
import com.example.ui.theme.CampusGreen
import com.example.ui.theme.CampusRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: LectraUiState,
    onNavigate: (LectraDestination) -> Unit,
    onOpenCourse: (Long) -> Unit,
    onToggleSession: (StudySessionEntity) -> Unit,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onStartFlashcardStudy: (Long?) -> Unit,
    onStartQuiz: (Long?) -> Unit,
    onQuickAddCourse: () -> Unit,
    onQuickAddSession: () -> Unit,
    onQuickAddTask: () -> Unit,
    onRequestPdfPicker: () -> Unit,
    onOpenPomodoro: (Long?) -> Unit = {}
) {
    val currentTimeInMinutes = uiState.currentHour * 60 + uiState.currentMinute
    val todayDayOfWeek = uiState.currentDayOfWeek

    // 1. Classes scheduled for the current day in chronological order (deduplicated)
    val allTodayClasses = uiState.classes
        .filter { it.dayOfWeek == todayDayOfWeek }
        .distinctBy { listOf(it.courseId, it.dayOfWeek, it.startHour, it.startMinute, it.endHour, it.topicOrLectureName) }
        .sortedBy { it.startHour * 60 + it.startMinute }

    // 2. Active or upcoming classes today (end time has NOT passed)
    val upcomingTodayClasses = allTodayClasses.filter {
        val classEndInMinutes = it.endHour * 60 + it.endMinute
        classEndInMinutes >= currentTimeInMinutes
    }

    // 3. Past classes today (for optional collapsible reference)
    val passedTodayClasses = allTodayClasses.filter {
        val classEndInMinutes = it.endHour * 60 + it.endMinute
        classEndInMinutes < currentTimeInMinutes
    }

    // 4. Find NEXT scheduled day with classes if no classes remain today
    val nextScheduledDayInfo = remember(uiState.classes, todayDayOfWeek, upcomingTodayClasses.isEmpty()) {
        if (upcomingTodayClasses.isEmpty() && uiState.classes.isNotEmpty()) {
            var foundDayNum: Int? = null
            var foundClasses: List<LectureClassEntity> = emptyList()
            for (offset in 1..6) {
                val candidateDay = ((todayDayOfWeek - 1 + offset) % 7) + 1
                val candidateClasses = uiState.classes
                    .filter { it.dayOfWeek == candidateDay }
                    .distinctBy { listOf(it.courseId, it.dayOfWeek, it.startHour, it.startMinute, it.endHour, it.topicOrLectureName) }
                    .sortedBy { it.startHour * 60 + it.startMinute }
                if (candidateClasses.isNotEmpty()) {
                    foundDayNum = candidateDay
                    foundClasses = candidateClasses
                    break
                }
            }
            if (foundDayNum != null) Pair(foundDayNum, foundClasses) else null
        } else null
    }

    // 5. Academic Study & Revision Computed Metrics
    val todayPlannedSessions = uiState.sessions.filter { it.dayOfWeek == todayDayOfWeek }
    val todayCompletedSessions = todayPlannedSessions.count { it.isCompleted }

    val unreadMaterials = uiState.materials.filter { it.lastReadPage < it.totalPages }
    val unmasteredTopicsWithNotes = uiState.courseTopics.filter { !it.isMastered && it.summaryNotes.isNotBlank() }
    val pendingPracticeQuestions = uiState.quizzes.filter { !it.hasAnswered }
    val upcomingAssessments = uiState.tasks.filter { !it.isCompleted }
        .sortedWith(compareBy({ it.dueDate == null }, { it.dueDate }))

    val weakRevisionCards = uiState.flashcards.filter {
        it.lastRecallGrade == "AGAIN" || it.lastRecallGrade == "HARD" || it.repetitions == 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_timetable_scroll"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. LECTRA Header & Today's Date & Lecture Timetable Summary
        item(key = "home_live_header") {
            TodayLiveHeader(
                dayOfWeekNum = todayDayOfWeek,
                currentHour = uiState.currentHour,
                currentMinute = uiState.currentMinute,
                totalCourses = uiState.courses.size,
                activeClassCount = upcomingTodayClasses.size
            )
        }

        // 1.5 Pomodoro Focus Session Quick Card
        item(key = "home_pomodoro_quick_card") {
            PomodoroQuickCard(
                isRunning = uiState.isTimerRunning,
                remainingSeconds = uiState.timerRemainingSeconds,
                completedCount = uiState.completedPomodoroCount,
                selectedCourse = uiState.courses.find { it.id == uiState.selectedTimerCourseId } ?: uiState.courses.firstOrNull(),
                onOpenPomodoro = { onOpenPomodoro(null) }
            )
        }

        // 2 & 3. Today's Scheduled Classes Timetable
        item(key = "home_timetable_header") {
            TimetableSectionHeader(
                hasClassesToday = upcomingTodayClasses.isNotEmpty(),
                onAddClass = onQuickAddSession
            )
        }

        if (upcomingTodayClasses.isNotEmpty()) {
            items(upcomingTodayClasses, key = { "home_class_${it.id}" }) { lectureClass ->
                val classStartInMinutes = lectureClass.startHour * 60 + lectureClass.startMinute
                val classEndInMinutes = lectureClass.endHour * 60 + lectureClass.endMinute
                val isActiveNow = currentTimeInMinutes in classStartInMinutes..classEndInMinutes

                val classTasks = uiState.tasks.filter {
                    it.lectureClassId == lectureClass.id ||
                            (it.courseId == lectureClass.courseId && it.topicTitle.equals(lectureClass.topicOrLectureName, ignoreCase = true))
                }.distinctBy { it.id }

                ClassScheduleCard(
                    lectureClass = lectureClass,
                    isActiveNow = isActiveNow,
                    tasks = classTasks,
                    onToggleTask = onToggleTask,
                    onOpenCourse = { onOpenCourse(lectureClass.courseId) }
                )
            }
        } else {
            item(key = "home_no_classes_banner") {
                NoClassesTodayBanner(
                    allTodayClassesCount = allTodayClasses.size,
                    passedTodayCount = passedTodayClasses.size,
                    nextDayInfo = nextScheduledDayInfo
                )
            }

            if (nextScheduledDayInfo != null) {
                val (nextDayNum, nextClasses) = nextScheduledDayInfo
                item(key = "home_next_classes_header") {
                    NextScheduledClassesHeader(nextDayNum = nextDayNum, classCount = nextClasses.size)
                }
                items(nextClasses, key = { "home_next_class_${it.id}" }) { lectureClass ->
                    val classTasks = uiState.tasks.filter {
                        it.lectureClassId == lectureClass.id ||
                                (it.courseId == lectureClass.courseId && it.topicTitle.equals(lectureClass.topicOrLectureName, ignoreCase = true))
                    }.distinctBy { it.id }
                    ClassScheduleCard(
                        lectureClass = lectureClass,
                        isActiveNow = false,
                        tasks = classTasks,
                        onToggleTask = onToggleTask,
                        onOpenCourse = { onOpenCourse(lectureClass.courseId) }
                    )
                }
            }
        }

        // 4. Today's Study Progress
        item(key = "home_progress_header") {
            Spacer(modifier = Modifier.height(20.dp))
            SectionDividerTitle(
                title = "Today's Study Progress",
                subtitle = "Logged sessions & term momentum"
            )
        }

        item(key = "home_progress_card") {
            TodayStudyProgressCard(
                plannedCount = todayPlannedSessions.size,
                completedCount = todayCompletedSessions,
                todaySessions = todayPlannedSessions,
                onToggleSession = onToggleSession,
                onAddSession = onQuickAddSession
            )
        }

        // 5. Individual Course Progress
        item(key = "home_syllabus_header") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionDividerTitle(
                title = "Course Syllabus Progress",
                subtitle = "Mastered units & topics"
            )
        }

        if (uiState.courses.isEmpty()) {
            item(key = "home_empty_courses") {
                EmptyNoticeCard("No enrolled courses yet. Tap '+ Course' to start.")
            }
        } else {
            items(uiState.courses, key = { "home_course_${it.id}" }) { course ->
                val courseTopics = uiState.courseTopics.filter { it.courseId == course.id }
                val masteredCount = courseTopics.count { it.isMastered }
                CourseProgressCard(
                    course = course,
                    masteredTopics = masteredCount,
                    totalTopics = courseTopics.size,
                    onClick = { onOpenCourse(course.id) }
                )
            }
        }

        // 6. Upcoming Assessments & Deadlines
        item(key = "home_assessments_header") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionDividerTitle(
                title = "Upcoming Assessments & Deadlines",
                subtitle = "${upcomingAssessments.size} active tasks scheduled"
            )
        }

        if (upcomingAssessments.isEmpty()) {
            item(key = "home_empty_assessments") {
                EmptyNoticeCard("No upcoming assignment deadlines recorded.")
            }
        } else {
            items(upcomingAssessments.take(4), key = { "home_task_${it.id}" }) { task ->
                UpcomingAssessmentItemCard(
                    task = task,
                    onToggle = { onToggleTask(task) }
                )
            }
        }

        // 7. Notes Due for Review
        item(key = "home_notes_review_header") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionDividerTitle(
                title = "Notes Requiring Review",
                subtitle = "${unmasteredTopicsWithNotes.size} key lecture topics needing active recall"
            )
        }

        if (unmasteredTopicsWithNotes.isEmpty()) {
            item(key = "home_empty_notes") {
                EmptyNoticeCard("All course notes reviewed and mastered! Excellent work.")
            }
        } else {
            items(unmasteredTopicsWithNotes.take(3), key = { "home_topic_${it.id}" }) { topic ->
                val course = uiState.courses.find { it.id == topic.courseId }
                NoteReviewCard(
                    topic = topic,
                    courseCode = course?.code ?: "Course",
                    onReview = { onOpenCourse(topic.courseId) }
                )
            }
        }

        // 8. Unread Lecture Materials
        item(key = "home_unread_materials_header") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionDividerTitle(
                title = "Unread Lecture Materials",
                subtitle = "${unreadMaterials.size} slide decks pending completion"
            )
        }

        if (unreadMaterials.isEmpty()) {
            item(key = "home_empty_materials") {
                EmptyNoticeCard("You have finished reading all imported lecture slide decks.")
            }
        } else {
            items(unreadMaterials.take(3), key = { "home_material_${it.id}" }) { material ->
                val course = uiState.courses.find { it.id == material.courseId }
                UnreadMaterialCard(
                    material = material,
                    course = course,
                    onOpen = { onOpenCourse(material.courseId) }
                )
            }
        }

        // 9 & 10. Pending Practice Questions & Weak Revision Areas
        item(key = "home_revision_header") {
            Spacer(modifier = Modifier.height(16.dp))
            SectionDividerTitle(
                title = "Revision & Weak Areas",
                subtitle = "Active recall questions & spaced repetition drills"
            )
        }

        item(key = "home_revision_card") {
            RevisionStatusCard(
                pendingQuizCount = pendingPracticeQuestions.size,
                weakCardsCount = weakRevisionCards.size,
                totalFlashcards = uiState.flashcards.size,
                onStartQuiz = { onStartQuiz(null) },
                onDrillWeakCards = { onStartFlashcardStudy(null) }
            )
        }
    }
}

// =========================================================================
// TIMETABLE & CLASS CARD COMPONENTS
// =========================================================================

@Composable
fun TodayLiveHeader(
    dayOfWeekNum: Int,
    currentHour: Int,
    currentMinute: Int,
    totalCourses: Int,
    activeClassCount: Int
) {
    val dayName = getDayName(dayOfWeekNum)
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date())
    val formattedTime = String.format("%02d:%02d", currentHour, currentMinute)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$dayName, $formattedDate".uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Lecture Timetable",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (activeClassCount > 0) {
                    "You have $activeClassCount upcoming lecture class${if (activeClassCount > 1) "es" else ""} remaining today."
                } else {
                    "No upcoming lectures active on your schedule for today."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimetableSectionHeader(
    hasClassesToday: Boolean,
    onAddClass: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Today's Scheduled Classes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hasClassesToday) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun ClassScheduleCard(
    lectureClass: LectureClassEntity,
    isActiveNow: Boolean,
    tasks: List<StudyTaskEntity>,
    onToggleTask: (StudyTaskEntity) -> Unit,
    onOpenCourse: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(lectureClass.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val startTimeFormatted = String.format("%02d:%02d", lectureClass.startHour, lectureClass.startMinute)
    val endTimeFormatted = String.format("%02d:%02d", lectureClass.endHour, lectureClass.endMinute)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("class_schedule_card_${lectureClass.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActiveNow) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top accent indicator for active vs scheduled
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isActiveNow) 6.dp else 4.dp)
                    .background(if (isActiveNow) CampusGreen else accentColor)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row: Course Code, Status Badge, Time Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = lectureClass.courseCode,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (isActiveNow) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = CampusGreen,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE NOW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Chronological Time Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$startTimeFormatted - $endTimeFormatted",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Course Name
                Text(
                    text = lectureClass.courseName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Topic / Lecture Title
                if (lectureClass.topicOrLectureName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lectureClass.topicOrLectureName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Venue
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Venue",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lectureClass.venue.isNotBlank()) lectureClass.venue else "Venue TBA / Online Session",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // =========================================================
                // CORRESPONDING ASSIGNMENTS (HIGHLIGHTED INSIDE CLASS CARD)
                // =========================================================
                Spacer(modifier = Modifier.height(14.dp))

                if (tasks.isNotEmpty()) {
                    tasks.forEach { task ->
                        HighlightedAssignmentCard(
                            task = task,
                            onToggle = { onToggleTask(task) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No pending assignments for this lecture.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightedAssignmentCard(
    task: StudyTaskEntity,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            ),
        color = if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSIGNMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Priority Chip
                    Surface(
                        color = when (task.priority) {
                            "HIGH" -> CampusRed.copy(alpha = 0.15f)
                            "LOW" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${task.priority} PRIORITY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = when (task.priority) {
                                "HIGH" -> CampusRed
                                "LOW" -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.secondary
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Actual Deadline
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = "Deadline",
                        tint = if (task.dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.dueDate != null) {
                            val sdf = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())
                            "Deadline: ${sdf.format(Date(task.dueDate))}"
                        } else {
                            "No deadline specified"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (task.dueDate != null) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (task.dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NoClassesTodayBanner(
    allTodayClassesCount: Int,
    passedTodayCount: Int,
    nextDayInfo: Pair<Int, List<LectureClassEntity>>?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (allTodayClassesCount > 0 && passedTodayCount == allTodayClassesCount) {
                    "All lectures for today have concluded."
                } else {
                    "No classes scheduled on your timetable for today."
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (nextDayInfo != null) {
                    "Showing your next scheduled lecture day below."
                } else {
                    "No lectures scheduled for upcoming days. Check Planner to add classes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NextScheduledClassesHeader(nextDayNum: Int, classCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Next Scheduled: ${getDayName(nextDayNum)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$classCount Classes",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =========================================================================
// SECTION 2 TO 7: STUDY PROGRESS, COURSE PROGRESS, NOTES, REVISION
// =========================================================================

@Composable
fun SectionDividerTitle(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TodayStudyProgressCard(
    plannedCount: Int,
    completedCount: Int,
    todaySessions: List<StudySessionEntity>,
    onToggleSession: (StudySessionEntity) -> Unit,
    onAddSession: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Revision Goal",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$completedCount of $plannedCount sessions logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddSession,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Session", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progress = if (plannedCount > 0) completedCount.toFloat() / plannedCount.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (todaySessions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                todaySessions.forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = session.isCompleted,
                            onCheckedChange = { onToggleSession(session) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${session.courseCode}: ${session.topicTitle}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (session.isCompleted) TextDecoration.LineThrough else null
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (session.targetGoal.isNotBlank()) {
                                Text(
                                    text = session.targetGoal,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun CourseProgressCard(
    course: CourseEntity,
    masteredTopics: Int,
    totalTopics: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${course.code} • ${course.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$masteredTopics / $totalTopics Mastered",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val progress = if (totalTopics > 0) masteredCount(masteredTopics, totalTopics) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun masteredCount(mastered: Int, total: Int): Float {
    if (total <= 0) return 0f
    return (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@Composable
fun NoteReviewCard(
    topic: TopicEntity,
    courseCode: String,
    onReview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onReview() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$courseCode: ${topic.title}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = topic.summaryNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UnreadMaterialCard(
    material: LectureMaterialEntity,
    course: CourseEntity?,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course?.code ?: "Course"} • Page ${material.lastReadPage} of ${material.totalPages} (${material.totalPages - material.lastReadPage} pages left)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RevisionStatusCard(
    pendingQuizCount: Int,
    weakCardsCount: Int,
    totalFlashcards: Int,
    onStartQuiz: () -> Unit,
    onDrillWeakCards: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Active Recall Sprint",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (weakCardsCount > 0) {
                    Surface(
                        color = CampusRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$weakCardsCount Weak Areas",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CampusRed
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You have $pendingQuizCount unattempted quiz questions and $weakCardsCount cards rated Hard or Again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDrillWeakCards,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Drill Weak Cards", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = onStartQuiz,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Practice Quiz ($pendingQuizCount)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun UpcomingAssessmentItemCard(
    task: StudyTaskEntity,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.courseCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (task.dueDate != null) {
                        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        Text(
                            text = sdf.format(Date(task.dueDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EmptyNoticeCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}

fun getDayName(dayNum: Int): String {
    return when (dayNum) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }
}

@Composable
fun PomodoroQuickCard(
    isRunning: Boolean,
    remainingSeconds: Int,
    completedCount: Int,
    selectedCourse: CourseEntity?,
    onOpenPomodoro: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenPomodoro() }
            .testTag("home_pomodoro_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.HourglassTop else Icons.Default.SelfImprovement,
                        contentDescription = "Pomodoro",
                        tint = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isRunning) "Active Focus Session" else "Pomodoro Focus Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) {
                            "${selectedCourse?.code ?: "Course"} • $timeFormatted remaining"
                        } else {
                            if (selectedCourse != null) "Ready for ${selectedCourse.code} • 25m Focus Block" else "Timed focus blocks for your courses"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onOpenPomodoro,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.testTag("home_start_pomodoro_button")
            ) {
                Text(
                    text = if (isRunning) "View" else "Focus",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
