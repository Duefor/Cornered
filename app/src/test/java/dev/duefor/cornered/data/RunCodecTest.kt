package dev.duefor.cornered.data

import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RunCodecTest {

    @Test
    fun `round trip keeps run and combat state`() {
        val run = RunState.newRun(42)
        run.enterNode(run.reachable.first())

        val engine = run.combat
        assertNotNull("第一个节点应该是战斗", engine)
        engine!!

        engine.playCard(0, 0)
        engine.endTurn()

        val restored = RunCodec.decode(RunCodec.encode(run))
        assertNotNull("存档应该能读回来", restored)
        restored!!

        assertEquals(run.phase, restored.phase)
        assertEquals(run.hp, restored.hp)
        assertEquals(run.maxHp, restored.maxHp)
        assertEquals(run.gold, restored.gold)
        assertEquals(run.deck.map { it.id }, restored.deck.map { it.id })
        assertEquals(run.deepestFloor, restored.deepestFloor)
        assertEquals(run.currentNodeId, restored.currentNodeId)
        assertEquals(run.reachable, restored.reachable)

        val re = restored.combat
        assertNotNull(re)
        re!!
        assertEquals(engine.hp, re.hp)
        assertEquals(engine.turn, re.turn)
        assertEquals(engine.energy, re.energy)
        assertEquals(engine.block, re.block)
        assertEquals(engine.hand.map { it.id }, re.hand.map { it.id })
        assertEquals(engine.drawPile.map { it.id }, re.drawPile.map { it.id })
        assertEquals(engine.discardPile.map { it.id }, re.discardPile.map { it.id })
        assertEquals(engine.enemies.map { it.def.id }, re.enemies.map { it.def.id })
        assertEquals(engine.enemies.map { it.hp }, re.enemies.map { it.hp })
        assertEquals(engine.enemies.map { it.moveIndex }, re.enemies.map { it.moveIndex })
    }

    @Test
    fun `map is rebuilt identically from the saved seed`() {
        val run = RunState.newRun(7)
        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals(run.map.nodes.size, restored.map.nodes.size)
        run.map.nodes.forEachIndexed { index, node ->
            assertEquals(node.type, restored.map.nodes[index].type)
            assertEquals(node.next, restored.map.nodes[index].next)
        }
    }

    @Test
    fun `reward screen state survives a round trip`() {
        val run = RunState.newRun(11)
        run.enterNode(run.reachable.first())
        val engine = run.combat!!
        // 直接打死敌人，进入奖励阶段
        while (!engine.isOver) {
            var played = false
            for (index in engine.hand.indices) {
                if (engine.playCard(index, 0)) {
                    played = true
                    break
                }
            }
            if (!played) engine.endTurn()
        }
        run.finishCombat()
        assertEquals(RunPhase.Reward, run.phase)

        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals(RunPhase.Reward, restored.phase)
        assertEquals(run.rewardCards.map { it.id }, restored.rewardCards.map { it.id })
        assertEquals(run.lastGoldGain, restored.lastGoldGain)
    }

    @Test
    fun `broken saves decode to null instead of throwing`() {
        assertNull(RunCodec.decode(""))
        assertNull(RunCodec.decode("{not json"))
        assertNull(RunCodec.decode("""{"version":1,"mapSeed":1}"""))
    }
}
