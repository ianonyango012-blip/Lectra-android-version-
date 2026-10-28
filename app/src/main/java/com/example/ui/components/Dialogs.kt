package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import com.example.data.model.CourseEntity

@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, title: String, lecturer: String, colorHex: String, targetHours: Int) -> Unit,
    onConfirmWithUnits: (code: String, title: String, lecturer: String, colorHex: String, targetHours: Int, units: List<String>) -> Unit = { c, t, l, cl, h, us -> onConfirm(c, t, l, cl, h) }
) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var lecturer by remember { mutableStateOf("") }
    var targetHours by remember { mutableStateOf("6") }
    var selectedColor by remember { mutableStateOf("#7565F6") }
    var isMultipleUnits by remember { mutableStateOf(true) }
    var unitList by remember { mutableStateOf(mutableListOf("Unit 1: Introduction", "Unit 2: Core Concepts")) }

    val colorPalette = listOf("#7565F6", "#2563EB", "#10B981", "#D97706", "#EC4899", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add University Course", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. CS 301)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_course_code_input")
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_course_title_input")
                )
                OutlinedTextField(
                    value = lecturer,
                    onValueChange = { lecturer = it },
                    label = { Text("Lecturer / Professor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it },
                    label = { Text("Target Weekly Study Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Course Structure",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isMultipleUnits = false }
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = !isMultipleUnits,
                            onClick = { isMultipleUnits = false }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Standalone", fontSize = 13.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isMultipleUnits = true }
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = isMultipleUnits,
                            onClick = { isMultipleUnits = true }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Multiple Units", fontSize = 13.sp)
                    }
                }

                if (isMultipleUnits) {
                    Text(
                        text = "Course Units",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    unitList.forEachIndexed { index, unitName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = unitName,
                                onValueChange = { newValue ->
                                    unitList = unitList.toMutableList().apply { this[index] = newValue }
                                },
                                label = { Text("Unit ${index + 1}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unit_input_$index")
                            )
                            if (unitList.size > 1) {
                                IconButton(onClick = {
                                    unitList = unitList.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Unit", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            unitList = unitList.toMutableList().apply { add("Unit ${size + 1}") }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Unit", fontSize = 13.sp)
                    }
                }

                Text(
                    text = "Select Course Accent Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorPalette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && title.isNotBlank()) {
                        val hours = targetHours.toIntOrNull() ?: 6
                        if (isMultipleUnits) {
                            onConfirmWithUnits(code, title, lecturer, selectedColor, hours, unitList)
                        } else {
                            onConfirmWithUnits(code, title, lecturer, selectedColor, hours, emptyList())
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_course_button")
            ) {
                Text("Add Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddUnitDialog(
    courseId: Long,
    nextUnitNumber: Int,
    onDismiss: () -> Unit,
    onConfirm: (unitNumber: Int, title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Course Unit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unit $nextUnitNumber", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Unit Title (e.g. Distributed Consensus)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Unit Description / Syllabus Focus") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(nextUnitNumber, title, description)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Unit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTopicDialog(
    unitTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic to $unitTitle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Topic Title (e.g. Raft vs Paxos)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lecture Takeaways / Key Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, notes)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudySessionDialog(
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (course: CourseEntity, topic: String, dayOfWeek: Int, startHour: Int, startMinute: Int, duration: Int, goal: String) -> Boolean
) {
    if (courses.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Courses Found") },
            text = { Text("Please create at least one course before scheduling a study session.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedCourse by remember { mutableStateOf(courses.first()) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var topic by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var selectedDay by remember { mutableIntStateOf(1) } // Monday = 1
    var startHour by remember { mutableIntStateOf(14) }
    var durationMinutes by remember { mutableIntStateOf(60) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Study Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Course Dropdown
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCourse.code}: ${selectedCourse.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} - ${course.title}") },
                                onClick = {
                                    selectedCourse = course
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic Focus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Day of the Week", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    days.forEachIndexed { index, dayName ->
                        val dayNum = index + 1
                        val isSelected = selectedDay == dayNum
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDay = dayNum }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startHour.toString(),
                        onValueChange = { startHour = it.toIntOrNull() ?: 14 },
                        label = { Text("Start Hour (0-23)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull() ?: 60 },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Session Goal / Target Outcome") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val topicTitle = if (topic.isNotBlank()) topic else "Lecture Review"
                    val success = onConfirm(selectedCourse, topicTitle, selectedDay, startHour, 0, durationMinutes, goal)
                    if (success) onDismiss()
                },
                modifier = Modifier.testTag("save_study_session_button")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyTaskDialog(
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Long, courseCode: String, title: String, dueDays: Int, priority: String) -> Unit
) {
    if (courses.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Courses Found") },
            text = { Text("Please create a course before adding an academic deadline.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedCourse by remember { mutableStateOf(courses.first()) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var dueDays by remember { mutableStateOf("3") }
    var priority by remember { mutableStateOf("HIGH") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment or Exam Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCourse.code}: ${selectedCourse.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Associated Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} - ${course.title}") },
                                onClick = {
                                    selectedCourse = course
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Assignment Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDays,
                    onValueChange = { dueDays = it },
                    label = { Text("Days until Deadline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                        val isSelected = priority == p
                        OutlinedButton(
                            onClick = { priority = p },
                            colors = if (isSelected) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(p, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val days = dueDays.toIntOrNull() ?: 3
                        onConfirm(selectedCourse.id, selectedCourse.code, title, days, priority)
                        onDismiss()
                    }
                }
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFlashcardDialog(
    courses: List<CourseEntity>,
    defaultCourseId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Long, prompt: String, answer: String, hint: String?) -> Unit
) {
    if (courses.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Courses") },
            text = { Text("Please create a course first to attach flashcards to.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    val initialCourse = courses.find { it.id == defaultCourseId } ?: courses.first()
    var selectedCourse by remember { mutableStateOf(initialCourse) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Revision Flashcard", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCourse.code}: ${selectedCourse.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course Deck") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} - ${course.title}") },
                                onClick = {
                                    selectedCourse = course
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Front Prompt (Question or Term)") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_prompt_input")
                )

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Back Answer (Explanation & Definition)") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_answer_input")
                )

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Optional Memory Cue / Hint") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank() && answer.isNotBlank()) {
                        onConfirm(selectedCourse.id, prompt, answer, hint)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_flashcard_button")
            ) {
                Text("Save Flashcard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuizDialog(
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (courseId: Long, question: String, optA: String, optB: String, optC: String, optD: String, correctIdx: Int, explanation: String) -> Unit
) {
    if (courses.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Courses") },
            text = { Text("Please create a course first to attach quizzes to.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedCourse by remember { mutableStateOf(courses.first()) }
    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var optA by remember { mutableStateOf("") }
    var optB by remember { mutableStateOf("") }
    var optC by remember { mutableStateOf("") }
    var optD by remember { mutableStateOf("") }
    var correctIdx by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Practice Quiz Question", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseDropdownExpanded,
                    onExpandedChange = { courseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCourse.code}: ${selectedCourse.title}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text("${course.code} - ${course.title}") },
                                onClick = {
                                    selectedCourse = course
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Options & Select Correct Answer", style = MaterialTheme.typography.labelMedium)

                listOf(
                    Pair("Option A", optA to { v: String -> optA = v }),
                    Pair("Option B", optB to { v: String -> optB = v }),
                    Pair("Option C", optC to { v: String -> optC = v }),
                    Pair("Option D", optD to { v: String -> optD = v })
                ).forEachIndexed { index, (label, statePair) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = correctIdx == index,
                            onClick = { correctIdx = index }
                        )
                        OutlinedTextField(
                            value = statePair.first,
                            onValueChange = statePair.second,
                            label = { Text(label) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("Explanation for the Correct Answer") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
                        onConfirm(selectedCourse.id, question, optA, optB, optC, optD, correctIdx, explanation)
                        onDismiss()
                    }
                }
            ) {
                Text("Save Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
