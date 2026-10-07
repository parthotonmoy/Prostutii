package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography

@Composable
fun NotebookBackground(
    modifier: Modifier = Modifier,
    drawGrid: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = ProstutiTheme.colors
    val gridAlpha = if (colors.isDark) 0.6f else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.paper)
    ) {
        if (drawGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 24.dp.toPx()
                val ruleColor = colors.rule.copy(alpha = gridAlpha)
                val width = size.width
                val height = size.height

                var x = 0f
                while (x <= width) {
                    drawLine(
                        color = ruleColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += step
                }

                var y = 0f
                while (y <= height) {
                    drawLine(
                        color = ruleColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += step
                }
            }
        }
        content()
    }
}

@Composable
fun DraftingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hasOffsetShadow: Boolean = true,
    topRuleColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colors = ProstutiTheme.colors
    val shape = RoundedCornerShape(6.dp)

    Box(modifier = modifier) {
        // Offset shadow (light mode: solid 2dp x 2dp offset in ink; dark mode: none)
        if (hasOffsetShadow && !colors.isDark) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = 2.dp)
                    .background(colors.ink, shape)
            )
        }

        // Primary Card
        val cardMod = Modifier
            .background(colors.card, shape)
            .border(
                width = 1.dp,
                color = if (colors.isDark) colors.rule else colors.ink,
                shape = shape
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

        Box(modifier = cardMod) {
            content()
            if (topRuleColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(top = 0.dp)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(
                            color = topRuleColor,
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultyBars(
    difficulty: Int, // 1..5
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.dp,
    spacing: Dp = 2.dp
) {
    val colors = ProstutiTheme.colors
    val clamped = difficulty.coerceIn(1, 5)

    Canvas(
        modifier = modifier
            .semantics { contentDescription = "Difficulty $clamped of 5" }
    ) {
        val w = barWidth.toPx()
        val sp = spacing.toPx()
        val totalH = size.height

        for (i in 1..5) {
            val barH = (totalH * (i / 5f)).coerceAtLeast(3.dp.toPx())
            val x = (i - 1) * (w + sp)
            val y = totalH - barH
            val isFilled = i <= clamped
            val color = if (isFilled) colors.ink else colors.rule
            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(w, barH)
            )
        }
    }
}

@Composable
fun SessionKindChip(
    kind: String, // learn, revise, practice
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val label = when (kind.lowercase()) {
        "learn" -> "Learn"
        "revise" -> "Revise"
        "practice" -> "Practice"
        else -> kind.replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = modifier
            .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
            .background(Color.Transparent, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
            Canvas(modifier = Modifier.padding(1.dp)) {
                val r = 3.5.dp.toPx()
                when (kind.lowercase()) {
                    "learn" -> {
                        // Filled circle
                        drawCircle(color = colors.ink2, radius = r, center = Offset(r, r))
                    }
                    "revise" -> {
                        // Half circle
                        drawArc(
                            color = colors.ink2,
                            startAngle = 90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                        )
                        drawCircle(
                            color = colors.ink2,
                            radius = r,
                            center = Offset(r, r),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
                        )
                    }
                    "practice" -> {
                        // Triangle
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(r, 0f)
                            lineTo(r * 2, r * 2)
                            lineTo(0f, r * 2)
                            close()
                        }
                        drawPath(path, color = colors.ink2)
                    }
                }
            }
            Text(
                text = label,
                style = ProstutiTypography.caption,
                color = colors.ink2
            )
        }
    }
}

@Composable
fun TopicStatusIndicator(
    status: String, // new, learning, revised, solid
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val (dotColor, isHollow, label) = when (status.lowercase()) {
        "new" -> Triple(colors.ink3, true, "New")
        "learning" -> Triple(colors.blue, false, "Learning")
        "revised" -> Triple(colors.amber, false, "Revised")
        "solid" -> Triple(colors.moss, false, "Solid")
        else -> Triple(colors.ink3, true, status)
    }

    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.padding(1.dp)) {
            val r = 3.dp.toPx()
            if (isHollow) {
                drawCircle(
                    color = dotColor,
                    radius = r,
                    center = Offset(r, r),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f)
                )
            } else {
                drawCircle(color = dotColor, radius = r, center = Offset(r, r))
            }
        }
        Text(
            text = label,
            style = ProstutiTypography.caption,
            color = if (isHollow) colors.ink3 else colors.ink2
        )
    }
}
