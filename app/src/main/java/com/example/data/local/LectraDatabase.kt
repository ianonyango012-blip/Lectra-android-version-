package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.FlashcardEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.QuizQuestionEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.data.model.UnitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        CourseEntity::class,
        UnitEntity::class,
        TopicEntity::class,
        LectureMaterialEntity::class,
        LectureClassEntity::class,
        StudySessionEntity::class,
        StudyTaskEntity::class,
        FlashcardEntity::class,
        QuizQuestionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LectraDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun lectureClassDao(): LectureClassDao
    abstract fun lectureMaterialDao(): LectureMaterialDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun revisionDao(): RevisionDao

    companion object {
        @Volatile
        private var INSTANCE: LectraDatabase? = null

        fun getDatabase(context: Context, coroutineScope: CoroutineScope): LectraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LectraDatabase::class.java,
                    "lectra_academic.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(LectraDatabaseCallback(coroutineScope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LectraDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Production clean start: Do not auto-populate initial data on database creation.
            // Users configure their semester via SemesterSetupScreen or load demo data explicitly via Settings.
        }
    }
}

suspend fun populateInitialData(database: LectraDatabase) {
    try {
        val courseDao = database.courseDao()
        val classDao = database.lectureClassDao()
        val materialDao = database.lectureMaterialDao()
        val sessionDao = database.studySessionDao()
        val revisionDao = database.revisionDao()

    // 1. Courses
    val csCourseId = courseDao.insertCourse(
        CourseEntity(
            code = "CS 301",
            title = "Distributed Systems & Cloud Computing",
            colorHex = "#D97706", // Amber
            lecturer = "Prof. Elena Rostova",
            semester = "Term 1 - 2026",
            targetWeeklyHours = 8
        )
    )

    val bioCourseId = courseDao.insertCourse(
        CourseEntity(
            code = "BIO 210",
            title = "Molecular Genetics & Cellular Biology",
            colorHex = "#2D5A46", // Sage Forest
            lecturer = "Dr. Marcus Sterling",
            semester = "Term 1 - 2026",
            targetWeeklyHours = 6
        )
    )

    val mathCourseId = courseDao.insertCourse(
        CourseEntity(
            code = "MATH 240",
            title = "Linear Algebra & Optimization",
            colorHex = "#B45309", // Cinnamon
            lecturer = "Prof. Sarah Jenkins",
            semester = "Term 1 - 2026",
            targetWeeklyHours = 5
        )
    )

    // 2. Units & Topics
    val csUnit1Id = courseDao.insertUnit(
        UnitEntity(
            courseId = csCourseId,
            unitNumber = 1,
            title = "Consensus & Fault Tolerance",
            description = "Paxos, Raft, State Machine Replication, and Quorum systems"
        )
    )
    val csUnit2Id = courseDao.insertUnit(
        UnitEntity(
            courseId = csCourseId,
            unitNumber = 2,
            title = "Distributed Storage & LSM Trees",
            description = "SSTables, MemTable architectures, and compaction heuristics"
        )
    )

    val csTopic1 = courseDao.insertTopic(
        TopicEntity(
            unitId = csUnit1Id,
            courseId = csCourseId,
            title = "Raft Leader Election & Safety Invariants",
            summaryNotes = "Split votes are mitigated via randomized election timeouts (150-300ms). Heartbeat intervals must be significantly smaller than election timeouts.",
            isMastered = true
        )
    )
    courseDao.insertTopic(
        TopicEntity(
            unitId = csUnit1Id,
            courseId = csCourseId,
            title = "Log Compaction & State Snapshots",
            summaryNotes = "Snapshotting transfers state-machine image to disk and discards preceding committed log entries.",
            isMastered = false
        )
    )
    courseDao.insertTopic(
        TopicEntity(
            unitId = csUnit2Id,
            courseId = csCourseId,
            title = "LSM-Trees vs B-Trees",
            summaryNotes = "Sequential append writes in memory vs random disk page updates. Bloom filters guard against read misses.",
            isMastered = false
        )
    )

    val bioUnit1Id = courseDao.insertUnit(
        UnitEntity(
            courseId = bioCourseId,
            unitNumber = 1,
            title = "DNA Replication Machinery",
            description = "Helicase unwinding, Okazaki fragments, and proofreading enzymes"
        )
    )
    courseDao.insertTopic(
        TopicEntity(
            unitId = bioUnit1Id,
            courseId = bioCourseId,
            title = "Leading & Lagging Strand Polymerization",
            summaryNotes = "DNA Polymerase III synthesizes 5' to 3'. Lagging strand requires continuous loop priming.",
            isMastered = true
        )
    )
    courseDao.insertTopic(
        TopicEntity(
            unitId = bioUnit1Id,
            courseId = bioCourseId,
            title = "Topoisomerase Supercoil Strain Release",
            summaryNotes = "Topoisomerase breaks and re-ligates phosphodiester backbones to relieve torsional strain ahead of Helicase.",
            isMastered = false
        )
    )

    val mathUnit1Id = courseDao.insertUnit(
        UnitEntity(
            courseId = mathCourseId,
            unitNumber = 1,
            title = "Matrix Decompositions",
            description = "Eigendecomposition, Singular Value Decomposition (SVD), and PCA"
        )
    )
    courseDao.insertTopic(
        TopicEntity(
            unitId = mathUnit1Id,
            courseId = mathCourseId,
            title = "Singular Value Decomposition (SVD)",
            summaryNotes = "A = U * Sigma * V^T, where U and V are orthonormal eigenvectors of A*A^T and A^T*A.",
            isMastered = false
        )
    )

    // Current day calculation
    val cal = Calendar.getInstance()
    val todayCalDay = cal.get(Calendar.DAY_OF_WEEK)
    val todayDayOfWeek = if (todayCalDay == Calendar.SUNDAY) 7 else todayCalDay - 1
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)

    // 3. Timetable Lecture Classes:
    // We seed classes for multiple days of the week, including today!
    // For today, we schedule an upcoming class and an active/earlier class so the user immediately experiences the real-time timeline.
    val todayUpcomingHour = (currentHour + 1).coerceAtMost(22)
    val todayUpcomingEndHour = (todayUpcomingHour + 1).coerceAtMost(23)

    val classTodayId = classDao.insertLectureClass(
        LectureClassEntity(
            courseId = csCourseId,
            courseCode = "CS 301",
            courseName = "Distributed Systems & Cloud Computing",
            topicOrLectureName = "Raft Consensus Protocols & Log Invariants",
            dayOfWeek = todayDayOfWeek,
            startHour = todayUpcomingHour,
            startMinute = 0,
            endHour = todayUpcomingEndHour,
            endMinute = 30,
            venue = "Turing Lecture Hall 104",
            colorHex = "#D97706"
        )
    )

    // Second class for today if early in day or next scheduled block
    if (todayUpcomingEndHour < 21) {
        val classToday2Id = classDao.insertLectureClass(
            LectureClassEntity(
                courseId = mathCourseId,
                courseCode = "MATH 240",
                courseName = "Linear Algebra & Optimization",
                topicOrLectureName = "Singular Value Decomposition (SVD) Projections",
                dayOfWeek = todayDayOfWeek,
                startHour = todayUpcomingEndHour + 1,
                startMinute = 0,
                endHour = todayUpcomingEndHour + 2,
                endMinute = 15,
                venue = "Euler Building Room 302",
                colorHex = "#B45309"
            )
        )

        sessionDao.insertTask(
            StudyTaskEntity(
                courseId = mathCourseId,
                courseCode = "MATH 240",
                lectureClassId = classToday2Id,
                topicTitle = "Singular Value Decomposition (SVD) Projections",
                title = "Problem Set 4: Derive singular values from covariance matrix",
                dueDate = System.currentTimeMillis() + 86400000L * 2,
                priority = "MEDIUM",
                isCompleted = false
            )
        )
    }

    // Associate assignment directly with today's main upcoming class:
    sessionDao.insertTask(
        StudyTaskEntity(
            courseId = csCourseId,
            courseCode = "CS 301",
            lectureClassId = classTodayId,
            topicTitle = "Raft Consensus Protocols & Log Invariants",
            title = "Lab 2: Implement Raft Leader Election in Go",
            dueDate = System.currentTimeMillis() + 86400000L * 3,
            priority = "HIGH",
            isCompleted = false
        )
    )

    // Classes for other days across the week
    val nextDay1 = if (todayDayOfWeek % 7 + 1 == 7) 1 else todayDayOfWeek % 7 + 1
    val nextDay2 = if ((todayDayOfWeek + 1) % 7 + 1 == 7) 2 else (todayDayOfWeek + 1) % 7 + 1

    val classNext1Id = classDao.insertLectureClass(
        LectureClassEntity(
            courseId = bioCourseId,
            courseCode = "BIO 210",
            courseName = "Molecular Genetics & Cellular Biology",
            topicOrLectureName = "DNA Replication Machinery & Polymerase III",
            dayOfWeek = nextDay1,
            startHour = 10,
            startMinute = 0,
            endHour = 11,
            endMinute = 30,
            venue = "Darwin Bio-Labs Room 215",
            colorHex = "#2D5A46"
        )
    )

    sessionDao.insertTask(
        StudyTaskEntity(
            courseId = bioCourseId,
            courseCode = "BIO 210",
            lectureClassId = classNext1Id,
            topicTitle = "DNA Replication Machinery & Polymerase III",
            title = "Literature Critique: Topoisomerase II Enzymatic Inhabitation",
            dueDate = System.currentTimeMillis() + 86400000L * 5,
            priority = "HIGH",
            isCompleted = false
        )
    )

    classDao.insertLectureClass(
        LectureClassEntity(
            courseId = csCourseId,
            courseCode = "CS 301",
            courseName = "Distributed Systems & Cloud Computing",
            topicOrLectureName = "LSM-Trees, MemTables & Compaction Heuristics",
            dayOfWeek = nextDay2,
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 30,
            venue = "Turing Lecture Hall 104",
            colorHex = "#D97706"
        )
    )

    // 4. Pre-populated Lecture Materials
    materialDao.insertMaterial(
        LectureMaterialEntity(
            courseId = csCourseId,
            unitId = csUnit1Id,
            topicId = csTopic1,
            title = "Lecture 04 - Raft Leader Election & Quorums.pdf",
            fileUriString = "lectra://bundled/cs301_raft_consensus.pdf",
            fileSizeBytes = 4820000,
            lastReadPage = 14,
            totalPages = 38,
            aiSummary = "Analyzes distributed consensus protocols. Demonstrates how Raft decomposes agreement into election, replication, and commitment invariants while guaranteeing safety under arbitrary network partitions."
        )
    )
    materialDao.insertMaterial(
        LectureMaterialEntity(
            courseId = csCourseId,
            unitId = csUnit2Id,
            title = "Lecture 08 - Log Structured Merge Storage.pdf",
            fileUriString = "lectra://bundled/cs301_lsm_trees.pdf",
            fileSizeBytes = 3200000,
            lastReadPage = 6,
            totalPages = 28,
            aiSummary = "Examines storage engine trade-offs between write-heavy workloads and read latencies. Covers SSTable compaction strategies and Bloom filter false-positive probability tuning."
        )
    )
    materialDao.insertMaterial(
        LectureMaterialEntity(
            courseId = bioCourseId,
            unitId = bioUnit1Id,
            title = "Bio 210 - Molecular Fork Architecture.pdf",
            fileUriString = "lectra://bundled/bio210_replication_fork.pdf",
            fileSizeBytes = 6100000,
            lastReadPage = 18,
            totalPages = 45,
            aiSummary = "Details the multi-enzyme replisome complex at eukaryotic replication forks, highlighting PCNA sliding clamps and topoisomerase strain release."
        )
    )

    // 5. Study Sessions
    sessionDao.insertSession(
        StudySessionEntity(
            courseId = csCourseId,
            courseCode = "CS 301",
            courseTitle = "Distributed Systems",
            topicTitle = "Raft Consensus Invariant Proofs",
            dayOfWeek = todayDayOfWeek,
            startHour = 18,
            startMinute = 0,
            durationMinutes = 90,
            isCompleted = false,
            targetGoal = "Complete slide deck 4 and write 5 active recall flashcards"
        )
    )

    // 6. Flashcards
    revisionDao.insertFlashcard(
        FlashcardEntity(
            courseId = csCourseId,
            frontPrompt = "What are the two sub-problems into which Raft decomposes consensus?",
            backAnswer = "Leader Election and Log Replication. Once a leader is established, consensus simplifies to normal log append flow.",
            hint = "Think about authority delegation first, then data flow.",
            lastRecallGrade = "GOOD"
        )
    )
    revisionDao.insertFlashcard(
        FlashcardEntity(
            courseId = csCourseId,
            frontPrompt = "Why do LSM-Trees exhibit superior write throughput compared to traditional B-Trees?",
            backAnswer = "LSM-Trees append writes sequentially into an in-memory MemTable before flushing to SSTables, converting random I/O into sequential disk I/O.",
            hint = "Consider sequential vs random disk operations.",
            lastRecallGrade = "AGAIN" // Weak revision area!
        )
    )
    revisionDao.insertFlashcard(
        FlashcardEntity(
            courseId = bioCourseId,
            frontPrompt = "What is the primary enzymatic function of DNA Topoisomerase ahead of the replication fork?",
            backAnswer = "Relieves torsional supercoiling stress generated by Helicase unwinding the double helix.",
            hint = "Relieves structural tension in the strand.",
            lastRecallGrade = "HARD" // Weak revision area!
        )
    )

    // 7. Practice Quizzes
    revisionDao.insertQuiz(
        QuizQuestionEntity(
            courseId = csCourseId,
            question = "Which component in an LSM-Tree architecture prevents expensive disk lookups for keys that do not exist?",
            optionA = "Bloom Filter",
            optionB = "Write-Ahead Log (WAL)",
            optionC = "Background Compactor",
            optionD = "Vector Clock Matrix",
            correctOptionIndex = 0,
            explanation = "Bloom Filters provide an in-memory probabilistic check with zero false negatives, skipping disk I/O entirely if a key is absent.",
            hasAnswered = false
        )
    )
    revisionDao.insertQuiz(
        QuizQuestionEntity(
            courseId = bioCourseId,
            question = "Which enzyme synthesizes short RNA primers required to initiate DNA replication on the lagging strand?",
            optionA = "DNA Primase",
            optionB = "DNA Ligase",
            optionC = "Telomerase",
            optionD = "Exonuclease III",
            correctOptionIndex = 0,
            explanation = "DNA Primase synthesizes RNA oligonucleotides (~10 nucleotides) providing the free 3'-OH group needed by DNA Polymerase.",
            hasAnswered = false
        )
    )
    } catch (e: Exception) {
        android.util.Log.e("LectraDatabase", "Error during populateInitialData", e)
    }
}
