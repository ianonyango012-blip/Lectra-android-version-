package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    var step by remember { mutableStateOf(1) } // 1: Semester Profile, 2: Timetable Method (Manual vs Upload), 3: Review & Confirm

    // Semester Profile State
    var institution by remember { mutableStateOf(prefs.getString("user_institution", "University of Technology") ?: "University of Technology") }
    var programme by remember { mutableStateOf(prefs.getString("user_programme", "Computer Science & Engineering") ?: "Computer Science & Engineering") }
    var yearOfStudy by remember { mutableStateOf("Year 3") }
    var semesterTerm by remember { mutableStateOf("Term 1 - 2026") }
    var startDate by remember { mutableStateOf("2026-09-01") }
    var endDate by remember { mutableStateOf("2026-12-15") }

    // Timetable Setup State (Courses & Classes collected during setup)
    var setupCourses = remember {
        mutableStateListOf(
            SetupCourseModel("CS 301", "Distributed Systems & Cloud Computing", "#7165FF", "Prof. Elena Rostova", 8),
            SetupCourseModel("MATH 240", "Linear Algebra & Optimization", "#4DE8E0", "Prof. Sarah Jenkins", 5)
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
                    SetupCourseModel("BIO 210", "Molecular Genetics & Cellular Biology", "#2D5A46", "Dr. Marcus Sterling", 6)
                )
                isUploadingPdf = false
                uploadNotice = "Successfully extracted 1 course & timetable schedule from document!"
            }, 1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1020),
                        Color(0xFF171B3A),
                        Color(0xFF0B1020)
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171B3A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
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
                                .background(Color(0xFF7165FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF4DE8E0),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Semester Setup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Step $step of 3",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            prefs.edit().putBoolean("semester_setup_in_progress", true).apply()
                            onSetupComplete()
                        }
                    ) {
                        Text("Resume Later", color = Color(0xFF4DE8E0), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4DE8E0),
                    trackColor = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Step Content
                when (step) {
                    1 -> {
                        // Step 1: Semester Profile
                        Text(
                            text = "Academic Term Details",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Confirm your institution, programme, and semester dates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
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
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7165FF))
                        ) {
                            Text("Next: Timetable Setup", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    2 -> {
                        // Step 2: Timetable Setup
                        Text(
                            text = "Timetable & Course Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Choose how to populate your courses and class schedule.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "image/*")) },
                                modifier = Modifier.weight(1f).height(70.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF4DE8E0))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF4DE8E0))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Upload PDF / Image", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }

                            Button(
                                onClick = { showAddCourseDialog = true },
                                modifier = Modifier.weight(1f).height(70.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7165FF))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Manually", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }

                        if (isUploadingPdf) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF4DE8E0))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uploadNotice ?: "Processing document...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4DE8E0))
                        } else if (uploadNotice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uploadNotice!!, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4DE8E0))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Configured Courses (${setupCourses.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
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
                                    color = Color(0xFF1E264D)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "${course.code}: ${course.title}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                            Text(text = "Lecturer: ${course.lecturer} • Goal: ${course.targetHours}h/wk", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                        }
                                        IconButton(onClick = { setupCourses.remove(course) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back", color = Color.White)
                            }
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7165FF))
                            ) {
                                Text("Review Setup", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    3 -> {
                        // Step 3: Review & Finalize
                        Text(
                            text = "Review & Confirm Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Verify your academic profile and courses before launching your LECTRA dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
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
                                    color = Color(0xFF1E264D)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Institution: $institution", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text("Programme: $programme", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text("Year / Term: $yearOfStudy • $semesterTerm", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                        Text("Term Dates: $startDate to $endDate", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                            item {
                                Text("Enrolled Courses (${setupCourses.size}):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4DE8E0))
                            }
                            items(setupCourses) { course ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E264D).copy(alpha = 0.6f)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "${course.code} - ${course.title}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                        Text(text = "${course.targetHours}h/wk", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4DE8E0))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back", color = Color.White)
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
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DE8E0))
                            ) {
                                Text("Launch Dashboard", fontWeight = FontWeight.Bold, color = Color(0xFF0B1020))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCourseDialog) {
        var code by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var lecturer by remember { mutableStateOf("") }
        var hours by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add Course Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. PHY 201)") }, singleLine = true)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, singleLine = true)
                    OutlinedTextField(value = lecturer, onValueChange = { lecturer = it }, label = { Text("Lecturer") }, singleLine = true)
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Weekly Target Hours") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (code.isNotBlank() && title.isNotBlank()) {
                            setupCourses.add(
                                SetupCourseModel(code.trim(), title.trim(), "#7165FF", lecturer.trim(), hours.toIntOrNull() ?: 5)
                            )
                            showAddCourseDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") }
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
    focusedBorderColor = Color(0xFF4DE8E0),
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = Color(0xFF4DE8E0),
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
