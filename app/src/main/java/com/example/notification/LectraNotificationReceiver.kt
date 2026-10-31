package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LectraDatabase
import com.example.data.preferences.LectraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for:
 * 1. Firing scheduled class and assignment reminder notifications.
 * 2. Restoring and rescheduling alarms on device boot (BOOT_COMPLETED), package replacement,
 *    and system time/timezone changes.
 */
class LectraNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            LectraNotificationConstants.ACTION_CLASS_REMINDER -> {
                handleClassReminder(context, intent)
            }
            LectraNotificationConstants.ACTION_ASSIGNMENT_REMINDER -> {
                handleAssignmentReminder(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                handleSystemRestore(context)
            }
        }
    }

    private fun handleClassReminder(context: Context, intent: Intent) {
        val classId = intent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L)
        if (classId <= 0) return

        val prefs = LectraPreferences(context)
        if (!prefs.masterNotificationsEnabled || !prefs.notifyClasses) {
            return
        }

        val courseName = intent.getStringExtra(LectraNotificationConstants.EXTRA_COURSE_NAME) ?: "Class"
        val courseCode = intent.getStringExtra(LectraNotificationConstants.EXTRA_COURSE_CODE) ?: ""
        val topicName = intent.getStringExtra(LectraNotificationConstants.EXTRA_TOPIC_NAME) ?: ""
        val venue = intent.getStringExtra(LectraNotificationConstants.EXTRA_VENUE) ?: ""
        val startHour = intent.getIntExtra(LectraNotificationConstants.EXTRA_START_HOUR, 9)
        val startMinute = intent.getIntExtra(LectraNotificationConstants.EXTRA_START_MINUTE, 0)
        val reminderMinutes = intent.getIntExtra(LectraNotificationConstants.EXTRA_REMINDER_MINUTES, 15)

        // Show the notification
        LectraNotificationHelper.showClassNotification(
            context = context,
            classId = classId,
            courseName = courseName,
            courseCode = courseCode,
            topicName = topicName,
            venue = venue,
            startHour = startHour,
            startMinute = startMinute,
            reminderMinutes = reminderMinutes
        )

        // Reschedule for next week's occurrence
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = LectraDatabase.getDatabase(context.applicationContext, this)
                val all = db.lectureClassDao().getAllLectureClassesList()
                val existing = all.find { it.id == classId }
                if (existing != null) {
                    LectraNotificationScheduler.scheduleClassReminder(context, existing)
                }
            } catch (e: Exception) {
                android.util.Log.e("LectraNotif", "Error rescheduling class next week: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleAssignmentReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return

        val prefs = LectraPreferences(context)
        if (!prefs.masterNotificationsEnabled || !prefs.notifyAssignments) {
            return
        }

        val title = intent.getStringExtra(LectraNotificationConstants.EXTRA_TASK_TITLE) ?: "Assignment"
        val courseCode = intent.getStringExtra(LectraNotificationConstants.EXTRA_COURSE_CODE) ?: ""
        val dueDate = intent.getLongExtra(LectraNotificationConstants.EXTRA_DUE_DATE, 0L)
        val reminderIndex = intent.getIntExtra(LectraNotificationConstants.EXTRA_REMINDER_INDEX, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = LectraDatabase.getDatabase(context.applicationContext, this)
                val tasks = db.studySessionDao().getAllTasksList()
                val existing = tasks.find { it.id == taskId }

                // Check that assignment is still present and not completed
                if (existing != null && !existing.isCompleted) {
                    LectraNotificationHelper.showAssignmentNotification(
                        context = context,
                        taskId = taskId,
                        title = title,
                        courseCode = courseCode,
                        dueDate = if (existing.dueDate != null) existing.dueDate else dueDate,
                        reminderIndex = reminderIndex
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("LectraNotif", "Error in handleAssignmentReminder: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSystemRestore(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LectraNotificationScheduler.rescheduleAllSync(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
