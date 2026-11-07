package dev.duefor.cornered.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.GameMap
import dev.duefor.cornered.game.MapNode
import dev.duefor.cornered.game.NodeType
import dev.duefor.cornered.game.RunState
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.AccentRed
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted
import kotlin.math.roundToInt

fun NodeType.nodeColor(): Color = when (this) {
    NodeType.Combat -> Color(0xFF7C8FA3)
    NodeType.Elite -> Color(0xFFD2646A)
    NodeType.Rest -> Color(0xFF6FCF97)
    NodeType.Treasure -> Color(0xFFE9B44C)
    NodeType.Boss -> Color(0xFFB07CE8)
}

/**
 * 地图：连线画在底层 Canvas 上，节点是真正的可点击组件，
 * 这样命中判定完全交给 Compose，不用手算坐标。
 */
@Composable
fun MapScreen(
    run: RunState,
    revision: Int,
    onEnter: (Int) -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = if (run.deepestFloor == 0) "起点" else "第 ${run.deepestFloor} 层",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "生命 ${run.hp}/${run.maxHp} · 金币 ${run.gold} · 牌库 ${run.deck.size}",
                    fontSize = 11.sp,
                    color = InkMuted,
                )
            }
            TextButton(onClick = onExit) { Text("退出", fontSize = 12.sp, color = InkMuted) }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val positions = remember(run.map, widthPx, heightPx) {
                buildMapLayout(run.map, widthPx, heightPx)
            }

            // 连线 + 可达节点的光晕
            Canvas(Modifier.matchParentSize()) {
                for (node in run.map.nodes) {
                    val from = positions[node.id] ?: continue
                    for (nextId in node.next) {
                        val to = positions[nextId] ?: continue
                        drawLine(
                            color = Color(0xFF243040),
                            start = from,
                            end = to,
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
                positions.forEach { (id, center) ->
                    if (run.isReachable(id)) {
                        drawCircle(
                            color = AccentCyan.copy(alpha = 0.18f),
                            radius = 30.dp.toPx(),
                            center = center,
                        )
                    }
                }
            }

            positions.forEach { (id, center) ->
                val node = run.map.node(id)
                val reachable = run.isReachable(id)
                MapNodeView(
                    node = node,
                    center = center,
                    reachable = reachable,
                    current = run.currentNodeId == id,
                    onClick = { if (reachable) onEnter(id) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NodeType.entries.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(type.nodeColor()),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(type.label, fontSize = 10.sp, color = InkMuted)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MapNodeView(
    node: MapNode,
    center: Offset,
    reachable: Boolean,
    current: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val diameter = if (node.type == NodeType.Boss) 50.dp else 40.dp
    val diameterPx = with(density) { diameter.toPx() }
    val color = node.type.nodeColor()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - diameterPx / 2f).roundToInt(),
                    (center.y - diameterPx / 2f).roundToInt(),
                )
            }
            .size(diameter)
            .clip(CircleShape)
            .background(if (reachable) color else color.copy(alpha = 0.20f))
            .then(
                when {
                    current -> Modifier.border(2.dp, Ink, CircleShape)
                    reachable -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                    else -> Modifier
                },
            )
            .clickable(enabled = reachable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = node.type.short,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (reachable) Color(0xFF0B0F14) else InkMuted.copy(alpha = 0.6f),
        )
    }
}

private fun buildMapLayout(map: GameMap, width: Float, height: Float): Map<Int, Offset> {
    val result = mutableMapOf<Int, Offset>()
    val rowHeight = height / (map.floorCount + 0.6f)
    for (floor in 0 until map.floorCount) {
        val row = map.row(floor)
        if (row.isEmpty()) continue
        row.forEachIndexed { index, node ->
            val x = width * (index + 1f) / (row.size + 1f)
            val y = height - rowHeight * (floor + 1.05f)
            result[node.id] = Offset(x, y)
        }
    }
    return result
}
