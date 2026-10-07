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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Session
import com.example.model.Subject
import com.example.model.Topic
import com.example.model.UserAvailability
import com.example.scheduler.SchedulerOverload
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.NotebookBackground
import com.example.ui.components.SegmentedControl
import com.example.ui.components.SessionKindChip
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.ui.theme.getSubjectColor
import com.example.util.DateUtil
import java.time.LocalDate

@Composable
fun PlanScreen(
    subjects: List<Subject>,
    topics: List<Topic>,
    sessions: List<Session>,
    availability: UserAvailability,
    overload: SchedulerOverload?,
    onResolveOverloadAdd30: () -> Unit,
    onResolveOverloadDropTopic: () -> Unit,
    onResolveOverloadKeep: () -> Unit,
    onSessionClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val todayEpoch = DateUtil.todayEpochDay()
    var viewModeIndex by remember { mutableIntStateOf(0) } // 0: Week, 1: Month
    var selectedDayEpoch by remember { mutableLongStateOf(todayEpoch) }

    val topicMap = remember(topics) { topics.associateBy { it.id } }
    val subjectMap = remember(subjects) { subjects.associateBy { it.id } }

    val focusMin = 25
    fun getDayCapacity(epochDay: Long): Int {
        val iso = DateUtil.epochDayToIso(epochDay)
        val override = availability.overrides[iso]
        val min = override ?: run {
            val idx = DateUtil.dayOfWeekIndex(epochDay)
            availability.weekdayMinutes.getOrElse(idx) { 180 }
        }
        return min / focusMin
    }

    NotebookBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overload banner if present
            if (overload != null) {
                item(key = "overload_banner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.vermilionSoft, RoundedCornerShape(6.dp))
                            .border(1.dp, colors.vermilion, RoundedCornerShape(6.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "This doesn't fit.",
                                style = ProstutiTypography.h3,
                                color = colors.vermilion
                            )
                            val shortHours = overload.shortByBlocks * focusMin / 60
                            val shortMins = (overload.shortByBlocks * focusMin) % 60
                            val shortDurationStr = "${shortHours}h ${shortMins}m"
                            val examDateStr = DateUtil.formatDayMonth(overload.examDateEpochDay)
                            Text(
                                text = "${overload.subjectName} needs $shortDurationStr more than you've set aside before $examDateStr.",
                                style = ProstutiTypography.bodyMedium,
                                color = colors.ink
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DraftingOutlinedButton(
                                    text = "Add 30 min",
                                    onClick = onResolveOverloadAdd30,
                                    borderColor = colors.vermilion,
                                    textColor = colors.vermilion,
                                    modifier = Modifier.weight(1f)
                                )
                                DraftingOutlinedButton(
                                    text = "Drop topic",
                                    onClick = onResolveOverloadDropTopic,
                                    borderColor = colors.ink2,
                                    textColor = colors.ink2,
                                    modifier = Modifier.weight(1f)
                                )
                                DraftingOutlinedButton(
                                    text = "Keep anyway",
                                    onClick = onResolveOverloadKeep,
                                    borderColor = colors.ink3,
                                    textColor = colors.ink3,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Segmented Control: Week / Month
            item(key = "view_mode_selector") {
                SegmentedControl(
                    options = listOf("Week", "Month"),
                    selectedIndex = viewModeIndex,
                    onOptionSelected = { viewModeIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (viewModeIndex == 0) {
                // WEEK VIEW: 7 Day Columns
                item(key = "week_grid") {
                    // Start from start of current week (Saturday by default)
                    val todayIndexInWeek = DateUtil.dayOfWeekIndex(todayEpoch)
                    val weekStartEpoch = todayEpoch - todayIndexInWeek
                    val weekDays = (0..6).map { weekStartEpoch + it }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (colors.isDark) colors.rule else colors.ink, RoundedCornerShape(6.dp))
                            .background(colors.card, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { dayEpoch ->
                            val isToday = dayEpoch == todayEpoch
                            val isSelected = dayEpoch == selectedDayEpoch
                            val dayIso = DateUtil.epochDayToIso(dayEpoch)
                            val daySessions = sessions.filter { it.date == dayIso }
                            val totalBlocks = daySessions.sumOf { it.blocks }
                            val capacity = getDayCapacity(dayEpoch)
                            val isOverCap = capacity > 0 && totalBlocks > capacity

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDayEpoch = dayEpoch }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = DateUtil.dayOfWeekShortName((dayEpoch - weekStartEpoch).toInt()),
                                    style = ProstutiTypography.caption,
                                    color = if (isSelected) colors.blue else colors.ink2
                                )
                                val dateNum = LocalDate.ofEpochDay(dayEpoch).dayOfMonth
                                Text(
                                    text = "$dateNum",
                                    style = ProstutiTypography.monoSmall,
                                    color = if (isSelected) colors.blue else colors.ink
                                )

                                // Vertical load bar
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(48.dp)
                                        .background(colors.rule.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    val loadFraction = if (capacity > 0) (totalBlocks.toFloat() / capacity.toFloat()).coerceIn(0f, 1f) else 0f
                                    val barColor = when {
                                        isOverCap -> colors.vermilion
                                        loadFraction > 0.7f -> colors.blue
                                        loadFraction > 0.2f -> colors.blueSoft
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height((48 * loadFraction).dp)
                                            .background(barColor, RoundedCornerShape(2.dp))
                                    )
                                }

                                // Today underline
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(3.dp)
                                            .background(colors.blue, RoundedCornerShape(1.5.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }

                // Selected Day Sessions as Blocks
                val selectedDayIso = DateUtil.epochDayToIso(selectedDayEpoch)
                val selectedSessions = sessions.filter { it.date == selectedDayIso }

                item(key = "selected_day_header") {
                    Text(
                        text = "${DateUtil.formatDayOfWeekDayMonth(selectedDayEpoch)} (${selectedSessions.sumOf { it.blocks }} blocks planned)",
                        style = ProstutiTypography.bodyLarge,
                        color = colors.ink,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (selectedSessions.isEmpty()) {
                    item(key = "selected_day_empty") {
                        Text(
                            text = "No sessions scheduled for this day.",
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink3,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(selectedSessions, key = { it.id }) { session ->
                        val topic = topicMap[session.topicId]
                        val subject = topic?.let { subjectMap[it.subjectId] }
                        val subjectColor = getSubjectColor(subject?.colorIndex ?: 0, colors.isDark)

                        // 18% pigment fill + 1dp pigment border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(subjectColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                .border(1.dp, subjectColor, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSessionClick(session) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = topic?.title ?: "Topic",
                                            style = ProstutiTypography.bodyLarge,
                                            color = colors.ink
                                        )
                                        if (session.pinned) {
                                            Text(
                                                text = "📌",
                                                style = ProstutiTypography.caption
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${subject?.name ?: "Subject"} • ${session.blocks} ${if (session.blocks == 1) "block" else "blocks"}",
                                        style = ProstutiTypography.caption,
                                        color = colors.ink2
                                    )
                                }
                                SessionKindChip(kind = session.kind)
                            }
                        }
                    }
                }
            } else {
                // MONTH VIEW: 35/42 Cell Calendar Grid
                item(key = "month_calendar_grid") {
                    val currentLocalDate = LocalDate.ofEpochDay(selectedDayEpoch)
                    val firstDayOfMonth = currentLocalDate.withDayOfMonth(1)
                    val daysInMonth = currentLocalDate.lengthOfMonth()
                    val startOffset = DateUtil.dayOfWeekIndex(firstDayOfMonth.toEpochDay())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (colors.isDark) colors.rule else colors.ink, RoundedCornerShape(6.dp))
                            .background(colors.card, RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Month Header
                        Text(
                            text = "${firstDayOfMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${firstDayOfMonth.year}",
                            style = ProstutiTypography.h3,
                            color = colors.ink,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // 7 weekday labels
                        Row(modifier = Modifier.fillMaxWidth()) {
                            (0..6).forEach { i ->
                                Text(
                                    text = DateUtil.dayOfWeekShortName(i),
                                    style = ProstutiTypography.caption,
                                    color = colors.ink3,
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Days grid
                        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
                        for (row in 0 until (totalCells / 7)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val cellDate = firstDayOfMonth.withDayOfMonth(dayNum)
                                        val cellEpoch = cellDate.toEpochDay()
                                        val cellIso = cellDate.toString()
                                        val daySessions = sessions.filter { it.date == cellIso }
                                        val totalBlocks = daySessions.sumOf { it.blocks }
                                        val cap = getDayCapacity(cellEpoch)
                                        val examOnDay = subjects.find { it.examDate == cellIso }

                                        val cellBg = when {
                                            cap > 0 && totalBlocks > 0 -> {
                                                val f = (totalBlocks.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
                                                if (f > 0.5f) colors.blueSoft else colors.paper
                                            }
                                            else -> colors.paper
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .padding(1.dp)
                                                .background(cellBg, RoundedCornerShape(3.dp))
                                                .clickable { selectedDayEpoch = cellEpoch },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = ProstutiTypography.caption,
                                                    color = if (cellEpoch == todayEpoch) colors.blue else colors.ink
                                                )
                                                if (examOnDay != null) {
                                                    // Exam day dot and first letter
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(colors.vermilion, RoundedCornerShape(2.dp))
                                                        )
                                                        Text(
                                                            text = examOnDay.name.take(1),
                                                            style = ProstutiTypography.caption,
                                                            color = colors.vermilion
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
