package dev.duefor.cornered

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.duefor.cornered.data.ScoreStore
import dev.duefor.cornered.ui.GameScreen
import dev.duefor.cornered.ui.MenuScreen
import dev.duefor.cornered.ui.theme.ArenaBackground
import dev.duefor.cornered.ui.theme.ArenaSurface
import dev.duefor.cornered.ui.theme.CorneredTheme
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.update.UpdateController
import dev.duefor.cornered.update.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CorneredTheme {
                CorneredApp()
            }
        }
    }
}

@Composable
private fun CorneredApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scoreStore = remember { ScoreStore(context.applicationContext) }
    val updater = remember { UpdateController(context.applicationContext) }

    var best by remember { mutableIntStateOf(scoreStore.best) }
    var playing by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }

    // 启动时静默检查一次；有新版才弹窗，绝不打扰正常玩。
    LaunchedEffect(Unit) {
        updater.check(silent = true)
        if (updater.release != null) showUpdate = true
    }

    // 提示条 3.5 秒后自动消失
    LaunchedEffect(updater.message) {
        val current = updater.message ?: return@LaunchedEffect
        delay(3500)
        if (updater.message == current) updater.message = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBackground),
    ) {
        if (playing) {
            GameScreen(
                best = best,
                onExit = { playing = false },
                onGameOver = { seconds ->
                    if (seconds > best) {
                        best = seconds
                        scoreStore.best = seconds
                    }
                },
            )
        } else {
            MenuScreen(
                best = best,
                version = BuildConfig.VERSION_NAME,
                checking = updater.checking,
                onPlay = { playing = true },
                onCheckUpdate = {
                    scope.launch {
                        updater.check(silent = false)
                        if (updater.release != null) showUpdate = true
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = updater.message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Text(
                text = updater.message.orEmpty(),
                color = Ink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(ArenaSurface, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }

    if (showUpdate) {
        UpdateDialog(
            release = updater.release,
            downloading = updater.downloading,
            progress = updater.progress,
            onDismiss = {
                updater.dismiss()
                showUpdate = false
            },
            onConfirm = { scope.launch { updater.downloadAndInstall() } },
        )
    }
}
