package com.example.myapplication

import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun achievementsAreReachableFromSettings() {
        dismissStartScreenIfPresent()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Achievements").performClick()
        composeRule.onNodeWithText("Achievements").assertIsDisplayed()
    }

    @Test
    fun unaffordableShopActionIsVisibleBeforeEarningCurrency() {
        dismissStartScreenIfPresent()
        composeRule.onNodeWithContentDescription("Open shop").performClick()
        composeRule.onAllNodesWithText("Buy")[0].assertIsDisplayed()
    }

    @Test
    fun statisticsExposeCompletionAndJourneyMetrics() {
        dismissStartScreenIfPresent()
        composeRule.onNodeWithContentDescription("Open statistics").performClick()
        composeRule.onNodeWithText("Progress statistics").assertIsDisplayed()
        composeRule.onNodeWithText("Journey progress").assertIsDisplayed()
        composeRule.onNodeWithText("Best combo").assertIsDisplayed()
    }

    @Test
    fun generatedButtonFramesStayOptimizedAndTransparent() {
        val resources = composeRule.activity.resources
        val frames = listOf(
            R.drawable.ui_button_primary_v5,
            R.drawable.ui_button_reward_v5,
            R.drawable.ui_button_danger_v5,
            R.drawable.ui_button_locked_v5
        )

        frames.forEach { drawable ->
            val bitmap = BitmapFactory.decodeResource(resources, drawable)
            assertTrue("Button width is too large", bitmap.width <= 768)
            assertTrue("Button height is too large", bitmap.height <= 256)
            assertEquals("Button corner must be transparent", 0, bitmap.getPixel(0, 0).ushr(24))
        }
    }

    private fun dismissStartScreenIfPresent() {
        val prompt = composeRule.onAllNodesWithText("Tap me to continue")
        if (prompt.fetchSemanticsNodes().isNotEmpty()) {
            prompt[0].performClick()
            composeRule.mainClock.advanceTimeBy(1_000)
        }
    }
}
