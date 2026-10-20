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

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
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
