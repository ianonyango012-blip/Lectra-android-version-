package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.LectraDatabase
import com.example.data.model.LectureClassEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.preferences.LectraPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Handles scheduling, rescheduling, and cancelling of precise AlarmManager alarms for
 * native class and assignment notifications.
 */
object LectraNotificationScheduler {

    /**
     * Checks if the app can schedule exact alarms on Android 12+ (API 31+).
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Schedules a single alarm using AlarmManager with graceful fallback if exact alarms are not granted.
     */
    fun scheduleAlarm(context: Context, triggerMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val isExactAllowed = canScheduleExactAlarms(context)

        try {
            if (isExactAllowed) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.w("LectraNotif", "Exact alarm denied, falling back to non-exact: ${e.message}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    // ==========================================
    // 1. CLASS REMINDERS
    // ==========================================

    /**
     * Calculates the next upcoming trigger time for a class reminder.
     * Prevents duplicate alarms and past reminders.
     */
    fun calculateNextClassReminder(
        dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        leadMinutes: Int,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        val targetCalDay = if (dayOfWeek == 7) Calendar.SUNDAY else dayOfWeek + 1

        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentCalDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysDiff = targetCalDay - currentCalDay
        if (daysDiff < 0) {
            daysDiff += 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysDiff)

        // Class start time for this occurrence
        cal.set(Calendar.HOUR_OF_DAY, startHour)
        cal.set(Calendar.MINUTE, startMinute)
        val classStartMillis = cal.timeInMillis
        val reminderMillis = classStartMillis - (leadMinutes * 60 * 1000L)

        // Class end time for this occurrence
        cal.set(Calendar.HOUR_OF_DAY, endHour)
        cal.set(Calendar.MINUTE, endMinute)
        val classEndMillis = cal.timeInMillis

        // If the class has already ended or the reminder time has already passed, schedule for next week
        return if (fromMillis >= reminderMillis || fromMillis >= classEndMillis) {
            reminderMillis + (7 * 24 * 60 * 60 * 1000L)
        } else {
            reminderMillis
        }
    }

    fun scheduleClassReminder(context: Context, lectureClass: LectureClassEntity) {
        val prefs = LectraPreferences(context)
        if (!prefs.masterNotificationsEnabled || !prefs.notifyClasses) {
            cancelClassReminder(context, lectureClass.id)
            return
        }

        val leadMinutes = prefs.timetableClassReminderMinutes
        val triggerMillis = calculateNextClassReminder(
            dayOfWeek = lectureClass.dayOfWeek,
            startHour = lectureClass.startHour,
            startMinute = lectureClass.startMinute,
            endHour = lectureClass.endHour,
            endMinute = lectureClass.endMinute,
            leadMinutes = leadMinutes
        )

        val intent = Intent(context, LectraNotificationReceiver::class.java).apply {
            action = LectraNotificationConstants.ACTION_CLASS_REMINDER
            putExtra(LectraNotificationConstants.EXTRA_CLASS_ID, lectureClass.id)
            putExtra(LectraNotificationConstants.EXTRA_COURSE_NAME, lectureClass.courseName)
            putExtra(LectraNotificationConstants.EXTRA_COURSE_CODE, lectureClass.courseCode)
            putExtra(LectraNotificationConstants.EXTRA_TOPIC_NAME, lectureClass.topicOrLectureName)
            putExtra(LectraNotificationConstants.EXTRA_VENUE, lectureClass.venue)
            putExtra(LectraNotificationConstants.EXTRA_START_HOUR, lectureClass.startHour)
            putExtra(LectraNotificationConstants.EXTRA_START_MINUTE, lectureClass.startMinute)
            putExtra(LectraNotificationConstants.EXTRA_DAY_OF_WEEK, lectureClass.dayOfWeek)
            putExtra(LectraNotificationConstants.EXTRA_REMINDER_MINUTES, leadMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            LectraNotificationConstants.getClassPendingIntentRequestCode(lectureClass.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(context, triggerMillis, pendingIntent)
    }

    fun cancelClassReminder(context: Context, classId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, LectraNotificationReceiver::class.java).apply {
            action = LectraNotificationConstants.ACTION_CLASS_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            LectraNotificationConstants.getClassPendingIntentRequestCode(classId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        LectraNotificationHelper.cancelClassNotification(context, classId)
    }

    // ==========================================
    // 2. ASSIGNMENT REMINDERS
    // ==========================================

    fun calculateDueDateReminderTriggerTime(dueDate: Long, now: Long): Long? {
        val twoHoursBefore = dueDate - 2 * 60 * 60 * 1000L
        return when {
            twoHoursBefore > now -> twoHoursBefore
            dueDate - 15 * 60 * 1000L > now -> dueDate - 15 * 60 * 1000L
            dueDate > now -> now + 5000L
            else -> null // Already past due date
        }
    }

    fun scheduleAssignmentReminders(context: Context, task: StudyTaskEntity) {
        val prefs = LectraPreferences(context)
        if (!prefs.masterNotificationsEnabled || !prefs.notifyAssignments || task.isCompleted || task.dueDate == null) {
            cancelAssignmentReminders(context, task.id)
            return
        }

        val dueDate = task.dueDate
        val now = System.currentTimeMillis()

        // If assignment is already overdue, do not schedule past reminders
        if (now >= dueDate) {
            cancelAssignmentReminders(context, task.id)
            return
        }

        val reminderConfigs = listOf(
            Triple(0, dueDate - 2 * 24 * 60 * 60 * 1000L, prefs.assignmentReminder2Days), // 2 days before
            Triple(1, dueDate - 24 * 60 * 60 * 1000L, prefs.assignmentReminder1Day),      // 1 day before
            Triple(2, calculateDueDateReminderTriggerTime(dueDate, now) ?: 0L, prefs.assignmentReminderDueDate) // On due date
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        for ((index, triggerTime, isEnabled) in reminderConfigs) {
            val requestCode = LectraNotificationConstants.getAssignmentPendingIntentRequestCode(task.id, index)
            val intent = Intent(context, LectraNotificationReceiver::class.java).apply {
                action = LectraNotificationConstants.ACTION_ASSIGNMENT_REMINDER
                putExtra(LectraNotificationConstants.EXTRA_TASK_ID, task.id)
                putExtra(LectraNotificationConstants.EXTRA_TASK_TITLE, task.title)
                putExtra(LectraNotificationConstants.EXTRA_COURSE_CODE, task.courseCode)
                putExtra(LectraNotificationConstants.EXTRA_DUE_DATE, dueDate)
                putExtra(LectraNotificationConstants.EXTRA_REMINDER_INDEX, index)
            }

            if (isEnabled && triggerTime > now && triggerTime < dueDate) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleAlarm(context, triggerTime, pendingIntent)
            } else {
                // Cancel this specific reminder if not enabled or in the past
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }
    }

    fun cancelAssignmentReminders(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        for (index in 0..2) {
            val requestCode = LectraNotificationConstants.getAssignmentPendingIntentRequestCode(taskId, index)
            val intent = Intent(context, LectraNotificationReceiver::class.java).apply {
                action = LectraNotificationConstants.ACTION_ASSIGNMENT_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        LectraNotificationHelper.cancelAssignmentNotifications(context, taskId)
    }

    // ==========================================
    // 3. BULK RESCHEDULING & RESTORATION
    // ==========================================

    /**
     * Reschedules all active class reminders and assignment reminders synchronously (suspend).
     */
    suspend fun rescheduleAllSync(context: Context) {
        LectraNotificationHelper.createNotificationChannels(context)
        try {
            val db = LectraDatabase.getDatabase(context.applicationContext, CoroutineScope(Dispatchers.IO))
            val classes = db.lectureClassDao().getAllLectureClassesList()
            val tasks = db.studySessionDao().getAllTasksList()

            val prefs = LectraPreferences(context)

            // 1. Reschedule or cancel classes
            if (prefs.masterNotificationsEnabled && prefs.notifyClasses) {
                for (c in classes) {
                    scheduleClassReminder(context, c)
                }
            } else {
                for (c in classes) {
                    cancelClassReminder(context, c.id)
                }
            }

            // 2. Reschedule or cancel assignments
            if (prefs.masterNotificationsEnabled && prefs.notifyAssignments) {
                for (t in tasks) {
                    if (!t.isCompleted && t.dueDate != null) {
                        scheduleAssignmentReminders(context, t)
                    } else {
                        cancelAssignmentReminders(context, t.id)
                    }
                }
            } else {
                for (t in tasks) {
                    cancelAssignmentReminders(context, t.id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LectraNotif", "Error in rescheduleAllSync: ${e.message}", e)
        }
    }

    /**
     * Reschedules all active class reminders and assignment reminders from the database.
     * Safe to invoke on device boot, time changes, settings updates, and app launch.
     */
    fun rescheduleAll(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            rescheduleAllSync(context)
        }
    }
}
