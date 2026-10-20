package ai.opencode.mangosteen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import ai.opencode.mangosteen.proactive.ProactiveNotifications
import ai.opencode.mangosteen.ui.ChatsScreen
import ai.opencode.mangosteen.ui.ChatsViewModel
import ai.opencode.mangosteen.ui.theme.MangosteenTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatsViewModel by viewModels()
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            MangosteenTheme {
                ChatsScreen(
                    onOpen = ::openChat,
                    onNewChat = {
                        if (viewModel.hasApiKey()) viewModel.createChat(::openChat)
                        else openSettings()
                    },
                    onOpenSettings = ::openSettings,
                    viewModel = viewModel,
                )
            }
        }

        ProactiveNotifications.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        viewModel.rescheduleReminders()
    }

    private fun openChat(id: Long) {
        startActivity(Intent(this, ChatActivity::class.java).putExtra(ChatActivity.EXTRA_ID, id))
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
