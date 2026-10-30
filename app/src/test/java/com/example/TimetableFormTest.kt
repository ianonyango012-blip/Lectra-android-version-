package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.CourseEntity
import com.example.ui.components.AddEditLectureClassDialog
import com.example.ui.theme.LectraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimetableFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun timeEditingWorksCorrectly() {
        var savedStartHour = 0
        var savedEndHour = 0

        composeTestRule.setContent {
            LectraTheme {
                AddEditLectureClassDialog(
                    courses = listOf(CourseEntity(1, "CS101", "Intro", "Prof", "#FFFFFF", 3)),
                    onDismiss = {},
                    onConfirm = { }
                )
            }
        }

        // Test editing start time
        composeTestRule.onNodeWithTag("class_start_hour_input").performTextReplacement("15")
        composeTestRule.onNodeWithTag("class_end_hour_input").performTextReplacement("16")

        // Save
        composeTestRule.onNodeWithTag("save_class_button").performClick()

        assert(savedStartHour == 15)
        assert(savedEndHour == 16)
    }
}
