package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography

@Composable
fun DraftingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ProstutiTheme.colors
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (enabled) colors.blue else colors.rule, shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ProstutiTypography.bodyLarge,
            color = if (enabled) colors.onBlueButton else colors.ink3
        )
    }
}

@Composable
fun DraftingOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    textColor: Color? = null
) {
    val colors = ProstutiTheme.colors
    val shape = RoundedCornerShape(6.dp)
    val stroke = borderColor ?: colors.ink
    val textCol = textColor ?: colors.ink

    Box(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, stroke, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ProstutiTypography.bodyLarge,
            color = textCol
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .border(1.dp, colors.rule, shape)
            .background(colors.card, shape)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val itemShape = RoundedCornerShape(4.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (isSelected) Modifier.background(colors.blue, itemShape)
                        else Modifier
                    )
                    .clip(itemShape)
                    .clickable { onOptionSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = ProstutiTypography.bodyMedium,
                    color = if (isSelected) colors.onBlueButton else colors.ink2
                )
            }
        }
    }
}

@Composable
fun LedgerSessionStrip(
    subjectName: String,
    topicTitle: String,
    pigmentColor: Color,
    timeText: String, // e.g. "16:00"
    blocks: Int,
    kind: String, // learn, revise, practice
    difficulty: Int,
    status: String, // pending, done, missed, skipped
    onStatusToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val isDone = status == "done"
    val isMissed = status == "missed"

    val bgColor = when {
        isDone -> colors.mossSoft
        isMissed -> colors.vermilionSoft
        else -> colors.card
    }

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, shape)
            .border(1.dp, if (colors.isDark) colors.rule else colors.ink, shape)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left pigment bar (4dp wide)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    if (isMissed) {
                        // Diagonal hatch in left bar
                        drawRect(color = pigmentColor)
                        val step = 4.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = colors.vermilion,
                                start = Offset(0f, y),
                                end = Offset(size.width, y + step),
                                strokeWidth = 1.5f
                            )
                            y += step
                        }
                    } else {
                        drawRect(color = pigmentColor)
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(10.dp))

            // Time column (monospace)
            Text(
                text = timeText,
                style = ProstutiTypography.monoSmall,
                color = colors.ink2,
                modifier = Modifier.width(46.dp)
            )

            // Topic details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = topicTitle,
                    style = ProstutiTypography.bodyLarge,
                    color = colors.ink,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = subjectName,
                        style = ProstutiTypography.caption,
                        color = colors.ink2
                    )
                    Text(
                        text = "•",
                        style = ProstutiTypography.caption,
                        color = colors.ink3
                    )
                    Text(
                        text = "$blocks ${if (blocks == 1) "block" else "blocks"}",
                        style = ProstutiTypography.caption,
                        color = colors.ink2
                    )
                    DifficultyBars(
                        difficulty = difficulty,
                        modifier = Modifier.size(width = 24.dp, height = 12.dp)
                    )
                    SessionKindChip(kind = kind)
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))

            // Complete button (Circle or checked)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Checkbox, onClick = onStatusToggle)
                    .semantics { contentDescription = if (isDone) "Mark pending" else "Mark completed" },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(colors.moss, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, colors.rule, RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}
