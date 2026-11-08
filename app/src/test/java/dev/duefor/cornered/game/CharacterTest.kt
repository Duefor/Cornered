package dev.duefor.cornered.game

import dev.duefor.cornered.TestContent
import dev.duefor.cornered.data.RunCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class CharacterTest {

    @Before
    fun setUp() = TestContent.ensure()

    @Test
    fun `new run uses the chosen character stats and deck`() {
        val shadow = GameContent.characters.first { it.id == "shadow" }
        val run = RunState.newRun(shadow, 5)

        assertEquals(shadow.id, run.character.id)
        assertEquals(shadow.maxHp, run.hp)
        assertEquals(shadow.maxHp, run.maxHp)
        assertEquals(shadow.starterDeck.size, run.deck.size)
    }

    @Test
    fun `characters are actually different`() {
        val ironclad = GameContent.characters.first { it.id == "ironclad" }
        val shadow = GameContent.characters.first { it.id == "shadow" }
        assertNotEquals(ironclad.maxHp, shadow.maxHp)
        assertNotEquals(
            ironclad.starterDeck.map { it.id },
            shadow.starterDeck.map { it.id },
        )
    }

    @Test
    fun `every character is consistent and addable`() {
        assertTrue(GameContent.characters.size >= 2)
        GameContent.characters.forEach { character ->
            assertTrue("${character.id} 起始牌组至少 8 张", character.starterDeck.size >= 8)
            assertTrue("${character.id} 奖励池不能为空", character.rewardPool.isNotEmpty())
            assertTrue("${character.id} 手牌数合理", character.handSize in 3..8)
            assertEquals(character.id, GameContent.characterOrDefault(character.id).id)
            character.starterDeck.forEach { card ->
                assertEquals(card.id, GameContent.cards.byId(card.id)?.id)
            }
        }
    }

    @Test
    fun `hand size and energy follow the character`() {
        val shadow = GameContent.characters.first { it.id == "shadow" }
        val enemy = EnemyState(
            EnemyDef("dummy", "木桩", EnemyKind.Normal, 40, 40, listOf(Intent.Attack(5))),
            40,
        )
        val combat = CombatEngine(
            startHp = shadow.maxHp,
            maxHp = shadow.maxHp,
            deck = shadow.starterDeck,
            enemies = listOf(enemy),
            rng = Random(1),
            maxEnergy = shadow.maxEnergy,
            handSize = shadow.handSize,
        )
        assertEquals(shadow.handSize, combat.hand.size)
        assertEquals(shadow.maxEnergy, combat.energy)
    }

    @Test
    fun `save keeps the selected character`() {
        val shadow = GameContent.characters.first { it.id == "shadow" }
        val run = RunState.newRun(shadow, 5)
        val restored = RunCodec.decode(RunCodec.encode(run))!!
        assertEquals("shadow", restored.character.id)
        assertEquals(run.maxHp, restored.maxHp)
        assertEquals(run.deck.map { it.id }, restored.deck.map { it.id })
    }
}
