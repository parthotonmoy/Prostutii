package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.PomodoroLog
import com.example.model.Session
import com.example.model.Topic
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.components.MathText
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.util.DateUtil
import com.example.viewmodel.TimerManager
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FocusScreen(
    timerManager: TimerManager,
    todaySessions: List<Session>,
    topics: List<Topic>,
    todayPomodoroLogs: List<PomodoroLog>,
    modifier: Modifier = Modifier
) {
    val colors = ProstutiTheme.colors
    val timerState by timerManager.currentTimerState.collectAsState()
    val remainingSec by timerManager.displayRemainingSec.collectAsState()
    val sessionDonePrompt by timerManager.sessionDonePrompt.collectAsState()

    val topicMap = remember(topics) { topics.associateBy { it.id } }
    val linkedSession = remember(timerState.sessionId, todaySessions) {
        todaySessions.find { it.id == timerState.sessionId }
    }
    val linkedTopic = remember(linkedSession, topicMap) {
        linkedSession?.let { topicMap[it.topicId] }
    }

    var showSessionPicker by remember { mutableStateOf(false) }

    // Request notification permission launcher
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission handled
    }

    val isBreak = timerState.phase == "break" || timerState.phase == "longBreak"
    val isRunning = timerState.phase != "idle" && timerState.pausedRemainingMs == null

    val minutes = remainingSec / 60
    val seconds = remainingSec % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val pageBg = if (isBreak) colors.mossSoft else colors.paper

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Pinned Formula Sheet / Session Chip
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // If topic has pinned formula sheet
                if (linkedTopic != null && linkedTopic.formulaSheet.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.card, RoundedCornerShape(6.dp))
                            .border(1.dp, colors.rule, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Formula Sheet: ${linkedTopic.title}",
                                style = ProstutiTypography.caption,
                                color = colors.ink2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            MathText(
                                text = linkedTopic.formulaSheet,
                                style = ProstutiTypography.bodyLarge,
                                color = colors.ink
                            )
                        }
                    }
                }

                // Linked session chip
                val chipText = if (linkedTopic != null) {
                    "Session: ${linkedTopic.title}"
                } else {
                    "No session (free focus)"
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                        .background(colors.card, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showSessionPicker = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chipText,
                        style = ProstutiTypography.bodyMedium,
                        color = colors.ink
                    )
                }
            }

            // Middle Section: Circular Dial of 60 ticks & Time Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = (size.width / 2f) - 16.dp.toPx()
                        val tickLen = 10.dp.toPx()

                        val totalPhaseSec = when (timerState.phase) {
                            "break" -> 5 * 60
                            "longBreak" -> 15 * 60
                            else -> 25 * 60
                        }
                        val elapsedSec = (totalPhaseSec - remainingSec).coerceAtLeast(0)
                        val elapsedFraction = (elapsedSec.toFloat() / totalPhaseSec.toFloat()).coerceIn(0f, 1f)
                        val elapsedTicks = (60 * elapsedFraction).toInt()

                        val activeColor = if (isBreak) colors.moss else colors.blue

                        for (i in 0 until 60) {
                            val angleRad = Math.toRadians((i * 6.0) - 90.0)
                            val cosA = cos(angleRad).toFloat()
                            val sinA = sin(angleRad).toFloat()

                            val isTickElapsed = i < elapsedTicks
                            val tickColor = if (isTickElapsed) activeColor else colors.rule
                            val strokeW = if (isTickElapsed) 2.5f else 1.2f

                            val startX = cx + (radius - tickLen) * cosA
                            val startY = cy + (radius - tickLen) * sinA
                            val endX = cx + radius * cosA
                            val endY = cy + radius * sinA

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeW
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            style = ProstutiTypography.monoHuge,
                            color = colors.ink
                        )
                        val phaseLabel = when (timerState.phase) {
                            "break" -> "Short break"
                            "longBreak" -> "Long break"
                            else -> "Focus ${timerState.cycleCount} of 4"
                        }
                        Text(
                            text = phaseLabel,
                            style = ProstutiTypography.bodyMedium,
                            color = colors.ink2
                        )
                    }
                }

                // Completed Pomodoros row (small filled squares in blue)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val completedCount = todayPomodoroLogs.size
                    if (completedCount == 0) {
                        Text(
                            text = "No blocks completed today yet",
                            style = ProstutiTypography.caption,
                            color = colors.ink3
                        )
                    } else {
                        (1..completedCount.coerceAtMost(12)).forEach { _ ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colors.blue, RoundedCornerShape(1.dp))
                            )
                        }
                        if (completedCount > 12) {
                            Text(
                                text = "+${completedCount - 12}",
                                style = ProstutiTypography.monoMicro,
                                color = colors.blue
                            )
                        }
                    }
                }

                // Battery note
                Text(
                    text = "If the phone sleeps the app, the time stays correct but the alert may arrive late.",
                    style = ProstutiTypography.caption,
                    color = colors.ink3,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Bottom Section: Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isRunning) {
                        DraftingPrimaryButton(
                            text = if (timerState.phase == "idle") "Start" else "Resume",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                timerManager.startTimer()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        DraftingOutlinedButton(
                            text = "Pause",
                            onClick = { timerManager.pauseTimer() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DraftingOutlinedButton(
                        text = "Skip",
                        onClick = { timerManager.skipPhase() },
                        modifier = Modifier.weight(1f)
                    )

                    DraftingOutlinedButton(
                        text = "Reset",
                        onClick = { timerManager.resetTimer() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Session Done Prompt Dialog
        if (sessionDonePrompt != null) {
            val session = sessionDonePrompt!!
            val topic = topicMap[session.topicId]
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { timerManager.dismissSessionDonePrompt() },
                title = {
                    Text(
                        text = "Mark this session done?",
                        style = ProstutiTypography.h3,
                        color = colors.ink
                    )
                },
                text = {
                    Text(
                        text = "Completed ${session.blocks} blocks for ${topic?.title ?: "this topic"}.",
                        style = ProstutiTypography.bodyLarge,
                        color = colors.ink2
                    )
                },
                confirmButton = {
                    DraftingPrimaryButton(
                        text = "Mark done",
                        onClick = { timerManager.markPromptedSessionDone(session) }
                    )
                },
                dismissButton = {
                    DraftingOutlinedButton(
                        text = "Keep pending",
                        onClick = { timerManager.dismissSessionDonePrompt() }
                    )
                },
                containerColor = colors.card
            )
        }

        // Session Picker Dialog
        if (showSessionPicker) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSessionPicker = false },
                title = {
                    Text(
                        text = "Link to today's session",
                        style = ProstutiTypography.h3,
                        color = colors.ink
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                                .clickable {
                                    timerManager.startTimer(linkedSessionId = null)
                                    showSessionPicker = false
                                }
                                .padding(10.dp)
                        ) {
                            Text("No session (free focus)", style = ProstutiTypography.bodyMedium, color = colors.ink)
                        }

                        todaySessions.forEach { sess ->
                            val t = topicMap[sess.topicId]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.rule, RoundedCornerShape(4.dp))
                                    .clickable {
                                        timerManager.startTimer(linkedSessionId = sess.id)
                                        showSessionPicker = false
                                    }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${t?.title ?: "Topic"} (${sess.blocks} blocks)",
                                    style = ProstutiTypography.bodyMedium,
                                    color = colors.ink
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    DraftingOutlinedButton(
                        text = "Close",
                        onClick = { showSessionPicker = false }
                    )
                },
                containerColor = colors.card
            )
        }
    }
}
