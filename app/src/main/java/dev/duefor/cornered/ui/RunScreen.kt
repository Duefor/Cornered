package dev.duefor.cornered.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.duefor.cornered.game.MapGenerator
import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState
import kotlin.random.Random

/**
 * 一局游戏的宿主：按 [RunPhase] 切换地图/战斗/奖励/营地/结算。
 * [seed] 变化即视为新的一局。
 */
@Composable
fun RunScreen(
    seed: Long,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    onRunEnd: (Boolean, Int) -> Unit,
) {
    val run = remember(seed) {
        RunState(
            rng = Random(seed),
            map = MapGenerator.generate(Random(seed * 31 + 7)),
        )
    }
    var revision by remember(seed) { mutableIntStateOf(0) }
    val refresh: () -> Unit = { revision++ }

    LaunchedEffect(seed, revision) {
        if (run.phase == RunPhase.Victory || run.phase == RunPhase.Defeat) {
            onRunEnd(run.phase == RunPhase.Victory, run.deepestFloor)
        }
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

        RunPhase.Victory, RunPhase.Defeat -> EndScreen(
            victory = run.phase == RunPhase.Victory,
            run = run,
            onRestart = onRestart,
            onExit = onExit,
        )
    }
}
