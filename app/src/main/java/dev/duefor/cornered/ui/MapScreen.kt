package dev.duefor.cornered.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
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

fun NodeType.nodeColor(): Color = when (this) {
    NodeType.Combat -> Color(0xFF6C7F92)
    NodeType.Elite -> AccentRed
    NodeType.Rest -> Color(0xFF6FCF97)
    NodeType.Treasure -> Color(0xFFE9B44C)
    NodeType.Boss -> Color(0xFFB07CE8)
}

@Composable
fun MapScreen(
    run: RunState,
    revision: Int,
    onEnter: (Int) -> Unit,
    onExit: () -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val measurer = rememberTextMeasurer()

    val layout = remember(run.map, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            emptyMap()
        } else {
            buildMapLayout(run.map, canvasSize.width.toFloat(), canvasSize.height.toFloat())
        }
    }

    val labelStyle = remember {
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B0F14))
    }

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
                Text("第 ${run.deepestFloor} 层", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("生命 ${run.hp}/${run.maxHp} · 金币 ${run.gold} · 牌库 ${run.deck.size}", fontSize = 11.sp, color = InkMuted)
            }
            TextButton(onClick = onExit) { Text("退出", fontSize = 12.sp, color = InkMuted) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(layout) {
                        detectTapGestures { point ->
                            val nearest = layout.entries.minByOrNull { (it.value - point).getDistance() }
                            if (nearest != null && (nearest.value - point).getDistance() <= 34.dp.toPx()) {
                                onEnter(nearest.key)
                            }
                        }
                    },
            ) {
                // 连线
                for (node in run.map.nodes) {
                    val from = layout[node.id] ?: continue
                    for (nextId in node.next) {
                        val to = layout[nextId] ?: continue
                        drawLine(
                            color = Color(0xFF243040),
                            start = from,
                            end = to,
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }

                // 节点
                for (node in run.map.nodes) {
                    val center = layout[node.id] ?: continue
                    val reachable = run.isReachable(node.id)
                    val isCurrent = run.currentNodeId == node.id
                    val color = node.type.nodeColor()
                    val radius = if (node.type == NodeType.Boss) 24.dp.toPx() else 19.dp.toPx()

                    if (reachable) {
                        drawCircle(
                            color = AccentCyan.copy(alpha = 0.22f),
                            radius = radius * 1.55f,
                            center = center,
                        )
                    }
                    drawCircle(
                        color = if (reachable) color else color.copy(alpha = 0.22f),
                        radius = radius,
                        center = center,
                    )
                    if (isCurrent) {
                        drawCircle(
                            color = Ink,
                            radius = radius + 3.dp.toPx(),
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                        )
                    }

                    val label = measurer.measure(
                        text = AnnotatedString(node.type.short),
                        style = labelStyle.copy(
                            color = if (reachable) Color(0xFF0B0F14) else InkMuted,
                        ),
                    )
                    drawText(
                        textLayoutResult = label,
                        topLeft = Offset(
                            center.x - label.size.width / 2f,
                            center.y - label.size.height / 2f,
                        ),
                    )
                }
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

private fun buildMapLayout(map: GameMap, width: Float, height: Float): Map<Int, Offset> {
    val result = mutableMapOf<Int, Offset>()
    val rowHeight = height / (map.floorCount + 0.6f)
    for (floor in 0 until map.floorCount) {
        val row = map.row(floor)
        if (row.isEmpty()) continue
        row.forEachIndexed { index, node: MapNode ->
            val x = width * (index + 1f) / (row.size + 1f)
            val y = height - rowHeight * (floor + 1.05f)
            result[node.id] = Offset(x, y)
        }
    }
    return result
}
