package ai.opencode.mangosteen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ai.opencode.mangosteen.ui.ChatScreen
import ai.opencode.mangosteen.ui.ChatViewModel
import ai.opencode.mangosteen.ui.theme.MangosteenTheme
import ai.opencode.mangosteen.ui.theme.ThemeCatalog
import ai.opencode.mangosteen.ui.theme.ThemeStore
import ai.opencode.mangosteen.ui.theme.applyWindowBackground

class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ID = "conversation_id"
    }

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeStore.load(this)
        val theme = ThemeCatalog.byId(ThemeStore.themeId)
        applyWindowBackground(this, theme)
        super.onCreate(savedInstanceState)
        val dark = theme.isDark
        enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0L) {
            finish()
            return
        }

        setContent {
            MangosteenTheme {
                ChatScreen(
                    conversationId = id,
                    onBack = { finish() },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    viewModel = viewModel,
                )
            }
        }
    }
}
