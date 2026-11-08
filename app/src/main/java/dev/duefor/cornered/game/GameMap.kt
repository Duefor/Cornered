package dev.duefor.cornered.game

import kotlin.math.abs
import kotlin.random.Random

enum class NodeType(val label: String, val short: String) {
    Combat("战斗", "战"),
    Elite("精英", "精"),
    Rest("营地", "营"),
    Treasure("宝箱", "宝"),
    Boss("首领", "王"),
}

class MapNode(
    val id: Int,
    val floor: Int,
    val column: Int,
    val type: NodeType,
) {
    val next = mutableListOf<Int>()
    val prev = mutableListOf<Int>()
}

class GameMap(
    val nodes: List<MapNode>,
    val floorCount: Int,
) {
    fun node(id: Int): MapNode = nodes[id]

    fun startNodes(): List<MapNode> = nodes.filter { it.floor == 0 }

    fun nextNodes(id: Int): List<MapNode> = nodes[id].next.map { nodes[it] }

    fun row(floor: Int): List<MapNode> = nodes.filter { it.floor == floor }
}

/**
 * 按幕配置生成一张尖塔式的分叉地图：
 *  - 每层 2-4 个节点，节点之间连 1-2 条边
 *  - 保证每层的每个节点都有来路，从起点一定能走到首领
 *  - 中段固定一排营地、首领前固定一排精英，其余按 [Act.weights] 随机
 */
object MapGenerator {

    fun generate(act: Act, rng: Random): GameMap {
        val floors = act.floors
        val nodes = mutableListOf<MapNode>()
        val grid = mutableListOf<MutableList<MapNode>>()
        var nextId = 0

        for (floor in 0 until floors) {
            val count = when (floor) {
                0 -> 3
                floors - 1 -> 1
                else -> 2 + rng.nextInt(3)
            }
            val row = mutableListOf<MapNode>()
            repeat(count) { column ->
                val node = MapNode(nextId++, floor, column, typeFor(act, floor, rng))
                row += node
                nodes += node
            }
            grid += row
        }

        for (floor in 0 until floors - 1) {
            val current = grid[floor]
            val next = grid[floor + 1]

            for (node in current) {
                val scaled = if (current.size <= 1) 0
                else node.column * (next.size - 1) / (current.size - 1)
                val candidates = listOf(scaled, scaled - 1, scaled + 1)
                    .filter { it in next.indices }
                    .distinct()
                val linkCount = if (candidates.size <= 1) 1 else 1 + rng.nextInt(2)
                candidates.shuffled(rng)
                    .take(linkCount.coerceAtMost(candidates.size))
                    .forEach { index -> connect(node, next[index]) }
            }

            for (target in next) {
                if (target.prev.isEmpty()) {
                    val parent = current.minByOrNull { abs(it.column - target.column) } ?: current.first()
                    connect(parent, target)
                }
            }
        }

        return GameMap(nodes, floors)
    }

    private fun connect(parent: MapNode, child: MapNode) {
        if (parent.next.contains(child.id)) return
        parent.next += child.id
        child.prev += parent.id
    }

    private fun typeFor(act: Act, floor: Int, rng: Random): NodeType {
        if (floor == 0) return NodeType.Combat
        if (floor == act.floors - 1) return NodeType.Boss
        if (floor == act.floors / 2) return NodeType.Rest
        if (floor == act.floors - 2) return NodeType.Elite

        val choices = act.weights.filterKeys { it != NodeType.Boss }
        val total = choices.values.sum()
        if (total <= 0) return NodeType.Combat

        var roll = rng.nextInt(total)
        for ((type, weight) in choices) {
            roll -= weight
            if (roll < 0) return type
        }
        return NodeType.Combat
    }
}
