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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Card
import com.example.model.Session
import com.example.model.Subject
import com.example.model.Topic
import com.example.ui.components.DayRuler
import com.example.ui.components.DraftingCard
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.LedgerSessionStrip
import com.example.ui.components.NotebookBackground
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.ui.theme.getSubjectColor
import com.example.util.DateUtil
import kotlin.math.max

@Composable
fun TodayScreen(
    subjects: List<Subject>,
    topics: List<Topic>,
    sessions: List<Session>,
    cards: List<Card>,
    onToggleSessionDone: (Session) -> Unit,
    onSessionSelected: (Session) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onMissedTodayClick: () -> Unit,
    onLightDayClick: () -> Unit,
    onCatchUpMoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()
    val todayIso = DateUtil.epochDayToIso(todayEpoch)

    val todayDateFormatted = DateUtil.formatDayOfWeekDayMonth(todayEpoch)

    // Active subjects sorted by examDate
    val activeSubjects = remember(subjects) {
        subjects.filter { !it.archived && it.examDateEpochDay > todayEpoch }
            .sortedBy { it.examDateEpochDay }
    }

    var selectedExamIndex by remember { mutableIntStateOf(0) }
    val nearestSubject = if (activeSubjects.isNotEmpty()) {
        activeSubjects[selectedExamIndex % activeSubjects.size]
    } else null

    // Sessions for today
    val todaySessions = remember(sessions) {
        sessions.filter { it.date == todayIso }
    }

    // Sessions from yesterday/past still open (catch-up banner)
    val overdueCount = remember(sessions) {
        sessions.count { it.date < todayIso && it.status == "pending" }
    }

    // Cards due today
    val dueCardsCount = remember(cards) {
        cards.count { it.due <= todayIso }
    }

    // Progress blocks
    val totalBlocks = todaySessions.sumOf { it.blocks }
    val doneBlocks = todaySessions.filter { it.status == "done" }.sumOf { it.blocks }

    // Topic map
    val topicMap = remember(topics) { topics.associateBy { it.id } }
    val subjectMap = remember(subjects) { subjects.associateBy { it.id } }

    NotebookBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = todayDateFormatted,
                            style = ProstutiTypography.h2,
                            color = colors.ink
                        )
                        val planStartEpoch = remember(sessions) {
                            sessions.minOfOrNull { it.dateEpochDay } ?: todayEpoch
                        }
                        val planDayNumber = max(1, (todayEpoch - planStartEpoch + 1).toInt())
                        Text(
                            text = "Day $planDayNumber of your plan",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.ink
                        )
                    }
                }
            }

            // Countdown & Day Ruler Card
            if (nearestSubject != null) {
                item(key = "countdown_card") {
                    val daysLeft = max(0, (nearestSubject.examDateEpochDay - todayEpoch).toInt())
                    val isCompressed = daysLeft > 60

                    val touchedTopicsCount = remember(topics, sessions, nearestSubject) {
                        val subjTopicIds = topics.filter { it.subjectId == nearestSubject.id }.map { it.id }.toSet()
                        sessions.filter { it.status == "done" && subjTopicIds.contains(it.topicId) }
                            .map { it.topicId }
                            .distinct()
                            .size
                    }
                    val totalSubjTopics = remember(topics, nearestSubject) {
                        topics.count { it.subjectId == nearestSubject.id }
                    }

                    DraftingCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (activeSubjects.size > 1) {
                                selectedExamIndex = (selectedExamIndex + 1) % activeSubjects.size
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = nearestSubject.name,
                                    style = ProstutiTypography.h3,
                                    color = colors.ink
                                )
                                if (activeSubjects.size > 1) {
                                    Text(
                                        text = "${(selectedExamIndex % activeSubjects.size) + 1} of ${activeSubjects.size}",
                                        style = ProstutiTypography.caption,
                                        color = colors.ink3
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$daysLeft",
                                    style = ProstutiTypography.monoHuge,
                                    color = colors.ink
                                )
                                Text(
                                    text = if (daysLeft == 1) "day left" else "days left",
                                    style = ProstutiTypography.bodyLarge,
                                    color = colors.ink2,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            // Day Ruler
                            val totalRulerDays = daysLeft + 5 // show past few days + days until exam
                            DayRuler(
                                totalDays = totalRulerDays,
                                todayIndex = 5,
                                isCompressedWeeks = isCompressed,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$touchedTopicsCount of $totalSubjTopics topics touched",
                                    style = ProstutiTypography.caption,
                                    color = colors.ink2
                                )
                                Text(
                                    text = DateUtil.formatDayMonth(nearestSubject.examDateEpochDay),
                                    style = ProstutiTypography.monoMicro,
                                    color = colors.vermilion
                                )
                            }
                        }
                    }
                }
            }

            // Catch-up banner
            if (overdueCount > 0) {
                item(key = "catchup_banner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.vermilionSoft, RoundedCornerShape(6.dp))
                            .border(1.dp, colors.vermilion, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$overdueCount sessions from yesterday are still open.",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink,
                                modifier = Modifier.weight(1f)
                            )
                            DraftingOutlinedButton(
                                text = "Move them",
                                onClick = onCatchUpMoveClick,
                                borderColor = colors.vermilion,
                                textColor = colors.vermilion
                            )
                        }
                    }
                }
            }

            // Cards Due Chip
            if (dueCardsCount > 0) {
                item(key = "cards_due_chip") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.card, RoundedCornerShape(4.dp))
                            .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onOpenReview)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$dueCardsCount cards due.",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.blue
                            )
                            Text(
                                text = "Review →",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.blue
                            )
                        }
                    }
                }
            }

            // Today Progress Bar
            if (totalBlocks > 0) {
                item(key = "today_progress") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's load",
                                style = ProstutiTypography.caption,
                                color = colors.ink2
                            )
                            Text(
                                text = "$doneBlocks of $totalBlocks blocks",
                                style = ProstutiTypography.monoSmall,
                                color = colors.ink
                            )
                        }
                        // Track & fill bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(colors.rule, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            val fraction = (doneBlocks.toFloat() / totalBlocks.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(6.dp)
                                    .background(colors.moss)
                            )
                        }
                    }
                }
            }

            // Ledger Sessions
            if (todaySessions.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Canvas(modifier = Modifier.size(80.dp, 28.dp)) {
                                drawLine(colors.rule, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                                for (i in 0..6) {
                                    val x = i * (size.width / 6f)
                                    drawLine(colors.ink3, Offset(x, size.height - 12f), Offset(x, size.height), 1.5f)
                                }
                            }
                            Text(
                                text = "Nothing planned today. Either you're ahead, or no exam has been added yet.",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink2,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(todaySessions, key = { it.id }) { session ->
                    val topic = topicMap[session.topicId]
                    val subject = topic?.let { subjectMap[it.subjectId] }
                    val subjectColor = getSubjectColor(subject?.colorIndex ?: 0, colors.isDark)

                    // Derived start time based on index (16:00, 16:30, ...)
                    val sessionIndex = todaySessions.indexOf(session)
                    val baseHour = 16
                    val totalMinutesOffset = sessionIndex * 30
                    val hour = (baseHour + totalMinutesOffset / 60) % 24
                    val min = totalMinutesOffset % 60
                    val timeStr = String.format("%02d:%02d", hour, min)

                    LedgerSessionStrip(
                        subjectName = subject?.name ?: "Subject",
                        topicTitle = topic?.title ?: "Topic",
                        pigmentColor = subjectColor,
                        timeText = timeStr,
                        blocks = session.blocks,
                        kind = session.kind,
                        difficulty = topic?.difficulty ?: 3,
                        status = session.status,
                        onStatusToggle = { onToggleSessionDone(session) },
                        onClick = { onSessionSelected(session) }
                    )
                }

                // Check if all sessions are done
                val allDone = todaySessions.all { it.status == "done" }
                if (allDone) {
                    item(key = "all_done_card") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.mossSoft, RoundedCornerShape(6.dp))
                                .border(1.dp, colors.moss, RoundedCornerShape(6.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All sessions done. Close the book.",
                                style = ProstutiTypography.bodyLarge,
                                color = colors.moss
                            )
                        }
                    }
                }

                // Bottom actions: "I missed today" (outlined vermilion) & "Light day"
                item(key = "today_footer_actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DraftingOutlinedButton(
                            text = "I missed today",
                            onClick = onMissedTodayClick,
                            borderColor = colors.vermilion,
                            textColor = colors.vermilion,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = onLightDayClick,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "Light day",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink2
                            )
                        }
                    }
                }
            }
        }
    }
}
