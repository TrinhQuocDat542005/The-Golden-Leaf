package com.example.giaodien

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.giaodien.ui.screens.UpcomingEventsSection

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.giaodien", appContext.packageName)
        // Exercise the actual event cards too: eager decoding of game.jpg used to
        // allocate a density-scaled large bitmap even though it is only a thumbnail.
        compose.setContent { MaterialTheme { UpcomingEventsSection() } }
        compose.onNodeWithContentDescription("Game Night Image").assertIsDisplayed()
        compose.onNodeWithContentDescription("Live Music Show Image").assertIsDisplayed()
    }
}
