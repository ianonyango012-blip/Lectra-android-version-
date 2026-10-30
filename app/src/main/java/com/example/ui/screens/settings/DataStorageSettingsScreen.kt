package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupOptions
import com.example.data.backup.BackupValidationResult
import com.example.data.backup.RestoreMode
import com.example.ui.LectraUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSettingsScreen(
    uiState: LectraUiState,
    backupManager: BackupManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryViolet = Color(0xFF7565F6)

    // Storage measurements state
    var cacheSizeBytes by remember {
        mutableLongStateOf(calculateFolderSize(context.cacheDir))
    }
    val pdfsSizeBytes by remember {
        mutableLongStateOf(calculateFolderSize(File(context.filesDir, "lectra_pdfs")))
    }
    val dbSizeBytes by remember {
        mutableLongStateOf(context.getDatabasePath("lectra_database").length().coerceAtLeast(0L))
    }
    val totalAppSizeBytes = pdfsSizeBytes + dbSizeBytes + cacheSizeBytes

    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    // Backup Options State
    var incCourses by remember { mutableStateOf(true) }
    var incTimetable by remember { mutableStateOf(true) }
    var incRevision by remember { mutableStateOf(true) }
    var incPrefs by remember { mutableStateOf(true) }

    // Backup Progress State
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var backupProgressPercent by remember { mutableStateOf<Int?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    // Last backup info
    var lastBackupInfo by remember { mutableStateOf(backupManager.getLastBackupInfo()) }

    // Restore Preview Dialog State
    var showRestorePreviewDialog by remember { mutableStateOf(false) }
    var restoreValidationResult by remember { mutableStateOf<BackupValidationResult?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }

    // Document Creators & Pickers
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isOperating = true
            backupProgressPercent = 10
            backupStatusMessage = "Preparing academic records..."
            coroutineScope.launch {
                val options = BackupOptions(
                    includeCoursesAndNotes = incCourses,
                    includeTimetableAndPlanner = incTimetable,
                    includeRevision = incRevision,
                    includePreferences = incPrefs
                )
                val result = backupManager.writeBackupToUri(uri, options) { pct, msg ->
                    backupProgressPercent = pct
                    backupStatusMessage = msg
                }
                isOperating = false
                if (result.isSuccess) {
                    val summary = result.getOrThrow()
                    backupStatusMessage = "Backup successfully exported! (${summary.totalItemsCount} items saved)"
                    lastBackupInfo = backupManager.getLastBackupInfo()
                } else {
                    backupStatusMessage = "Backup failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                }
            }
        }
    }

    val backupImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isOperating = true
            backupStatusMessage = "Reading and verifying backup archive..."
            coroutineScope.launch {
                val result = backupManager.validateBackupUri(uri)
                isOperating = false
                if (result.isSuccess) {
                    val validation = result.getOrThrow()
                    if (validation.isValid) {
                        restoreValidationResult = validation
                        showRestorePreviewDialog = true
                    } else {
                        backupStatusMessage = "Validation failed: ${validation.errorMessage}"
                    }
                } else {
                    backupStatusMessage = "Could not parse backup file: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Data and Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("data_storage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("data_storage_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. STORAGE MANAGEMENT SECTION
            // ==========================================
            item(key = "storage_management_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Storage Management",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "On-device disk usage across academic materials",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Storage Breakdown Cards
                        StorageItemRow(
                            title = "Total Storage Used",
                            subtitle = "All LECTRA study assets on device",
                            sizeText = formatFileSize(totalAppSizeBytes),
                            isHighlight = true
                        )

                        StorageItemRow(
                            title = "Imported Lecture PDFs",
                            subtitle = "${uiState.materials.size} documents stored locally",
                            sizeText = formatFileSize(pdfsSizeBytes)
                        )

                        StorageItemRow(
                            title = "Notes & Study Database",
                            subtitle = "Local SQLite Room database & flashcards",
                            sizeText = formatFileSize(dbSizeBytes)
                        )

                        StorageItemRow(
                            title = "Temporary Cache Files",
                            subtitle = "Rendered page caches & transient files",
                            sizeText = formatFileSize(cacheSizeBytes)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clear Cache Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.cacheDir.deleteRecursively()
                                    context.cacheDir.mkdirs()
                                    val newSize = calculateFolderSize(context.cacheDir)
                                    withContext(Dispatchers.Main) {
                                        cacheSizeBytes = newSize
                                        cacheClearedMessage = "Temporary cache cleared safely."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_cache_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Temporary Cache", fontSize = 13.sp)
                        }

                        cacheClearedMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. BACKUP & RESTORE SECTION
            // ==========================================
            item(key = "backup_restore_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Backup and Restore",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "User-controlled local archiving without account creation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "LECTRA works entirely without mandatory cloud sync. You can create complete offline backups of your notes, timetable, and flashcards, and restore them anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        lastBackupInfo?.let { (_, details) ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Last Archive: $details",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Include in Backup Archive:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = incCourses,
                                onClick = { incCourses = !incCourses },
                                label = { Text("Courses & Notes", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = incTimetable,
                                onClick = { incTimetable = !incTimetable },
                                label = { Text("Class Timetable", fontSize = 11.sp) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = incRevision,
                                onClick = { incRevision = !incRevision },
                                label = { Text("Flashcards & Quizzes", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = incPrefs,
                                onClick = { incPrefs = !incPrefs },
                                label = { Text("Preferences", fontSize = 11.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Backup Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    backupExportLauncher.launch("lectra_backup_$timestamp.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                                enabled = !isOperating
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Backup", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    backupImportLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_backup_button"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isOperating
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Archive", fontSize = 12.sp)
                            }
                        }

                        if (isOperating) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = backupStatusMessage ?: "Processing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryViolet
                            )
                        } else if (backupStatusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = backupStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. EXPORT ACADEMIC DATA SECTION
            // ==========================================
            item(key = "export_data_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryViolet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = primaryViolet,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Academic Data",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Share notes, timetable, and study records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Export formatted study summaries, enrolled courses, and timetable schedules to share or open in other productivity apps via Android's native share sheet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val summaryText = buildAcademicExportText(uiState)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "LECTRA Academic Records Export")
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Export Academic Records"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_academic_records_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryViolet)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share / Export Study Summary", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    if (showRestorePreviewDialog && restoreValidationResult != null) {
        val validation = restoreValidationResult!!
        val summary = validation.summary

        AlertDialog(
            onDismissRequest = {
                showRestorePreviewDialog = false
                restoreValidationResult = null
            },
            title = {
                Text(
                    text = "Confirm Restore Archive",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Archive summary from ${summary?.formattedDate ?: "Unknown"}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${summary?.coursesCount ?: 0} Courses and Syllabi")
                    Text("• ${summary?.classesCount ?: 0} Timetable Classes")
                    Text("• ${summary?.sessionsCount ?: 0} Study Sessions")
                    Text("• ${summary?.flashcardsCount ?: 0} Flashcards & Quizzes")

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Restore Mode:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.MERGE,
                            onClick = { selectedRestoreMode = RestoreMode.MERGE }
                        )
                        Text("Merge with current records", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.REPLACE,
                            onClick = { selectedRestoreMode = RestoreMode.REPLACE }
                        )
                        Text("Replace existing records", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backupData = validation.data
                        showRestorePreviewDialog = false
                        if (backupData != null) {
                            isOperating = true
                            backupStatusMessage = "Restoring academic records..."
                            coroutineScope.launch {
                                val result = backupManager.restoreBackupData(backupData, selectedRestoreMode) { pct, msg ->
                                    backupProgressPercent = pct
                                    backupStatusMessage = msg
                                }
                                isOperating = false
                                if (result.isSuccess) {
                                    backupStatusMessage = "Records restored successfully!"
                                } else {
                                    backupStatusMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                                }
                                restoreValidationResult = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryViolet)
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestorePreviewDialog = false
                    restoreValidationResult = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageItemRow(
    title: String,
    subtitle: String,
    sizeText: String,
    isHighlight: Boolean = false
) {
    val primaryViolet = Color(0xFF7565F6)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isHighlight) primaryViolet else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = sizeText,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (isHighlight) primaryViolet else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun calculateFolderSize(file: File?): Long {
    if (file == null || !file.exists()) return 0L
    return try {
        file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (e: Throwable) {
        0L
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

private fun buildAcademicExportText(uiState: LectraUiState): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("LECTRA ACADEMIC STUDY RECORDS EXPORT")
    sb.appendLine("Exported: ${SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
    sb.appendLine("==========================================")
    sb.appendLine()

    sb.appendLine("ENROLLED COURSES (${uiState.courses.size}):")
    uiState.courses.forEach { course ->
        sb.appendLine("• ${course.code} - ${course.title} (Lecturer: ${course.lecturer}, ${course.targetWeeklyHours}h/week)")
    }
    sb.appendLine()

    sb.appendLine("CLASS TIMETABLE (${uiState.classes.size}):")
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    uiState.classes.sortedWith(compareBy({ it.dayOfWeek }, { it.startHour })).forEach { cls ->
        val dayName = dayNames.getOrElse(cls.dayOfWeek - 1) { "Day ${cls.dayOfWeek}" }
        sb.appendLine("• [$dayName] ${cls.courseCode} ${cls.topicOrLectureName} @ ${cls.venue} (${cls.startHour}:${String.format("%02d", cls.startMinute)} - ${cls.endHour}:${String.format("%02d", cls.endMinute)})")
    }
    sb.appendLine()

    sb.appendLine("PENDING ASSIGNMENTS (${uiState.tasks.count { !it.isCompleted }}):")
    uiState.tasks.filter { !it.isCompleted }.forEach { task ->
        sb.appendLine("• [${task.courseCode}] ${task.title} (Priority: ${task.priority})")
    }
    sb.appendLine()

    sb.appendLine("STUDY MATERIALS & REVISION:")
    sb.appendLine("• ${uiState.materials.size} Imported Lecture PDFs")
    sb.appendLine("• ${uiState.flashcards.size} Active Recall Flashcards")
    sb.appendLine("• ${uiState.quizzes.size} Practice Quiz Questions")
    sb.appendLine()
    sb.appendLine("Generated by LECTRA — Native Local-First University Study Companion")
    return sb.toString()
}
