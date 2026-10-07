package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ProstutiApp
import com.example.data.AppRepository
import com.example.data.DataStoreManager
import com.example.model.Card
import com.example.model.PastPaper
import com.example.model.PomodoroLog
import com.example.model.Problem
import com.example.model.Session
import com.example.model.Snapshot
import com.example.model.Subject
import com.example.model.Topic
import com.example.model.UserAvailability
import com.example.model.UserSettings
import com.example.scheduler.Scheduler
import com.example.scheduler.SchedulerAvailability
import com.example.scheduler.SchedulerOverload
import com.example.util.DateUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

sealed class SubScreen {
    data object None : SubScreen()
    data class TopicDetail(val topicId: String) : SubScreen()
    data class Review(val topicId: String? = null) : SubScreen()
    data class Practice(val topicId: String? = null) : SubScreen()
    data object Settings : SubScreen()
    data object About : SubScreen()
    data object Onboarding : SubScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ProstutiApp
    val repository: AppRepository = app.repository
    val dataStoreManager: DataStoreManager = app.dataStoreManager

    val timerManager = TimerManager(
        context = application,
        dataStoreManager = dataStoreManager,
        repository = repository,
        scope = viewModelScope
    )

    // Data Flows
    val subjects: StateFlow<List<Subject>> = repository.allSubjectsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val topics: StateFlow<List<Topic>> = repository.allTopicsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sessions: StateFlow<List<Session>> = repository.allSessionsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val cards: StateFlow<List<Card>> = repository.allCardsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val problems: StateFlow<List<Problem>> = repository.allProblemsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pastPapers: StateFlow<List<PastPaper>> = repository.allPastPapersFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pomodoroLogs: StateFlow<List<PomodoroLog>> = repository.allPomodoroLogsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val snapshots: StateFlow<List<Snapshot>> = repository.allSnapshotsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val availability: StateFlow<UserAvailability> = dataStoreManager.availabilityFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserAvailability()
    )

    val settings: StateFlow<UserSettings> = dataStoreManager.settingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings()
    )

    // UI Navigation State
    private val _currentTab = MutableStateFlow("today")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _currentSubScreen = MutableStateFlow<SubScreen>(SubScreen.None)
    val currentSubScreen: StateFlow<SubScreen> = _currentSubScreen.asStateFlow()

    // Overload & Undo Notification
    private val _currentOverload = MutableStateFlow<SchedulerOverload?>(null)
    val currentOverload: StateFlow<SchedulerOverload?> = _currentOverload.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var undoSnapshotTimestamp: Long = 0L
    private var lastSnapshotId: String? = null

    init {
        viewModelScope.launch {
            dataStoreManager.settingsFlow.collect { s ->
                if (!s.onboardingDone && _currentSubScreen.value is SubScreen.None) {
                    _currentSubScreen.value = SubScreen.Onboarding
                }
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun openTopicDetail(topicId: String) {
        _currentSubScreen.value = SubScreen.TopicDetail(topicId)
    }

    fun openReview(topicId: String? = null) {
        _currentSubScreen.value = SubScreen.Review(topicId)
    }

    fun openPractice(topicId: String? = null) {
        _currentSubScreen.value = SubScreen.Practice(topicId)
    }

    fun openSettings() {
        _currentSubScreen.value = SubScreen.Settings
    }

    fun openAbout() {
        _currentSubScreen.value = SubScreen.About
    }

    fun closeSubScreen() {
        _currentSubScreen.value = SubScreen.None
    }

    fun showSnackbar(message: String, snapshotIdForUndo: String? = null) {
        _snackbarMessage.value = message
        if (snapshotIdForUndo != null) {
            lastSnapshotId = snapshotIdForUndo
            undoSnapshotTimestamp = System.currentTimeMillis()
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun isUndoAvailable(): Boolean {
        return lastSnapshotId != null && (System.currentTimeMillis() - undoSnapshotTimestamp < 3600000L) // 1 hour
    }

    fun undo() {
        val snapId = lastSnapshotId ?: return
        viewModelScope.launch {
            repository.restoreSnapshot(snapId)
            lastSnapshotId = null
            _snackbarMessage.value = "Restored previous state."
        }
    }

    private fun getSchedulerAvailability(): SchedulerAvailability {
        val avail = availability.value
        val sett = settings.value
        val startDay = try {
            DayOfWeek.valueOf(sett.weekStart.uppercase())
        } catch (e: Exception) {
            DayOfWeek.SATURDAY
        }
        val overridesMap = avail.overrides.mapKeys { DateUtil.isoToEpochDay(it.key) }
        return SchedulerAvailability(
            weekdayMinutes = avail.weekdayMinutes,
            overrides = overridesMap,
            focusMin = sett.focusMin,
            bufferDays = sett.bufferDays,
            startDayOfWeek = startDay
        )
    }

    fun regeneratePlan(reason: String = "Plan updated") {
        viewModelScope.launch {
            val oldSessions = sessions.value
            val schedulerAvail = getSchedulerAvailability()
            val result = repository.regeneratePlan(
                todayEpochDay = DateUtil.todayEpochDay(),
                availability = schedulerAvail,
                reason = reason
            )
            _currentOverload.value = result.overloads.firstOrNull()
        }
    }

    fun toggleSessionDone(session: Session) {
        viewModelScope.launch {
            val newStatus = if (session.status == "done") "pending" else "done"
            repository.updateSession(session.copy(status = newStatus))
            // Also update topic blocksDone
            val topic = repository.getTopicById(session.topicId)
            if (topic != null) {
                val delta = if (newStatus == "done") session.blocks else -session.blocks
                val newDone = (topic.blocksDone + delta).coerceAtLeast(0)
                repository.updateTopic(topic.copy(blocksDone = newDone))
            }
        }
    }

    fun rescheduleMissedToday() {
        viewModelScope.launch {
            val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
            val todaySessions = sessions.value.filter { it.date == todayIso && it.status == "pending" }
            if (todaySessions.isEmpty()) return@launch

            repository.createSnapshot("Before reschedule missed today")
            val snap = snapshots.value.firstOrNull()

            // Mark today pending sessions as missed
            for (s in todaySessions) {
                repository.updateSession(s.copy(status = "missed"))
            }

            val oldSessions = sessions.value
            val schedulerAvail = getSchedulerAvailability()
            val result = repository.regeneratePlan(
                todayEpochDay = DateUtil.todayEpochDay(),
                availability = schedulerAvail,
                reason = "Rescheduled missed today"
            )

            val subjMap = subjects.value.associate {
                it.id to com.example.scheduler.SchedulerSubject(it.id, it.name, it.colorIndex, it.examDateEpochDay)
            }
            val diffMsg = Scheduler.computeRescheduleDiffSummary(
                oldSessions = oldSessions.map {
                    com.example.scheduler.SchedulerSession(
                        it.id, it.dateEpochDay, it.topicId,
                        topics.value.find { t -> t.id == it.topicId }?.subjectId ?: "",
                        it.kind, it.blocks, it.status, it.pinned, it.createdBy
                    )
                },
                newSessions = result.newSessions,
                subjects = subjMap,
                focusMin = settings.value.focusMin
            )

            showSnackbar(diffMsg, snap?.id)
        }
    }

    fun applyLightDay() {
        viewModelScope.launch {
            val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
            val currentAvail = availability.value
            val idx = DateUtil.dayOfWeekIndex(DateUtil.todayEpochDay())
            val defaultMin = currentAvail.weekdayMinutes.getOrElse(idx) { 180 }
            val halfMin = defaultMin / 2
            val updatedOverrides = currentAvail.overrides.toMutableMap()
            updatedOverrides[todayIso] = halfMin

            val newAvail = currentAvail.copy(overrides = updatedOverrides)
            dataStoreManager.saveAvailability(newAvail)
            regeneratePlan("Light day applied")
            showSnackbar("Light day set: $halfMin minutes available today.")
        }
    }

    fun applyPartialDay(session: Session, minutesDone: Int) {
        viewModelScope.launch {
            val focusMin = settings.value.focusMin
            val blocksDone = minutesDone / focusMin
            val remainderBlocks = (session.blocks - blocksDone).coerceAtLeast(0)

            repository.createSnapshot("Partial session completion")
            val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())

            if (blocksDone > 0) {
                repository.insertPomodoroLog(
                    PomodoroLog(
                        id = UUID.randomUUID().toString(),
                        date = todayIso,
                        minutes = minutesDone,
                        sessionId = session.id
                    )
                )
            }

            if (remainderBlocks == 0) {
                repository.updateSession(session.copy(status = "done", actualMinutes = minutesDone))
            } else {
                repository.updateSession(session.copy(blocks = blocksDone, status = "done", actualMinutes = minutesDone))
                // Return remainder to queue
                repository.insertSession(
                    Session(
                        id = UUID.randomUUID().toString(),
                        date = DateUtil.epochDayToIso(DateUtil.todayEpochDay() + 1),
                        topicId = session.topicId,
                        kind = session.kind,
                        blocks = remainderBlocks,
                        status = "pending",
                        pinned = false,
                        createdBy = "auto"
                    )
                )
            }
            regeneratePlan("After partial day session")
        }
    }

    fun catchUpMoveSessions() {
        viewModelScope.launch {
            val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())
            val overdue = sessions.value.filter { it.date < todayIso && it.status == "pending" }
            for (s in overdue) {
                repository.updateSession(s.copy(status = "missed"))
            }
            regeneratePlan("Catch up overdue sessions")
            showSnackbar("Overdue sessions moved to remaining days.")
        }
    }

    // Overload Actions
    fun resolveOverloadAdd30() {
        viewModelScope.launch {
            val currentAvail = availability.value
            val newWeekdays = currentAvail.weekdayMinutes.map { it + 30 }
            val newAvail = currentAvail.copy(weekdayMinutes = newWeekdays)
            dataStoreManager.saveAvailability(newAvail)
            _currentOverload.value = null
            regeneratePlan("Added 30 minutes to daily study hours")
            showSnackbar("Added 30 min to daily study hours.")
        }
    }

    fun resolveOverloadDropTopic(topicId: String) {
        viewModelScope.launch {
            val topic = repository.getTopicById(topicId)
            if (topic != null) {
                repository.deleteTopic(topic)
                _currentOverload.value = null
                regeneratePlan("Dropped topic: ${topic.title}")
                showSnackbar("Topic dropped and plan recalculated.")
            }
        }
    }

    fun resolveOverloadKeep() {
        _currentOverload.value = null
        showSnackbar("Plan kept as-is with unplaced work listed.")
    }

    // Subject & Topic CRUD
    fun addSubject(name: String, examDate: String, examTime: String?, weight: Int, colorIndex: Int) {
        viewModelScope.launch {
            val subject = Subject(
                id = UUID.randomUUID().toString(),
                name = name,
                colorIndex = colorIndex,
                examDate = examDate,
                examTime = examTime,
                weight = weight
            )
            repository.insertSubject(subject)
            regeneratePlan("Subject added: $name")
        }
    }

    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            val snap = snapshots.value.firstOrNull()
            repository.deleteSubject(subjectId)
            regeneratePlan("Subject deleted")
            showSnackbar("Subject deleted.", snap?.id)
        }
    }

    fun addTopicsToSubject(subjectId: String, topicItems: List<Pair<String, Int>>) {
        viewModelScope.launch {
            val currentCount = topics.value.filter { it.subjectId == subjectId }.size
            val newTopics = topicItems.mapIndexed { index, pair ->
                val diff = pair.second.coerceIn(1, 5)
                Topic(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    title = pair.first,
                    difficulty = diff,
                    estBlocks = Scheduler.defaultEstBlocks(diff),
                    order = currentCount + index + 1
                )
            }
            repository.insertTopics(newTopics)
            regeneratePlan("Topics added to subject")
        }
    }
}
