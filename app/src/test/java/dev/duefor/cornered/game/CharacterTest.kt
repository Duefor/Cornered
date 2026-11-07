package dev.duefor.cornered.game

import dev.duefor.cornered.data.RunCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CharacterTest {

    @Test
    fun `new run uses the chosen character stats and deck`() {
        val shadow = Characters.byId("shadow")!!
        val run = RunState.newRun(shadow, 5)

        assertEquals(shadow.id, run.character.id)
        assertEquals(68, run.hp)
        assertEquals(68, run.maxHp)
        assertEquals(shadow.starterDeck.size, run.deck.size)
        assertEquals(10, run.deck.size)
    }

    @Test
    fun `characters are actually different`() {
        val ironclad = Characters.ironclad
        val shadow = Characters.shadow
        assertNotEquals(ironclad.id, shadow.id)
        assertNotEquals(ironclad.maxHp, shadow.maxHp)
        assertNotEquals(
            ironclad.starterDeck.map { it.id },
            shadow.starterDeck.map { it.id },
        )
    }

    @Test
    fun `every character is consistent and addable`() {
        assertTrue(Characters.all.size >= 2)
        Characters.all.forEach { character ->
            assertTrue("${character.id} 起始牌组至少 8 张", character.starterDeck.size >= 8)
            assertTrue("${character.id} 奖励池不能为空", character.rewardPool.isNotEmpty())
            assertTrue("${character.id} 手牌数合理", character.handSize in 3..8)
            assertEquals(character.id, Characters.byId(character.id)?.id)
            // 卡牌 id 必须能反查，否则存档会坏
            character.starterDeck.forEach { card ->
                assertEquals(card.id, CardPool.byId(card.id)?.id)
            }
        }
    }

    @Test
    fun `hand size and energy follow the character`() {
        val enemy = EnemyState(
            EnemyDef("dummy", "木桩", EnemyKind.Normal, 40, 40, listOf(Intent.Attack(5))),
            40,
        )
        val shadowCombat = CombatEngine(
            startHp = 68,
            maxHp = 68,
            deck = Characters.shadow.starterDeck,
            enemies = listOf(enemy),
            rng = Random(1),
            maxEnergy = Characters.shadow.maxEnergy,
            handSize = Characters.shadow.handSize,
        )
        assertEquals(6, shadowCombat.hand.size)
        assertEquals(3, shadowCombat.energy)
    }

    @Test
    fun `save keeps the selected character`() {
        val run = RunState.newRun(Characters.shadow, 5)
        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals("shadow", restored.character.id)
        assertEquals(run.maxHp, restored.maxHp)
        assertEquals(run.deck.map { it.id }, restored.deck.map { it.id })
    }
}
