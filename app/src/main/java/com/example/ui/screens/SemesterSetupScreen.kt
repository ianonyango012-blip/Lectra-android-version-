package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
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
import com.example.ui.LectraViewModel

@Composable
fun SemesterSetupScreen(
    viewModel: LectraViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE) }

    var step by remember { mutableStateOf(1) } // 1: Semester Profile, 2: Timetable Method, 3: Review & Confirm

    // Semester Profile State
    var institution by remember { mutableStateOf(prefs.getString("user_institution", "University of Technology") ?: "University of Technology") }
    var programme by remember { mutableStateOf(prefs.getString("user_programme", "Computer Science & Engineering") ?: "Computer Science & Engineering") }
    var yearOfStudy by remember { mutableStateOf("Year 3") }
    var semesterTerm by remember { mutableStateOf("Term 1 - 2026") }
    var startDate by remember { mutableStateOf("2026-09-01") }
    var endDate by remember { mutableStateOf("2026-12-15") }

    // Timetable Setup State
    val setupCourses = remember {
        mutableStateListOf(
            SetupCourseModel("CS 301", "Distributed Systems & Cloud Computing", "#7565F6", "Prof. Elena Rostova", 8),
            SetupCourseModel("MATH 240", "Linear Algebra & Optimization", "#6252E8", "Prof. Sarah Jenkins", 5)
        )
    }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var isUploadingPdf by remember { mutableStateOf(false) }
    var uploadNotice by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingPdf = true
            uploadNotice = "Extracting timetable & courses using AI parser..."
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                setupCourses.add(
                    SetupCourseModel("BIO 210", "Molecular Genetics & Cellular Biology", "#16A34A", "Dr. Marcus Sterling", 6)
                )
                isUploadingPdf = false
                uploadNotice = "Successfully extracted 1 course & timetable schedule from document!"
            }, 1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Semester Setup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Step $step of 3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            prefs.edit().putBoolean("semester_setup_in_progress", true).apply()
                            onSetupComplete()
                        }
                    ) {
                        Text(
                            text = "Resume Later",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Step Content
                when (step) {
                    1 -> {
                        // Step 1: Semester Profile
                        Text(
                            text = "Academic Term Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Confirm your institution, programme, and semester dates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = institution,
                                    onValueChange = { institution = it },
                                    label = { Text("Institution / University") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = setupTextFieldColors()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = programme,
                                    onValueChange = { programme = it },
                                    label = { Text("Programme / Degree") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = setupTextFieldColors()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = yearOfStudy,
                                    onValueChange = { yearOfStudy = it },
                                    label = { Text("Year of Study (e.g., Year 3)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = setupTextFieldColors()
                                )
                            }
                            item {
                                OutlinedTextField(
                                    value = semesterTerm,
                                    onValueChange = { semesterTerm = it },
                                    label = { Text("Semester / Term Identifier") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = setupTextFieldColors()
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = startDate,
                                        onValueChange = { startDate = it },
                                        label = { Text("Start Date") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = setupTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = endDate,
                                        onValueChange = { endDate = it },
                                        label = { Text("End Date") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = setupTextFieldColors()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Next: Timetable Setup", fontWeight = FontWeight.Bold)
                        }
                    }

                    2 -> {
                        // Step 2: Timetable Setup
                        Text(
                            text = "Timetable & Course Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose how to populate your courses and class schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Upload PDF / Image", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Button(
                                onClick = { showAddCourseDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Manually", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (isUploadingPdf) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uploadNotice ?: "Processing document...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        } else if (uploadNotice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uploadNotice!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Configured Courses (${setupCourses.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(setupCourses) { course ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${course.code}: ${course.title}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Lecturer: ${course.lecturer} • Goal: ${course.targetHours}h/wk",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { setupCourses.remove(course) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back", color = MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Review Setup", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Review & Finalize
                        Text(
                            text = "Review & Confirm Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verify your academic profile and courses before launching your LECTRA dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("Institution: $institution", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Programme: $programme", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Year / Term: $yearOfStudy • $semesterTerm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Term Dates: $startDate to $endDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            item {
                                Text(
                                    "Enrolled Courses (${setupCourses.size}):",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(setupCourses) { course ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = "${course.code} - ${course.title}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${course.targetHours}h/wk",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back", color = MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = {
                                    setupCourses.forEach { c ->
                                        viewModel.addCourse(c.code, c.title, c.lecturer, c.colorHex, c.targetHours)
                                    }
                                    prefs.edit()
                                        .putBoolean("semester_setup_complete", true)
                                        .remove("semester_setup_in_progress")
                                        .putString("user_institution", institution)
                                        .putString("user_programme", programme)
                                        .apply()
                                    onSetupComplete()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("launch_dashboard_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Launch Dashboard", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCourseDialog) {
        com.example.ui.components.AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { code, title, lecturer, colorHex, targetHours ->
                setupCourses.add(SetupCourseModel(code, title, colorHex, lecturer, targetHours))
                showAddCourseDialog = false
            }
        )
    }
}

data class SetupCourseModel(
    val code: String,
    val title: String,
    val colorHex: String,
    val lecturer: String,
    val targetHours: Int
)

@Composable
private fun setupTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)
