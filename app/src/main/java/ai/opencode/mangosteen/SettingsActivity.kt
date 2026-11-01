package ai.opencode.mangosteen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ai.opencode.mangosteen.ui.SettingsScreen
import ai.opencode.mangosteen.ui.SettingsViewModel
import ai.opencode.mangosteen.ui.theme.MangosteenTheme
import ai.opencode.mangosteen.ui.theme.ThemeCatalog
import ai.opencode.mangosteen.ui.theme.ThemeStore
import ai.opencode.mangosteen.ui.theme.applyWindowBackground

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeStore.load(this)
        val theme = ThemeCatalog.byId(ThemeStore.themeId)
        applyWindowBackground(this, theme)
        val dark = theme.isDark
        enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MangosteenTheme {
                SettingsScreen(
                    onBack = { finish() },
                    viewModel = viewModel,
                )
            }
        }
    }
}
