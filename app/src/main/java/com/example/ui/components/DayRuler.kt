package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProstutiTheme
import kotlin.math.max

@Composable
fun DayRuler(
    totalDays: Int, // total days from start to exam
    todayIndex: Int, // 0-based index of today
    isCompressedWeeks: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val tickCount = if (isCompressedWeeks) max(2, totalDays / 7) else max(2, totalDays)
    val todayTick = if (isCompressedWeeks) todayIndex / 7 else todayIndex

    // Stagger animation: ticks animate in once on load (12ms per tick)
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(tickCount) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = (tickCount * 12).coerceIn(120, 600),
                easing = LinearEasing
            )
        )
    }

    Box(modifier = modifier.fillMaxWidth().height(36.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            val w = size.width
            val baseH = size.height * 0.4f
            val todayH = size.height * 0.7f
            val examH = size.height * 0.95f
            val step = if (tickCount > 1) w / (tickCount - 1) else w

            val visibleTicks = (tickCount * animProgress.value).toInt()

            for (i in 0 until visibleTicks) {
                val x = i * step
                val isToday = i == todayTick
                val isExam = i == tickCount - 1
                val isPassed = i < todayTick

                val (tickHeight, tickColor, strokeW) = when {
                    isExam -> Triple(examH, colors.vermilion, 2.5f)
                    isToday -> Triple(todayH, colors.blue, 2.5f)
                    isPassed -> Triple(baseH, colors.ink, 1.5f)
                    else -> Triple(baseH, colors.rule, 1.2f)
                }

                val startY = size.height - tickHeight
                drawLine(
                    color = tickColor,
                    start = Offset(x, startY),
                    end = Offset(x, size.height),
                    strokeWidth = strokeW
                )
            }
        }
    }
}
