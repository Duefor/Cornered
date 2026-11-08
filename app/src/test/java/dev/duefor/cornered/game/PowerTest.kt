package dev.duefor.cornered.game

import dev.duefor.cornered.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class PowerTest {

    @Before
    fun setUp() = TestContent.ensure()

    private fun card(id: String) = TestContent.card(id)

    private fun dummy(
        hp: Int = 200,
        pattern: List<Intent> = listOf(Intent.Defend(0)),
    ): EnemyState = EnemyState(EnemyDef("dummy", "木桩", EnemyKind.Normal, hp, hp, pattern), hp)

    @Test
    fun `power card stays active and does not go to discard`() {
        val enemy = dummy()
        val combat = CombatEngine(
            80, 80, List(5) { card("metallicize") }, listOf(enemy), Random(1),
        )
        assertTrue(combat.playCard(0, 0))

        assertEquals(1, combat.activePowers.size)
        assertEquals("能力牌不该进弃牌堆", 0, combat.discardPile.size)
        assertEquals(2, combat.energy)
    }

    @Test
    fun `metallicize grants block at end of turn`() {
        val enemy = dummy(pattern = listOf(Intent.Attack(5)))
        val combat = CombatEngine(80, 80, List(5) { card("metallicize") }, listOf(enemy), Random(1))
        combat.playCard(0, 0)

        combat.endTurn()
        // 3 点格挡抵消 5 点攻击中的 3 点
        assertEquals(78, combat.hp)
    }

    @Test
    fun `demon form adds strength every turn`() {
        val enemy = dummy()
        val combat = CombatEngine(
            80, 80, List(5) { card("demon_form") }, listOf(enemy), Random(1), maxEnergy = 5,
        )
        combat.playCard(0, 0)
        assertEquals(0, combat.statusOf(Status.Strength))

        combat.endTurn()
        // 第 2 回合开始触发 +2 力量
        assertEquals(2, combat.statusOf(Status.Strength))

        combat.endTurn()
        assertEquals(4, combat.statusOf(Status.Strength))
    }

    @Test
    fun `barricade keeps block between turns`() {
        val enemy = dummy()
        val deck = listOf(card("barricade")) + List(4) { card("defend") }
        val combat = CombatEngine(80, 80, deck, listOf(enemy), Random(3), maxEnergy = 6)

        val defendIndex = combat.hand.indexOfFirst { it.id == "defend" }
        assertTrue(combat.playCard(defendIndex, 0))
        assertEquals(5, combat.block)
        // 出牌后手牌左移，重新找能力牌的下标
        val barricadeIndex = combat.hand.indexOfFirst { it.id == "barricade" }
        assertTrue(combat.playCard(barricadeIndex, 0))

        combat.endTurn()
        assertEquals("有了屏障，格挡应该保留", 5, combat.block)
    }

    @Test
    fun `combo master draws a card every three attacks`() {
        val enemy = dummy()
        val deck = listOf(card("combo_master")) + List(4) { card("strike") }
        val combat = CombatEngine(80, 80, deck, listOf(enemy), Random(5), maxEnergy = 6)

        val comboIndex = combat.hand.indexOfFirst { it.id == "combo_master" }
        combat.playCard(comboIndex, 0)

        // 打两张攻击牌：还不该抽
        combat.playCard(combat.hand.indexOfFirst { it.id == "strike" }, 0)
        val afterTwo = combat.hand.size
        combat.playCard(combat.hand.indexOfFirst { it.id == "strike" }, 0)

        // 第三张打完之后抽 1 张（弃牌堆洗回）
        combat.playCard(combat.hand.indexOfFirst { it.id == "strike" }, 0)
        assertEquals(afterTwo - 2 + 1, combat.hand.size)
    }

    @Test
    fun `relic triggers at combat start`() {
        val enemy = dummy()
        val lantern = Relic(
            id = "lantern",
            name = "提灯",
            text = "战斗开始时获得 5 点格挡",
            triggers = listOf(
                TriggeredEffect(TriggerEvent.CombatStart, listOf(Effect.Block(5))),
            ),
        )
        val combat = CombatEngine(
            80, 80, List(5) { card("strike") }, listOf(enemy), Random(1), relics = listOf(lantern),
        )
        assertEquals(5, combat.block)
        assertEquals(1, combat.relics.size)
    }

    @Test
    fun `thorns damages all enemies when player is hit`() {
        val enemy = dummy(pattern = listOf(Intent.Attack(6)))
        val deck = listOf(card("thorns")) + List(4) { card("defend") }
        val combat = CombatEngine(80, 80, deck, listOf(enemy), Random(7), maxEnergy = 5)

        val thornsIndex = combat.hand.indexOfFirst { it.id == "thorns" }
        combat.playCard(thornsIndex, 0)
        combat.endTurn()

        assertEquals("荆棘应该反伤 2 点", 198, enemy.hp)
    }

    @Test
    fun `enemy killed trigger fires`() {
        var healed = 0
        val ring = Relic(
            id = "ring",
            name = "荆棘之环",
            text = "击败敌人时回复 3 点生命",
            triggers = listOf(
                TriggeredEffect(TriggerEvent.EnemyKilled, listOf(Effect.Heal(3))),
            ),
        )
        val enemy = dummy(hp = 6)
        val combat = CombatEngine(
            60, 80, List(5) { card("strike") }, listOf(enemy), Random(1), relics = listOf(ring),
        )
        combat.playCard(0, 0)
        healed = combat.hp
        assertEquals(63, healed)
    }
}
