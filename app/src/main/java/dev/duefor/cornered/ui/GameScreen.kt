package dev.duefor.cornered.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.GameEngine
import dev.duefor.cornered.game.Phase
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.AccentRed
import dev.duefor.cornered.ui.theme.ArenaBackground
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted
import kotlin.math.max
import kotlin.math.sin

private const val FLICK_MAX_MS = 220L

@Composable
fun GameScreen(
    best: Int,
    onExit: () -> Unit,
    onGameOver: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val pxPerDp = density.density
    val engine = remember { GameEngine() }
    val onGameOverState = rememberUpdatedState(onGameOver)

    var tick by remember { mutableLongStateOf(0L) }
    var phase by remember { mutableStateOf(Phase.Ready) }
    var score by remember { mutableIntStateOf(0) }
    var finalScore by remember { mutableIntStateOf(0) }
    var baselineBest by remember { mutableIntStateOf(best) }
    var started by remember { mutableStateOf(false) }

    val touchSlop = with(density) { 10.dp.toPx() }

    // 每帧推进一次逻辑；渲染由 Canvas 读取 tick 触发，不会整棵树重组。
    LaunchedEffect(Unit) {
        var previousNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (previousNanos == 0L) {
                    0f
                } else {
                    ((now - previousNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                }
                previousNanos = now

                engine.update(dt)
                if (engine.score != score) score = engine.score
                if (engine.phase == Phase.GameOver && phase != Phase.GameOver) {
                    phase = Phase.GameOver
                    finalScore = engine.score
                    onGameOverState.value(engine.score)
                }
                tick = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBackground)
            .onSizeChanged { size ->
                engine.resize(size.width.toFloat(), size.height.toFloat(), pxPerDp)
                if (!started) {
                    started = true
                    engine.start()
                    phase = Phase.Running
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = Offset.Zero
                    var last = down.position
                    var lastUptime = down.uptimeMillis
                    var released = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        lastUptime = change.uptimeMillis
                        if (!change.pressed) {
                            released = true
                            break
                        }
                        total += change.position - last
                        last = change.position
                        engine.setInput(total.x, total.y, touchSlop)
                        change.consume()
                    }

                    val duration = lastUptime - down.uptimeMillis
                    if (released && duration <= FLICK_MAX_MS && total.getDistance() > touchSlop * 2.2f) {
                        engine.dash(total.x, total.y)
                    }
                    engine.clearInput()
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawScene(engine, pxPerDp, tick)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = score.toString(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Ink,
            )
            Text(
                text = "最高 ${max(best, finalScore)} 秒",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
        }

        TextButton(
            onClick = onExit,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp)
                .align(Alignment.TopStart),
        ) {
            Text("退出", color = InkMuted, style = MaterialTheme.typography.labelLarge)
        }

        if (phase == Phase.Running && score < 5) {
            Text(
                text = "拖动躲避 · 快速甩动冲刺",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 28.dp),
            )
        }

        if (phase == Phase.GameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD9070A0E)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "被逼到墙角了",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentRed,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = finalScore.toString(),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink,
                    )
                    Text(
                        text = "秒",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (finalScore > baselineBest) "新纪录！" else "历史最高 $baselineBest 秒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (finalScore > baselineBest) AccentCyan else InkMuted,
                    )
                    Spacer(Modifier.height(30.dp))
                    Button(
                        onClick = {
                            baselineBest = best
                            finalScore = 0
                            score = 0
                            engine.start()
                            phase = Phase.Running
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(54.dp),
                    ) {
                        Text("再来一局", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onExit) {
                        Text("返回主页", color = InkMuted)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawScene(engine: GameEngine, pxPerDp: Float, nowNanos: Long) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF13212C), Color(0xFF070A0E)),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = max(w, h) * 0.95f,
        ),
    )

    // 背景点阵
    val step = 46f * pxPerDp
    var gy = step
    while (gy < h) {
        var gx = step
        while (gx < w) {
            drawCircle(Color(0xFF1A2733), radius = 1.3f * pxPerDp, center = Offset(gx, gy))
            gx += step
        }
        gy += step
    }

    // 收缩中的场地
    val inset = engine.inset
    val left = inset
    val top = inset
    val right = w - inset
    val bottom = h - inset
    if (right > left && bottom > top) {
        val corner = CornerRadius(20f * pxPerDp)
        val frameSize = Size(right - left, bottom - top)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x1F4DECC8), Color(0x034DECC8)),
                startY = top,
                endY = bottom,
            ),
            topLeft = Offset(left, top),
            size = frameSize,
            cornerRadius = corner,
        )
        drawRoundRect(
            color = Color(0x994DECC8).copy(alpha = 0.55f),
            topLeft = Offset(left, top),
            size = frameSize,
            cornerRadius = corner,
            style = Stroke(width = 2f * pxPerDp),
        )

        // 四角支架：提示「角」在收拢
        val pulse = 0.6f + 0.4f * sin(nowNanos / 1_000_000_000f * 2.2f)
        val arm = 26f * pxPerDp
        val gap = 12f * pxPerDp
        val bracketColor = AccentCyan.copy(alpha = 0.35f + 0.45f * pulse)
        val strokeWidth = 4f * pxPerDp

        drawLine(bracketColor, Offset(left + gap, top + gap + arm), Offset(left + gap, top + gap), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + gap, top + gap), Offset(left + gap + arm, top + gap), strokeWidth, StrokeCap.Round)

        drawLine(bracketColor, Offset(right - gap - arm, top + gap), Offset(right - gap, top + gap), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right - gap, top + gap), Offset(right - gap, top + gap + arm), strokeWidth, StrokeCap.Round)

        drawLine(bracketColor, Offset(left + gap, bottom - gap - arm), Offset(left + gap, bottom - gap), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(left + gap, bottom - gap), Offset(left + gap + arm, bottom - gap), strokeWidth, StrokeCap.Round)

        drawLine(bracketColor, Offset(right - gap - arm, bottom - gap), Offset(right - gap, bottom - gap), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(right - gap, bottom - gap), Offset(right - gap, bottom - gap - arm), strokeWidth, StrokeCap.Round)
    }

    // 敌人
    for (enemy in engine.enemies) {
        val half = enemy.radius
        rotate(degrees = enemy.angle, pivot = Offset(enemy.x, enemy.y)) {
            drawRoundRect(
                color = Color(0xFF2A1119),
                topLeft = Offset(enemy.x - half, enemy.y - half),
                size = Size(half * 2f, half * 2f),
                cornerRadius = CornerRadius(half * 0.34f),
            )
            drawRoundRect(
                color = AccentRed,
                topLeft = Offset(enemy.x - half, enemy.y - half),
                size = Size(half * 2f, half * 2f),
                cornerRadius = CornerRadius(half * 0.34f),
                style = Stroke(width = 2f * pxPerDp),
            )
        }
    }

    // 玩家
    val px = engine.playerX
    val py = engine.playerY
    val pr = engine.playerRadius

    if (engine.isDashing) {
        engine.trail.forEachIndexed { index, point ->
            val alpha = 0.05f + 0.22f * (index + 1).toFloat() / engine.trail.size
            drawCircle(AccentCyan.copy(alpha = alpha), radius = pr * 0.85f, center = point)
        }
    }

    drawCircle(AccentCyan.copy(alpha = 0.14f), radius = pr * 2.1f, center = Offset(px, py))
    drawCircle(AccentCyan, radius = pr, center = Offset(px, py))
    drawCircle(Color(0xFF06231F), radius = pr * 0.42f, center = Offset(px, py))

    // 冲刺冷却环
    val gaugeRadius = 20f * pxPerDp
    val gaugeCenter = Offset(w - 40f * pxPerDp, h - 78f * pxPerDp)
    drawCircle(Color(0x14FFFFFF), radius = gaugeRadius, center = gaugeCenter)
    drawArc(
        color = if (engine.dashProgress >= 1f) AccentCyan else Color(0x66FFFFFF),
        startAngle = -90f,
        sweepAngle = 360f * engine.dashProgress,
        useCenter = false,
        topLeft = Offset(gaugeCenter.x - gaugeRadius, gaugeCenter.y - gaugeRadius),
        size = Size(gaugeRadius * 2f, gaugeRadius * 2f),
        style = Stroke(width = 3f * pxPerDp, cap = StrokeCap.Round),
    )
}
