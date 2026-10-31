package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles creation of notification channels and building/displaying native Android notifications.
 */
object LectraNotificationHelper {

    /**
     * Initializes the notification channels for upcoming classes and assignment deadlines.
     * Safe to call multiple times as Android ignores redundant channel creations.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // 1. Class reminders channel (High priority)
            val classesChannel = NotificationChannel(
                LectraNotificationConstants.CHANNEL_CLASSES_ID,
                LectraNotificationConstants.CHANNEL_CLASSES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = LectraNotificationConstants.CHANNEL_CLASSES_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            // 2. Assignment reminders channel (High priority)
            val assignmentsChannel = NotificationChannel(
                LectraNotificationConstants.CHANNEL_ASSIGNMENTS_ID,
                LectraNotificationConstants.CHANNEL_ASSIGNMENTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = LectraNotificationConstants.CHANNEL_ASSIGNMENTS_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(classesChannel)
            notificationManager.createNotificationChannel(assignmentsChannel)
        }
    }

    /**
     * Checks if notification permissions are granted (including Android 13+ POST_NOTIFICATIONS).
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Builds and posts a class reminder notification.
     * Example text format: "Anatomy class starts in 15 minutes. Venue: Lecture Hall 2."
     */
    fun showClassNotification(
        context: Context,
        classId: Long,
        courseName: String,
        courseCode: String,
        topicName: String,
        venue: String,
        startHour: Int,
        startMinute: Int,
        reminderMinutes: Int
    ) {
        if (!areNotificationsEnabled(context)) {
            return
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LectraNotificationConstants.getClassPendingIntentRequestCode(classId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeString = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
        val venueString = if (venue.isNotBlank()) "Venue: $venue." else "Venue: Campus."
        val reminderDuration = if (reminderMinutes >= 60) {
            "${reminderMinutes / 60} hour"
        } else {
            "$reminderMinutes minutes"
        }

        val title = "$courseName ($courseCode)"
        val contentText = "$courseName class starts in $reminderDuration. $venueString"

        val bigText = buildString {
            append("$courseName class starts at $timeString (in $reminderDuration).\n")
            append(venueString)
            if (topicName.isNotBlank() && !topicName.equals(courseName, ignoreCase = true)) {
                append("\nTopic: $topicName")
            }
        }

        val builder = NotificationCompat.Builder(context, LectraNotificationConstants.CHANNEL_CLASSES_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(
                LectraNotificationConstants.getClassNotificationId(classId),
                builder.build()
            )
        } catch (e: SecurityException) {
            android.util.Log.w("LectraNotif", "SecurityException posting class notification: ${e.message}")
        }
    }

    /**
     * Builds and posts an assignment deadline reminder notification.
     * Example: "Assignment reminder: Complete your Biochemistry assignment. Deadline: Tomorrow at 11:59 PM."
     */
    fun showAssignmentNotification(
        context: Context,
        taskId: Long,
        title: String,
        courseCode: String,
        dueDate: Long,
        reminderIndex: Int
    ) {
        if (!areNotificationsEnabled(context)) {
            return
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            LectraNotificationConstants.getAssignmentPendingIntentRequestCode(taskId, reminderIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deadlineStr = formatDeadline(dueDate)
        val notifTitle = "Assignment Reminder: $courseCode"
        val contentText = "Assignment reminder: Complete your $title assignment. Deadline: $deadlineStr."

        val reminderDescription = when (reminderIndex) {
            0 -> "First reminder (2 days before deadline)"
            1 -> "Second reminder (1 day before deadline)"
            else -> "Final reminder (Due today)"
        }

        val bigText = "$contentText\n$reminderDescription"

        val builder = NotificationCompat.Builder(context, LectraNotificationConstants.CHANNEL_ASSIGNMENTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notifTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(
                LectraNotificationConstants.getAssignmentNotificationId(taskId, reminderIndex),
                builder.build()
            )
        } catch (e: SecurityException) {
            android.util.Log.w("LectraNotif", "SecurityException posting assignment notification: ${e.message}")
        }
    }

    fun cancelClassNotification(context: Context, classId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(LectraNotificationConstants.getClassNotificationId(classId))
    }

    fun cancelAssignmentNotifications(context: Context, taskId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        for (idx in 0..2) {
            notificationManager.cancel(LectraNotificationConstants.getAssignmentNotificationId(taskId, idx))
        }
    }

    fun formatDeadline(epochMillis: Long): String {
        val sdf = SimpleDateFormat("EEE, d MMM 'at' h:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }
}
