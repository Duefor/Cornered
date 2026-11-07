package dev.duefor.cornered.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 集成测试：用一个很笨的 AI（能出的牌就出、然后结束回合）把整局从地图打到结算，
 * 用来兜住流程状态机里的死循环和异常。
 */
class RunFlowTest {

    @Test
    fun `a full run always reaches a terminal phase`() {
        repeat(5) { seed ->
            val run = RunState.newRun(seed = seed.toLong())
            var steps = 0

            while (steps < 20_000 && run.phase != RunPhase.Victory && run.phase != RunPhase.Defeat) {
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
                    RunPhase.Rest -> run.restHeal()
                    RunPhase.Treasure -> run.leaveTreasure()
                    RunPhase.Victory, RunPhase.Defeat -> Unit
                }
                steps++
            }

            assertTrue(
                "seed=$seed 没有走到结算，停在 ${run.phase}（${run.deepestFloor} 层）",
                run.phase == RunPhase.Victory || run.phase == RunPhase.Defeat,
            )
            assertTrue("seed=$seed 步数异常", steps < 20_000)
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
