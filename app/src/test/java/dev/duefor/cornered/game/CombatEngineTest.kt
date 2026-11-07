package dev.duefor.cornered.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CombatEngineTest {

    private fun enemy(
        hp: Int = 40,
        pattern: List<Intent> = listOf(Intent.Attack(7)),
    ): EnemyState = EnemyState(
        EnemyDef(
            id = "dummy",
            name = "木桩",
            kind = EnemyKind.Normal,
            minHp = hp,
            maxHp = hp,
            pattern = pattern,
        ),
        hp,
    )

    private fun engine(
        deck: List<Card>,
        enemies: List<EnemyState>,
        hp: Int = 80,
        seed: Int = 1,
    ): CombatEngine = CombatEngine(hp, hp, deck, enemies, Random(seed))

    @Test
    fun `strike deals six damage`() {
        val dummy = enemy()
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        assertTrue(combat.playCard(0, 0))
        assertEquals(34, dummy.hp)
    }

    @Test
    fun `block absorbs enemy attack then leftover hits hp`() {
        val dummy = enemy(pattern = listOf(Intent.Attack(7)))
        val combat = engine(List(5) { CardPool.defend }, listOf(dummy))
        combat.playCard(0, 0)
        assertEquals(5, combat.block)

        combat.endTurn()
        // 7 点攻击 - 5 点格挡 = 2 点生命
        assertEquals(78, combat.hp)
    }

    @Test
    fun `vulnerable increases damage by half`() {
        val dummy = enemy()
        dummy.statuses[Status.Vulnerable] = 2
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        combat.playCard(0, 0)
        // 6 * 1.5 = 9
        assertEquals(31, dummy.hp)
    }

    @Test
    fun `weak on the attacker reduces its damage`() {
        val dummy = enemy()
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        // 虚弱挂在攻击方（玩家）身上才降伤害
        combat.statuses[Status.Weak] = 1
        combat.playCard(0, 0)
        // 6 * 0.75 = 4.5 -> 4
        assertEquals(36, dummy.hp)
    }

    @Test
    fun `weak on a weak enemy softens its attack`() {
        val dummy = enemy(pattern = listOf(Intent.Attack(8)))
        dummy.statuses[Status.Weak] = 1
        val combat = engine(List(5) { CardPool.defend }, listOf(dummy))
        combat.endTurn()
        // 8 * 0.75 = 6 点伤害，没有格挡
        assertEquals(74, combat.hp)
    }

    @Test
    fun `strength adds flat damage`() {
        val dummy = enemy()
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        combat.statuses[Status.Strength] = 3
        combat.playCard(0, 0)
        assertEquals(31, dummy.hp)
    }

    @Test
    fun `energy limits how many cards can be played`() {
        val dummy = enemy(hp = 99)
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        assertTrue(combat.playCard(0, 0))
        assertTrue(combat.playCard(0, 0))
        assertTrue(combat.playCard(0, 0))
        assertFalse(combat.playCard(0, 0))
        assertEquals(0, combat.energy)
    }

    @Test
    fun `hand refills to five after ending turn and reshuffles discard`() {
        val dummy = enemy(hp = 999)
        val combat = engine(List(10) { CardPool.defend }, listOf(dummy))
        combat.playCard(0, 0)
        combat.endTurn()
        assertEquals(5, combat.hand.size)
        assertEquals(2, combat.turn)
    }

    @Test
    fun `killing the only enemy wins the combat`() {
        val dummy = enemy(hp = 6)
        val combat = engine(List(5) { CardPool.strike }, listOf(dummy))
        combat.playCard(0, 0)
        assertEquals(CombatPhase.Victory, combat.phase)
        assertTrue(combat.isOver)
    }

    @Test
    fun `dying to an enemy ends in defeat`() {
        val dummy = enemy(pattern = listOf(Intent.Attack(50)))
        val combat = engine(List(5) { CardPool.defend }, listOf(dummy), hp = 20)
        combat.endTurn()
        assertEquals(0, combat.hp)
        assertEquals(CombatPhase.Defeat, combat.phase)
    }

    @Test
    fun `exhaust cards leave the deck cycle`() {
        val dummy = enemy(hp = 999)
        val combat = engine(List(5) { CardPool.battleTrance }, listOf(dummy))
        combat.playCard(0, 0)
        assertEquals(1, combat.exhaustPile.size)
        assertEquals(0, combat.discardPile.size)
    }
}
