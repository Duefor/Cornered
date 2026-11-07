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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.ArenaBackground
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted
import kotlin.math.max

@Composable
fun MenuScreen(
    bestFloor: Int,
    victories: Int,
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
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = Ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "构筑牌组，一层层爬上去",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "最深 $bestFloor 层" + if (victories > 0) " · 通关 $victories 次" else "",
                fontSize = 13.sp,
                color = AccentCyan,
            )

            Spacer(Modifier.height(36.dp))

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
                Text("开始新一局", fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "v$version",
                style = MaterialTheme.typography.labelSmall,
                color = InkMuted.copy(alpha = 0.55f),
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
            center = Offset(w * 0.5f, h * 0.35f),
            radius = max(w, h) * 0.95f,
        ),
    )

    val step = 46f * density
    var y = step
    while (y < h) {
        var x = step
        while (x < w) {
            drawCircle(Color(0xFF1A2733), radius = 1.2f * density, center = Offset(x, y))
            x += step
        }
        y += step
    }

    // 四角支架，呼应应用图标；放在边缘，不会压到任何控件
    val m = 24f * density
    val arm = 26f * density
    val bracket = AccentCyan.copy(alpha = 0.18f)
    val sw = 2f * density
    drawLine(bracket, Offset(m, m + arm), Offset(m, m), sw)
    drawLine(bracket, Offset(m, m), Offset(m + arm, m), sw)
    drawLine(bracket, Offset(w - m - arm, m), Offset(w - m, m), sw)
    drawLine(bracket, Offset(w - m, m), Offset(w - m, m + arm), sw)
    drawLine(bracket, Offset(m, h - m - arm), Offset(m, h - m), sw)
    drawLine(bracket, Offset(m, h - m), Offset(m + arm, h - m), sw)
    drawLine(bracket, Offset(w - m - arm, h - m), Offset(w - m, h - m), sw)
    drawLine(bracket, Offset(w - m, h - m), Offset(w - m, h - m - arm), sw)

    // 底部压暗，保证版本号等小字读得清
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x00000000), Color(0x88070A0E)),
            startY = h * 0.6f,
            endY = h,
        ),
    )
}
