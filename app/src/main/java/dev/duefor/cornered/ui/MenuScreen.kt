package dev.duefor.cornered.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.AccentRed
import dev.duefor.cornered.ui.theme.ArenaBackground
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted
import kotlin.math.max

@Composable
fun MenuScreen(
    best: Int,
    version: String,
    checking: Boolean,
    onPlay: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaBackground),
    ) {
        Canvas(Modifier.fillMaxSize()) { drawBackdrop() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CORNERED",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 9.sp,
                color = Ink,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "四面合围，别让角落吞掉你",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(44.dp))

            Text(
                text = "历史最高",
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
            Text(
                text = "$best 秒",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color(0xFF06231F),
                ),
            ) {
                Text("开始游戏", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = onCheckUpdate, enabled = !checking) {
                Text(
                    text = if (checking) "正在检查更新…" else "检查更新",
                    color = if (checking) InkMuted else AccentCyan,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "拖动躲避 · 快速甩动冲刺 · 场地会越来越小",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted.copy(alpha = 0.5f),
            )
        }
    }
}

private fun DrawScope.drawBackdrop() {
    val w = size.width
    val h = size.height

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF12202B), Color(0xFF070A0E)),
            center = Offset(w * 0.5f, h * 0.4f),
            radius = max(w, h) * 0.9f,
        ),
    )

    val step = 48f * density
    var y = step
    while (y < h) {
        var x = step
        while (x < w) {
            drawCircle(Color(0xFF1A2733), radius = 1.3f * density, center = Offset(x, y))
            x += step
        }
        y += step
    }

    // 右侧收拢的角落
    val inset = minOf(w, h) * 0.1f
    val corner = CornerRadius(24f * density)
    drawRoundRect(
        color = Color(0x334DECC8),
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2f, h - inset * 2f),
        cornerRadius = corner,
        style = Stroke(width = 2f * density),
    )

    val arm = 34f * density
    val color = AccentCyan.copy(alpha = 0.75f)
    val sw = 5f * density
    drawLine(color, Offset(inset, inset + arm), Offset(inset, inset), sw)
    drawLine(color, Offset(inset, inset), Offset(inset + arm, inset), sw)
    drawLine(color, Offset(w - inset - arm, h - inset), Offset(w - inset, h - inset), sw)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset, h - inset - arm), sw)

    // 几个游荡的方块，纯装饰
    val half = 13f * density
    val spots = listOf(
        Offset(w * 0.2f, h * 0.24f),
        Offset(w * 0.82f, h * 0.3f),
        Offset(w * 0.15f, h * 0.74f),
        Offset(w * 0.78f, h * 0.8f),
    )
    spots.forEachIndexed { index, point ->
        val radius = if (index % 2 == 0) half else half * 0.8f
        drawRoundRect(
            color = AccentRed.copy(alpha = 0.18f),
            topLeft = Offset(point.x - radius, point.y - radius),
            size = Size(radius * 2f, radius * 2f),
            cornerRadius = CornerRadius(radius * 0.34f),
        )
        drawRoundRect(
            color = AccentRed.copy(alpha = 0.45f),
            topLeft = Offset(point.x - radius, point.y - radius),
            size = Size(radius * 2f, radius * 2f),
            cornerRadius = CornerRadius(radius * 0.34f),
            style = Stroke(width = 1.5f * density),
        )
    }
}
