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

class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ID = "conversation_id"
    }

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
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
