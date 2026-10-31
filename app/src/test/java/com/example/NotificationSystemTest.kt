package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LectraDatabase
import com.example.data.model.CourseEntity
import com.example.data.model.LectureClassEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.preferences.LectraPreferences
import com.example.notification.LectraNotificationConstants
import com.example.notification.LectraNotificationHelper
import com.example.notification.LectraNotificationReceiver
import com.example.notification.LectraNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationSystemTest {

    private lateinit var context: Context
    private lateinit var preferences: LectraPreferences
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferences = LectraPreferences(context)
        preferences.masterNotificationsEnabled = true
        preferences.notifyClasses = true
        preferences.notifyAssignments = true
        preferences.timetableClassReminderMinutes = 15
        preferences.assignmentReminder2Days = true
        preferences.assignmentReminder1Day = true
        preferences.assignmentReminderDueDate = true

        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Initialize channels
        LectraNotificationHelper.createNotificationChannels(context)
    }

    @Test
    fun testNotificationChannelsCreated() {
        val channels = notificationManager.notificationChannels
        assertTrue("At least two notification channels should exist", channels.size >= 2)
        val classChannel = notificationManager.getNotificationChannel(LectraNotificationConstants.CHANNEL_CLASSES_ID)
        assertNotNull("Class notifications channel must exist", classChannel)
        assertEquals(LectraNotificationConstants.CHANNEL_CLASSES_NAME, classChannel.name)

        val assignmentChannel = notificationManager.getNotificationChannel(LectraNotificationConstants.CHANNEL_ASSIGNMENTS_ID)
        assertNotNull("Assignment notifications channel must exist", assignmentChannel)
        assertEquals(LectraNotificationConstants.CHANNEL_ASSIGNMENTS_NAME, assignmentChannel.name)
    }

    @Test
    fun testScenario1_CreateClassSchedulesAlarm() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        
        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1
        
        val lectureClass = LectureClassEntity(
            id = 101L,
            courseId = 1L,
            courseCode = "CS301",
            courseName = "Operating Systems",
            topicOrLectureName = "Memory Virtualization",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 3) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 5) % 24,
            endMinute = 0,
            venue = "Lecture Hall 402"
        )

        LectraNotificationScheduler.scheduleClassReminder(context, lectureClass)

        // Verify alarm is scheduled in AlarmManager
        val scheduledAlarms = shadowAlarm.scheduledAlarms
        assertTrue("Alarm should be scheduled for new class", scheduledAlarms.isNotEmpty())
        
        val matchingAlarm = scheduledAlarms.find { alarm ->
            val intent = Shadows.shadowOf(alarm.operation).savedIntent
            intent.action == LectraNotificationConstants.ACTION_CLASS_REMINDER &&
                    intent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 101L
        }
        assertNotNull("Scheduled alarm for class 101 must exist", matchingAlarm)
        assertTrue("Trigger time must be in future", matchingAlarm!!.triggerAtTime > System.currentTimeMillis())
    }

    @Test
    fun testScenario2_EditClassTimeUpdatesAlarm() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        val initialClass = LectureClassEntity(
            id = 102L,
            courseId = 1L,
            courseCode = "MED101",
            courseName = "Anatomy",
            topicOrLectureName = "Skeletal System",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 2) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 4) % 24,
            endMinute = 0,
            venue = "Lecture Hall 2"
        )

        LectraNotificationScheduler.scheduleClassReminder(context, initialClass)
        val firstTrigger = shadowAlarm.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 102L
        }!!.triggerAtTime

        // Now edit class time (move 1 hour later)
        val updatedClass = initialClass.copy(
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 3) % 24
        )
        LectraNotificationScheduler.scheduleClassReminder(context, updatedClass)

        val updatedTrigger = shadowAlarm.scheduledAlarms.find {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 102L
        }!!.triggerAtTime

        assertTrue("Rescheduled alarm trigger time must differ after edit", firstTrigger != updatedTrigger)
    }

    @Test
    fun testScenario3_DeleteClassCancelsAlarm() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        val lectureClass = LectureClassEntity(
            id = 103L,
            courseId = 2L,
            courseCode = "BIO200",
            courseName = "Microbiology",
            topicOrLectureName = "Gram Staining",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 3) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 5) % 24,
            endMinute = 0,
            venue = "Lab B"
        )

        LectraNotificationScheduler.scheduleClassReminder(context, lectureClass)
        assertTrue(shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 103L
        })

        // Cancel
        LectraNotificationScheduler.cancelClassReminder(context, 103L)

        val stillScheduled = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 103L
        }
        assertFalse("Class alarm must be cancelled", stillScheduled)
    }

    @Test
    fun testScenario4_CreateAssignment3DaysAwaySchedules3Reminders() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val now = System.currentTimeMillis()
        val threeDaysAway = now + (3 * 24 * 3600 * 1000L)

        val task = StudyTaskEntity(
            id = 201L,
            courseId = 1L,
            courseCode = "CS301",
            title = "Final Project Submission",
            dueDate = threeDaysAway,
            isCompleted = false
        )

        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)

        // Check alarms for taskId 201
        val taskAlarms = shadowAlarm.scheduledAlarms.filter {
            val intent = Shadows.shadowOf(it.operation).savedIntent
            intent.action == LectraNotificationConstants.ACTION_ASSIGNMENT_REMINDER &&
                    intent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 201L
        }

        assertEquals("Should schedule all 3 reminder intervals (2 days, 1 day, due date)", 3, taskAlarms.size)
    }

    @Test
    fun testScenario5_CreateAssignment1DayAwaySchedulesOnlyFutureReminders() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val now = System.currentTimeMillis()
        // 18 hours away -> 2-days-before reminder has already passed, only 1-day or due date remaining
        val eighteenHoursAway = now + (18 * 3600 * 1000L)

        val task = StudyTaskEntity(
            id = 202L,
            courseId = 1L,
            courseCode = "CS301",
            title = "Problem Set 4",
            dueDate = eighteenHoursAway,
            isCompleted = false
        )

        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)

        val taskAlarms = shadowAlarm.scheduledAlarms.filter {
            val intent = Shadows.shadowOf(it.operation).savedIntent
            intent.action == LectraNotificationConstants.ACTION_ASSIGNMENT_REMINDER &&
                    intent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 202L
        }

        assertTrue("Past reminders must NOT be scheduled", taskAlarms.size in 1..2)
        taskAlarms.forEach { alarm ->
            assertTrue("Trigger time must be in future", alarm.triggerAtTime > now)
        }
    }

    @Test
    fun testScenario6_MarkAssignmentCompletedCancelsAllReminders() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val now = System.currentTimeMillis()
        val fourDaysAway = now + (4 * 24 * 3600 * 1000L)

        val task = StudyTaskEntity(
            id = 203L,
            courseId = 1L,
            courseCode = "MATH201",
            title = "Calculus Assignment 2",
            dueDate = fourDaysAway,
            isCompleted = false
        )

        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)
        assertTrue(shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 203L
        })

        // Now cancel (as called when task is completed or deleted)
        LectraNotificationScheduler.cancelAssignmentReminders(context, 203L)

        val stillScheduled = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 203L
        }
        assertFalse("All reminders for completed assignment must be cancelled", stillScheduled)
    }

    @Test
    fun testScenario7_ChangeAssignmentDeadlineReschedulesReminders() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val now = System.currentTimeMillis()
        val originalDue = now + (3 * 24 * 3600 * 1000L)

        val task = StudyTaskEntity(
            id = 204L,
            courseId = 1L,
            courseCode = "PHY101",
            title = "Lab Report 1",
            dueDate = originalDue,
            isCompleted = false
        )

        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)
        val initialAlarm = shadowAlarm.scheduledAlarms.find {
            val intent = Shadows.shadowOf(it.operation).savedIntent
            intent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 204L &&
                    intent.getIntExtra(LectraNotificationConstants.EXTRA_REMINDER_INDEX, -1) == LectraNotificationConstants.REMINDER_INDEX_2_DAYS
        }
        assertNotNull(initialAlarm)
        val initialTrigger = initialAlarm!!.triggerAtTime

        // Extended by 3 more days
        val extendedTask = task.copy(dueDate = originalDue + (3 * 24 * 3600 * 1000L))
        LectraNotificationScheduler.scheduleAssignmentReminders(context, extendedTask)

        val updatedAlarm = shadowAlarm.scheduledAlarms.find {
            val intent = Shadows.shadowOf(it.operation).savedIntent
            intent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 204L &&
                    intent.getIntExtra(LectraNotificationConstants.EXTRA_REMINDER_INDEX, -1) == LectraNotificationConstants.REMINDER_INDEX_2_DAYS
        }
        assertNotNull(updatedAlarm)
        assertTrue("New trigger time should be pushed out by 3 days", updatedAlarm!!.triggerAtTime > initialTrigger)
    }

    @Test
    fun testScenario8_DisableClassNotificationsDoesNotAffectAssignmentNotifications() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        preferences.notifyClasses = false
        preferences.notifyAssignments = true

        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        val lectureClass = LectureClassEntity(
            id = 105L,
            courseId = 1L,
            courseCode = "CHEM101",
            courseName = "General Chemistry",
            topicOrLectureName = "Acid-Base Titration",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 3) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 5) % 24,
            endMinute = 0,
            venue = "Chemistry Lab"
        )
        LectraNotificationScheduler.scheduleClassReminder(context, lectureClass)

        val hasClassAlarm = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 105L
        }
        assertFalse("Class alarm must NOT be scheduled when class notifications disabled", hasClassAlarm)

        // But assignments should still schedule!
        val task = StudyTaskEntity(
            id = 205L,
            courseId = 1L,
            courseCode = "CHEM101",
            title = "Titration Writeup",
            dueDate = System.currentTimeMillis() + (3 * 24 * 3600 * 1000L),
            isCompleted = false
        )
        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)

        val hasTaskAlarm = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 205L
        }
        assertTrue("Assignment notifications must still work when class notifications are disabled", hasTaskAlarm)
    }

    @Test
    fun testScenario9_DisableAssignmentNotificationsDoesNotAffectClassNotifications() {
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        preferences.notifyClasses = true
        preferences.notifyAssignments = false

        val task = StudyTaskEntity(
            id = 206L,
            courseId = 1L,
            courseCode = "HIST101",
            title = "History Essay",
            dueDate = System.currentTimeMillis() + (3 * 24 * 3600 * 1000L),
            isCompleted = false
        )
        LectraNotificationScheduler.scheduleAssignmentReminders(context, task)

        val hasTaskAlarm = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_TASK_ID, -1L) == 206L
        }
        assertFalse("Assignment alarm must NOT be scheduled when assignment notifications disabled", hasTaskAlarm)

        // But classes should still schedule!
        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        val lectureClass = LectureClassEntity(
            id = 106L,
            courseId = 1L,
            courseCode = "HIST101",
            courseName = "World History",
            topicOrLectureName = "Industrial Revolution",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 3) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 5) % 24,
            endMinute = 0,
            venue = "West Hall 10"
        )
        LectraNotificationScheduler.scheduleClassReminder(context, lectureClass)

        val hasClassAlarm = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 106L
        }
        assertTrue("Class notifications must still work when assignment notifications are disabled", hasClassAlarm)
    }

    @Test
    fun testScenario10_SystemRestoreReschedulesFromDatabase() = runBlocking {
        val db = LectraDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val course = CourseEntity(
            id = 10L,
            code = "ENG101",
            title = "Literature",
            colorHex = "#7565F6",
            lecturer = "Prof. Smith",
            semester = "Semester 1"
        )
        db.courseDao().insertCourse(course)

        val cal = Calendar.getInstance()
        val currentDay = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1

        val lecture = LectureClassEntity(
            id = 501L,
            courseId = 10L,
            courseCode = "ENG101",
            courseName = "Literature",
            topicOrLectureName = "Modern Poetry",
            dayOfWeek = currentDay,
            startHour = (cal.get(Calendar.HOUR_OF_DAY) + 2) % 24,
            startMinute = 0,
            endHour = (cal.get(Calendar.HOUR_OF_DAY) + 4) % 24,
            endMinute = 0,
            venue = "Room 303"
        )
        db.lectureClassDao().insertLectureClass(lecture)

        // Simulate restore
        LectraNotificationScheduler.rescheduleAllSync(context)

        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val hasRestoredClass = shadowAlarm.scheduledAlarms.any {
            Shadows.shadowOf(it.operation).savedIntent.getLongExtra(LectraNotificationConstants.EXTRA_CLASS_ID, -1L) == 501L
        }
        assertTrue("Class reminder should be restored after BOOT_COMPLETED", hasRestoredClass)
    }

    @Test
    fun testScenario11_OfflineExecutionZeroNetworkDependency() {
        // Test that notification building and displaying works completely offline
        LectraNotificationHelper.showClassNotification(
            context = context,
            classId = 999L,
            courseName = "Compiler Design",
            courseCode = "CS401",
            topicName = "Lexical Analysis",
            venue = "Auditorium A",
            startHour = 14,
            startMinute = 0,
            reminderMinutes = 15
        )

        val shadowNotif = Shadows.shadowOf(notificationManager)
        assertEquals("Notification should be posted locally without internet", 1, shadowNotif.size())
        val notification = shadowNotif.allNotifications.first()
        assertNotNull(notification)

        LectraNotificationHelper.showAssignmentNotification(
            context = context,
            taskId = 888L,
            title = "Parser Phase 1",
            courseCode = "CS401",
            dueDate = System.currentTimeMillis() + 86400000L,
            reminderIndex = LectraNotificationConstants.REMINDER_INDEX_2_DAYS
        )

        assertEquals("Second notification posted locally", 2, shadowNotif.size())
    }

    @Test
    fun testScenario12_NotificationPermissionCheck() {
        // Tests permission helper returns valid boolean status
        val canPost = LectraNotificationHelper.areNotificationsEnabled(context)
        // Under Robolectric, notification permissions are enabled by default
        assertTrue("Notifications enabled under test environment", canPost)
    }
}
