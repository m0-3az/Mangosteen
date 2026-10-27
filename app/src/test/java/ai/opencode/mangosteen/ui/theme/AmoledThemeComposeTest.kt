package ai.opencode.mangosteen.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AmoledThemeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `amoled background is pure black and text has sufficient contrast`() {
        val theme = ThemeCatalog.byId("amoled")
        val scheme = theme.scheme()

        assertEquals(Color(0xFF000000), scheme.background)
        assertTrue(
            "onBackground contrast is too low",
            contrast(scheme.onBackground, scheme.background) >= 4.5f,
        )
        assertTrue(
            "onSurface contrast is too low",
            contrast(scheme.onSurface, theme.surfaceLow) >= 4.5f,
        )

        composeRule.setContent {
            MaterialTheme(colorScheme = scheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag("amoled-bg"),
                ) {
                    Text(
                        text = "AMOLED",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag("amoled-text"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("amoled-text").assertIsDisplayed()
        composeRule.onNodeWithTag("amoled-bg").assertIsDisplayed()
        composeRule.waitForIdle()
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
    }
}
