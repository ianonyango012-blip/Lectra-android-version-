package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CourseEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.PdfAnnotationEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.data.model.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY createdAt DESC")
    suspend fun getAllCoursesList(): List<CourseEntity>

    @Query("SELECT * FROM units ORDER BY unitNumber ASC")
    suspend fun getAllUnitsList(): List<UnitEntity>

    @Query("SELECT * FROM units ORDER BY unitNumber ASC")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    suspend fun getAllTopicsList(): List<TopicEntity>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseById(courseId: Long): Flow<CourseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>): List<Long>

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    // Units
    @Query("SELECT * FROM units WHERE courseId = :courseId ORDER BY unitNumber ASC")
    fun getUnitsForCourse(courseId: Long): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>): List<Long>

    @Delete
    suspend fun deleteUnit(unit: UnitEntity)

    // Topics
    @Query("SELECT * FROM topics WHERE unitId = :unitId ORDER BY id ASC")
    fun getTopicsForUnit(unitId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE courseId = :courseId ORDER BY id ASC")
    fun getAllTopicsForCourse(courseId: Long): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)
}

@Dao
interface LectureClassDao {
    @Query("SELECT * FROM lecture_classes ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllLectureClasses(): Flow<List<LectureClassEntity>>

    @Query("SELECT * FROM lecture_classes ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    suspend fun getAllLectureClassesList(): List<LectureClassEntity>

    @Query("SELECT * FROM lecture_classes WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    fun getClassesForDay(dayOfWeek: Int): Flow<List<LectureClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectureClass(lectureClass: LectureClassEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectureClasses(classes: List<LectureClassEntity>): List<Long>

    @Update
    suspend fun updateLectureClass(lectureClass: LectureClassEntity)

    @Delete
    suspend fun deleteLectureClass(lectureClass: LectureClassEntity)
}

@Dao
interface LectureMaterialDao {
    @Query("SELECT * FROM lecture_materials ORDER BY importDate DESC")
    fun getAllMaterials(): Flow<List<LectureMaterialEntity>>

    @Query("SELECT * FROM lecture_materials ORDER BY importDate DESC")
    suspend fun getAllMaterialsList(): List<LectureMaterialEntity>

    @Query("SELECT * FROM lecture_materials WHERE courseId = :courseId ORDER BY importDate DESC")
    fun getMaterialsForCourse(courseId: Long): Flow<List<LectureMaterialEntity>>

    @Query("SELECT * FROM lecture_materials WHERE id = :id LIMIT 1")
    fun getMaterialById(id: Long): Flow<LectureMaterialEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: LectureMaterialEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterials(materials: List<LectureMaterialEntity>): List<Long>

    @Update
    suspend fun updateMaterial(material: LectureMaterialEntity)

    @Delete
    suspend fun deleteMaterial(material: LectureMaterialEntity)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    suspend fun getAllSessionsList(): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    fun getSessionsForDay(dayOfWeek: Int): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySessionEntity>): List<Long>

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)

    // Tasks
    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, dueDate ASC")
    suspend fun getAllTasksList(): List<StudyTaskEntity>

    @Query("SELECT * FROM study_tasks WHERE lectureClassId = :classId ORDER BY isCompleted ASC, dueDate ASC")
    fun getTasksForClass(classId: Long): Flow<List<StudyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    suspend fun getAllFlashcardsList(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE courseId = :courseId ORDER BY id DESC")
    fun getFlashcardsForCourse(courseId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(flashcard: FlashcardEntity)

    // Quizzes
    @Query("SELECT * FROM quiz_questions ORDER BY id ASC")
    fun getAllQuizzes(): Flow<List<QuizQuestionEntity>>

    @Query("SELECT * FROM quiz_questions ORDER BY id ASC")
    suspend fun getAllQuizzesList(): List<QuizQuestionEntity>

    @Query("SELECT * FROM quiz_questions WHERE courseId = :courseId ORDER BY id ASC")
    fun getQuizzesForCourse(courseId: Long): Flow<List<QuizQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizQuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizQuestionEntity>): List<Long>

    @Update
    suspend fun updateQuiz(quiz: QuizQuestionEntity)

    @Delete
    suspend fun deleteQuiz(quiz: QuizQuestionEntity)

    @Query("UPDATE quiz_questions SET hasAnswered = 0, selectedOption = NULL, isCorrect = NULL WHERE courseId = :courseId")
    suspend fun resetQuizProgressForCourse(courseId: Long)

    @Query("UPDATE quiz_questions SET hasAnswered = 0, selectedOption = NULL, isCorrect = NULL")
    suspend fun resetAllQuizProgress()
}

@Dao
interface PdfAnnotationDao {
    @Query("SELECT * FROM pdf_annotations WHERE materialId = :materialId ORDER BY pageIndex ASC, createdAt ASC")
    fun getAnnotationsForMaterial(materialId: Long): Flow<List<PdfAnnotationEntity>>

    @Query("SELECT * FROM pdf_annotations WHERE materialId = :materialId ORDER BY pageIndex ASC, createdAt ASC")
    suspend fun getAnnotationsForMaterialList(materialId: Long): List<PdfAnnotationEntity>

    @Query("SELECT * FROM pdf_annotations WHERE materialId = :materialId AND pageIndex = :pageIndex ORDER BY createdAt ASC")
    fun getAnnotationsForPage(materialId: Long, pageIndex: Int): Flow<List<PdfAnnotationEntity>>

    @Query("SELECT * FROM pdf_annotations WHERE materialId = :materialId AND type = 'BOOKMARK' ORDER BY pageIndex ASC")
    fun getBookmarksForMaterial(materialId: Long): Flow<List<PdfAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PdfAnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<PdfAnnotationEntity>): List<Long>

    @Update
    suspend fun updateAnnotation(annotation: PdfAnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: PdfAnnotationEntity)

    @Query("DELETE FROM pdf_annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM pdf_annotations WHERE materialId = :materialId AND pageIndex = :pageIndex AND type = 'BOOKMARK'")
    suspend fun removeBookmark(materialId: Long, pageIndex: Int)

    @Query("DELETE FROM pdf_annotations WHERE materialId = :materialId")
    suspend fun deleteAnnotationsForMaterial(materialId: Long)
}
