package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiServiceStatus
import com.example.data.backup.*
import com.example.ui.LectraUiState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    uiState: LectraUiState,
    backupManager: BackupManager,
    onThemeModeChanged: (com.example.ui.theme.ThemeMode) -> Unit = {},
    onResetDemoData: () -> Unit,
    onReconfigureSemester: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE) }

    var showResetDialog by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appearance & Theme Mode Card
        item(key = "settings_appearance") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_appearance_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Appearance & Theme",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Appearance & Theme",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Clean white for daytime, charcoal for evening focus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentTheme = uiState.themeMode

                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Light",
                            subtitle = "Default",
                            icon = Icons.Default.LightMode,
                            isSelected = currentTheme == com.example.ui.theme.ThemeMode.LIGHT,
                            testTag = "theme_option_light",
                            onClick = { onThemeModeChanged(com.example.ui.theme.ThemeMode.LIGHT) }
                        )

                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "Dark",
                            subtitle = "Night",
                            icon = Icons.Default.DarkMode,
                            isSelected = currentTheme == com.example.ui.theme.ThemeMode.DARK,
                            testTag = "theme_option_dark",
                            onClick = { onThemeModeChanged(com.example.ui.theme.ThemeMode.DARK) }
                        )

                        ThemeOptionItem(
                            modifier = Modifier.weight(1f),
                            title = "System",
                            subtitle = "Auto",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = currentTheme == com.example.ui.theme.ThemeMode.SYSTEM,
                            testTag = "theme_option_system",
                            onClick = { onThemeModeChanged(com.example.ui.theme.ThemeMode.SYSTEM) }
                        )
                    }
                }
            }
        }

        // Academic Profile & Semester Configuration Card
        item(key = "settings_profile") {
            val institution = prefs.getString("user_institution", "University Scholar") ?: "University Scholar"
            val programme = prefs.getString("user_programme", "Degree Programme") ?: "Degree Programme"
            val term = prefs.getString("user_semester", "Term 1 - 2026") ?: "Term 1 - 2026"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Academic Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = institution,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = programme,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Current Term: $term",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onReconfigureSemester,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reconfigure_semester_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconfigure Semester & Timetable")
                    }
                }
            }
        }

        // Local Export, Backup & Restore Card
        item(key = "settings_backup_restore") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local Backup & Offline Export",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Safely export your study records to local storage or restore anytime without needing an account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    lastBackupInfo?.let { (_, details) ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Last Export: $details",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Customize Export Contents:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = incCourses,
                            onClick = { incCourses = !incCourses },
                            label = { Text("Courses & Notes", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = incTimetable,
                            onClick = { incTimetable = !incTimetable },
                            label = { Text("Timetable", fontSize = 11.sp) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = incRevision,
                            onClick = { incRevision = !incRevision },
                            label = { Text("Flashcards & Quizzes", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = incPrefs,
                            onClick = { incPrefs = !incPrefs },
                            label = { Text("App Settings", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: Export and Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
                                backupExportLauncher.launch("lectra_backup_$timestamp.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_button"),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isOperating
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 13.sp)
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
                            Text("Restore Archive", fontSize = 13.sp)
                        }
                    }

                    // Progress & Status Display
                    if (isOperating) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            backupProgressPercent?.let { pct ->
                                LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = backupStatusMessage ?: "Processing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { backupManager.cancelOperation() }
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else if (backupStatusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = backupStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Local Storage Inventory
        item(key = "settings_storage") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local Storage & Data Inventory",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StorageStatRow("Enrolled Courses", "${uiState.courses.size}")
                    StorageStatRow("Imported Lecture PDFs", "${uiState.materials.size} documents")
                    StorageStatRow("Scheduled Study Sessions", "${uiState.sessions.size} sessions")
                    StorageStatRow("Active Flashcard Decks", "${uiState.flashcards.size} cards")
                    StorageStatRow("Practice Quiz Questions", "${uiState.quizzes.size} questions")
                    StorageStatRow("Persistence Engine", "Local SQLite Room Database")
                }
            }
        }

        // Privacy & Disclosures
        item(key = "settings_privacy") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Local-First Principles",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Local-First Architecture: All lecture notes, timetables, and quizzes are saved on this phone in your local SQLite Room database.\n\n" +
                                "2. Account-Free Freedom: No mandatory sign-in, tracking, or cloud lock-in. Backups are exported directly to files you control.\n\n" +
                                "3. AI Privacy: Lecture summaries and quizzes only generate when explicitly requested. Optional Gemini enhancements can be configured via AI Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Data Management / Reset Demo Data
        item(key = "settings_demo") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Academic Demo Dataset",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reload sample university courses (CS 301, BIO 210, MATH 240) with syllabus units, timetable schedules, and flashcard decks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_demo_data_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset & Reload Sample Data")
                    }
                }
            }
        }

        // App Version
        item(key = "settings_version") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LECTRA v1.0.0",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Native Android Local-First University Study Companion",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    // Restore Preview & Confirmation Dialog
    if (showRestorePreviewDialog && restoreValidationResult != null) {
        val validation = restoreValidationResult!!
        val summary = validation.summary

        AlertDialog(
            onDismissRequest = {
                showRestorePreviewDialog = false
                restoreValidationResult = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestorePage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Restore Archive")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "A valid LECTRA backup from ${summary?.formattedDate ?: "an earlier date"} was found.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Courses: ${summary?.coursesCount ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("Timetable Classes: ${summary?.classesCount ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("Study Sessions: ${summary?.sessionsCount ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("Flashcards: ${summary?.flashcardsCount ?: 0}", style = MaterialTheme.typography.bodySmall)
                            Text("Quizzes: ${summary?.quizzesCount ?: 0}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Restore Conflict Strategy:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRestoreMode = RestoreMode.MERGE }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.MERGE,
                            onClick = { selectedRestoreMode = RestoreMode.MERGE }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Merge with Existing Data (Safe)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("Keeps all your current records and imports new ones", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRestoreMode = RestoreMode.REPLACE }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.REPLACE,
                            onClick = { selectedRestoreMode = RestoreMode.REPLACE }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Replace All Data", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text("Clears current records and replaces with backup", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = validation.data
                        showRestorePreviewDialog = false
                        restoreValidationResult = null
                        if (data != null) {
                            isOperating = true
                            backupStatusMessage = "Restoring records..."
                            coroutineScope.launch {
                                val restoreResult = backupManager.restoreBackupData(data, selectedRestoreMode) { pct, msg ->
                                    backupProgressPercent = pct
                                    backupStatusMessage = msg
                                }
                                isOperating = false
                                if (restoreResult.isSuccess) {
                                    val restoredSummary = restoreResult.getOrThrow()
                                    backupStatusMessage = "Restoration complete! ${restoredSummary.totalItemsCount} items restored."
                                } else {
                                    backupStatusMessage = "Restoration failed: ${restoreResult.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    }
                ) {
                    Text("Execute Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestorePreviewDialog = false
                        restoreValidationResult = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Sample Data Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Sample Data?") },
            text = { Text("This will re-seed LECTRA with the realistic university courses, timetable schedules, flashcard decks, and practice quizzes.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoData()
                        showResetDialog = false
                    }
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ThemeOptionItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .heightIn(min = 84.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
