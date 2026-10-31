package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.ThemeMode

/**
 * Centralized preferences manager for user-configurable settings.
 * All settings persist locally via SharedPreferences and survive app restarts.
 */
class LectraPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)

    // ==========================================
    // 1. APPEARANCE PREFERENCES
    // ==========================================
    var themeMode: ThemeMode
        get() {
            val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return runCatching { ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    // ==========================================
    // 2. ACADEMIC NOTIFICATION PREFERENCES
    // ==========================================
    var masterNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_MASTER, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_MASTER, value).apply()

    var notifyClasses: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_CLASSES, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_CLASSES, value).apply()

    var notifyAssignments: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASSIGNMENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ASSIGNMENTS, value).apply()

    var notifySessions: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_SESSIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_SESSIONS, value).apply()

    var notifyRevision: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_REVISION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_REVISION, value).apply()

    var notificationLeadTimeMinutes: Int
        get() = prefs.getInt(KEY_NOTIF_LEAD_TIME, 15)
        set(value) = prefs.edit().putInt(KEY_NOTIF_LEAD_TIME, value).apply()

    var assignmentReminder2Days: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASSIGNMENT_2_DAYS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ASSIGNMENT_2_DAYS, value).apply()

    var assignmentReminder1Day: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASSIGNMENT_1_DAY, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ASSIGNMENT_1_DAY, value).apply()

    var assignmentReminderDueDate: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_ASSIGNMENT_DUE_DATE, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_ASSIGNMENT_DUE_DATE, value).apply()

    // ==========================================
    // 3. TIMETABLE PREFERENCES
    // ==========================================
    var timetableFirstDayOfWeek: Int
        get() = prefs.getInt(KEY_TIMETABLE_FIRST_DAY, 1) // 1 = Monday, 7 = Sunday
        set(value) = prefs.edit().putInt(KEY_TIMETABLE_FIRST_DAY, value).apply()

    var timetableClassReminderMinutes: Int
        get() = prefs.getInt(KEY_TIMETABLE_REMINDER_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_TIMETABLE_REMINDER_MINUTES, value).apply()

    var timetableDefaultViewMode: String
        get() = prefs.getString(KEY_TIMETABLE_VIEW_MODE, "DAILY") ?: "DAILY"
        set(value) = prefs.edit().putString(KEY_TIMETABLE_VIEW_MODE, value).apply()

    // ==========================================
    // 4. READING PREFERENCES
    // ==========================================
    var pdfOpeningMode: String
        get() = prefs.getString(KEY_PDF_OPENING_MODE, "built_in") ?: "built_in"
        set(value) = prefs.edit().putString(KEY_PDF_OPENING_MODE, value).apply()

    var pdfNightReadingMode: String
        get() = prefs.getString(KEY_PDF_NIGHT_MODE, "STANDARD") ?: "STANDARD"
        set(value) = prefs.edit().putString(KEY_PDF_NIGHT_MODE, value).apply()

    var pdfDefaultPageDisplay: String
        get() = prefs.getString(KEY_PDF_PAGE_DISPLAY, "CONTINUOUS") ?: "CONTINUOUS"
        set(value) = prefs.edit().putString(KEY_PDF_PAGE_DISPLAY, value).apply()

    var pdfAutoSaveAnnotations: Boolean
        get() = prefs.getBoolean(KEY_PDF_AUTO_SAVE_ANNOTATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_PDF_AUTO_SAVE_ANNOTATIONS, value).apply()

    // ==========================================
    // 5. REVISION PREFERENCES
    // ==========================================
    var flashcardReviewOrder: String
        get() = prefs.getString(KEY_FLASHCARD_REVIEW_ORDER, "SHUFFLE") ?: "SHUFFLE"
        set(value) = prefs.edit().putString(KEY_FLASHCARD_REVIEW_ORDER, value).apply()

    var quizDifficultyFilter: String
        get() = prefs.getString(KEY_QUIZ_DIFFICULTY, "ALL") ?: "ALL"
        set(value) = prefs.edit().putString(KEY_QUIZ_DIFFICULTY, value).apply()

    var revisionDurationMinutes: Int
        get() = prefs.getInt(KEY_REVISION_DURATION_MINUTES, 25)
        set(value) = prefs.edit().putInt(KEY_REVISION_DURATION_MINUTES, value).apply()

    // ==========================================
    // 6. CLEAR ALL PREFERENCES
    // ==========================================
    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val KEY_THEME_MODE = "app_theme_mode"
        const val KEY_NOTIF_MASTER = "notif_master_enabled"
        const val KEY_NOTIF_CLASSES = "notif_classes_enabled"
        const val KEY_NOTIF_ASSIGNMENTS = "notif_assignments_enabled"
        const val KEY_NOTIF_SESSIONS = "notif_sessions_enabled"
        const val KEY_NOTIF_REVISION = "notif_revision_enabled"
        const val KEY_NOTIF_LEAD_TIME = "notif_lead_time_minutes"
        const val KEY_NOTIF_ASSIGNMENT_2_DAYS = "notif_assignment_2_days"
        const val KEY_NOTIF_ASSIGNMENT_1_DAY = "notif_assignment_1_day"
        const val KEY_NOTIF_ASSIGNMENT_DUE_DATE = "notif_assignment_due_date"
        const val KEY_TIMETABLE_FIRST_DAY = "timetable_first_day_of_week"
        const val KEY_TIMETABLE_REMINDER_MINUTES = "timetable_class_reminder_minutes"
        const val KEY_TIMETABLE_VIEW_MODE = "timetable_default_view_mode"
        const val KEY_PDF_OPENING_MODE = "pdf_default_opening_mode"
        const val KEY_PDF_NIGHT_MODE = "pdf_night_reading_mode"
        const val KEY_PDF_PAGE_DISPLAY = "pdf_default_page_display"
        const val KEY_PDF_AUTO_SAVE_ANNOTATIONS = "pdf_auto_save_annotations"
        const val KEY_FLASHCARD_REVIEW_ORDER = "flashcard_review_order"
        const val KEY_QUIZ_DIFFICULTY = "quiz_difficulty_filter"
        const val KEY_REVISION_DURATION_MINUTES = "revision_default_duration_min"
    }
}
