package dev.duefor.cornered.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState

/**
 * 一局游戏的宿主：按 [RunPhase] 切换地图/战斗/奖励/营地/结算。
 * 每次状态变化后回调 [onPersist]，退出时再存一次，保证「继续游戏」能从任意一步接上。
 */
@Composable
fun RunScreen(
    run: RunState,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onRunEnd: (Boolean, Int) -> Unit,
    onPersist: (RunState) -> Unit,
) {
    var revision by remember(run) { mutableIntStateOf(0) }
    val refresh: () -> Unit = { revision++ }

    val finished = run.phase == RunPhase.Victory || run.phase == RunPhase.Defeat

    LaunchedEffect(run, revision) {
        if (finished) {
            onRunEnd(run.phase == RunPhase.Victory, run.deepestFloor)
        } else {
            onPersist(run)
        }
    }

    DisposableEffect(run) {
        onDispose { if (!finished) onPersist(run) }
    }

    when (run.phase) {
        RunPhase.Map -> MapScreen(
            run = run,
            revision = revision,
            onEnter = { id ->
                run.enterNode(id)
                refresh()
            },
            onExit = onExit,
        )

        RunPhase.Combat -> {
            val engine = run.combat
            if (engine != null) {
                CombatScreen(
                    engine = engine,
                    revision = revision,
                    onFinished = {
                        run.finishCombat()
                        refresh()
                    },
                    onExit = onExit,
                )
            }
        }

        RunPhase.Reward -> RewardScreen(
            run = run,
            revision = revision,
            onTake = { card ->
                run.takeReward(card)
                refresh()
            },
        )

        RunPhase.Rest -> RestScreen(
            run = run,
            onRest = {
                run.restHeal()
                refresh()
            },
            onTrain = {
                run.restTrain()
                refresh()
            },
        )

        RunPhase.Treasure -> TreasureScreen(
            run = run,
            onContinue = {
                run.leaveTreasure()
                refresh()
            },
        )

        RunPhase.ActClear -> ActClearScreen(
            run = run,
            onTake = { card ->
                run.takeActClearReward(card)
                refresh()
            },
        )

        RunPhase.Victory, RunPhase.Defeat -> EndScreen(
            victory = run.phase == RunPhase.Victory,
            run = run,
            onRestart = onRestart,
            onExit = onExit,
        )
    }
}
