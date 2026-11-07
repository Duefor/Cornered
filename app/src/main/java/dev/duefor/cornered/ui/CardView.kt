package dev.duefor.cornered.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.duefor.cornered.game.Card
import dev.duefor.cornered.game.CardType
import dev.duefor.cornered.ui.theme.AccentCyan
import dev.duefor.cornered.ui.theme.Ink
import dev.duefor.cornered.ui.theme.InkMuted

fun CardType.accentColor(): Color = when (this) {
    CardType.Attack -> Color(0xFFE0595C)
    CardType.Skill -> Color(0xFF4B9FD4)
    CardType.Power -> Color(0xFFB07CE8)
}

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    width: Dp = 96.dp,
    playable: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val accent = card.type.accentColor()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        selected -> AccentCyan
        playable -> accent.copy(alpha = 0.85f)
        else -> Color(0xFF2A333D)
    }
    val background = Brush.verticalGradient(
        listOf(
            accent.copy(alpha = if (playable) 0.22f else 0.07f),
            Color(0xFF10171E),
        ),
    )

    Column(
        modifier = modifier
            .width(width)
            .height(width * 1.46f)
            .clip(shape)
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(enabled = playable, onClick = onClick)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (playable) 0.95f else 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${card.cost}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B0F14),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(card.type.label, fontSize = 9.sp, color = InkMuted)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = card.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (playable) Ink else InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = card.description,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = Ink.copy(alpha = if (playable) 0.85f else 0.45f),
            overflow = TextOverflow.Ellipsis,
        )
        if (card.exhaust) {
            Spacer(Modifier.height(3.dp))
            Text("消耗", fontSize = 8.sp, color = InkMuted.copy(alpha = 0.7f))
        }
    }
}
