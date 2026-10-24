package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import kotlinx.coroutines.flow.Flow

class LectraRepository(
    private val database: LectraDatabase,
    private val context: Context
) {
    private val courseDao = database.courseDao()
    private val classDao = database.lectureClassDao()
    private val materialDao = database.lectureMaterialDao()
    private val sessionDao = database.studySessionDao()
    private val revisionDao = database.revisionDao()

    // Courses & Topics
    fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAllCourses()
    fun getCourseById(courseId: Long): Flow<CourseEntity?> = courseDao.getCourseById(courseId)
    suspend fun insertCourse(course: CourseEntity): Long = courseDao.insertCourse(course)
    suspend fun updateCourse(course: CourseEntity) = courseDao.updateCourse(course)
    suspend fun deleteCourse(course: CourseEntity) = courseDao.deleteCourse(course)

    fun getUnitsForCourse(courseId: Long): Flow<List<UnitEntity>> = courseDao.getUnitsForCourse(courseId)
    suspend fun insertUnit(unit: UnitEntity): Long = courseDao.insertUnit(unit)
    suspend fun deleteUnit(unit: UnitEntity) = courseDao.deleteUnit(unit)

    fun getTopicsForUnit(unitId: Long): Flow<List<TopicEntity>> = courseDao.getTopicsForUnit(unitId)
    fun getAllTopicsForCourse(courseId: Long): Flow<List<TopicEntity>> = courseDao.getAllTopicsForCourse(courseId)
    suspend fun insertTopic(topic: TopicEntity): Long = courseDao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = courseDao.updateTopic(topic)
    suspend fun deleteTopic(topic: TopicEntity) = courseDao.deleteTopic(topic)

    // Lecture Classes (Actual University Timetable)
    fun getAllLectureClasses(): Flow<List<LectureClassEntity>> = classDao.getAllLectureClasses()
    fun getClassesForDay(dayOfWeek: Int): Flow<List<LectureClassEntity>> = classDao.getClassesForDay(dayOfWeek)
    suspend fun insertLectureClass(lectureClass: LectureClassEntity): Long = classDao.insertLectureClass(lectureClass)
    suspend fun updateLectureClass(lectureClass: LectureClassEntity) = classDao.updateLectureClass(lectureClass)
    suspend fun deleteLectureClass(lectureClass: LectureClassEntity) = classDao.deleteLectureClass(lectureClass)

    // Materials
    fun getAllMaterials(): Flow<List<LectureMaterialEntity>> = materialDao.getAllMaterials()
    fun getMaterialsForCourse(courseId: Long): Flow<List<LectureMaterialEntity>> = materialDao.getMaterialsForCourse(courseId)
    suspend fun insertMaterial(material: LectureMaterialEntity): Long = materialDao.insertMaterial(material)
    suspend fun updateMaterial(material: LectureMaterialEntity) = materialDao.updateMaterial(material)
    suspend fun deleteMaterial(material: LectureMaterialEntity) = materialDao.deleteMaterial(material)

    // Study Sessions & Tasks
    fun getAllSessions(): Flow<List<StudySessionEntity>> = sessionDao.getAllSessions()
    fun getSessionsForDay(dayOfWeek: Int): Flow<List<StudySessionEntity>> = sessionDao.getSessionsForDay(dayOfWeek)
    suspend fun insertSession(session: StudySessionEntity): Long = sessionDao.insertSession(session)
    suspend fun updateSession(session: StudySessionEntity) = sessionDao.updateSession(session)
    suspend fun deleteSession(session: StudySessionEntity) = sessionDao.deleteSession(session)

    fun getAllTasks(): Flow<List<StudyTaskEntity>> = sessionDao.getAllTasks()
    fun getTasksForClass(classId: Long): Flow<List<StudyTaskEntity>> = sessionDao.getTasksForClass(classId)
    suspend fun insertTask(task: StudyTaskEntity): Long = sessionDao.insertTask(task)
    suspend fun updateTask(task: StudyTaskEntity) = sessionDao.updateTask(task)
    suspend fun deleteTask(task: StudyTaskEntity) = sessionDao.deleteTask(task)

    // Revision (Flashcards & Quizzes)
    fun getAllFlashcards(): Flow<List<FlashcardEntity>> = revisionDao.getAllFlashcards()
    fun getFlashcardsForCourse(courseId: Long): Flow<List<FlashcardEntity>> = revisionDao.getFlashcardsForCourse(courseId)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long = revisionDao.insertFlashcard(flashcard)
    suspend fun updateFlashcard(flashcard: FlashcardEntity) = revisionDao.updateFlashcard(flashcard)
    suspend fun deleteFlashcard(flashcard: FlashcardEntity) = revisionDao.deleteFlashcard(flashcard)

    fun getAllQuizzes(): Flow<List<QuizQuestionEntity>> = revisionDao.getAllQuizzes()
    fun getQuizzesForCourse(courseId: Long): Flow<List<QuizQuestionEntity>> = revisionDao.getQuizzesForCourse(courseId)
    suspend fun insertQuiz(quiz: QuizQuestionEntity): Long = revisionDao.insertQuiz(quiz)
    suspend fun updateQuiz(quiz: QuizQuestionEntity) = revisionDao.updateQuiz(quiz)
    suspend fun deleteQuiz(quiz: QuizQuestionEntity) = revisionDao.deleteQuiz(quiz)
    suspend fun resetQuizzesForCourse(courseId: Long) = revisionDao.resetQuizProgressForCourse(courseId)
    suspend fun resetAllQuizzes() = revisionDao.resetAllQuizProgress()

    // SAF Document Helper
    fun resolveDocumentDetails(uri: Uri): Pair<String, Long> {
        var name = "Document.pdf"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            name = uri.lastPathSegment ?: "Document.pdf"
        }
        return Pair(name, size)
    }

    suspend fun resetAndReseedDatabase() {
        database.clearAllTables()
        com.example.data.local.populateInitialData(database)
    }
}
