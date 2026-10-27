package com.example.data.backup

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

data class BackupOptions(
    val includeCoursesAndNotes: Boolean = true,
    val includeTimetableAndPlanner: Boolean = true,
    val includeRevision: Boolean = true,
    val includePreferences: Boolean = true
)

data class BackupSummary(
    val timestamp: Long,
    val formattedDate: String,
    val coursesCount: Int,
    val unitsCount: Int,
    val topicsCount: Int,
    val materialsCount: Int,
    val classesCount: Int,
    val sessionsCount: Int,
    val tasksCount: Int,
    val flashcardsCount: Int,
    val quizzesCount: Int,
    val fileSizeBytes: Long = 0L
) {
    val totalItemsCount: Int
        get() = coursesCount + unitsCount + topicsCount + materialsCount + classesCount + sessionsCount + tasksCount + flashcardsCount + quizzesCount
}

data class LectraBackupData(
    val version: Int,
    val timestamp: Long,
    val preferences: Map<String, String>,
    val courses: List<CourseEntity>,
    val units: List<UnitEntity>,
    val topics: List<TopicEntity>,
    val materials: List<LectureMaterialEntity>,
    val lectureClasses: List<LectureClassEntity>,
    val sessions: List<StudySessionEntity>,
    val tasks: List<StudyTaskEntity>,
    val flashcards: List<FlashcardEntity>,
    val quizzes: List<QuizQuestionEntity>
)

data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val version: Int = 1,
    val timestamp: Long = 0L,
    val summary: BackupSummary? = null,
    val data: LectraBackupData? = null
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

sealed class BackupProgressState {
    object Idle : BackupProgressState()
    data class InProgress(val percent: Int, val message: String) : BackupProgressState()
    data class Success(val message: String, val summary: BackupSummary) : BackupProgressState()
    data class Error(val message: String) : BackupProgressState()
    object Cancelled : BackupProgressState()
}

