package com.example

import android.app.Application
import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.LectraUiState
import com.example.ui.LectraViewModel
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LectraTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `default theme mode is SYSTEM when no preference exists and persists changes`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("app_theme_mode").apply()

        val viewModel = LectraViewModel(app)
        // Verify default on clean startup without saved preference is SYSTEM as required
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        // Change to DARK
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.DARK.name, prefs.getString("app_theme_mode", null))

        // Change to LIGHT
        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.LIGHT.name, prefs.getString("app_theme_mode", null))

        // Change back to SYSTEM
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.SYSTEM.name, prefs.getString("app_theme_mode", null))
    }

    @Test
    fun `settings screen displays theme options and invokes callback`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LectraViewModel(app)
        var selectedTheme: ThemeMode? = null

        composeTestRule.setContent {
            LectraTheme(themeMode = ThemeMode.LIGHT) {
                SettingsScreen(
                    uiState = LectraUiState(themeMode = ThemeMode.LIGHT),
                    backupManager = viewModel.backupManager,
                    onThemeModeChanged = { selectedTheme = it },
                    onResetDemoData = {},
                    onReconfigureSemester = {}
                )
            }
        }

        // Open Appearance category
        composeTestRule.onNodeWithTag("category_appearance_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("category_appearance_card").performClick()

        // Verify appearance screen and all 3 options exist
        composeTestRule.onNodeWithTag("theme_selection_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_option_light").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_option_dark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_option_system").assertIsDisplayed()

        // Click Dark option
        composeTestRule.onNodeWithTag("theme_option_dark").performClick()
        assertEquals(ThemeMode.DARK, selectedTheme)

        // Click System option
        composeTestRule.onNodeWithTag("theme_option_system").performClick()
        assertEquals(ThemeMode.SYSTEM, selectedTheme)

        // Click Light option
        composeTestRule.onNodeWithTag("theme_option_light").performClick()
        assertEquals(ThemeMode.LIGHT, selectedTheme)
    }

    @Test
    fun `LectraTheme renders correctly with dynamic theme switching`() {
        var currentMode by androidx.compose.runtime.mutableStateOf(ThemeMode.LIGHT)
        composeTestRule.setContent {
            LectraTheme(themeMode = currentMode) {
                Text(
                    text = "Lectra ${currentMode.name} Mode",
                    modifier = androidx.compose.ui.Modifier.testTag("theme_label")
                )
            }
        }
        composeTestRule.onNodeWithTag("theme_label").assertIsDisplayed()

        currentMode = ThemeMode.DARK
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_label").assertIsDisplayed()

        currentMode = ThemeMode.SYSTEM
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_label").assertIsDisplayed()
    }
}
