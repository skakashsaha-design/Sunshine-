package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity launches SkyPulseRoot and HomeScreen with ActivityResultRegistryOwner`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("home_location_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("home_gps_button").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
    }
}
