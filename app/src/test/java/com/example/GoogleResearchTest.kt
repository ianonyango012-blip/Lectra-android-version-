package com.example

import android.app.Application
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.LectraUiState
import com.example.ui.LectraViewModel
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.ResearchScreen
import com.example.ui.theme.LectraTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleResearchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `openResearch sets SubScreen to RESEARCH and updates query`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LectraViewModel(app)

        assertEquals(SubScreen.NONE, viewModel.uiState.value.currentSubScreen)

        viewModel.openResearch("Eigenvalues & Eigenvectors")
        assertEquals(SubScreen.RESEARCH, viewModel.uiState.value.currentSubScreen)
        assertEquals("Eigenvalues & Eigenvectors", viewModel.uiState.value.researchQuery)

        viewModel.setResearchQuery("Fourier Transform")
        assertEquals("Fourier Transform", viewModel.uiState.value.researchQuery)

        viewModel.closeSubScreen()
        assertEquals(SubScreen.NONE, viewModel.uiState.value.currentSubScreen)
    }

    @Test
    fun `google search url builder properly encodes query with spaces and symbols`() {
        val rawQuery = "Distributed Systems & Cloud Computing: Raft vs Paxos?"
        val searchUri = Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .appendPath("search")
            .appendQueryParameter("q", rawQuery)
            .build()

        assertEquals("https", searchUri.scheme)
        assertEquals("www.google.com", searchUri.authority)
        assertEquals("/search", searchUri.path)
        assertEquals(rawQuery, searchUri.getQueryParameter("q"))
        
        // Confirm string representation encodes spaces and special symbols
        val uriString = searchUri.toString()
        assertTrue(uriString.startsWith("https://www.google.com/search?q="))
        assertTrue(uriString.contains("%20") || uriString.contains("+"))
        assertTrue(uriString.contains("%26")) // & encoded
        assertTrue(uriString.contains("%3F")) // ? encoded
    }

    @Test
    fun `empty query does not open browser and sets user notice`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LectraViewModel(app)

        viewModel.executeGoogleSearch(app, "   ")
        assertEquals("Please enter a topic or concept to research.", viewModel.uiState.value.userNotice)
    }

    @Test
    fun `ResearchScreen renders search field with placeholder and search button`() {
        var submittedSearch = ""
        val currentQuery = androidx.compose.runtime.mutableStateOf("")
        var backClicked = false

        composeTestRule.setContent {
            LectraTheme {
                ResearchScreen(
                    uiState = LectraUiState(
                        researchQuery = currentQuery.value
                    ),
                    onBackClick = { backClicked = true },
                    onQueryChanged = { currentQuery.value = it },
                    onExecuteSearch = { submittedSearch = it }
                )
            }
        }

        // Verify title & placeholder
        composeTestRule.onNodeWithText("Google Search").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search for a topic or concept").assertIsDisplayed()

        // Verify search button
        composeTestRule.onNodeWithTag("research_search_button").assertIsDisplayed()

        // Type query in search field
        composeTestRule.onNodeWithTag("research_search_field").performTextInput("Quantum Computing")
        assertEquals("Quantum Computing", currentQuery.value)

        // Click search button
        composeTestRule.onNodeWithTag("research_search_button").performClick()
        assertEquals("Quantum Computing", submittedSearch)

        // Click back button
        composeTestRule.onNodeWithTag("research_back_button").performClick()
        assertTrue(backClicked)
    }
}
