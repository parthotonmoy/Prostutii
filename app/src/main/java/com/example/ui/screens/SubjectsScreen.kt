package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.model.Problem
import com.example.model.Subject
import com.example.model.Topic
import com.example.ui.components.DraftingCard
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.ui.theme.getSubjectColor
import com.example.util.DateUtil
import kotlin.math.max

@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    topics: List<Topic>,
    cards: List<Card>,
    problems: List<Problem>,
    onSubjectClick: (Subject) -> Unit,
    onAddSubjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()

    val topicsBySubject = remember(topics) { topics.groupBy { it.subjectId } }

    NotebookBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "header") {
                    Text(
                        text = "Subjects",
                        style = ProstutiTypography.h2,
                        color = colors.ink
                    )
                }

                if (subjects.isEmpty()) {
                    item(key = "empty_subjects") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No subjects yet. Add one with its exam date and I'll build the plan.",
                                style = ProstutiTypography.bodyLarge,
                                color = colors.ink2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    items(subjects, key = { it.id }) { subject ->
                        val subjTopics = topicsBySubject[subject.id] ?: emptyList()
                        val daysLeft = max(0, (subject.examDateEpochDay - todayEpoch).toInt())
                        val pigmentColor = getSubjectColor(subject.colorIndex, colors.isDark)

                        // Readiness formula:
                        // 0.5 * coverage + 0.3 * revision + 0.2 * recall
                        val totalEst = subjTopics.sumOf { it.estBlocks }.coerceAtLeast(1)
                        val totalDone = subjTopics.sumOf { it.blocksDone }
                        val coverage = (totalDone.toDouble() / totalEst.toDouble()).coerceIn(0.0, 1.0)

                        val revisedCount = subjTopics.count { it.status == "revised" || it.status == "solid" }
                        val revision = if (subjTopics.isNotEmpty()) (revisedCount.toDouble() / subjTopics.size.toDouble()) else 0.0

                        val subjTopicIds = subjTopics.map { it.id }.toSet()
                        val subjCards = cards.filter { subjTopicIds.contains(it.topicId) }
                        val cardMaturity = if (subjCards.isNotEmpty()) {
                            subjCards.count { it.intervalDays >= 7 }.toDouble() / subjCards.size.toDouble()
                        } else 0.0

                        val readinessPercent = ((0.5 * coverage + 0.3 * revision + 0.2 * cardMaturity) * 100.0).toInt().coerceIn(0, 100)

                        // Days-left pill style
                        val (pillBg, pillText, pillBorder) = when {
                            daysLeft <= 3 -> Triple(colors.vermilionSoft, colors.vermilion, colors.vermilion)
                            daysLeft <= 7 -> Triple(colors.amberSoft, colors.amber, colors.amber)
                            else -> Triple(Color.Transparent, colors.ink2, colors.rule)
                        }

                        DraftingCard(
                            modifier = Modifier.fillMaxWidth(),
                            topRuleColor = pigmentColor,
                            onClick = { onSubjectClick(subject) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subject.name,
                                        style = ProstutiTypography.h3,
                                        color = colors.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Days-left pill
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                            .background(pillBg, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (daysLeft == 1) "1 day left" else "$daysLeft days left",
                                            style = ProstutiTypography.caption,
                                            color = pillText
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = DateUtil.formatDayMonth(subject.examDateEpochDay),
                                        style = ProstutiTypography.monoMedium,
                                        color = colors.ink2
                                    )
                                    Text(
                                        text = "$readinessPercent% ready",
                                        style = ProstutiTypography.monoMedium,
                                        color = colors.ink
                                    )
                                }

                                // Topic status segment strip (new / learning / revised / solid)
                                if (subjTopics.isNotEmpty()) {
                                    val newCount = subjTopics.count { it.status == "new" }
                                    val learnCount = subjTopics.count { it.status == "learning" }
                                    val revCount = subjTopics.count { it.status == "revised" }
                                    val solidCount = subjTopics.count { it.status == "solid" }
                                    val total = subjTopics.size.toFloat()

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(colors.rule, RoundedCornerShape(3.dp))
                                            .clip(RoundedCornerShape(3.dp))
                                    ) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            if (solidCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(solidCount / total)
                                                        .height(6.dp)
                                                        .background(colors.moss)
                                                )
                                            }
                                            if (revCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(revCount / total)
                                                        .height(6.dp)
                                                        .background(colors.amber)
                                                )
                                            }
                                            if (learnCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(learnCount / total)
                                                        .height(6.dp)
                                                        .background(colors.blue)
                                                )
                                            }
                                            if (newCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(newCount / total)
                                                        .height(6.dp)
                                                        .background(colors.rule)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Action Button: Add Subject
            FloatingActionButton(
                onClick = onAddSubjectClick,
                containerColor = colors.blue,
                contentColor = colors.onBlueButton,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subject"
                )
            }
        }
    }
}
