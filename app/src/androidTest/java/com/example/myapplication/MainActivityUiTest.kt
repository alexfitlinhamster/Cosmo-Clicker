package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Before
import org.junit.Test

class MainActivityUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun freezeGameAnimations() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun applicationIdAndMainGameAreAvailable() {
        assertEquals(
            "com.orbitsalvagers.droneclicker",
            composeRule.activity.applicationContext.packageName
        )

        dismissStartScreenIfPresent()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("debris").assertIsDisplayed()
    }

    @Test
    fun settingsLanguageAndResetDialogsAreReachable() {
        dismissStartScreenIfPresent()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        composeRule.onNodeWithText("Language").performClick()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Русский").assertIsDisplayed()
        composeRule.onNodeWithText("Español").assertIsDisplayed()
        composeRule.onNodeWithText("English").performClick()

        composeRule.onNodeWithText("Reset game progress").performClick()
        composeRule.onNodeWithText("Reset all progress?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onAllNodesWithText("Reset all progress?").assertCountEquals(0)
    }

    private fun dismissStartScreenIfPresent() {
        val prompt = composeRule.onAllNodesWithText("Tap me to continue")
        if (prompt.fetchSemanticsNodes().isNotEmpty()) {
            prompt[0].performClick()
            composeRule.mainClock.advanceTimeBy(1_000)
        }
    }
}
