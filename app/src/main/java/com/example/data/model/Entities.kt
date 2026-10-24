package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val title: String,
    val colorHex: String,
    val lecturer: String,
    val semester: String,
    val targetWeeklyHours: Int = 6,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val unitNumber: Int,
    val title: String,
    val description: String = ""
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("unitId"), Index("courseId")]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val courseId: Long,
    val title: String,
    val summaryNotes: String = "",
    val isMastered: Boolean = false
)

@Entity(
    tableName = "lecture_materials",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("unitId"), Index("topicId")]
)
data class LectureMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val unitId: Long? = null,
    val topicId: Long? = null,
    val title: String,
    val fileUriString: String,
    val fileSizeBytes: Long = 0,
    val mimeType: String = "application/pdf",
    val importDate: Long = System.currentTimeMillis(),
    val lastReadPage: Int = 1,
    val totalPages: Int = 24,
    val aiSummary: String? = null
)

@Entity(tableName = "lecture_classes")
data class LectureClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val courseName: String,
    val topicOrLectureName: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val endHour: Int, // 0..23
    val endMinute: Int, // 0..59
    val venue: String = "", // e.g. "Turing Lecture Hall 104" or "" if absent
    val colorHex: String = "#D97706"
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val courseTitle: String,
    val topicTitle: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val startHour: Int, // 0..23
    val startMinute: Int, // 0..59
    val durationMinutes: Int = 60,
    val isCompleted: Boolean = false,
    val targetGoal: String = "",
    val scheduledDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val lectureClassId: Long? = null, // link to corresponding scheduled class
    val topicTitle: String = "",
    val title: String,
    val dueDate: Long? = null, // epoch deadline timestamp if available, nullable
    val priority: String = "HIGH", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val topicId: Long? = null,
    val frontPrompt: String,
    val backAnswer: String,
    val hint: String? = null,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val intervalDays: Int = 1,
    val lastReviewedAt: Long = 0,
    val nextReviewDate: Long = 0,
    val lastRecallGrade: String? = null // AGAIN, HARD, GOOD, EASY
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val topicId: Long? = null,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int, // 0..3
    val explanation: String,
    val hasAnswered: Boolean = false,
    val selectedOption: Int? = null,
    val isCorrect: Boolean? = null
)
