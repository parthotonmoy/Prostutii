package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.model.Session
import com.example.model.Topic
import com.example.ui.components.DraftingOutlinedButton
import com.example.ui.components.DraftingPrimaryButton
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AddSubjectSheet
import com.example.ui.screens.AddTopicsSheet
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ReviewScreen
import com.example.ui.screens.SessionActionSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.TopicDetailScreen
import com.example.ui.screens.UnstickSheet
import com.example.ui.theme.ProstutiAppTheme
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography
import com.example.util.DateUtil
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SubScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val timerState by viewModel.timerManager.currentTimerState.collectAsState()

            // Keep screen on while timer is running
            LaunchedEffect(timerState.phase, timerState.pausedRemainingMs) {
                if (timerState.phase != "idle" && timerState.pausedRemainingMs == null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            ProstutiAppTheme(themeSetting = settings.theme) {
                ProstutiMainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ProstutiMainApp(viewModel: MainViewModel) {
    val colors = ProstutiTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    val currentTab by viewModel.currentTab.collectAsState()
    val subScreen by viewModel.currentSubScreen.collectAsState()

    val subjects by viewModel.subjects.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val problems by viewModel.problems.collectAsState()
    val pastPapers by viewModel.pastPapers.collectAsState()
    val pomodoroLogs by viewModel.pomodoroLogs.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val availability by viewModel.availability.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val overload by viewModel.currentOverload.collectAsState()

    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    // Sheet states
    var showAddSubjectSheet by remember { mutableStateOf(false) }
    var addTopicsForSubjectId by remember { mutableStateOf<String?>(null) }
    var selectedSessionForAction by remember { mutableStateOf<Session?>(null) }
    var showUnstickForSession by remember { mutableStateOf<Session?>(null) }
    var showMissedTodayConfirm by remember { mutableStateOf(false) }
    var partialSessionTarget by remember { mutableStateOf<Session?>(null) }
    var partialMinutesInput by remember { mutableStateOf("25") }

    // Snackbar effect
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            val hasUndo = viewModel.isUndoAvailable()
            val actionLabel = if (hasUndo) "Undo" else null
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = actionLabel,
                duration = if (hasUndo) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            }
            viewModel.dismissSnackbar()
        }
    }

    // Back handling for sub-screens
    if (subScreen !is SubScreen.None) {
        BackHandler {
            viewModel.closeSubScreen()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (subScreen is SubScreen.None) {
                ProstutiBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = subScreen) {
                is SubScreen.Onboarding -> {
                    OnboardingScreen(
                        availability = availability,
                        onSaveAvailability = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.dataStoreManager.saveAvailability(it)
                            }
                        },
                        onCompleteOnboarding = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.dataStoreManager.saveSettings(settings.copy(onboardingDone = true))
                                viewModel.closeSubScreen()
                            }
                        },
                        onLoadSampleData = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.loadSampleData()
                                viewModel.dataStoreManager.saveSettings(settings.copy(onboardingDone = true))
                                viewModel.closeSubScreen()
                            }
                        },
                        onAddFirstSubject = { name, date ->
                            viewModel.addSubject(name, date, null, 2, 0)
                        }
                    )
                }
                is SubScreen.Settings -> {
                    SettingsScreen(
                        settings = settings,
                        availability = availability,
                        snapshots = snapshots,
                        onUpdateSettings = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.dataStoreManager.saveSettings(it)
                            }
                        },
                        onUpdateAvailability = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.dataStoreManager.saveAvailability(it)
                            }
                        },
                        onRestoreSnapshot = { snapId ->
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.restoreSnapshot(snapId)
                                viewModel.showSnackbar("Restored snapshot.")
                            }
                        },
                        onLoadSampleData = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.loadSampleData()
                                viewModel.showSnackbar("Sample data loaded.")
                            }
                        },
                        onRemoveSampleData = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.removeSampleData(
                                    DateUtil.todayEpochDay(),
                                    com.example.scheduler.SchedulerAvailability(
                                        weekdayMinutes = availability.weekdayMinutes,
                                        focusMin = settings.focusMin,
                                        bufferDays = settings.bufferDays
                                    )
                                )
                                viewModel.showSnackbar("Sample data removed.")
                            }
                        },
                        onResetAllData = {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.resetAllData()
                                viewModel.showSnackbar("All data reset.")
                            }
                        },
                        onExportBackup = { uri ->
                            viewModel.viewModelScopeLaunch {
                                try {
                                    val json = viewModel.repository.exportToJson()
                                    viewModel.getApplication<ProstutiApp>().contentResolver.openOutputStream(uri)?.use { os ->
                                        os.write(json.toByteArray())
                                    }
                                    viewModel.showSnackbar("Backup exported successfully.")
                                } catch (e: Exception) {
                                    viewModel.showSnackbar("Export failed: ${e.message}")
                                }
                            }
                        },
                        onImportBackupData = { json, mode ->
                            viewModel.viewModelScopeLaunch {
                                val res = viewModel.repository.importBackup(
                                    backupJson = json,
                                    mode = mode,
                                    todayEpochDay = DateUtil.todayEpochDay(),
                                    availability = com.example.scheduler.SchedulerAvailability(
                                        weekdayMinutes = availability.weekdayMinutes,
                                        focusMin = settings.focusMin,
                                        bufferDays = settings.bufferDays
                                    )
                                )
                                if (res.isSuccess) {
                                    viewModel.showSnackbar(res.getOrNull() ?: "Import complete.")
                                } else {
                                    viewModel.showSnackbar(res.exceptionOrNull()?.message ?: "Import failed.")
                                }
                            }
                        },
                        onOpenAbout = { viewModel.openAbout() },
                        onBack = { viewModel.closeSubScreen() }
                    )
                }
                is SubScreen.About -> {
                    AboutScreen(onBack = { viewModel.closeSubScreen() })
                }
                is SubScreen.TopicDetail -> {
                    val topic = topics.find { it.id == screen.topicId }
                    if (topic != null) {
                        val topicCards = cards.filter { it.topicId == topic.id }
                        val topicProblems = problems.filter { it.topicId == topic.id }

                        TopicDetailScreen(
                            topic = topic,
                            cards = topicCards,
                            problems = topicProblems,
                            onUpdateTopic = {
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.updateTopic(it)
                                    viewModel.regeneratePlan("Topic updated")
                                }
                            },
                            onDeleteTopic = {
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.deleteTopic(it)
                                    viewModel.regeneratePlan("Topic deleted")
                                    viewModel.closeSubScreen()
                                }
                            },
                            onAddCard = { front, back ->
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.insertCard(
                                        com.example.model.Card(
                                            id = java.util.UUID.randomUUID().toString(),
                                            topicId = topic.id,
                                            front = front,
                                            back = back,
                                            due = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
                                        )
                                    )
                                }
                            },
                            onDeleteCard = {
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.deleteCard(it)
                                }
                            },
                            onAddProblem = { prompt, solution, marks, source ->
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.insertProblem(
                                        com.example.model.Problem(
                                            id = java.util.UUID.randomUUID().toString(),
                                            topicId = topic.id,
                                            prompt = prompt,
                                            solution = solution,
                                            marks = marks,
                                            source = source
                                        )
                                    )
                                }
                            },
                            onDeleteProblem = {
                                viewModel.viewModelScopeLaunch {
                                    viewModel.repository.deleteProblem(it)
                                }
                            },
                            onReviewTopicCards = {
                                viewModel.openReview(topic.id)
                            },
                            onPracticeTopicProblems = {
                                viewModel.openPractice(topic.id)
                            },
                            onBack = { viewModel.closeSubScreen() }
                        )
                    } else {
                        viewModel.closeSubScreen()
                    }
                }
                is SubScreen.Review -> {
                    val dueToday = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
                    val reviewCards = if (screen.topicId != null) {
                        cards.filter { it.topicId == screen.topicId }
                    } else {
                        cards.filter { it.due <= dueToday }
                    }

                    ReviewScreen(
                        dueCards = reviewCards,
                        topics = topics,
                        onCardReviewed = { card, newDue, newEase, newInt, newReps, newLapses ->
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.updateCard(
                                    card.copy(
                                        due = newDue,
                                        ease = newEase,
                                        intervalDays = newInt,
                                        reps = newReps,
                                        lapses = newLapses
                                    )
                                )
                            }
                        },
                        onTopicMarkRevised = { top ->
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.updateTopic(top.copy(status = "revised"))
                                viewModel.regeneratePlan("Topic marked revised")
                            }
                        },
                        onClose = { viewModel.closeSubScreen() }
                    )
                }
                is SubScreen.Practice -> {
                    val targetTopic = if (screen.topicId != null) topics.find { it.id == screen.topicId } else null
                    val practiceProblems = if (targetTopic != null) {
                        problems.filter { it.topicId == targetTopic.id }
                    } else {
                        problems
                    }

                    PracticeScreen(
                        topic = targetTopic,
                        problems = practiceProblems,
                        onProblemAttempted = { problem, result ->
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.updateProblem(
                                    problem.copy(attempts = problem.attempts + 1, lastResult = result)
                                )
                            }
                        },
                        onTopicConfidenceUpdated = { top, newConfidence, newStatus ->
                            viewModel.viewModelScopeLaunch {
                                val updated = if (newStatus != null) {
                                    top.copy(confidence = newConfidence, status = newStatus)
                                } else {
                                    top.copy(confidence = newConfidence)
                                }
                                viewModel.repository.updateTopic(updated)
                            }
                        },
                        onAddProblemClick = {
                            if (targetTopic != null) {
                                viewModel.openTopicDetail(targetTopic.id)
                            }
                        },
                        onClose = { viewModel.closeSubScreen() }
                    )
                }
                is SubScreen.None -> {
                    when (currentTab) {
                        "today" -> {
                            TodayScreen(
                                subjects = subjects,
                                topics = topics,
                                sessions = sessions,
                                cards = cards,
                                onToggleSessionDone = { viewModel.toggleSessionDone(it) },
                                onSessionSelected = { selectedSessionForAction = it },
                                onOpenSettings = { viewModel.openSettings() },
                                onOpenReview = { viewModel.openReview() },
                                onMissedTodayClick = { showMissedTodayConfirm = true },
                                onLightDayClick = { viewModel.applyLightDay() },
                                onCatchUpMoveClick = { viewModel.catchUpMoveSessions() }
                            )
                        }
                        "plan" -> {
                            PlanScreen(
                                subjects = subjects,
                                topics = topics,
                                sessions = sessions,
                                availability = availability,
                                overload = overload,
                                onResolveOverloadAdd30 = { viewModel.resolveOverloadAdd30() },
                                onResolveOverloadDropTopic = {
                                    val firstTopic = topics.firstOrNull()
                                    if (firstTopic != null) viewModel.resolveOverloadDropTopic(firstTopic.id)
                                },
                                onResolveOverloadKeep = { viewModel.resolveOverloadKeep() },
                                onSessionClick = { selectedSessionForAction = it }
                            )
                        }
                        "focus" -> {
                            val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
                            val todaySessions = sessions.filter { it.date == todayIso }
                            val todayLogs = pomodoroLogs.filter { it.date == todayIso }

                            FocusScreen(
                                timerManager = viewModel.timerManager,
                                todaySessions = todaySessions,
                                topics = topics,
                                todayPomodoroLogs = todayLogs
                            )
                        }
                        "subjects" -> {
                            SubjectsScreen(
                                subjects = subjects,
                                topics = topics,
                                cards = cards,
                                problems = problems,
                                onSubjectClick = { subj ->
                                    val firstTopic = topics.find { it.subjectId == subj.id }
                                    if (firstTopic != null) {
                                        viewModel.openTopicDetail(firstTopic.id)
                                    } else {
                                        addTopicsForSubjectId = subj.id
                                    }
                                },
                                onAddSubjectClick = { showAddSubjectSheet = true }
                            )
                        }
                        "progress" -> {
                            ProgressScreen(
                                subjects = subjects,
                                topics = topics,
                                sessions = sessions,
                                cards = cards,
                                problems = problems,
                                pastPapers = pastPapers,
                                pomodoroLogs = pomodoroLogs,
                                onClearTopicHelp = { top ->
                                    viewModel.viewModelScopeLaunch {
                                        viewModel.repository.updateTopic(top.copy(needsHelp = false))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Add Subject Sheet
            if (showAddSubjectSheet) {
                AddSubjectSheet(
                    onDismiss = { showAddSubjectSheet = false },
                    onConfirm = { name, examDate, examTime, weight, colorIdx ->
                        viewModel.addSubject(name, examDate, examTime, weight, colorIdx)
                    }
                )
            }

            // Add Topics Sheet
            addTopicsForSubjectId?.let { subjId ->
                AddTopicsSheet(
                    onDismiss = { addTopicsForSubjectId = null },
                    onConfirm = { items ->
                        viewModel.addTopicsToSubject(subjId, items)
                    }
                )
            }

            // Session Action Sheet
            selectedSessionForAction?.let { sess ->
                val topic = topics.find { it.id == sess.topicId }
                SessionActionSheet(
                    session = sess,
                    topic = topic,
                    onDismiss = { selectedSessionForAction = null },
                    onStartFocus = {
                        viewModel.timerManager.startTimer(linkedSessionId = sess.id)
                        viewModel.setTab("focus")
                    },
                    onOpenTopic = {
                        if (topic != null) viewModel.openTopicDetail(topic.id)
                    },
                    onOpenUnstick = {
                        showUnstickForSession = sess
                    },
                    onMoveSession = { daysAhead ->
                        viewModel.viewModelScopeLaunch {
                            val newDateIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay() + daysAhead)
                            viewModel.repository.updateSession(sess.copy(date = newDateIso, pinned = true))
                            viewModel.regeneratePlan("Moved session to $newDateIso")
                        }
                    },
                    onPinSession = {
                        viewModel.viewModelScopeLaunch {
                            viewModel.repository.updateSession(sess.copy(pinned = !sess.pinned))
                        }
                    },
                    onPartialDay = {
                        partialSessionTarget = sess
                    },
                    onSkipSession = {
                        viewModel.viewModelScopeLaunch {
                            viewModel.repository.updateSession(sess.copy(status = "skipped"))
                        }
                    },
                    onStartPractice = {
                        viewModel.openPractice(sess.topicId)
                    }
                )
            }

            // Unstick Guide Sheet
            showUnstickForSession?.let { sess ->
                val topic = topics.find { it.id == sess.topicId }
                UnstickSheet(
                    session = sess,
                    topic = topic,
                    onDismiss = { showUnstickForSession = null },
                    onAddCardClick = {
                        if (topic != null) viewModel.openTopicDetail(topic.id)
                    },
                    onMarkNeedsHelp = {
                        if (topic != null) {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.updateTopic(topic.copy(needsHelp = true))
                                viewModel.showSnackbar("Added to teacher list.")
                            }
                        }
                    },
                    onAddBlockAndRegenerate = {
                        if (topic != null) {
                            viewModel.viewModelScopeLaunch {
                                viewModel.repository.updateTopic(topic.copy(estBlocks = topic.estBlocks + 1))
                                viewModel.regeneratePlan("Added 1 block to ${topic.title}")
                            }
                        }
                    }
                )
            }

            // "I missed today" Confirm Sheet
            if (showMissedTodayConfirm) {
                val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
                val todayPending = sessions.filter { it.date == todayIso && it.status == "pending" }
                val totalMin = todayPending.sumOf { it.blocks } * settings.focusMin
                val durationStr = DateUtil.formatDuration(totalMin)

                AlertDialog(
                    onDismissRequest = { showMissedTodayConfirm = false },
                    title = { Text("Reschedule today's work?", style = ProstutiTypography.h3, color = colors.ink) },
                    text = {
                        Text(
                            text = "That happens. I'll move today's ${todayPending.size} sessions ($durationStr) into the days you have left, and tell you exactly what changed.",
                            style = ProstutiTypography.bodyLarge,
                            color = colors.ink2
                        )
                    },
                    confirmButton = {
                        DraftingPrimaryButton(
                            text = "Reschedule",
                            onClick = {
                                viewModel.rescheduleMissedToday()
                                showMissedTodayConfirm = false
                            }
                        )
                    },
                    dismissButton = {
                        DraftingOutlinedButton(
                            text = "Cancel",
                            onClick = { showMissedTodayConfirm = false }
                        )
                    },
                    containerColor = colors.card
                )
            }

            // "I did part of it" Dialog
            partialSessionTarget?.let { sess ->
                AlertDialog(
                    onDismissRequest = { partialSessionTarget = null },
                    title = { Text("How many minutes did you do?", style = ProstutiTypography.h3, color = colors.ink) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("The remainder returns to the queue.", style = ProstutiTypography.bodyMedium, color = colors.ink2)
                            OutlinedTextField(
                                value = partialMinutesInput,
                                onValueChange = { partialMinutesInput = it },
                                label = { Text("Minutes completed") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        DraftingPrimaryButton(
                            text = "Confirm",
                            onClick = {
                                val mins = partialMinutesInput.toIntOrNull() ?: 25
                                viewModel.applyPartialDay(sess, mins)
                                partialSessionTarget = null
                            }
                        )
                    },
                    dismissButton = {
                        DraftingOutlinedButton(
                            text = "Cancel",
                            onClick = { partialSessionTarget = null }
                        )
                    },
                    containerColor = colors.card
                )
            }
        }
    }
}

private fun MainViewModel.viewModelScopeLaunch(block: suspend () -> Unit) {
    this.launch { block() }
}

@Composable
fun ProstutiBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val colors = ProstutiTheme.colors
    val tabs = listOf(
        Triple("today", "Today", Icons.Default.DateRange),
        Triple("plan", "Plan", Icons.Default.List),
        Triple("focus", "Focus", Icons.Default.PlayArrow),
        Triple("subjects", "Subjects", Icons.Default.Menu),
        Triple("progress", "Progress", Icons.Default.Check)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.card)
            .border(width = 1.dp, color = colors.ink)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tabId, label, icon) ->
                val isSelected = currentTab == tabId
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeightIfPossible()
                        .clickable { onTabSelected(tabId) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 3dp blue bar above icon when active
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .background(colors.blue, RoundedCornerShape(1.5.dp))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) colors.blue else colors.ink3,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = ProstutiTypography.caption,
                        color = if (isSelected) colors.blue else colors.ink3
                    )
                }
            }
        }
    }
}

private fun Modifier.fillMaxHeightIfPossible(): Modifier = this.then(Modifier.padding(vertical = 2.dp))
