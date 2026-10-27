package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.splash.SplashScreen
import com.example.ui.welcome.WelcomeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LaunchScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Screen A minimalist splash contains only wordmark and tagline`() {
        var splashFinished = false

        composeTestRule.setContent {
            SplashScreen(
                autoDismiss = false,
                onSplashFinished = { splashFinished = true }
            )
        }

        // Verify Wordmark "Lectra" is displayed
        composeTestRule.onNodeWithTag("splash_wordmark").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lectra").assertIsDisplayed()

        // Verify two-line tagline is displayed
        composeTestRule.onNodeWithTag("splash_tagline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your notes. Your structure.\nYour success.").assertIsDisplayed()

        // Strict verification: MUST NOT contain book logo or Get Started button
        composeTestRule.onNodeWithTag("welcome_book_logo").assertDoesNotExist()
        composeTestRule.onNodeWithTag("welcome_get_started_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Get Started").assertDoesNotExist()
        composeTestRule.onNodeWithText("Study smarter, not harder.").assertDoesNotExist()

        // Tapping screen immediately dismisses splash
        composeTestRule.onNodeWithTag("splash_screen_container").performClick()
        assertTrue("Tapping splash must finish it immediately", splashFinished)
    }

    @Test
    fun `Screen A minimalist splash auto-dismisses after timeout`() {
        var splashFinished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SplashScreen(
                autoDismiss = true,
                splashDurationMillis = 1000L,
                onSplashFinished = { splashFinished = true }
            )
        }

        assertFalse("Splash should not finish immediately", splashFinished)
        composeTestRule.mainClock.advanceTimeBy(1001L)
        assertTrue("Splash must auto-dismiss after timeout", splashFinished)
    }

    @Test
    fun `Screen B welcome screen displays book logo, title, taglines, and Get Started button`() {
        var getStartedClicked = false

        composeTestRule.setContent {
            WelcomeScreen(
                onGetStarted = { getStartedClicked = true }
            )
        }

        // 1. Open-book logo
        composeTestRule.onNodeWithTag("welcome_book_logo").assertIsDisplayed()

        // 2. App Title "Lectra"
        composeTestRule.onNodeWithTag("welcome_app_title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lectra").assertIsDisplayed()

        // 3. Main two-line tagline
        composeTestRule.onNodeWithTag("welcome_main_tagline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your notes. Your structure.\nYour success.").assertIsDisplayed()

        // 4. "Get Started" wide pill button
        composeTestRule.onNodeWithTag("welcome_get_started_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()

        // 5. Bottom tagline "Study smarter, not harder."
        composeTestRule.onNodeWithTag("welcome_bottom_tagline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Study smarter, not harder.").assertIsDisplayed()

        // Click Get Started button
        composeTestRule.onNodeWithTag("welcome_get_started_button").performClick()
        assertTrue("Clicking Get Started button must trigger callback", getStartedClicked)
    }

    @Test
    fun `test onboarding flag persistence in shared preferences`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = app.getSharedPreferences("lectra_prefs", Context.MODE_PRIVATE)

        // Clear onboarding flag to simulate fresh install
        prefs.edit().remove("onboarding_completed").apply()
        assertEquals(false, prefs.getBoolean("onboarding_completed", false))

        // Complete onboarding
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        assertEquals(true, prefs.getBoolean("onboarding_completed", false))
    }
}
