package com.example.ui

import android.app.Application
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
import java.util.Calendar

data class LectraUiState(
    val currentDestination: LectraDestination = LectraDestination.HOME,
    val currentSubScreen: SubScreen = SubScreen.NONE,
    val courses: List<CourseEntity> = emptyList(),
    val classes: List<LectureClassEntity> = emptyList(),
    val materials: List<LectureMaterialEntity> = emptyList(),
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
    // AI Status
    val aiStatus: AiServiceStatus = AiServiceStatus.MISSING_API_KEY,
    // Feedback / Error messages
    val userNotice: String? = null
)

class LectraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LectraRepository
    private val aiService = LectraAiStudyService()

    private val _uiState = MutableStateFlow(LectraUiState())
    val uiState: StateFlow<LectraUiState> = _uiState.asStateFlow()

    init {
        val db = LectraDatabase.getDatabase(application, viewModelScope)
        repository = LectraRepository(db, application)

        updateDeviceTime()

        // Periodic ticker to keep class statuses up-to-date in real time
        viewModelScope.launch {
            while (isActive) {
                delay(30000) // check every 30 seconds
                updateDeviceTime()
            }
        }


        // Observe local database flows
        viewModelScope.launch {
            combine(
                repository.getAllCourses(),
                repository.getAllLectureClasses(),
                repository.getAllMaterials()
            ) { courses, classes, materials ->
                Triple(courses, classes, materials)
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
            ) { (courses, classes, materials), (sessions, tasks, revPair) ->
                val (flashcards, quizzes) = revPair
                _uiState.value.copy(
                    courses = courses,
                    classes = classes,
                    materials = materials,
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
            repository.insertLectureClass(
                LectureClassEntity(
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
            )
            showNotice("Lecture class added to timetable.")
        }
    }

    fun deleteLectureClass(lectureClass: LectureClassEntity) {
        viewModelScope.launch {
            repository.deleteLectureClass(lectureClass)
            showNotice("Lecture class removed.")
        }
    }

    // PDF SAF Import
    fun handlePdfImport(uri: Uri, courseId: Long, unitId: Long? = null, topicId: Long? = null) {
        viewModelScope.launch {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val (fileName, fileSize) = repository.resolveDocumentDetails(uri)
            repository.insertMaterial(
                LectureMaterialEntity(
                    courseId = courseId,
                    unitId = unitId,
                    topicId = topicId,
                    title = fileName,
                    fileUriString = uri.toString(),
                    fileSizeBytes = fileSize,
                    mimeType = "application/pdf",
                    totalPages = 20,
                    aiSummary = null
                )
            )
            showNotice("Imported '$fileName' to course materials.")
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
            repository.insertTask(
                StudyTaskEntity(
                    courseId = courseId,
                    courseCode = courseCode,
                    lectureClassId = lectureClassId,
                    topicTitle = topicTitle.trim(),
                    title = title.trim(),
                    dueDate = dueDate,
                    priority = priority
                )
            )
            showNotice("Assignment task added.")
        }
    }

    fun toggleTaskCompletion(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
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
}
