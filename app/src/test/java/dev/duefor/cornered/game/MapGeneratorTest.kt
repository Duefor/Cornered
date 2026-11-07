package dev.duefor.cornered.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MapGeneratorTest {

    @Test
    fun `every node is reachable from the first floor`() {
        repeat(20) { seed ->
            val map = MapGenerator.generate(Random(seed))
            val visited = mutableSetOf<Int>()
            val queue = ArrayDeque(map.startNodes().map { it.id })

            while (queue.isNotEmpty()) {
                val id = queue.removeFirst()
                if (!visited.add(id)) continue
                map.node(id).next.forEach { queue.addLast(it) }
            }

            assertEquals("seed=$seed", map.nodes.size, visited.size)
        }
    }

    @Test
    fun `non starting nodes always have an incoming edge`() {
        repeat(20) { seed ->
            val map = MapGenerator.generate(Random(seed))
            map.nodes.filter { it.floor > 0 }.forEach { node ->
                assertTrue("seed=$seed node=${node.id}", node.prev.isNotEmpty())
            }
        }
    }

    @Test
    fun `single boss sits on the last floor`() {
        repeat(20) { seed ->
            val map = MapGenerator.generate(Random(seed))
            val last = map.row(MapGenerator.FLOORS - 1)
            assertEquals(1, last.size)
            assertEquals(NodeType.Boss, last.single().type)
        }
    }

    @Test
    fun `same seed produces the same map`() {
        val a = MapGenerator.generate(Random(7))
        val b = MapGenerator.generate(Random(7))
        assertEquals(a.nodes.size, b.nodes.size)
        a.nodes.forEachIndexed { index, node ->
            assertEquals(node.type, b.nodes[index].type)
            assertEquals(node.next, b.nodes[index].next)
        }
    }
}
