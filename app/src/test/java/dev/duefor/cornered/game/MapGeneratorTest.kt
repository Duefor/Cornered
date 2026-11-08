package dev.duefor.cornered.game

import dev.duefor.cornered.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class MapGeneratorTest {

    @Before
    fun setUp() = TestContent.ensure()

    @Test
    fun `every node is reachable from the first floor`() {
        GameContent.acts.forEach { act ->
            repeat(8) { seed ->
                val map = MapGenerator.generate(act, Random(seed))
                val visited = mutableSetOf<Int>()
                val queue = ArrayDeque(map.startNodes().map { it.id })

                while (queue.isNotEmpty()) {
                    val id = queue.removeFirst()
                    if (!visited.add(id)) continue
                    map.node(id).next.forEach { queue.addLast(it) }
                }

                assertEquals("act=${act.id} seed=$seed", map.nodes.size, visited.size)
            }
        }
    }

    @Test
    fun `non starting nodes always have an incoming edge`() {
        val act = GameContent.act(0)
        repeat(10) { seed ->
            val map = MapGenerator.generate(act, Random(seed))
            map.nodes.filter { it.floor > 0 }.forEach { node ->
                assertTrue("seed=$seed node=${node.id}", node.prev.isNotEmpty())
            }
        }
    }

    @Test
    fun `single boss sits on the last floor`() {
        GameContent.acts.forEach { act ->
            repeat(6) { seed ->
                val map = MapGenerator.generate(act, Random(seed))
                val last = map.row(act.floors - 1)
                assertEquals(1, last.size)
                assertEquals(NodeType.Boss, last.single().type)
            }
        }
    }

    @Test
    fun `same seed produces the same map`() {
        val act = GameContent.act(0)
        val a = MapGenerator.generate(act, Random(7))
        val b = MapGenerator.generate(act, Random(7))
        assertEquals(a.nodes.size, b.nodes.size)
        a.nodes.forEachIndexed { index, node ->
            assertEquals(node.type, b.nodes[index].type)
            assertEquals(node.next, b.nodes[index].next)
        }
    }
}
