package dev.duefor.cornered.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class RunPhase { Map, Combat, Reward, Rest, Treasure, Victory, Defeat }

/**
 * 一局游戏的全部状态与流程：地图 -> 战斗 -> 奖励 -> 下一层。
 * 纯 Kotlin，界面只负责把 [phase] 画出来。
 */
class RunState(
    val rng: Random,
    val map: GameMap,
) {
    var maxHp: Int = START_HP
        private set
    var hp: Int = START_HP
        private set
    var gold: Int = 0
        private set

    val deck: MutableList<Card> = CardPool.starterDeck.toMutableList()

    var phase: RunPhase = RunPhase.Map
        private set
    var currentNodeId: Int? = null
        private set
    var deepestFloor: Int = 0
        private set
    var reachable: Set<Int> = map.startNodes().map { it.id }.toSet()
        private set

    var combat: CombatEngine? = null
        private set
    var rewardCards: List<Card> = emptyList()
        private set
    var lastGoldGain: Int = 0
        private set

    val currentNode: MapNode? get() = currentNodeId?.let { map.node(it) }

    fun isReachable(id: Int): Boolean = phase == RunPhase.Map && id in reachable

    fun enterNode(id: Int) {
        if (!isReachable(id)) return
        val node = map.node(id)
        currentNodeId = id
        deepestFloor = max(deepestFloor, node.floor + 1)

        when (node.type) {
            NodeType.Combat, NodeType.Elite, NodeType.Boss -> {
                val enemies = EnemyPool.forNode(node.type, node.floor, rng).map { def ->
                    EnemyState(def, rng.nextInt(def.minHp, def.maxHp + 1))
                }
                combat = CombatEngine(hp, maxHp, deck, enemies, rng)
                phase = RunPhase.Combat
            }

            NodeType.Rest -> phase = RunPhase.Rest

            NodeType.Treasure -> {
                lastGoldGain = 25 + rng.nextInt(26)
                gold += lastGoldGain
                phase = RunPhase.Treasure
            }
        }
    }

    /** 战斗画面在引擎进入 Victory/Defeat 后调用，结算血量与掉落。 */
    fun finishCombat() {
        val engine = combat ?: return
        hp = engine.hp
        if (engine.phase == CombatPhase.Defeat) {
            phase = RunPhase.Defeat
            return
        }
        val node = currentNode ?: return
        lastGoldGain = when (node.type) {
            NodeType.Boss -> 120
            NodeType.Elite -> 45
            else -> 15 + rng.nextInt(11)
        }
        gold += lastGoldGain
        rewardCards = CardPool.rewardChoices(rng)
        phase = RunPhase.Reward
    }

    fun takeReward(card: Card?) {
        if (phase != RunPhase.Reward) return
        if (card != null) deck += card
        if (currentNode?.type == NodeType.Boss) {
            phase = RunPhase.Victory
            return
        }
        advanceFromCurrent()
    }

    fun restHeal() {
        if (phase != RunPhase.Rest) return
        hp = min(maxHp, hp + max(10, maxHp * 3 / 10))
        advanceFromCurrent()
    }

    fun restTrain() {
        if (phase != RunPhase.Rest) return
        maxHp += 5
        hp += 5
        advanceFromCurrent()
    }

    fun leaveTreasure() {
        if (phase != RunPhase.Treasure) return
        advanceFromCurrent()
    }

    private fun advanceFromCurrent() {
        val current = currentNodeId
        reachable = if (current == null) {
            map.startNodes().map { it.id }.toSet()
        } else {
            map.nextNodes(current).map { it.id }.toSet()
        }
        phase = RunPhase.Map
        combat = null
        rewardCards = emptyList()
    }

    companion object {
        const val START_HP = 80
    }
}
