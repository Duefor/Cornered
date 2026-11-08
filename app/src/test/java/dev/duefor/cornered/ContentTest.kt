package dev.duefor.cornered

import dev.duefor.cornered.game.CardType
import dev.duefor.cornered.game.GameContent
import dev.duefor.cornered.game.NodeType
import dev.duefor.cornered.game.TriggerEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 内容层校验：内容都在 assets/content 下的 JSON 里，这些测试保证「改 json 不会把游戏改坏」。
 * 加卡 / 删卡 / 改效果之后跑一遍这个测试就能发现引用问题。
 */
class ContentTest {

    @Before
    fun setUp() = TestContent.ensure()

    @Test
    fun `cards are well formed`() {
        val cards = GameContent.cards.all
        assertTrue("卡池太小", cards.size >= 25)
        cards.forEach { card ->
            assertTrue("${card.id} 名字为空", card.name.isNotBlank())
            assertTrue("${card.id} 描述为空", card.description.isNotBlank())
            assertTrue("${card.id} 费用不合理", card.cost in 0..4)
            if (card.type == CardType.Power) {
                assertTrue(
                    "${card.id} 是能力牌，但没有 triggers 也没有 keepBlock",
                    card.triggers.isNotEmpty() || card.keepBlock,
                )
                assertTrue("${card.id} 能力牌不应消耗", !card.exhaust)
            } else {
                assertTrue("${card.id} 普通牌不该有 triggers", card.triggers.isEmpty())
            }
            card.triggers.forEach { trigger ->
                assertTrue("${card.id} 的触发器没有效果", trigger.effects.isNotEmpty())
                assertTrue("${card.id} 的 every 必须 >= 1", trigger.every >= 1)
                if (trigger.on != TriggerEvent.CardPlayed) {
                    assertTrue("${card.id} 只有 cardPlayed 能带 cardType", trigger.cardType == null)
                }
            }
        }
    }

    @Test
    fun `every card referenced by a character exists`() {
        GameContent.characters.forEach { character ->
            assertTrue(character.starterDeck.size >= 8)
            character.rewardPool.forEach { card ->
                assertNotNull("角色 ${character.id} 引用了不存在的卡 ${card.id}", GameContent.cards.byId(card.id))
            }
        }
    }

    @Test
    fun `acts reference existing enemies and have sane weights`() {
        assertTrue("至少要有 3 幕", GameContent.acts.size >= 3)
        GameContent.acts.forEach { act ->
            assertTrue(act.floors >= 8)
            assertTrue(act.weights.containsKey(NodeType.Combat))
            (act.encounters + act.elites + act.bosses).forEach { group ->
                group.forEach { id ->
                    assertNotNull("幕 ${act.id} 引用了不存在的敌人 $id", GameContent.enemies.byId(id))
                }
            }
            act.bosses.forEach { group ->
                group.forEach { id ->
                    assertEquals(
                        "幕 ${act.id} 的 boss 种类不对：$id",
                        dev.duefor.cornered.game.EnemyKind.Boss,
                        GameContent.enemies.byId(id)!!.kind,
                    )
                }
            }
        }
    }

    @Test
    fun `relics have text and at least one trigger`() {
        assertTrue("遗物太少", GameContent.relics.size >= 8)
        GameContent.relics.forEach { relic ->
            assertTrue("${relic.id} 没有描述", relic.text.isNotBlank())
            assertTrue("${relic.id} 没有触发效果", relic.triggers.isNotEmpty())
        }
    }

    @Test
    fun `every art has at least one power card available`() {
        GameContent.characters.forEach { character ->
            val powers = character.rewardPool.count { it.type == CardType.Power }
            assertTrue("角色 ${character.id} 的奖励池没有能力牌", powers >= 4)
        }
    }
}
