package dev.duefor.cornered.game

import dev.duefor.cornered.TestContent
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 集成测试：用贪心 AI 把整局从第一幕打到结算，兜住状态机里的死循环和异常。
 * 现在是 3 幕，一局最多 33 层。
 */
class RunFlowTest {

    @Before
    fun setUp() = TestContent.ensure()

    @Test
    fun `a full run always reaches a terminal phase`() {
        repeat(4) { seed ->
            val run = RunState.newRun(GameContent.characters.first(), seed.toLong())
            var steps = 0

            while (steps < 40_000 && run.phase != RunPhase.Victory && run.phase != RunPhase.Defeat) {
                when (run.phase) {
                    RunPhase.Map -> {
                        val target = run.reachable.firstOrNull() ?: break
                        run.enterNode(target)
                    }

                    RunPhase.Combat -> {
                        val engine = run.combat ?: break
                        playGreedily(engine)
                        if (!engine.isOver) engine.endTurn()
                        if (engine.isOver) run.finishCombat()
                    }

                    RunPhase.Reward -> run.takeReward(run.rewardCards.firstOrNull())
                    RunPhase.ActClear -> run.takeActClearReward(run.rewardCards.firstOrNull())
                    RunPhase.Rest -> run.restHeal()
                    RunPhase.Treasure -> run.leaveTreasure()
                    RunPhase.Victory, RunPhase.Defeat -> Unit
                }
                steps++
            }

            assertTrue(
                "seed=$seed 没有走到结算，停在 ${run.phase}（第 ${run.actIndex + 1} 幕）",
                run.phase == RunPhase.Victory || run.phase == RunPhase.Defeat,
            )
            assertTrue("seed=$seed 步数异常", steps < 40_000)
        }
    }

    private fun playGreedily(engine: CombatEngine) {
        var index = 0
        while (index < engine.hand.size && !engine.isOver) {
            if (engine.canPlay(index) && engine.playCard(index, 0)) {
                // 出牌后手牌左移，下标不动
            } else {
                index++
            }
        }
    }
}