class BackupManager(
    private val repository: LectraRepository,
    private val context: Context
) {
    private val isCancelled = AtomicBoolean(false)

    fun requestCancellation() {
        isCancelled.set(true)
    }

    fun cancelOperation() {
        requestCancellation()
    }

    /**
     * Creates a structured JSON backup string from current local Room database.
     */
    suspend fun createBackupJson(
        options: BackupOptions,
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): Result<Pair<String, BackupSummary>> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        try {
            onProgress(10, "Gathering academic profile & preferences...")
            val prefs = context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)
            val preferencesMap = mutableMapOf<String, String>()
            if (options.includePreferences) {
                prefs.all.forEach { (key, value) ->
                    if (value is String) preferencesMap[key] = value
                }
            }

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Backup was cancelled."))

            onProgress(30, "Reading enrolled courses, units, and notes...")
            val courses = if (options.includeCoursesAndNotes) repository.getAllCoursesList() else emptyList()
            val units = if (options.includeCoursesAndNotes) repository.getAllUnitsList() else emptyList()
            val topics = if (options.includeCoursesAndNotes) repository.getAllTopicsList() else emptyList()
            val materials = if (options.includeCoursesAndNotes) repository.getAllMaterialsList() else emptyList()

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Backup was cancelled."))

            onProgress(60, "Exporting timetable classes & study sessions...")
            val classes = if (options.includeTimetableAndPlanner) repository.getAllLectureClassesList() else emptyList()
            val sessions = if (options.includeTimetableAndPlanner) repository.getAllSessionsList() else emptyList()
            val tasks = if (options.includeTimetableAndPlanner) repository.getAllTasksList() else emptyList()

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Backup was cancelled."))

            onProgress(80, "Serializing flashcards & revision quizzes...")
            val flashcards = if (options.includeRevision) repository.getAllFlashcardsList() else emptyList()
            val quizzes = if (options.includeRevision) repository.getAllQuizzesList() else emptyList()

            val now = System.currentTimeMillis()
            val rootJson = JSONObject().apply {
                put("app", "LECTRA")
                put("version", 1)
                put("timestamp", now)

                // Preferences
                val prefJson = JSONObject()
                preferencesMap.forEach { (k, v) -> prefJson.put(k, v) }
                put("preferences", prefJson)

                // Courses
                val coursesArray = JSONArray()
                courses.forEach { c ->
                    coursesArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("code", c.code)
                        put("title", c.title)
                        put("colorHex", c.colorHex)
                        put("lecturer", c.lecturer)
                        put("semester", c.semester)
                        put("targetWeeklyHours", c.targetWeeklyHours)
                        put("createdAt", c.createdAt)
                    })
                }
                put("courses", coursesArray)

                // Units
                val unitsArray = JSONArray()
                units.forEach { u ->
                    unitsArray.put(JSONObject().apply {
                        put("id", u.id)
                        put("courseId", u.courseId)
                        put("unitNumber", u.unitNumber)
                        put("title", u.title)
                        put("description", u.description)
                    })
                }
                put("units", unitsArray)

                // Topics
                val topicsArray = JSONArray()
                topics.forEach { t ->
                    topicsArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("unitId", t.unitId)
                        put("courseId", t.courseId)
                        put("title", t.title)
                        put("summaryNotes", t.summaryNotes)
                        put("isMastered", t.isMastered)
                    })
                }
                put("topics", topicsArray)

                // Materials
                val materialsArray = JSONArray()
                materials.forEach { m ->
                    materialsArray.put(JSONObject().apply {
                        put("id", m.id)
                        put("courseId", m.courseId)
                        m.unitId?.let { put("unitId", it) }
                        m.topicId?.let { put("topicId", it) }
                        put("title", m.title)
                        put("fileUriString", m.fileUriString)
                        put("fileSizeBytes", m.fileSizeBytes)
                        put("mimeType", m.mimeType)
                        put("importDate", m.importDate)
                        put("lastReadPage", m.lastReadPage)
                        put("totalPages", m.totalPages)
                        m.aiSummary?.let { put("aiSummary", it) }
                    })
                }
                put("materials", materialsArray)

                // Classes
                val classesArray = JSONArray()
                classes.forEach { cl ->
                    classesArray.put(JSONObject().apply {
                        put("id", cl.id)
                        put("courseId", cl.courseId)
                        put("courseCode", cl.courseCode)
                        put("courseName", cl.courseName)
                        put("topicOrLectureName", cl.topicOrLectureName)
                        put("dayOfWeek", cl.dayOfWeek)
                        put("startHour", cl.startHour)
                        put("startMinute", cl.startMinute)
                        put("endHour", cl.endHour)
                        put("endMinute", cl.endMinute)
                        put("venue", cl.venue)
                        put("colorHex", cl.colorHex)
                    })
                }
                put("lecture_classes", classesArray)

                // Sessions
                val sessionsArray = JSONArray()
                sessions.forEach { s ->
                    sessionsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("courseId", s.courseId)
                        put("courseCode", s.courseCode)
                        put("courseTitle", s.courseTitle)
                        put("topicTitle", s.topicTitle)
                        put("dayOfWeek", s.dayOfWeek)
                        put("startHour", s.startHour)
                        put("startMinute", s.startMinute)
                        put("durationMinutes", s.durationMinutes)
                        put("isCompleted", s.isCompleted)
                        put("targetGoal", s.targetGoal)
                    })
                }
                put("study_sessions", sessionsArray)

                // Tasks
                val tasksArray = JSONArray()
                tasks.forEach { tk ->
                    tasksArray.put(JSONObject().apply {
                        put("id", tk.id)
                        put("courseId", tk.courseId)
                        tk.lectureClassId?.let { put("lectureClassId", it) }
                        put("courseCode", tk.courseCode)
                        put("topicTitle", tk.topicTitle)
                        put("title", tk.title)
                        tk.dueDate?.let { put("dueDate", it) }
                        put("priority", tk.priority)
                        put("isCompleted", tk.isCompleted)
                    })
                }
                put("study_tasks", tasksArray)

                // Flashcards
                val flashcardsArray = JSONArray()
                flashcards.forEach { f ->
                    flashcardsArray.put(JSONObject().apply {
                        put("id", f.id)
                        put("courseId", f.courseId)
                        f.topicId?.let { put("topicId", it) }
                        put("frontPrompt", f.frontPrompt)
                        put("backAnswer", f.backAnswer)
                        f.hint?.let { put("hint", it) }
                        put("easeFactor", f.easeFactor.toDouble())
                        put("repetitions", f.repetitions)
                        put("intervalDays", f.intervalDays)
                        put("lastReviewedAt", f.lastReviewedAt)
                        put("nextReviewDate", f.nextReviewDate)
                        f.lastRecallGrade?.let { put("lastRecallGrade", it) }
                    })
                }
                put("flashcards", flashcardsArray)

                // Quizzes
                val quizzesArray = JSONArray()
                quizzes.forEach { q ->
                    quizzesArray.put(JSONObject().apply {
                        put("id", q.id)
                        put("courseId", q.courseId)
                        q.topicId?.let { put("topicId", it) }
                        put("question", q.question)
                        put("optionA", q.optionA)
                        put("optionB", q.optionB)
                        put("optionC", q.optionC)
                        put("optionD", q.optionD)
                        put("correctOptionIndex", q.correctOptionIndex)
                        put("explanation", q.explanation)
                        put("hasAnswered", q.hasAnswered)
                        q.selectedOption?.let { put("selectedOption", it) }
                        q.isCorrect?.let { put("isCorrect", it) }
                    })
                }
                put("quizzes", quizzesArray)
            }

            val jsonString = rootJson.toString(2)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val summary = BackupSummary(
                timestamp = now,
                formattedDate = dateFormat.format(Date(now)),
                coursesCount = courses.size,
                unitsCount = units.size,
                topicsCount = topics.size,
                materialsCount = materials.size,
                classesCount = classes.size,
                sessionsCount = sessions.size,
                tasksCount = tasks.size,
                flashcardsCount = flashcards.size,
                quizzesCount = quizzes.size,
                fileSizeBytes = jsonString.toByteArray().size.toLong()
            )

            onProgress(100, "Backup generated successfully.")
            Result.success(Pair(jsonString, summary))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Writes backup directly to an Android Storage Access Framework (SAF) Uri.
     */
    suspend fun writeBackupToUri(
        destinationUri: Uri,
        options: BackupOptions,
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        val result = createBackupJson(options, onProgress)
        if (result.isFailure) {
            return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to serialize backup."))
        }
        val (jsonString, summary) = result.getOrThrow()

        try {
            onProgress(90, "Writing backup to destination file...")
            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                OutputStreamWriter(outStream).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open destination stream for backup."))

            // Persist last backup record in preferences
            val prefs = context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("last_backup_timestamp", summary.timestamp)
                .putString("last_backup_formatted_date", summary.formattedDate)
                .putInt("last_backup_items_count", summary.totalItemsCount)
                .putString("last_backup_status", "SUCCESS")
                .apply()

            onProgress(100, "Backup saved successfully.")
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads and validates a backup JSON file before restoration.
     */
    suspend fun validateBackupUri(sourceUri: Uri): Result<BackupValidationResult> = withContext(Dispatchers.IO) {
        try {
            val jsonStringBuilder = StringBuilder()
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                BufferedReader(InputStreamReader(inStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonStringBuilder.append(line)
                    }
                }
            } ?: return@withContext Result.failure(Exception("Cannot open backup file."))

            parseBackupString(jsonStringBuilder.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates and parses raw backup JSON string into strongly-typed backup model.
     */
    fun parseBackupString(jsonString: String): Result<BackupValidationResult> {
        return try {
            val rootJson = JSONObject(jsonString)
            if (!rootJson.has("app") || rootJson.getString("app") != "LECTRA") {
                return Result.success(
                    BackupValidationResult(
                        isValid = false,
                        errorMessage = "Invalid backup file: Not a recognized LECTRA archive."
                    )
                )
            }

            val version = rootJson.optInt("version", 1)
            val timestamp = rootJson.optLong("timestamp", System.currentTimeMillis())

            // Preferences
            val prefMap = mutableMapOf<String, String>()
            if (rootJson.has("preferences")) {
                val prefJson = rootJson.getJSONObject("preferences")
                prefJson.keys().forEach { k -> prefMap[k] = prefJson.getString(k) }
            }

            // Courses
            val coursesList = mutableListOf<CourseEntity>()
            val coursesArr = rootJson.optJSONArray("courses") ?: JSONArray()
            for (i in 0 until coursesArr.length()) {
                val o = coursesArr.getJSONObject(i)
                coursesList.add(
                    CourseEntity(
                        id = o.optLong("id", 0L),
                        code = o.getString("code"),
                        title = o.getString("title"),
                        colorHex = o.optString("colorHex", "#D97706"),
                        lecturer = o.optString("lecturer", ""),
                        semester = o.optString("semester", ""),
                        targetWeeklyHours = o.optInt("targetWeeklyHours", 6),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Units
            val unitsList = mutableListOf<UnitEntity>()
            val unitsArr = rootJson.optJSONArray("units") ?: JSONArray()
            for (i in 0 until unitsArr.length()) {
                val o = unitsArr.getJSONObject(i)
                unitsList.add(
                    UnitEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        unitNumber = o.getInt("unitNumber"),
                        title = o.getString("title"),
                        description = o.optString("description", "")
                    )
                )
            }

            // Topics
            val topicsList = mutableListOf<TopicEntity>()
            val topicsArr = rootJson.optJSONArray("topics") ?: JSONArray()
            for (i in 0 until topicsArr.length()) {
                val o = topicsArr.getJSONObject(i)
                topicsList.add(
                    TopicEntity(
                        id = o.optLong("id", 0L),
                        unitId = o.getLong("unitId"),
                        courseId = o.getLong("courseId"),
                        title = o.getString("title"),
                        summaryNotes = o.optString("summaryNotes", ""),
                        isMastered = o.optBoolean("isMastered", false)
                    )
                )
            }

            // Materials
            val materialsList = mutableListOf<LectureMaterialEntity>()
            val materialsArr = rootJson.optJSONArray("materials") ?: JSONArray()
            for (i in 0 until materialsArr.length()) {
                val o = materialsArr.getJSONObject(i)
                materialsList.add(
                    LectureMaterialEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        unitId = if (o.has("unitId")) o.getLong("unitId") else null,
                        topicId = if (o.has("topicId")) o.getLong("topicId") else null,
                        title = o.getString("title"),
                        fileUriString = o.getString("fileUriString"),
                        fileSizeBytes = o.optLong("fileSizeBytes", 0L),
                        mimeType = o.optString("mimeType", "application/pdf"),
                        importDate = o.optLong("importDate", System.currentTimeMillis()),
                        lastReadPage = o.optInt("lastReadPage", 1),
                        totalPages = o.optInt("totalPages", 20),
                        aiSummary = if (o.has("aiSummary")) o.getString("aiSummary") else null
                    )
                )
            }

            // Classes
            val classesList = mutableListOf<LectureClassEntity>()
            val classesArr = rootJson.optJSONArray("lecture_classes") ?: JSONArray()
            for (i in 0 until classesArr.length()) {
                val o = classesArr.getJSONObject(i)
                classesList.add(
                    LectureClassEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        courseCode = o.getString("courseCode"),
                        courseName = o.getString("courseName"),
                        topicOrLectureName = o.getString("topicOrLectureName"),
                        dayOfWeek = o.getInt("dayOfWeek"),
                        startHour = o.getInt("startHour"),
                        startMinute = o.getInt("startMinute"),
                        endHour = o.getInt("endHour"),
                        endMinute = o.getInt("endMinute"),
                        venue = o.optString("venue", ""),
                        colorHex = o.optString("colorHex", "#7165FF")
                    )
                )
            }

            // Sessions
            val sessionsList = mutableListOf<StudySessionEntity>()
            val sessionsArr = rootJson.optJSONArray("study_sessions") ?: JSONArray()
            for (i in 0 until sessionsArr.length()) {
                val o = sessionsArr.getJSONObject(i)
                sessionsList.add(
                    StudySessionEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        courseCode = o.getString("courseCode"),
                        courseTitle = o.getString("courseTitle"),
                        topicTitle = o.getString("topicTitle"),
                        dayOfWeek = o.getInt("dayOfWeek"),
                        startHour = o.getInt("startHour"),
                        startMinute = o.getInt("startMinute"),
                        durationMinutes = o.getInt("durationMinutes"),
                        isCompleted = o.optBoolean("isCompleted", false),
                        targetGoal = o.optString("targetGoal", "")
                    )
                )
            }

            // Tasks
            val tasksList = mutableListOf<StudyTaskEntity>()
            val tasksArr = rootJson.optJSONArray("study_tasks") ?: JSONArray()
            for (i in 0 until tasksArr.length()) {
                val o = tasksArr.getJSONObject(i)
                tasksList.add(
                    StudyTaskEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        lectureClassId = if (o.has("lectureClassId")) o.getLong("lectureClassId") else null,
                        courseCode = o.getString("courseCode"),
                        topicTitle = o.optString("topicTitle", ""),
                        title = o.getString("title"),
                        dueDate = if (o.has("dueDate")) o.getLong("dueDate") else null,
                        priority = o.optString("priority", "HIGH"),
                        isCompleted = o.optBoolean("isCompleted", false)
                    )
                )
            }

            // Flashcards
            val flashcardsList = mutableListOf<FlashcardEntity>()
            val flashcardsArr = rootJson.optJSONArray("flashcards") ?: JSONArray()
            for (i in 0 until flashcardsArr.length()) {
                val o = flashcardsArr.getJSONObject(i)
                flashcardsList.add(
                    FlashcardEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        topicId = if (o.has("topicId")) o.getLong("topicId") else null,
                        frontPrompt = o.getString("frontPrompt"),
                        backAnswer = o.getString("backAnswer"),
                        hint = if (o.has("hint")) o.getString("hint") else null,
                        easeFactor = o.optDouble("easeFactor", 2.5).toFloat(),
                        repetitions = o.optInt("repetitions", 0),
                        intervalDays = o.optInt("intervalDays", 1),
                        lastReviewedAt = o.optLong("lastReviewedAt", 0L),
                        nextReviewDate = o.optLong("nextReviewDate", 0L),
                        lastRecallGrade = if (o.has("lastRecallGrade")) o.getString("lastRecallGrade") else null
                    )
                )
            }

            // Quizzes
            val quizzesList = mutableListOf<QuizQuestionEntity>()
            val quizzesArr = rootJson.optJSONArray("quizzes") ?: JSONArray()
            for (i in 0 until quizzesArr.length()) {
                val o = quizzesArr.getJSONObject(i)
                quizzesList.add(
                    QuizQuestionEntity(
                        id = o.optLong("id", 0L),
                        courseId = o.getLong("courseId"),
                        topicId = if (o.has("topicId")) o.getLong("topicId") else null,
                        question = o.getString("question"),
                        optionA = o.getString("optionA"),
                        optionB = o.getString("optionB"),
                        optionC = o.getString("optionC"),
                        optionD = o.getString("optionD"),
                        correctOptionIndex = o.getInt("correctOptionIndex"),
                        explanation = o.optString("explanation", ""),
                        hasAnswered = o.optBoolean("hasAnswered", false),
                        selectedOption = if (o.has("selectedOption")) o.getInt("selectedOption") else null,
                        isCorrect = if (o.has("isCorrect")) o.getBoolean("isCorrect") else null
                    )
                )
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val summary = BackupSummary(
                timestamp = timestamp,
                formattedDate = dateFormat.format(Date(timestamp)),
                coursesCount = coursesList.size,
                unitsCount = unitsList.size,
                topicsCount = topicsList.size,
                materialsCount = materialsList.size,
                classesCount = classesList.size,
                sessionsCount = sessionsList.size,
                tasksCount = tasksList.size,
                flashcardsCount = flashcardsList.size,
                quizzesCount = quizzesList.size,
                fileSizeBytes = jsonString.length.toLong()
            )

            val backupData = LectraBackupData(
                version = version,
                timestamp = timestamp,
                preferences = prefMap,
                courses = coursesList,
                units = unitsList,
                topics = topicsList,
                materials = materialsList,
                lectureClasses = classesList,
                sessions = sessionsList,
                tasks = tasksList,
                flashcards = flashcardsList,
                quizzes = quizzesList
            )

            Result.success(
                BackupValidationResult(
                    isValid = true,
                    version = version,
                    timestamp = timestamp,
                    summary = summary,
                    data = backupData
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores data into local Room database with explicit mode (MERGE or REPLACE).
     */
    suspend fun restoreBackupData(
        data: LectraBackupData,
        mode: RestoreMode,
        onProgress: (percent: Int, message: String) -> Unit = { _, _ -> }
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        try {
            if (mode == RestoreMode.REPLACE) {
                onProgress(15, "Clearing existing local database tables...")
                repository.clearAllTables()
            }

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Restore was cancelled."))

            onProgress(35, "Restoring courses, units, and topic notes...")
            if (data.courses.isNotEmpty()) repository.insertCourses(data.courses)
            if (data.units.isNotEmpty()) repository.insertUnits(data.units)
            if (data.topics.isNotEmpty()) repository.insertTopics(data.topics)
            if (data.materials.isNotEmpty()) repository.insertMaterials(data.materials)

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Restore was cancelled."))

            onProgress(65, "Restoring timetable classes and study planner...")
            if (data.lectureClasses.isNotEmpty()) repository.insertLectureClasses(data.lectureClasses)
            if (data.sessions.isNotEmpty()) repository.insertSessions(data.sessions)
            if (data.tasks.isNotEmpty()) repository.insertTasks(data.tasks)

            if (isCancelled.get()) return@withContext Result.failure(InterruptedException("Restore was cancelled."))

            onProgress(85, "Restoring revision flashcards and practice quizzes...")
            if (data.flashcards.isNotEmpty()) repository.insertFlashcards(data.flashcards)
            if (data.quizzes.isNotEmpty()) repository.insertQuizzes(data.quizzes)

            // Restore preferences
            val prefs = context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            data.preferences.forEach { (k, v) -> editor.putString(k, v) }
            editor.putString("last_restore_status", "SUCCESS")
            editor.putLong("last_restore_timestamp", System.currentTimeMillis())
            editor.apply()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val summary = BackupSummary(
                timestamp = data.timestamp,
                formattedDate = dateFormat.format(Date(data.timestamp)),
                coursesCount = data.courses.size,
                unitsCount = data.units.size,
                topicsCount = data.topics.size,
                materialsCount = data.materials.size,
                classesCount = data.lectureClasses.size,
                sessionsCount = data.sessions.size,
                tasksCount = data.tasks.size,
                flashcardsCount = data.flashcards.size,
                quizzesCount = data.quizzes.size
            )

            onProgress(100, "Academic records restored successfully.")
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to read last backup summary from SharedPreferences.
     */
    fun getLastBackupInfo(): Pair<Long, String>? {
        val prefs = context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("last_backup_timestamp", 0L)
        if (timestamp == 0L) return null
        val dateStr = prefs.getString("last_backup_formatted_date", "") ?: ""
        val itemsCount = prefs.getInt("last_backup_items_count", 0)
        return Pair(timestamp, "$dateStr ($itemsCount items)")
    }
}
