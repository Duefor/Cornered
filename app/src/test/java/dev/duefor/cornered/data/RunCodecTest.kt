package dev.duefor.cornered.data

import dev.duefor.cornered.TestContent
import dev.duefor.cornered.game.GameContent
import dev.duefor.cornered.game.RunPhase
import dev.duefor.cornered.game.RunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class RunCodecTest {

    @Before
    fun setUp() = TestContent.ensure()

    private fun newRun(seed: Long) = RunState.newRun(GameContent.characters.first(), seed)

    @Test
    fun `round trip keeps run and combat state`() {
        val run = newRun(42)
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
        assertEquals(run.actIndex, restored.actIndex)
        assertEquals(run.character.id, restored.character.id)
        assertEquals(run.deck.map { it.id }, restored.deck.map { it.id })
        assertEquals(run.currentNodeId, restored.currentNodeId)
        assertEquals(run.reachable, restored.reachable)

        val re = restored.combat
        assertNotNull(re)
        re!!
        assertEquals(engine.hp, re.hp)
        assertEquals(engine.turn, re.turn)
        assertEquals(engine.energy, re.energy)
        assertEquals(engine.hand.map { it.id }, re.hand.map { it.id })
        assertEquals(engine.drawPile.map { it.id }, re.drawPile.map { it.id })
        assertEquals(engine.enemies.map { it.hp }, re.enemies.map { it.hp })
    }

    @Test
    fun `relics survive a round trip`() {
        val base = newRun(9)
        val lantern = GameContent.relics.first()
        val run = RunState.restore(
            rng = Random(9),
            mapSeed = base.mapSeed,
            map = base.map,
            character = base.character,
            actIndex = 0,
            hp = base.hp,
            maxHp = base.maxHp,
            gold = 0,
            deck = base.deck.toList(),
            relics = listOf(lantern),
            deepestFloor = 0,
            currentNodeId = null,
            reachable = base.reachable,
            phase = RunPhase.Map,
            combat = null,
            rewardCards = emptyList(),
            pendingRelic = null,
            lastGoldGain = 0,
        )

        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals(listOf(lantern.id), restored.relics.map { it.id })
    }

    @Test
    fun `map is rebuilt identically from act and seed`() {
        val run = newRun(7)
        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals(run.map.nodes.size, restored.map.nodes.size)
        run.map.nodes.forEachIndexed { index, node ->
            assertEquals(node.type, restored.map.nodes[index].type)
            assertEquals(node.next, restored.map.nodes[index].next)
        }
    }

    @Test
    fun `reward screen state survives a round trip`() {
        val run = newRun(11)
        run.enterNode(run.reachable.first())
        val engine = run.combat!!
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
        assertTrue(run.phase == RunPhase.Reward || run.phase == RunPhase.Defeat)

        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals(run.phase, restored.phase)
        assertEquals(run.rewardCards.map { it.id }, restored.rewardCards.map { it.id })
        assertEquals(run.pendingRelic?.id, restored.pendingRelic?.id)
    }

    @Test
    fun `broken or old saves decode to null`() {
        assertNull(RunCodec.decode(""))
        assertNull(RunCodec.decode("{not json"))
        assertNull(RunCodec.decode("""{"version":1,"mapSeed":1}"""))
    }
}
