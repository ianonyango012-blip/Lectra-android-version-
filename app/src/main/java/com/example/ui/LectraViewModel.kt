package com.example.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiServiceStatus
import com.example.data.ai.LectraAiStudyService
import com.example.data.local.LectraDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.data.model.UnitEntity
import com.example.data.repository.LectraRepository
import com.example.ui.navigation.LectraDestination
import com.example.ui.navigation.SubScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class PomodoroMode(val title: String, val defaultMinutes: Int) {
    FOCUS("Focus Session", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

data class LectraUiState(
    val currentDestination: LectraDestination = LectraDestination.HOME,
    val currentSubScreen: SubScreen = SubScreen.NONE,
    val courses: List<CourseEntity> = emptyList(),
    val classes: List<LectureClassEntity> = emptyList(),
    val materials: List<LectureMaterialEntity> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val tasks: List<StudyTaskEntity> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val quizzes: List<QuizQuestionEntity> = emptyList(),
    val selectedCourseId: Long? = null,
    val courseUnits: List<UnitEntity> = emptyList(),
    val courseTopics: List<TopicEntity> = emptyList(),
    // Device time tracking
    val currentDayOfWeek: Int = 1, // 1 = Monday ... 7 = Sunday
    val currentHour: Int = 12,
    val currentMinute: Int = 0,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    // Active Flashcard session
    val activeFlashcardDeckCourseId: Long? = null,
    val flashcardCurrentIndex: Int = 0,
    val isFlashcardFlipped: Boolean = false,
    // Active Quiz session
    val activeQuizCourseId: Long? = null,
    val quizCurrentIndex: Int = 0,
    val quizSelectedOption: Int? = null,
    val isQuizAnswerRevealed: Boolean = false,
    val quizScore: Int = 0,
    val isQuizFinished: Boolean = false,
    // Pomodoro Study Timer
    val selectedTimerCourseId: Long? = null,
    val timerMode: PomodoroMode = PomodoroMode.FOCUS,
    val timerTotalSeconds: Int = 25 * 60,
    val timerRemainingSeconds: Int = 25 * 60,
    val isTimerRunning: Boolean = false,
    val completedPomodoroCount: Int = 0,
    val timerSessionGoal: String = "",
    val timerElapsedSecondsThisSession: Int = 0,
    // Global Search Query
    val searchQuery: String = "",
    // Google Research Query & Recent Searches
    val researchQuery: String = "",
    val recentResearchSearches: List<String> = listOf(
        "Distributed consensus Raft",
        "Linear algebra eigenvectors",
        "Molecular genetics replication fork"
    ),
    // AI Status
    val aiStatus: AiServiceStatus = AiServiceStatus.MISSING_API_KEY,
    // Theme Mode (SYSTEM by default when no previous user preference exists)
    val themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.SYSTEM,
    // Active PDF Study Workspace
    val activeReadingMaterial: LectureMaterialEntity? = null,
    val activePdfAnnotations: List<com.example.data.model.PdfAnnotationEntity> = emptyList(),
    val isImportingPdf: Boolean = false,
    val pdfImportProgress: String? = null,
    // Feedback / Error messages
    val userNotice: String? = null
)

class LectraViewModel(application: Application) : AndroidViewModel(application) {

    val repository: LectraRepository
    val backupManager: com.example.data.backup.BackupManager
    private val aiService = LectraAiStudyService()
    private val prefs = application.getSharedPreferences("lectra_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LectraUiState())
    val uiState: StateFlow<LectraUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null
    private var annotationsJob: Job? = null

    init {
        val db = LectraDatabase.getDatabase(application, viewModelScope)
        repository = LectraRepository(db, application)
        backupManager = com.example.data.backup.BackupManager(repository, application)

        val savedThemeStr = prefs.getString("app_theme_mode", com.example.ui.theme.ThemeMode.SYSTEM.name)
            ?: com.example.ui.theme.ThemeMode.SYSTEM.name
        val initialTheme = runCatching { com.example.ui.theme.ThemeMode.valueOf(savedThemeStr) }
            .getOrDefault(com.example.ui.theme.ThemeMode.SYSTEM)
        _uiState.value = _uiState.value.copy(themeMode = initialTheme)

        updateDeviceTime()

        // Initialize local notification channels and verify scheduled alarms
        com.example.notification.LectraNotificationHelper.createNotificationChannels(application)
        com.example.notification.LectraNotificationScheduler.rescheduleAll(application)

        // Observe local database flows
        viewModelScope.launch {
            combine(
                repository.getAllCourses(),
                repository.getAllLectureClasses(),
                repository.getAllMaterials(),
                repository.getAllUnits()
            ) { courses, classes, materials, units ->
                CoursesBundle(courses, classes, materials, units)
            }.combine(
                combine(
                    repository.getAllSessions(),
                    repository.getAllTasks(),
                    combine(
                        repository.getAllFlashcards(),
                        repository.getAllQuizzes()
                    ) { flashcards, quizzes -> Pair(flashcards, quizzes) }
                ) { sessions, tasks, (flashcards, quizzes) ->
                    Triple(sessions, tasks, Pair(flashcards, quizzes))
                }
            ) { bundle, (sessions, tasks, revPair) ->
                val (flashcards, quizzes) = revPair
                _uiState.value.copy(
                    courses = bundle.courses,
                    classes = bundle.classes,
                    materials = bundle.materials,
                    units = bundle.units,
                    sessions = sessions,
                    tasks = tasks,
                    flashcards = flashcards,
                    quizzes = quizzes,
                    aiStatus = aiService.getStatus()
                )
            }.collect { combinedState ->
                _uiState.value = combinedState
                _uiState.value.selectedCourseId?.let { courseId ->
                    loadCourseDetails(courseId)
                }
            }
        }
    }

    fun updateDeviceTime() {
        val cal = Calendar.getInstance()
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val millis = cal.timeInMillis

        _uiState.value = _uiState.value.copy(
            currentDayOfWeek = dayOfWeek,
            currentHour = hour,
            currentMinute = minute,
            currentTimeMillis = millis
        )
    }

    fun navigateTo(destination: LectraDestination) {
        updateDeviceTime()
        _uiState.value = _uiState.value.copy(
            currentDestination = destination,
            currentSubScreen = SubScreen.NONE,
            selectedCourseId = null
        )
    }

    fun openCourseDetail(courseId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedCourseId = courseId,
            currentSubScreen = SubScreen.COURSE_DETAIL
        )
        loadCourseDetails(courseId)
    }

    fun closeSubScreen(): Boolean {
        if (_uiState.value.currentSubScreen == SubScreen.PDF_VIEWER) {
            annotationsJob?.cancel()
            val returnSubScreen = if (_uiState.value.selectedCourseId != null) SubScreen.COURSE_DETAIL else SubScreen.NONE
            _uiState.value = _uiState.value.copy(
                currentSubScreen = returnSubScreen,
                activeReadingMaterial = null,
                activePdfAnnotations = emptyList()
            )
            return true
        }
        if (_uiState.value.currentSubScreen != SubScreen.NONE) {
            _uiState.value = _uiState.value.copy(
                currentSubScreen = SubScreen.NONE,
                selectedCourseId = null,
                isFlashcardFlipped = false,
                isQuizAnswerRevealed = false
            )
            return true
        }
        if (_uiState.value.currentDestination != LectraDestination.HOME) {
            _uiState.value = _uiState.value.copy(currentDestination = LectraDestination.HOME)
            return true
        }
        return false
    }

    private fun loadCourseDetails(courseId: Long) {
        viewModelScope.launch {
            repository.getUnitsForCourse(courseId).collect { units ->
                _uiState.value = _uiState.value.copy(courseUnits = units)
            }
        }
        viewModelScope.launch {
            repository.getAllTopicsForCourse(courseId).collect { topics ->
                _uiState.value = _uiState.value.copy(courseTopics = topics)
            }
        }
    }

    // Courses Actions
    fun addCourse(code: String, title: String, lecturer: String, colorHex: String, targetHours: Int) {
        viewModelScope.launch {
            val id = repository.insertCourse(
                CourseEntity(
                    code = code.trim(),
                    title = title.trim(),
                    colorHex = colorHex,
                    lecturer = lecturer.trim(),
                    semester = "Current Term",
                    targetWeeklyHours = targetHours
                )
            )
            repository.insertUnit(
                UnitEntity(
                    courseId = id,
                    unitNumber = 1,
                    title = "Unit 1: Course Fundamentals",
                    description = "Introductory concepts and syllabus outline"
                )
            )
            showNotice("Course $code created successfully.")
        }
    }

    fun addCourseWithUnits(
        code: String,
        title: String,
        lecturer: String,
        colorHex: String,
        targetHours: Int,
        unitTitles: List<String>
    ) {
        viewModelScope.launch {
            val id = repository.insertCourse(
                CourseEntity(
                    code = code.trim(),
                    title = title.trim(),
                    colorHex = colorHex,
                    lecturer = lecturer.trim(),
                    semester = "Current Term",
                    targetWeeklyHours = targetHours
                )
            )
            if (unitTitles.isNotEmpty()) {
                unitTitles.forEachIndexed { index, unitTitle ->
                    if (unitTitle.isNotBlank()) {
                        repository.insertUnit(
                            UnitEntity(
                                courseId = id,
                                unitNumber = index + 1,
                                title = unitTitle.trim(),
                                description = "Course unit"
                            )
                        )
                    }
                }
            } else {
                // Standalone or default unit
                repository.insertUnit(
                    UnitEntity(
                        courseId = id,
                        unitNumber = 1,
                        title = "General Course Content",
                        description = "Standalone course materials"
                    )
                )
            }
            showNotice("Course $code created successfully.")
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            if (_uiState.value.selectedCourseId == course.id) {
                closeSubScreen()
            }
            showNotice("Course ${course.code} deleted.")
        }
    }

    fun addUnit(courseId: Long, unitNumber: Int, title: String, description: String) {
        viewModelScope.launch {
            repository.insertUnit(
                UnitEntity(
                    courseId = courseId,
                    unitNumber = unitNumber,
                    title = title.trim(),
                    description = description.trim()
                )
            )
            loadCourseDetails(courseId)
            showNotice("Unit added.")
        }
    }

    fun addTopic(unitId: Long, courseId: Long, title: String, notes: String) {
        viewModelScope.launch {
            repository.insertTopic(
                TopicEntity(
                    unitId = unitId,
                    courseId = courseId,
                    title = title.trim(),
                    summaryNotes = notes.trim()
                )
            )
            loadCourseDetails(courseId)
            showNotice("Topic '$title' created.")
        }
    }

    fun toggleTopicMastered(topic: TopicEntity) {
        viewModelScope.launch {
            repository.updateTopic(topic.copy(isMastered = !topic.isMastered))
            loadCourseDetails(topic.courseId)
        }
    }

    // Lecture Classes (Timetable)
    fun addLectureClass(
        courseId: Long,
        courseCode: String,
        courseName: String,
        topicName: String,
        dayOfWeek: Int,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        venue: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val entity = LectureClassEntity(
                courseId = courseId,
                courseCode = courseCode,
                courseName = courseName,
                topicOrLectureName = topicName.trim(),
                dayOfWeek = dayOfWeek,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                venue = venue.trim(),
                colorHex = colorHex
            )
            val newId = repository.insertLectureClass(entity)
            val classWithId = entity.copy(id = newId)
            com.example.notification.LectraNotificationScheduler.scheduleClassReminder(getApplication(), classWithId)
            showNotice("Lecture class added to timetable.")
        }
    }

    fun updateLectureClass(lectureClass: LectureClassEntity) {
        viewModelScope.launch {
            repository.updateLectureClass(lectureClass)
            com.example.notification.LectraNotificationScheduler.scheduleClassReminder(getApplication(), lectureClass)
            showNotice("Lecture class updated.")
        }
    }

    fun deleteLectureClass(lectureClass: LectureClassEntity) {
        viewModelScope.launch {
            repository.deleteLectureClass(lectureClass)
            com.example.notification.LectraNotificationScheduler.cancelClassReminder(getApplication(), lectureClass.id)
            showNotice("Lecture class removed.")
        }
    }

    // Multi-format Document Import with Validation & Duplicate Detection
    fun handlePdfImport(uri: Uri, courseId: Long, unitId: Long? = null, topicId: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImportingPdf = true,
                pdfImportProgress = "Validating document..."
            )

            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            try {
                val app = getApplication<Application>()
                val (docName, docSize) = com.example.data.document.DocumentStorageManager.resolveMetadata(app, uri)

                // Duplicate Check: check if already exists in this course
                val isDuplicate = _uiState.value.materials.any {
                    it.courseId == courseId && it.title.equals(docName, ignoreCase = true) &&
                            (it.fileSizeBytes == docSize || docSize == 0L)
                }
                if (isDuplicate) {
                    _uiState.value = _uiState.value.copy(isImportingPdf = false, pdfImportProgress = null)
                    showNotice("'$docName' is already imported in this course.")
                    return@launch
                }

                _uiState.value = _uiState.value.copy(pdfImportProgress = "Verifying document structure & saving locally...")
                val outcome = withContext(Dispatchers.IO) {
                    com.example.data.document.DocumentStorageManager.importDocument(app, uri)
                }

                when (outcome) {
                    is com.example.data.document.DocumentImportOutcome.Success -> {
                        val materialId = repository.insertMaterial(
                            LectureMaterialEntity(
                                courseId = courseId,
                                unitId = unitId,
                                topicId = topicId,
                                title = outcome.displayName,
                                fileUriString = outcome.localPath,
                                fileSizeBytes = outcome.fileSizeBytes,
                                mimeType = outcome.documentType.primaryMimeType,
                                lastReadPage = 1,
                                totalPages = outcome.pageOrUnitCount,
                                aiSummary = "Offline verified ${outcome.documentType.displayName}. Total: ${outcome.pageOrUnitCount}."
                            )
                        )
                        _uiState.value = _uiState.value.copy(isImportingPdf = false, pdfImportProgress = null)
                        showNotice("Imported '${outcome.displayName}' (${outcome.documentType.filterCategory}).")
                    }
                    is com.example.data.document.DocumentImportOutcome.Error -> {
                        _uiState.value = _uiState.value.copy(isImportingPdf = false, pdfImportProgress = null)
                        showNotice(outcome.message)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImportingPdf = false, pdfImportProgress = null)
                showNotice("Failed to import document: ${e.localizedMessage}")
            }
        }
    }

    fun openPdfViewer(material: LectureMaterialEntity) {
        _uiState.value = _uiState.value.copy(
            activeReadingMaterial = material,
            currentSubScreen = SubScreen.DOCUMENT_VIEWER
        )
        annotationsJob?.cancel()
        annotationsJob = viewModelScope.launch {
            repository.getAnnotationsForMaterial(material.id).collect { annotations ->
                _uiState.value = _uiState.value.copy(activePdfAnnotations = annotations)
            }
        }
    }

    fun openPdfMaterial(material: LectureMaterialEntity, context: android.content.Context) {
        openPdfViewer(material)
    }

    fun addPdfAnnotation(
        materialId: Long,
        pageIndex: Int,
        type: String,
        colorHex: String,
        content: String = "",
        dataJson: String = "",
        relX: Float = 0f,
        relY: Float = 0f
    ) {
        viewModelScope.launch {
            repository.insertAnnotation(
                com.example.data.model.PdfAnnotationEntity(
                    materialId = materialId,
                    pageIndex = pageIndex,
                    type = type,
                    colorHex = colorHex,
                    content = content,
                    dataJson = dataJson,
                    relativeX = relX,
                    relativeY = relY
                )
            )
        }
    }

    fun deletePdfAnnotation(annotationId: Long) {
        viewModelScope.launch {
            repository.deleteAnnotationById(annotationId)
        }
    }

    fun togglePdfBookmark(materialId: Long, pageIndex: Int) {
        viewModelScope.launch {
            val existing = _uiState.value.activePdfAnnotations.find {
                it.materialId == materialId && it.pageIndex == pageIndex && it.type == "BOOKMARK"
            }
            if (existing != null) {
                repository.deleteAnnotationById(existing.id)
                showNotice("Bookmark removed for page ${pageIndex + 1}.")
            } else {
                repository.insertAnnotation(
                    com.example.data.model.PdfAnnotationEntity(
                        materialId = materialId,
                        pageIndex = pageIndex,
                        type = "BOOKMARK",
                        colorHex = "#F59E0B",
                        content = "Page ${pageIndex + 1} Bookmark"
                    )
                )
                showNotice("Page ${pageIndex + 1} bookmarked.")
            }
        }
    }

    fun updatePdfReadingProgress(materialId: Long, pageNumber: Int, totalPages: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(materialId, pageNumber, totalPages)
        }
    }

    fun deleteMaterial(material: LectureMaterialEntity) {
        viewModelScope.launch {
            repository.deleteMaterial(material)
            showNotice("Lecture material removed.")
        }
    }

    // Study Sessions & Overlap Validation
    fun addStudySession(
        courseId: Long,
        courseCode: String,
        courseTitle: String,
        topicTitle: String,
        dayOfWeek: Int,
        startHour: Int,
        startMinute: Int,
        durationMinutes: Int,
        targetGoal: String
    ): Boolean {
        val proposedStart = startHour * 60 + startMinute
        val proposedEnd = proposedStart + durationMinutes

        val daySessions = _uiState.value.sessions.filter { it.dayOfWeek == dayOfWeek }
        for (session in daySessions) {
            val sessionStart = session.startHour * 60 + session.startMinute
            val sessionEnd = sessionStart + session.durationMinutes
            val hasOverlap = maxOf(proposedStart, sessionStart) < minOf(proposedEnd, sessionEnd)
            if (hasOverlap) {
                showNotice("Conflict! Overlaps with ${session.courseCode} (${session.startHour}:${String.format("%02d", session.startMinute)}).")
                return false
            }
        }

        viewModelScope.launch {
            repository.insertSession(
                StudySessionEntity(
                    courseId = courseId,
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    topicTitle = topicTitle.trim(),
                    dayOfWeek = dayOfWeek,
                    startHour = startHour,
                    startMinute = startMinute,
                    durationMinutes = durationMinutes,
                    targetGoal = targetGoal.trim()
                )
            )
            showNotice("Study session scheduled for $courseCode.")
        }
        return true
    }

    fun toggleSessionCompletion(session: StudySessionEntity) {
        viewModelScope.launch {
            repository.updateSession(session.copy(isCompleted = !session.isCompleted))
        }
    }

    fun deleteSession(session: StudySessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            showNotice("Session deleted.")
        }
    }

    // Tasks & Assignments
    fun addTask(
        courseId: Long,
        courseCode: String,
        lectureClassId: Long? = null,
        topicTitle: String = "",
        title: String,
        dueDate: Long? = null,
        priority: String = "HIGH"
    ) {
        viewModelScope.launch {
            val entity = StudyTaskEntity(
                courseId = courseId,
                courseCode = courseCode,
                lectureClassId = lectureClassId,
                topicTitle = topicTitle.trim(),
                title = title.trim(),
                dueDate = dueDate,
                priority = priority
            )
            val newId = repository.insertTask(entity)
            val taskWithId = entity.copy(id = newId)
            com.example.notification.LectraNotificationScheduler.scheduleAssignmentReminders(getApplication(), taskWithId)
            showNotice("Assignment task added.")
        }
    }

    fun updateTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (task.isCompleted) {
                com.example.notification.LectraNotificationScheduler.cancelAssignmentReminders(getApplication(), task.id)
            } else {
                com.example.notification.LectraNotificationScheduler.scheduleAssignmentReminders(getApplication(), task)
            }
            showNotice("Assignment deadline updated.")
        }
    }

    fun toggleTaskCompletion(task: StudyTaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            if (updated.isCompleted) {
                com.example.notification.LectraNotificationScheduler.cancelAssignmentReminders(getApplication(), updated.id)
            } else {
                com.example.notification.LectraNotificationScheduler.scheduleAssignmentReminders(getApplication(), updated)
            }
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            com.example.notification.LectraNotificationScheduler.cancelAssignmentReminders(getApplication(), task.id)
        }
    }

    // Flashcard Study Hub
    fun startFlashcardStudy(courseId: Long? = null) {
        val available = if (courseId == null) {
            _uiState.value.flashcards
        } else {
            _uiState.value.flashcards.filter { it.courseId == courseId }
        }

        if (available.isEmpty()) {
            showNotice("No flashcards found for this deck. Add cards first!")
            return
        }

        _uiState.value = _uiState.value.copy(
            activeFlashcardDeckCourseId = courseId,
            flashcardCurrentIndex = 0,
            isFlashcardFlipped = false,
            currentSubScreen = SubScreen.FLASHCARD_STUDY
        )
    }

    fun flipFlashcard() {
        _uiState.value = _uiState.value.copy(isFlashcardFlipped = !_uiState.value.isFlashcardFlipped)
    }

    fun rateFlashcard(rating: String) {
        val deck = currentActiveFlashcards()
        val currentIndex = _uiState.value.flashcardCurrentIndex
        if (currentIndex < deck.size) {
            val card = deck[currentIndex]
            viewModelScope.launch {
                val newReps = card.repetitions + 1
                val newInterval = when (rating) {
                    "AGAIN" -> 1
                    "HARD" -> maxOf(1, card.intervalDays)
                    "GOOD" -> card.intervalDays * 2
                    "EASY" -> card.intervalDays * 3
                    else -> 1
                }
                repository.updateFlashcard(
                    card.copy(
                        repetitions = newReps,
                        intervalDays = newInterval,
                        lastReviewedAt = System.currentTimeMillis(),
                        lastRecallGrade = rating
                    )
                )
            }
        }

        if (currentIndex + 1 < deck.size) {
            _uiState.value = _uiState.value.copy(
                flashcardCurrentIndex = currentIndex + 1,
                isFlashcardFlipped = false
            )
        } else {
            showNotice("Deck completed! Well done.")
            _uiState.value = _uiState.value.copy(
                currentSubScreen = SubScreen.NONE,
                isFlashcardFlipped = false
            )
        }
    }

    fun currentActiveFlashcards(): List<FlashcardEntity> {
        val filterId = _uiState.value.activeFlashcardDeckCourseId
        return if (filterId == null) {
            _uiState.value.flashcards
        } else {
            _uiState.value.flashcards.filter { it.courseId == filterId }
        }
    }

    fun addFlashcard(courseId: Long, prompt: String, answer: String, hint: String?) {
        viewModelScope.launch {
            repository.insertFlashcard(
                FlashcardEntity(
                    courseId = courseId,
                    frontPrompt = prompt.trim(),
                    backAnswer = answer.trim(),
                    hint = hint?.trim()?.ifBlank { null }
                )
            )
            showNotice("Flashcard added to deck.")
        }
    }

    fun deleteFlashcard(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteFlashcard(card)
            showNotice("Flashcard removed.")
        }
    }

    // Quiz Study Hub
    fun startQuizStudy(courseId: Long? = null) {
        val available = if (courseId == null) {
            _uiState.value.quizzes
        } else {
            _uiState.value.quizzes.filter { it.courseId == courseId }
        }

        if (available.isEmpty()) {
            showNotice("No quiz questions available for this course. Add questions first!")
            return
        }

        _uiState.value = _uiState.value.copy(
            activeQuizCourseId = courseId,
            quizCurrentIndex = 0,
            quizSelectedOption = null,
            isQuizAnswerRevealed = false,
            quizScore = 0,
            isQuizFinished = false,
            currentSubScreen = SubScreen.QUIZ_STUDY
        )
    }

    fun selectQuizOption(optionIndex: Int) {
        if (!_uiState.value.isQuizAnswerRevealed) {
            _uiState.value = _uiState.value.copy(quizSelectedOption = optionIndex)
        }
    }

    fun confirmQuizAnswer() {
        val deck = currentActiveQuizzes()
        val index = _uiState.value.quizCurrentIndex
        val selected = _uiState.value.quizSelectedOption ?: return
        if (index < deck.size) {
            val q = deck[index]
            val isCorrect = selected == q.correctOptionIndex
            val newScore = if (isCorrect) _uiState.value.quizScore + 1 else _uiState.value.quizScore
            _uiState.value = _uiState.value.copy(
                isQuizAnswerRevealed = true,
                quizScore = newScore
            )
            viewModelScope.launch {
                repository.updateQuiz(
                    q.copy(
                        hasAnswered = true,
                        selectedOption = selected,
                        isCorrect = isCorrect
                    )
                )
            }
        }
    }

    fun nextQuizQuestion() {
        val deck = currentActiveQuizzes()
        val nextIndex = _uiState.value.quizCurrentIndex + 1
        if (nextIndex < deck.size) {
            _uiState.value = _uiState.value.copy(
                quizCurrentIndex = nextIndex,
                quizSelectedOption = null,
                isQuizAnswerRevealed = false
            )
        } else {
            _uiState.value = _uiState.value.copy(isQuizFinished = true)
        }
    }

    fun resetQuizSession() {
        viewModelScope.launch {
            _uiState.value.activeQuizCourseId?.let { courseId ->
                repository.resetQuizzesForCourse(courseId)
            } ?: repository.resetAllQuizzes()
            _uiState.value = _uiState.value.copy(
                quizCurrentIndex = 0,
                quizSelectedOption = null,
                isQuizAnswerRevealed = false,
                quizScore = 0,
                isQuizFinished = false
            )
        }
    }

    fun addQuizQuestion(
        courseId: Long,
        question: String,
        optA: String,
        optB: String,
        optC: String,
        optD: String,
        correctIdx: Int,
        explanation: String
    ) {
        viewModelScope.launch {
            repository.insertQuiz(
                QuizQuestionEntity(
                    courseId = courseId,
                    question = question.trim(),
                    optionA = optA.trim(),
                    optionB = optB.trim(),
                    optionC = optC.trim(),
                    optionD = optD.trim(),
                    correctOptionIndex = correctIdx,
                    explanation = explanation.trim()
                )
            )
            showNotice("Practice quiz question created.")
        }
    }

    fun deleteQuiz(quiz: QuizQuestionEntity) {
        viewModelScope.launch {
            repository.deleteQuiz(quiz)
            showNotice("Quiz question removed.")
        }
    }

    fun currentActiveQuizzes(): List<QuizQuestionEntity> {
        val filterId = _uiState.value.activeQuizCourseId
        return if (filterId == null) {
            _uiState.value.quizzes
        } else {
            _uiState.value.quizzes.filter { it.courseId == filterId }
        }
    }

    // ==========================================
    // POMODORO STUDY TIMER
    // ==========================================

    fun openPomodoroTimer(courseId: Long? = null) {
        val targetCourseId = courseId 
            ?: _uiState.value.selectedTimerCourseId 
            ?: _uiState.value.courses.firstOrNull()?.id
        _uiState.value = _uiState.value.copy(
            selectedTimerCourseId = targetCourseId,
            currentSubScreen = SubScreen.POMODORO_TIMER
        )
    }

    fun selectTimerCourse(courseId: Long) {
        _uiState.value = _uiState.value.copy(selectedTimerCourseId = courseId)
    }

    fun setTimerSessionGoal(goal: String) {
        _uiState.value = _uiState.value.copy(timerSessionGoal = goal)
    }

    fun setTimerMode(mode: PomodoroMode, durationMinutes: Int? = null) {
        timerJob?.cancel()
        val mins = durationMinutes ?: mode.defaultMinutes
        val totalSecs = mins * 60
        _uiState.value = _uiState.value.copy(
            timerMode = mode,
            timerTotalSeconds = totalSecs,
            timerRemainingSeconds = totalSecs,
            timerElapsedSecondsThisSession = 0,
            isTimerRunning = false
        )
    }

    fun startPomodoroTimer() {
        if (_uiState.value.isTimerRunning) return

        // If time is already 0, reset to full duration before starting
        if (_uiState.value.timerRemainingSeconds <= 0) {
            _uiState.value = _uiState.value.copy(
                timerRemainingSeconds = _uiState.value.timerTotalSeconds,
                timerElapsedSecondsThisSession = 0
            )
        }

        // Default to first course if none is selected
        if (_uiState.value.selectedTimerCourseId == null && _uiState.value.courses.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                selectedTimerCourseId = _uiState.value.courses.first().id
            )
        }

        _uiState.value = _uiState.value.copy(isTimerRunning = true)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isTimerRunning) {
                delay(1000L)
                if (!_uiState.value.isTimerRunning) break

                val currentRemaining = _uiState.value.timerRemainingSeconds
                val currentElapsed = _uiState.value.timerElapsedSecondsThisSession
                if (currentRemaining > 1) {
                    _uiState.value = _uiState.value.copy(
                        timerRemainingSeconds = currentRemaining - 1,
                        timerElapsedSecondsThisSession = currentElapsed + 1
                    )
                } else {
                    handleTimerCompleted()
                    break
                }
            }
        }
    }

    fun pausePomodoroTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
    }

    fun stopAndLogPomodoroSession() {
        timerJob?.cancel()
        val state = _uiState.value
        val elapsedSeconds = state.timerElapsedSecondsThisSession
        val elapsedMinutes = elapsedSeconds / 60

        _uiState.value = state.copy(
            isTimerRunning = false,
            timerRemainingSeconds = state.timerTotalSeconds,
            timerElapsedSecondsThisSession = 0
        )

        if (state.timerMode == PomodoroMode.FOCUS && elapsedMinutes >= 1) {
            logCompletedSessionToDatabase(elapsedMinutes)
            val courseCode = _uiState.value.courses.find { it.id == state.selectedTimerCourseId }?.code ?: "course"
            showNotice("Session logged: $elapsedMinutes min focused on $courseCode.")
        } else if (state.timerMode == PomodoroMode.FOCUS) {
            showNotice("Focus session stopped.")
        } else {
            showNotice("Break session ended.")
        }
    }

    fun resetPomodoroTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isTimerRunning = false,
            timerRemainingSeconds = _uiState.value.timerTotalSeconds,
            timerElapsedSecondsThisSession = 0
        )
    }

    fun skipToNextPomodoroMode() {
        timerJob?.cancel()
        val currentMode = _uiState.value.timerMode
        val nextMode = when (currentMode) {
            PomodoroMode.FOCUS -> PomodoroMode.SHORT_BREAK
            PomodoroMode.SHORT_BREAK -> PomodoroMode.FOCUS
            PomodoroMode.LONG_BREAK -> PomodoroMode.FOCUS
        }
        setTimerMode(nextMode)
    }

    private fun handleTimerCompleted() {
        val state = _uiState.value
        _uiState.value = state.copy(
            isTimerRunning = false,
            timerRemainingSeconds = 0
        )

        if (state.timerMode == PomodoroMode.FOCUS) {
            val newCompletedCount = state.completedPomodoroCount + 1
            val elapsedMinutes = (state.timerTotalSeconds / 60).coerceAtLeast(1)

            logCompletedSessionToDatabase(elapsedMinutes)

            val nextMode = if (newCompletedCount % 4 == 0) PomodoroMode.LONG_BREAK else PomodoroMode.SHORT_BREAK
            val nextTotalSeconds = nextMode.defaultMinutes * 60

            val courseCode = _uiState.value.courses.find { it.id == state.selectedTimerCourseId }?.code ?: "course"
            _uiState.value = _uiState.value.copy(
                completedPomodoroCount = newCompletedCount,
                timerMode = nextMode,
                timerTotalSeconds = nextTotalSeconds,
                timerRemainingSeconds = nextTotalSeconds,
                timerElapsedSecondsThisSession = 0
            )
            showNotice("🎉 Focus session completed for $courseCode! Time for a ${nextMode.title.lowercase()} (${nextMode.defaultMinutes}m).")
        } else {
            val nextMode = PomodoroMode.FOCUS
            val nextTotalSeconds = nextMode.defaultMinutes * 60
            _uiState.value = _uiState.value.copy(
                timerMode = nextMode,
                timerTotalSeconds = nextTotalSeconds,
                timerRemainingSeconds = nextTotalSeconds,
                timerElapsedSecondsThisSession = 0
            )
            showNotice("☕ Break finished! Ready for your next focus session.")
        }
    }

    private fun logCompletedSessionToDatabase(minutes: Int) {
        val course = _uiState.value.courses.find { it.id == _uiState.value.selectedTimerCourseId }
            ?: _uiState.value.courses.firstOrNull()

        if (course != null && minutes > 0) {
            val goal = _uiState.value.timerSessionGoal.ifBlank { "Pomodoro Focus Session" }
            val cal = Calendar.getInstance()
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            viewModelScope.launch {
                repository.insertSession(
                    StudySessionEntity(
                        courseId = course.id,
                        courseCode = course.code,
                        courseTitle = course.title,
                        topicTitle = goal,
                        dayOfWeek = dayOfWeek,
                        startHour = cal.get(Calendar.HOUR_OF_DAY),
                        startMinute = cal.get(Calendar.MINUTE),
                        durationMinutes = minutes,
                        isCompleted = true,
                        targetGoal = goal,
                        scheduledDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ==========================================
    // GLOBAL SEARCH FOR LESSONS, NOTES & PDFS
    // ==========================================

    fun openGlobalSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            currentSubScreen = SubScreen.GLOBAL_SEARCH
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // ==========================================
    // GOOGLE RESEARCH & WEB CONCEPT SEARCH
    // ==========================================

    fun openResearch(initialQuery: String = "") {
        _uiState.value = _uiState.value.copy(
            researchQuery = initialQuery,
            currentSubScreen = SubScreen.RESEARCH
        )
    }

    fun setResearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(researchQuery = query)
    }

    fun executeGoogleSearch(context: Context, rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) {
            showNotice("Please enter a topic or concept to research.")
            return
        }

        // Add to recent searches (deduplicated, latest first)
        val currentRecent = _uiState.value.recentResearchSearches.toMutableList()
        currentRecent.remove(trimmed)
        currentRecent.add(0, trimmed)
        _uiState.value = _uiState.value.copy(
            researchQuery = trimmed,
            recentResearchSearches = currentRecent.take(10)
        )

        // Properly construct search URL using Android Uri.Builder for automatic standard percent-encoding
        val searchUri = Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .appendPath("search")
            .appendQueryParameter("q", trimmed)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
            showNotice("Opening Google Search for '$trimmed'...")
        } catch (_: ActivityNotFoundException) {
            showNotice("No web browser found to open search results.")
        } catch (e: Exception) {
            showNotice("Could not open browser: ${e.localizedMessage}")
        }
    }

    fun setThemeMode(mode: com.example.ui.theme.ThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun toggleTheme() {
        val nextMode = if (_uiState.value.themeMode == com.example.ui.theme.ThemeMode.DARK) {
            com.example.ui.theme.ThemeMode.LIGHT
        } else {
            com.example.ui.theme.ThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.resetAndReseedDatabase()
            updateDeviceTime()
            showNotice("Academic demo data reloaded successfully.")
        }
    }

    fun clearNotice() {
        _uiState.value = _uiState.value.copy(userNotice = null)
    }

    private fun showNotice(message: String) {
        _uiState.value = _uiState.value.copy(userNotice = message)
    }

    public override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        annotationsJob?.cancel()
    }
}

private data class CoursesBundle(
    val courses: List<CourseEntity>,
    val classes: List<LectureClassEntity>,
    val materials: List<LectureMaterialEntity>,
    val units: List<UnitEntity>
)
