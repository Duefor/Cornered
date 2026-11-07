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
 * 生成一张尖塔式的分叉地图：
 *  - 每层 2-4 个节点，节点之间连 1-2 条边
 *  - 保证每一层的每个节点都有来路，从起点一定能走到首领
 *  - 中段固定一排营地、首领前固定一排精英，其余按权重随机
 */
object MapGenerator {

    const val FLOORS = 11

    fun generate(rng: Random): GameMap {
        val nodes = mutableListOf<MapNode>()
        val grid = mutableListOf<MutableList<MapNode>>()
        var nextId = 0

        for (floor in 0 until FLOORS) {
            val count = when (floor) {
                0 -> 3
                FLOORS - 1 -> 1
                else -> 2 + rng.nextInt(3)
            }
            val row = mutableListOf<MapNode>()
            repeat(count) { column ->
                val node = MapNode(nextId++, floor, column, typeFor(floor, rng))
                row += node
                nodes += node
            }
            grid += row
        }

        for (floor in 0 until FLOORS - 1) {
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

            // 兜底：任何没有来路的节点，从最接近的父节点补一条边
            for (target in next) {
                if (target.prev.isEmpty()) {
                    val parent = current.minByOrNull { abs(it.column - target.column) } ?: current.first()
                    connect(parent, target)
                }
            }
        }

        return GameMap(nodes, FLOORS)
    }

    private fun connect(parent: MapNode, child: MapNode) {
        if (parent.next.contains(child.id)) return
        parent.next += child.id
        child.prev += parent.id
    }

    private fun typeFor(floor: Int, rng: Random): NodeType = when {
        floor == 0 -> NodeType.Combat
        floor == FLOORS - 1 -> NodeType.Boss
        floor == FLOORS / 2 -> NodeType.Rest
        floor == FLOORS - 2 -> NodeType.Elite
        floor < 3 -> if (rng.nextInt(100) < 78) NodeType.Combat else NodeType.Treasure
        else -> {
            val roll = rng.nextInt(100)
            when {
                roll < 55 -> NodeType.Combat
                roll < 70 -> NodeType.Treasure
                roll < 85 -> NodeType.Elite
                else -> NodeType.Rest
            }
        }
    }
}
