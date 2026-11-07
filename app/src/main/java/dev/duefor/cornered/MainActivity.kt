package dev.duefor.cornered

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import dev.duefor.cornered.data.RunRecords
import dev.duefor.cornered.data.RunRepository
import dev.duefor.cornered.game.Character
import dev.duefor.cornered.game.Characters
import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState
import dev.duefor.cornered.ui.CharacterScreen
import dev.duefor.cornered.ui.MenuScreen
import dev.duefor.cornered.ui.RunScreen
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
        // 系统栏图标用浅色，背景交给内容自己画，避免出现一条浅色导航栏
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            CorneredTheme {
                CorneredApp()
            }
        }
    }
}

private enum class Screen { Menu, Character, Run }

@Composable
private fun CorneredApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val records = remember { RunRecords(context.applicationContext) }
    val repository = remember { RunRepository(context.applicationContext) }
    val updater = remember { UpdateController(context.applicationContext) }

    var screen by remember { mutableStateOf(Screen.Menu) }
    var run by remember { mutableStateOf<RunState?>(null) }
    var hasSave by remember { mutableStateOf(repository.hasSave()) }
    var bestFloor by remember { mutableIntStateOf(records.bestFloor) }
    var victories by remember { mutableIntStateOf(records.victories) }
    var showUpdate by remember { mutableStateOf(false) }

    // 启动时静默检查一次；有新版才弹窗
    LaunchedEffect(Unit) {
        updater.check(silent = true)
        if (updater.release != null) showUpdate = true
    }

    LaunchedEffect(updater.message) {
        val current = updater.message ?: return@LaunchedEffect
        delay(3500)
        if (updater.message == current) updater.message = null
    }

    fun startNewRun(character: Character) {
        repository.clear()
        hasSave = false
        run = RunState.newRun(character)
        screen = Screen.Run
    }

    fun continueRun() {
        val loaded = repository.load()
        if (loaded != null) {
            run = loaded
            screen = Screen.Run
        } else {
            hasSave = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBackground),
    ) {
        when (screen) {
            Screen.Menu -> MenuScreen(
                bestFloor = bestFloor,
                victories = victories,
                hasSave = hasSave,
                version = BuildConfig.VERSION_NAME,
                checking = updater.checking,
                onContinue = { continueRun() },
                onPlay = { screen = Screen.Character },
                onCheckUpdate = {
                    scope.launch {
                        updater.check(silent = false)
                        if (updater.release != null) showUpdate = true
                    }
                },
            )

            Screen.Character -> CharacterScreen(
                characters = Characters.all,
                onStart = { character -> startNewRun(character) },
                onBack = { screen = Screen.Menu },
            )

            Screen.Run -> {
                val current = run
                if (current != null) {
                    RunScreen(
                        run = current,
                        onExit = {
                            screen = Screen.Menu
                            hasSave = repository.hasSave()
                        },
                        onRestart = { startNewRun(current.character) },
                        onRunEnd = { victory, floor ->
                            records.record(floor, victory)
                            if (floor > bestFloor) bestFloor = floor
                            victories = records.victories
                            repository.clear()
                            hasSave = false
                        },
                        onPersist = { state ->
                            if (state.phase != RunPhase.Victory && state.phase != RunPhase.Defeat) {
                                repository.save(state)
                                hasSave = true
                            }
                        },
                    )
                }
            }
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
