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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.model.PastPaper
import com.example.model.PomodoroLog
import com.example.model.Problem
import com.example.model.Session
import com.example.model.Subject
import com.example.model.Topic
import com.example.ui.components.DraftingCard
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.ui.theme.getSubjectColor
import com.example.util.DateUtil
import kotlin.math.max

@Composable
fun ProgressScreen(
    subjects: List<Subject>,
    topics: List<Topic>,
    sessions: List<Session>,
    cards: List<Card>,
    problems: List<Problem>,
    pastPapers: List<PastPaper>,
    pomodoroLogs: List<PomodoroLog>,
    onClearTopicHelp: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()
    val topicsBySubject = remember(topics) { topics.groupBy { it.subjectId } }

    var showArchived by remember { mutableStateOf(false) }

    // This week minutes studied (last 7 days: todayEpoch - 6 until todayEpoch)
    val last7Days = remember(todayEpoch) { (todayEpoch - 6..todayEpoch).toList() }
    val minutesPerDay = remember(pomodoroLogs, last7Days) {
        last7Days.map { dayEpoch ->
            val dayIso = DateUtil.epochDayToIso(dayEpoch)
            pomodoroLogs.filter { it.date == dayIso }.sumOf { it.minutes }
        }
    }
    val daysStudiedCount = remember(minutesPerDay) {
        minutesPerDay.count { it > 0 }
    }

    NotebookBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                Text(
                    text = "Progress",
                    style = ProstutiTypography.h2,
                    color = colors.ink
                )
            }

            // Days studied summary
            item(key = "days_studied_line") {
                Text(
                    text = "Days studied: $daysStudiedCount of the last 7",
                    style = ProstutiTypography.bodyLarge,
                    color = colors.ink
                )
            }

            // This Week Bar Chart (Canvas)
            item(key = "this_week_chart") {
                DraftingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Minutes studied this week",
                            style = ProstutiTypography.caption,
                            color = colors.ink2
                        )

                        val maxMinutes = max(180, (minutesPerDay.maxOrNull() ?: 180))
                        val dailyTargetMinutes = 180

                        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val barWidth = 24.dp.toPx()
                                val step = w / 7f

                                // Target dashed line at dailyTargetMinutes
                                val targetY = h - (h * (dailyTargetMinutes.toFloat() / maxMinutes.toFloat()))
                                drawLine(
                                    color = colors.vermilion,
                                    start = Offset(0f, targetY),
                                    end = Offset(w, targetY),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )

                                for (i in 0..6) {
                                    val m = minutesPerDay[i]
                                    val barH = if (maxMinutes > 0) (h * (m.toFloat() / maxMinutes.toFloat())) else 0f
                                    val x = (i * step) + (step / 2f) - (barWidth / 2f)
                                    val y = h - barH

                                    drawRect(
                                        color = colors.blue,
                                        topLeft = Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barH)
                                    )
                                }
                            }
                        }

                        // Day names row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            last7Days.forEach { dayEpoch ->
                                val dayName = DateUtil.dayOfWeekShortName((dayEpoch - last7Days.first()).toInt())
                                Text(
                                    text = dayName,
                                    style = ProstutiTypography.caption,
                                    color = colors.ink3,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Readiness per subject
            item(key = "readiness_section") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Readiness by subject",
                        style = ProstutiTypography.h3,
                        color = colors.ink
                    )
                    Text(
                        text = "A rough guide from your own progress, not a prediction.",
                        style = ProstutiTypography.caption,
                        color = colors.ink3
                    )
                }
            }

            items(subjects.filter { !it.archived }, key = { it.id }) { subj ->
                val subjTopics = topicsBySubject[subj.id] ?: emptyList()
                val subjTopicIds = subjTopics.map { it.id }.toSet()
                val pigment = getSubjectColor(subj.colorIndex, colors.isDark)

                // Coverage: blocks done weighted by difficulty / total weighted blocks
                val totalWeighted = subjTopics.sumOf { it.estBlocks * it.difficulty }.coerceAtLeast(1)
                val doneWeighted = subjTopics.sumOf { it.blocksDone * it.difficulty }
                val coverage = (doneWeighted.toDouble() / totalWeighted.toDouble()).coerceIn(0.0, 1.0)

                // Revision: share of topics revised at least once
                val revisedCount = subjTopics.count { it.revisionCount > 0 || it.status == "revised" || it.status == "solid" }
                val revision = if (subjTopics.isNotEmpty()) (revisedCount.toDouble() / subjTopics.size.toDouble()) else 0.0

                // Recall
                val subjCards = cards.filter { subjTopicIds.contains(it.topicId) }
                val cardMaturity = if (subjCards.isNotEmpty()) {
                    subjCards.count { it.intervalDays >= 7 }.toDouble() / subjCards.size.toDouble()
                } else 0.0

                val hasCardsOrProblems = subjCards.isNotEmpty()
                val readinessPercent = if (hasCardsOrProblems) {
                    ((0.5 * coverage + 0.3 * revision + 0.2 * cardMaturity) * 100.0).toInt().coerceIn(0, 100)
                } else {
                    ((0.6 * coverage + 0.4 * revision) * 100.0).toInt().coerceIn(0, 100)
                }

                // Planned vs Done
                val plannedBlocks = sessions.filter { it.topicId in subjTopicIds }.sumOf { it.blocks }
                val doneBlocks = sessions.filter { it.topicId in subjTopicIds && it.status == "done" }.sumOf { it.blocks }

                DraftingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(subj.name, style = ProstutiTypography.bodyLarge, color = colors.ink)
                            Text("$readinessPercent%", style = ProstutiTypography.monoLarge, color = colors.ink)
                        }

                        // Readiness bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(colors.rule, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(readinessPercent / 100f)
                                    .height(6.dp)
                                    .background(pigment)
                            )
                        }

                        // Paired thin bars: Planned vs Done
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Work: $doneBlocks of $plannedBlocks blocks done", style = ProstutiTypography.caption, color = colors.ink2)
                        }
                    }
                }
            }

            // "Take these to a teacher or friend"
            val helpTopics = topics.filter { it.needsHelp }
            if (helpTopics.isNotEmpty()) {
                item(key = "help_section_header") {
                    Text(
                        text = "Take these to a teacher or friend",
                        style = ProstutiTypography.h3,
                        color = colors.ink,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(helpTopics, key = { it.id }) { top ->
                    val subj = subjects.find { it.id == top.subjectId }
                    DraftingCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(top.title, style = ProstutiTypography.bodyLarge, color = colors.ink)
                                Text(subj?.name ?: "Subject", style = ProstutiTypography.caption, color = colors.ink2)
                            }
                            DraftingOutlinedButton(
                                text = "Cleared",
                                onClick = { onClearTopicHelp(top) }
                            )
                        }
                    }
                }
            }

            // Missed log (plain dates, no judgement)
            val missedSessions = sessions.filter { it.status == "missed" }
            if (missedSessions.isNotEmpty()) {
                item(key = "missed_log_header") {
                    Text("Missed log", style = ProstutiTypography.h3, color = colors.ink)
                }
                val groupedMissed = missedSessions.groupBy { it.date }
                groupedMissed.forEach { (dateStr, list) ->
                    item(key = "missed_$dateStr") {
                        val epoch = DateUtil.isoToEpochDay(dateStr)
                        val totalB = list.sumOf { it.blocks }
                        Text(
                            text = "${DateUtil.formatDayOfWeekDayMonth(epoch)}: $totalB missed blocks",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )
                    }
                }
            }

            // Past Papers summary
            if (pastPapers.isNotEmpty()) {
                item(key = "past_papers_header") {
                    Text("Past papers", style = ProstutiTypography.h3, color = colors.ink)
                }
                items(pastPapers, key = { it.id }) { paper ->
                    val subj = subjects.find { it.id == paper.subjectId }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${subj?.name ?: ""}: ${paper.label}", style = ProstutiTypography.bodyMedium, color = colors.ink)
                        Text(
                            text = if (paper.done) (paper.scorePercent?.let { "$it%" } ?: "Done") else "Pending",
                            style = ProstutiTypography.caption,
                            color = if (paper.done) colors.moss else colors.ink3
                        )
                    }
                }
            }
        }
    }
}
