package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.document.DocumentType
import com.example.data.model.CourseEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.TopicEntity
import com.example.data.model.UnitEntity
import com.example.ui.components.AddTopicDialog
import com.example.ui.components.AddUnitDialog
import com.example.ui.theme.ThemeMode

/**
 * Course Workspace Screen (CourseDetailScreen) implementing clean academic UI,
 * compact overview counts, Units, Notes, and Tasks tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: CourseEntity,
    units: List<UnitEntity>,
    topics: List<TopicEntity>,
    materials: List<LectureMaterialEntity>,
    tasks: List<StudyTaskEntity> = emptyList(),
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onBackClick: () -> Unit,
    onAddUnit: (unitNumber: Int, title: String, description: String) -> Unit,
    onAddTopic: (unitId: Long, courseId: Long, title: String, notes: String) -> Unit,
    onToggleTopicMastered: (TopicEntity) -> Unit,
    onPdfSelected: (Uri, courseId: Long, unitId: Long?, topicId: Long?) -> Unit,
    onDeleteMaterial: (LectureMaterialEntity) -> Unit,
    onStartFlashcards: (Long) -> Unit,
    onStartQuiz: (Long) -> Unit,
    onStartPomodoro: (Long) -> Unit = {},
    onOpenMaterial: (LectureMaterialEntity) -> Unit = {},
    onDeleteCourse: (CourseEntity) -> Unit = {},
    onToggleTask: (StudyTaskEntity) -> Unit = {}
) {
    BackHandler { onBackClick() }

    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val mainBackground = if (isDark) Color(0xFF0B1020) else Color(0xFFFFFFFF)
    val cardBackground = if (isDark) Color(0xFF111B36) else Color(0xFFFFFFFF)
    val secondaryCardBg = if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0B1020)
    val secondaryText = if (isDark) Color(0xFFB7C3E0) else Color(0xFF64748B)
    val primaryViolet = if (isDark) Color(0xFF806BFF) else Color(0xFF7565F6)
    val borderColor = if (isDark) Color(0xFF293966) else Color(0xFFE3E7F2)

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Units, 1: Notes, 2: Tasks
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var unitForNewTopic by remember { mutableStateOf<UnitEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedDocFilter by remember { mutableStateOf("All") }
    var docSearchQuery by remember { mutableStateOf("") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onPdfSelected(it, course.id, units.firstOrNull()?.id, null)
        }
    }

    val accentColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (_: Exception) {
        primaryViolet
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBackground)
            .statusBarsPadding()
            .testTag("course_detail_screen")
    ) {
        // ==========================================
        // 1. HEADER CARD & MANAGEMENT MENU
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to courses",
                            tint = primaryText
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = course.code,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Course Options",
                                    tint = primaryText
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Start Focus Timer") },
                                    onClick = {
                                        showMenu = false
                                        onStartPomodoro(course.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Practice Flashcards") },
                                    onClick = {
                                        showMenu = false
                                        onStartFlashcards(course.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Take Practice Quiz") },
                                    onClick = {
                                        showMenu = false
                                        onStartQuiz(course.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Course", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = course.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )

                if (course.lecturer.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instructor: ${course.lecturer} • ${course.semester}",
                        fontSize = 13.sp,
                        color = secondaryText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 2. COMPACT OVERVIEW COUNTS
                // ==========================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewCountPill(label = "${units.size} Units", isDark = isDark, modifier = Modifier.weight(1f))
                    OverviewCountPill(label = "${materials.size} Notes", isDark = isDark, modifier = Modifier.weight(1f))
                    OverviewCountPill(label = "${tasks.size} Tasks", isDark = isDark, modifier = Modifier.weight(1f))
                }
            }
        }

        // ==========================================
        // 3. THREE NAVIGATION SECTIONS (TABS)
        // ==========================================
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = cardBackground,
            contentColor = primaryViolet
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Units (${units.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Notes (${materials.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Tasks (${tasks.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        // ==========================================
        // 4. TAB CONTENT
        // ==========================================
        when (selectedTabIndex) {
            0 -> {
                // Units Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    item(key = "units_header_row") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Course Units & Syllabus",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            FilledTonalButton(
                                onClick = { showAddUnitDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = secondaryCardBg),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_unit_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = primaryViolet, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Unit", fontSize = 13.sp, color = primaryText)
                            }
                        }
                    }

                    if (units.isEmpty()) {
                        item(key = "empty_units_state") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No units or topics added yet.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showAddUnitDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Create Unit 1", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        items(units, key = { "course_unit_${it.id}" }) { unit ->
                            val unitTopics = topics.filter { it.unitId == unit.id }
                            UnitAccordionCard(
                                unit = unit,
                                topics = unitTopics,
                                isDark = isDark,
                                cardBackground = cardBackground,
                                borderColor = borderColor,
                                primaryViolet = primaryViolet,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onAddTopic = { unitForNewTopic = unit },
                                onToggleMastered = onToggleTopicMastered
                            )
                        }
                    }
                }
            }

            1 -> {
                // Notes & Lecture Documents Tab (PDF, DOC/DOCX, XLS/XLSX, PPT/PPTX, TXT, EPUB)
                val filteredMaterials = materials.filter { mat ->
                    val type = DocumentType.fromMimeOrFilename(mat.mimeType, mat.title) ?: DocumentType.PDF
                    val matchesFilter = selectedDocFilter.equals("All", ignoreCase = true) ||
                            type.filterCategory.equals(selectedDocFilter, ignoreCase = true)
                    val matchesSearch = docSearchQuery.isBlank() ||
                            mat.title.contains(docSearchQuery, ignoreCase = true)
                    matchesFilter && matchesSearch
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    item(key = "import_pdf_banner") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = secondaryCardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Import Lecture Materials",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Attach PDF, Word, Excel, PowerPoint, Text & EPUB files.",
                                        fontSize = 12.sp,
                                        color = secondaryText
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = { pdfPickerLauncher.launch(DocumentType.SUPPORTED_MIME_TYPES) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("pick_pdf_button")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import File", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    item(key = "document_filters_row") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Document Library",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    text = "${materials.size} documents",
                                    fontSize = 12.sp,
                                    color = secondaryText
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(DocumentType.ALL_FILTER_CATEGORIES) { category ->
                                    val isSelected = selectedDocFilter.equals(category, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDocFilter = category },
                                        label = { Text(category) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryViolet,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("filter_chip_$category")
                                    )
                                }
                            }
                        }
                    }

                    if (materials.isNotEmpty()) {
                        item(key = "document_search_bar") {
                            OutlinedTextField(
                                value = docSearchQuery,
                                onValueChange = { docSearchQuery = it },
                                placeholder = { Text("Search documents...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (docSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { docSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = secondaryText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = cardBackground,
                                    unfocusedContainerColor = cardBackground,
                                    focusedBorderColor = primaryViolet,
                                    unfocusedBorderColor = borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("doc_search_field")
                            )
                        }
                    }

                    if (materials.isEmpty()) {
                        item(key = "empty_materials_state") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No lecture notes or documents in this course yet.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Import PDF, DOC, XLS, PPT, TXT or EPUB files to start reading.",
                                        fontSize = 12.sp,
                                        color = secondaryText.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else if (filteredMaterials.isEmpty()) {
                        item(key = "empty_filtered_state") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No documents found matching \"$selectedDocFilter\".",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        selectedDocFilter = "All"
                                        docSearchQuery = ""
                                    }) {
                                        Text("Show All Documents")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredMaterials, key = { "course_material_${it.id}" }) { material ->
                            val unitTitle = units.find { it.id == material.unitId }?.let { "Unit ${it.unitNumber}: ${it.title}" }
                            LectureMaterialCard(
                                material = material,
                                unitTitle = unitTitle,
                                isDark = isDark,
                                cardBackground = cardBackground,
                                borderColor = borderColor,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onDelete = { onDeleteMaterial(material) },
                                onClick = { onOpenMaterial(material) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Tasks Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                ) {
                    item(key = "tasks_header") {
                        Text(
                            text = "Course Assignments & Tasks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    }

                    if (tasks.isEmpty()) {
                        item(key = "empty_tasks_state") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No pending assignments or tasks for this course.",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = secondaryText
                                    )
                                }
                            }
                        }
                    } else {
                        items(tasks, key = { "course_task_${it.id}" }) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleTask(task) }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Priority: ${task.priority}",
                                            fontSize = 12.sp,
                                            color = secondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddUnitDialog) {
        AddUnitDialog(
            courseId = course.id,
            nextUnitNumber = units.size + 1,
            onDismiss = { showAddUnitDialog = false },
            onConfirm = { num, title, desc ->
                onAddUnit(num, title, desc)
            }
        )
    }

    unitForNewTopic?.let { unit ->
        AddTopicDialog(
            unitTitle = "Unit ${unit.unitNumber}: ${unit.title}",
            onDismiss = { unitForNewTopic = null },
            onConfirm = { title, notes ->
                onAddTopic(unit.id, course.id, title, notes)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Course", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${course.code}: ${course.title}? All units and notes will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCourse(course)
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OverviewCountPill(label: String, isDark: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFB7C3E0) else Color(0xFF7565F6)
        )
    }
}

@Composable
private fun UnitAccordionCard(
    unit: UnitEntity,
    topics: List<TopicEntity>,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryViolet: Color,
    primaryText: Color,
    secondaryText: Color,
    onAddTopic: () -> Unit,
    onToggleMastered: (TopicEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit ${unit.unitNumber}: ${unit.title}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    if (unit.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = unit.description,
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }
                }
                TextButton(onClick = onAddTopic) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Topic", fontSize = 12.sp)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                if (topics.isEmpty()) {
                    Text(
                        text = "No topics in this unit yet.",
                        fontSize = 12.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topics.forEach { topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF17244A) else Color(0xFFF8FAFC))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topic.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    if (topic.summaryNotes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = topic.summaryNotes,
                                            fontSize = 11.sp,
                                            color = secondaryText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(onClick = { onToggleMastered(topic) }) {
                                    Icon(
                                        imageVector = if (topic.isMastered) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                        contentDescription = "Toggle Mastered",
                                        tint = if (topic.isMastered) Color(0xFF10B981) else secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureMaterialCard(
    material: LectureMaterialEntity,
    unitTitle: String?,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val docType = remember(material.mimeType, material.fileUriString, material.title) {
        DocumentType.fromMimeOrFilename(material.mimeType, material.title) ?: DocumentType.PDF
    }

    val formattedSize = remember(material.fileSizeBytes) {
        if (material.fileSizeBytes <= 0L) {
            ""
        } else if (material.fileSizeBytes < 1024 * 1024) {
            "${(material.fileSizeBytes / 1024).coerceAtLeast(1)} KB"
        } else {
            String.format(java.util.Locale.US, "%.1f MB", material.fileSizeBytes.toDouble() / (1024 * 1024))
        }
    }

    val progressDetail = when (docType.filterCategory) {
        "PPT" -> "Slide ${material.lastReadPage} of ${material.totalPages}"
        "EPUB" -> "Chapter ${material.lastReadPage} of ${material.totalPages}"
        "XLS" -> "${material.totalPages} Sheet(s)"
        "TXT" -> "${material.totalPages} Lines"
        else -> "Page ${material.lastReadPage} of ${material.totalPages}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("lecture_material_card_${material.title}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(docType.badgeColor.copy(alpha = if (isDark) 0.25f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = docType.filterCategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = docType.badgeColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = material.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = progressDetail,
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                        if (formattedSize.isNotBlank()) {
                            Text(
                                text = " • $formattedSize",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                        if (!unitTitle.isNullOrBlank()) {
                            Text(
                                text = " • $unitTitle",
                                fontSize = 11.sp,
                                color = docType.badgeColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Document",
                    tint = secondaryText
                )
            }
        }
    }
}
