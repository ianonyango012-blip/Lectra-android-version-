package com.example.notification

/**
 * Constants and unique ID utilities for LECTRA's native local notification system.
 */
object LectraNotificationConstants {
    // Actions for AlarmManager intents
    const val ACTION_CLASS_REMINDER = "com.example.lectra.ACTION_CLASS_REMINDER"
    const val ACTION_ASSIGNMENT_REMINDER = "com.example.lectra.ACTION_ASSIGNMENT_REMINDER"

    // Notification Channel Identifiers
    const val CHANNEL_CLASSES_ID = "lectra_classes_channel"
    const val CHANNEL_CLASSES_NAME = "Class Reminders"
    const val CHANNEL_CLASSES_DESC = "Notifications for upcoming university lectures, labs, and classes"

    const val CHANNEL_ASSIGNMENTS_ID = "lectra_assignments_channel"
    const val CHANNEL_ASSIGNMENTS_NAME = "Assignment Deadlines"
    const val CHANNEL_ASSIGNMENTS_DESC = "Notifications for coursework, assignment, and project submission deadlines"

    // Intent Extra Keys - Class
    const val EXTRA_CLASS_ID = "extra_class_id"
    const val EXTRA_COURSE_NAME = "extra_course_name"
    const val EXTRA_COURSE_CODE = "extra_course_code"
    const val EXTRA_TOPIC_NAME = "extra_topic_name"
    const val EXTRA_VENUE = "extra_venue"
    const val EXTRA_START_HOUR = "extra_start_hour"
    const val EXTRA_START_MINUTE = "extra_start_minute"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_REMINDER_MINUTES = "extra_reminder_minutes"

    // Intent Extra Keys - Assignment
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_DUE_DATE = "extra_due_date"
    const val EXTRA_REMINDER_INDEX = "extra_reminder_index" // 0 = 2 days before, 1 = 1 day before, 2 = on due date

    const val REMINDER_INDEX_2_DAYS = 0
    const val REMINDER_INDEX_1_DAY = 1
    const val REMINDER_INDEX_DUE_DATE = 2

    // Unique ID generation to guarantee no collisions between classes and assignment reminders
    fun getClassNotificationId(classId: Long): Int {
        return 100_000 + (classId.hashCode() and 0x7FFF)
    }

    fun getClassPendingIntentRequestCode(classId: Long): Int {
        return getClassNotificationId(classId)
    }

    fun getAssignmentNotificationId(taskId: Long, reminderIndex: Int): Int {
        return 200_000 + ((taskId.hashCode() and 0x3FFF) * 10) + (reminderIndex and 0x7)
    }

    fun getAssignmentPendingIntentRequestCode(taskId: Long, reminderIndex: Int): Int {
        return getAssignmentNotificationId(taskId, reminderIndex)
    }
}
