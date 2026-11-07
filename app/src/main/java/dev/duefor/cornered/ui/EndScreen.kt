package dev.duefor.cornered.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.RunState
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.AccentRed
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

@Composable
fun EndScreen(
    victory: Boolean,
    run: RunState,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (victory) "登顶" else "倒下了",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = if (victory) AccentCyan else AccentRed,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (victory) "你击败了深渊之眼" else "第 ${run.deepestFloor} 层止步",
            fontSize = 14.sp,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        StatRow("到达层数", "${run.deepestFloor}")
        StatRow("牌组张数", "${run.deck.size}")
        StatRow("金币", "${run.gold}")

        Spacer(Modifier.height(34.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan,
                contentColor = Color(0xFF06231F),
            ),
        ) {
            Text("再来一局", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onExit) { Text("返回主页", color = InkMuted) }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, fontSize = 12.sp, color = InkMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}
