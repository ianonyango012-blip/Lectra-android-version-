package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupManager
import com.example.data.preferences.LectraPreferences
import com.example.ui.LectraUiState
import com.example.ui.screens.settings.AcademicPreferencesScreen
import com.example.ui.screens.settings.AppearanceSettingsScreen
import com.example.ui.screens.settings.DataStorageSettingsScreen
import com.example.ui.screens.settings.PrivacySupportScreen
import com.example.ui.theme.ThemeMode

enum class SettingsSubScreen {
    NONE,
    APPEARANCE,
    ACADEMIC_PREFERENCES,
    DATA_STORAGE,
    PRIVACY_SUPPORT
}

@Composable
fun SettingsScreen(
    uiState: LectraUiState,
    backupManager: BackupManager,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onResetDemoData: () -> Unit,
    onReconfigureSemester: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { LectraPreferences(context) }
    val primaryViolet = Color(0xFF7565F6)

    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.NONE) }

    // Intercept back button when inside a category sub-screen
    BackHandler(enabled = activeSubScreen != SettingsSubScreen.NONE) {
        activeSubScreen = SettingsSubScreen.NONE
    }

    when (activeSubScreen) {
        SettingsSubScreen.APPEARANCE -> {
            AppearanceSettingsScreen(
                currentThemeMode = uiState.themeMode,
                onThemeModeChanged = onThemeModeChanged,
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE }
            )
        }

        SettingsSubScreen.ACADEMIC_PREFERENCES -> {
            AcademicPreferencesScreen(
                preferences = preferences,
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE }
            )
        }

        SettingsSubScreen.DATA_STORAGE -> {
            DataStorageSettingsScreen(
                uiState = uiState,
                backupManager = backupManager,
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE }
            )
        }

        SettingsSubScreen.PRIVACY_SUPPORT -> {
            PrivacySupportScreen(
                onClearAllData = onResetDemoData,
                onBackClick = { activeSubScreen = SettingsSubScreen.NONE }
            )
        }

        SettingsSubScreen.NONE -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. ACADEMIC PROFILE CARD
                // ==========================================
                item(key = "settings_profile_card") {
                    val sharedPrefs = remember { context.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE) }
                    val institution = sharedPrefs.getString("user_institution", "University Scholar") ?: "University Scholar"
                    val programme = sharedPrefs.getString("user_programme", "Degree Programme") ?: "Degree Programme"
                    val term = sharedPrefs.getString("user_semester", "Term 1 - 2026") ?: "Term 1 - 2026"

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
                                    color = primaryViolet.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Academic Profile",
                                        tint = primaryViolet,
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
                                        color = primaryViolet,
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
                                Text("Reconfigure Semester & Courses")
                            }
                        }
                    }
                }

                // ==========================================
                // 2. CATEGORY A: APPEARANCE
                // ==========================================
                item(key = "category_appearance") {
                    val currentThemeLabel = when (uiState.themeMode) {
                        ThemeMode.LIGHT -> "Light Mode"
                        ThemeMode.DARK -> "Dark Mode"
                        ThemeMode.SYSTEM -> "System Default"
                    }

                    SettingsCategoryCard(
                        categoryTitle = "Appearance",
                        categorySubtitle = "Theme, accent colour, and font display",
                        categoryIcon = Icons.Default.Palette,
                        categoryTestTag = "category_appearance_card",
                        onOpenCategory = { activeSubScreen = SettingsSubScreen.APPEARANCE },
                        items = listOf(
                            SettingsRowData(
                                icon = when (uiState.themeMode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                },
                                title = "Theme",
                                subtitle = "Currently: $currentThemeLabel",
                                testTag = "setting_row_theme"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.Palette,
                                title = "Accent Colour",
                                subtitle = "LECTRA Signature Violet (#7565F6)",
                                testTag = "setting_row_accent"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.FontDownload,
                                title = "Font and Display",
                                subtitle = "Inherits Android device font family (e.g. Samsung Sans)",
                                testTag = "setting_row_font"
                            )
                        )
                    )
                }

                // ==========================================
                // 3. CATEGORY B: ACADEMIC PREFERENCES
                // ==========================================
                item(key = "category_academic") {
                    SettingsCategoryCard(
                        categoryTitle = "Academic Preferences",
                        categorySubtitle = "Timetable, reading, revision, and alerts",
                        categoryIcon = Icons.Default.AutoStories,
                        categoryTestTag = "category_academic_card",
                        onOpenCategory = { activeSubScreen = SettingsSubScreen.ACADEMIC_PREFERENCES },
                        items = listOf(
                            SettingsRowData(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                subtitle = "Upcoming classes, deadlines & study alerts",
                                testTag = "setting_row_notifications"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.CalendarMonth,
                                title = "Timetable Preferences",
                                subtitle = "First day of week (${if (preferences.timetableFirstDayOfWeek == 1) "Monday" else "Sunday"}) & layout",
                                testTag = "setting_row_timetable"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.AutoStories,
                                title = "Reading Preferences",
                                subtitle = "Built-in PDF reader, night palette & auto-save",
                                testTag = "setting_row_reading"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.Style,
                                title = "Revision Preferences",
                                subtitle = "Flashcard review order, question scope & timer",
                                testTag = "setting_row_revision"
                            )
                        )
                    )
                }

                // ==========================================
                // 4. CATEGORY C: DATA AND STORAGE
                // ==========================================
                item(key = "category_data_storage") {
                    val lastBackup = backupManager.getLastBackupInfo()?.second ?: "No export yet"

                    SettingsCategoryCard(
                        categoryTitle = "Data and Storage",
                        categorySubtitle = "Local storage, offline backups, and exports",
                        categoryIcon = Icons.Default.Storage,
                        categoryTestTag = "category_data_storage_card",
                        onOpenCategory = { activeSubScreen = SettingsSubScreen.DATA_STORAGE },
                        items = listOf(
                            SettingsRowData(
                                icon = Icons.Default.Storage,
                                title = "Storage Management",
                                subtitle = "${uiState.materials.size} lecture PDFs • Cache cleanup",
                                testTag = "setting_row_storage"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.FolderZip,
                                title = "Backup and Restore",
                                subtitle = "Offline JSON archive • Last: $lastBackup",
                                testTag = "setting_row_backup"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.Share,
                                title = "Export Data",
                                subtitle = "Share notes, timetable & study records",
                                testTag = "setting_row_export"
                            )
                        )
                    )
                }

                // ==========================================
                // 5. CATEGORY D: PRIVACY AND SUPPORT
                // ==========================================
                item(key = "category_privacy_support") {
                    SettingsCategoryCard(
                        categoryTitle = "Privacy and Support",
                        categorySubtitle = "Local-first principles, FAQs, and about",
                        categoryIcon = Icons.Default.Security,
                        categoryTestTag = "category_privacy_support_card",
                        onOpenCategory = { activeSubScreen = SettingsSubScreen.PRIVACY_SUPPORT },
                        items = listOf(
                            SettingsRowData(
                                icon = Icons.Default.Security,
                                title = "Privacy and Security",
                                subtitle = "100% on-device SQLite storage • Reset data",
                                testTag = "setting_row_privacy"
                            ),
                            SettingsRowData(
                                icon = Icons.AutoMirrored.Filled.HelpOutline,
                                title = "Help and Support",
                                subtitle = "Academic guides and frequently asked questions",
                                testTag = "setting_row_help"
                            ),
                            SettingsRowData(
                                icon = Icons.Default.Info,
                                title = "About LECTRA",
                                subtitle = "Version 1.2.0 • Open-source licences",
                                testTag = "setting_row_about"
                            )
                        )
                    )
                }

                // Footer
                item(key = "settings_footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LECTRA v1.2.0",
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
        }
    }
}

private data class SettingsRowData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val testTag: String
)

@Composable
private fun SettingsCategoryCard(
    categoryTitle: String,
    categorySubtitle: String,
    categoryIcon: ImageVector,
    categoryTestTag: String,
    onOpenCategory: () -> Unit,
    items: List<SettingsRowData>
) {
    val primaryViolet = Color(0xFF7565F6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenCategory() }
            .testTag(categoryTestTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = primaryViolet.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = categoryIcon,
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
                            text = categoryTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = categorySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open $categoryTitle",
                    tint = primaryViolet,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Setting items preview list
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag(item.testTag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = primaryViolet.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
