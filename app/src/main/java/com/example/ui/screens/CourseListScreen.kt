package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.CourseEntity
import com.example.data.model.LectureMaterialEntity
import com.example.data.model.StudyTaskEntity
import com.example.data.model.UnitEntity
import com.example.ui.theme.ThemeMode

/**
 * Option 4: Course Dashboard Screen implementing Option 4 visual specification.
 */
@Composable
fun CourseListScreen(
    courses: List<CourseEntity>,
    materials: List<LectureMaterialEntity>,
    units: List<UnitEntity> = emptyList(),
    tasks: List<StudyTaskEntity> = emptyList(),
    onCourseClick: (Long) -> Unit,
    onAddCourseClick: () -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.LIGHT
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Exact palette
    val mainBackground = if (isDark) Color(0xFF0B1020) else Color(0xFFFFFFFF)
    val cardBackground = if (isDark) Color(0xFF111B36) else Color(0xFFFFFFFF)
    val secondaryCardBg = if (isDark) Color(0xFF17244A) else Color(0xFFF5F3FF)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0B1020)
    val secondaryText = if (isDark) Color(0xFFB7C3E0) else Color(0xFF64748B)
    val primaryViolet = if (isDark) Color(0xFF806BFF) else Color(0xFF7565F6)
    val borderColor = if (isDark) Color(0xFF293966) else Color(0xFFE3E7F2)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Multiple Units, Standalone
    var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }

    val filteredCourses = remember(courses, units, searchQuery, selectedFilter) {
        courses.filter { course ->
            val matchesSearch = course.title.contains(searchQuery, ignoreCase = true) ||
                    course.code.contains(searchQuery, ignoreCase = true) ||
                    course.lecturer.contains(searchQuery, ignoreCase = true)

            val courseUnitsList = units.filter { it.courseId == course.id }
            val isMultiple = courseUnitsList.size > 1 || (courseUnitsList.size == 1 && !courseUnitsList[0].title.contains("General", ignoreCase = true))

            val matchesFilter = when (selectedFilter) {
                "Multiple Units" -> isMultiple
                "Standalone" -> !isMultiple
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(mainBackground)
            .statusBarsPadding()
            .testTag("course_list_screen"),
        containerColor = mainBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCourseClick,
                containerColor = primaryViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_course_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                    Text("Add Course", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 1. SCREEN HEADER
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Courses",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your academic space",
                        fontSize = 14.sp,
                        color = secondaryText
                    )
                }

                Button(
                    onClick = onAddCourseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = secondaryCardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.testTag("header_add_course_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = primaryViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Course", fontSize = 13.sp, color = primaryText, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 2. SEARCH & FILTER CHIPS
            // ==========================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by course name or code", color = secondaryText) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = secondaryText)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryViolet,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground,
                    focusedTextColor = primaryText,
                    unfocusedTextColor = primaryText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("course_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Multiple Units", "Standalone").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = cardBackground,
                            selectedContainerColor = secondaryCardBg,
                            labelColor = secondaryText,
                            selectedLabelColor = primaryViolet
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = borderColor,
                            selectedBorderColor = primaryViolet
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. COURSE CARDS GRID OR EMPTY STATE
            // ==========================================
            if (filteredCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(secondaryCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = primaryViolet,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your courses start here",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add your university courses and organize your lecture notes, units, and study materials.",
                            fontSize = 13.sp,
                            color = secondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddCourseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_add_course_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Course", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("course_grid"),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCourses, key = { "course_grid_item_${it.id}" }) { course ->
                        val courseUnitsList = units.filter { it.courseId == course.id }
                        val courseMaterialsList = materials.filter { it.courseId == course.id }
                        val courseTasksList = tasks.filter { it.courseId == course.id }
                        val isMultiple = courseUnitsList.size > 1 || (courseUnitsList.size == 1 && !courseUnitsList[0].title.contains("General", ignoreCase = true))

                        CourseDashboardCard(
                            course = course,
                            isMultipleUnits = isMultiple,
                            unitCount = courseUnitsList.size,
                            noteCount = courseMaterialsList.size,
                            assignmentCount = courseTasksList.size,
                            isDark = isDark,
                            cardBackground = cardBackground,
                            borderColor = borderColor,
                            primaryViolet = primaryViolet,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { onCourseClick(course.id) },
                            onLongClick = { courseToDelete = course }
                        )
                    }
                }
            }
        }
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Delete Course", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${course.code}: ${course.title}? All associated units, notes, and materials will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCourse(course)
                        courseToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Individual Course Card in the two-column grid.
 */
@Composable
private fun CourseDashboardCard(
    course: CourseEntity,
    isMultipleUnits: Boolean,
    unitCount: Int,
    noteCount: Int,
    assignmentCount: Int,
    isDark: Boolean,
    cardBackground: Color,
    borderColor: Color,
    primaryViolet: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(course.colorHex))
    } catch (_: Exception) {
        primaryViolet
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("course_card_${course.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Tinted Icon Container & Code Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF1F2E5E) else Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = course.code,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Course Name
            Text(
                text = course.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Structure Label
            Text(
                text = if (isMultipleUnits) "Multiple units" else "Standalone course",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = primaryViolet
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Optional Counts (Units, Notes, Assignments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unitCount > 0) {
                    CountBadge(label = "$unitCount units", isDark = isDark)
                }
                if (noteCount > 0) {
                    CountBadge(label = "$noteCount notes", isDark = isDark)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom "Open course" cue & arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open course",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun CountBadge(label: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDark) Color(0xFF17244A) else Color(0xFFF8FAFC))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}
