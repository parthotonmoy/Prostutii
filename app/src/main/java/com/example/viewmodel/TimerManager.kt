package com.example.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AppRepository
import com.example.data.DataStoreManager
import com.example.model.PomodoroLog
import com.example.model.Session
import com.example.model.TimerState
import com.example.model.UserSettings
import com.example.receiver.TimerAlarmReceiver
import com.example.util.DateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class TimerManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val repository: AppRepository,
    private val scope: CoroutineScope
) {
    private val _displayRemainingSec = MutableStateFlow(25 * 60)
    val displayRemainingSec: StateFlow<Int> = _displayRemainingSec.asStateFlow()

    private val _currentTimerState = MutableStateFlow(TimerState())
    val currentTimerState: StateFlow<TimerState> = _currentTimerState.asStateFlow()

    private val _sessionDonePrompt = MutableStateFlow<Session?>(null)
    val sessionDonePrompt: StateFlow<Session?> = _sessionDonePrompt.asStateFlow()

    private var tickerJob: Job? = null
    private var cachedSettings: UserSettings = UserSettings()

    init {
        scope.launch {
            dataStoreManager.settingsFlow.collect { settings ->
                cachedSettings = settings
                updateDisplayFromState(_currentTimerState.value)
            }
        }

        scope.launch {
            dataStoreManager.timerStateFlow.collect { state ->
                _currentTimerState.value = state
                updateDisplayFromState(state)
                if (state.phase != "idle" && state.pausedRemainingMs == null) {
                    startTicker()
                } else {
                    stopTicker()
                }
            }
        }
    }

    private fun getPhaseDurationSec(phase: String): Int {
        return when (phase) {
            "focus" -> cachedSettings.focusMin * 60
            "break" -> cachedSettings.breakMin * 60
            "longBreak" -> cachedSettings.longBreakMin * 60
            else -> cachedSettings.focusMin * 60
        }
    }

    private fun updateDisplayFromState(state: TimerState) {
        if (state.phase == "idle") {
            _displayRemainingSec.value = getPhaseDurationSec("focus")
        } else if (state.pausedRemainingMs != null) {
            _displayRemainingSec.value = (state.pausedRemainingMs / 1000L).toInt().coerceAtLeast(0)
        } else {
            val now = System.currentTimeMillis()
            val remMs = state.endsAt - now
            _displayRemainingSec.value = (remMs / 1000L).toInt().coerceAtLeast(0)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val state = _currentTimerState.value
                if (state.phase != "idle" && state.pausedRemainingMs == null) {
                    val now = System.currentTimeMillis()
                    val remMs = state.endsAt - now
                    if (remMs <= 0) {
                        _displayRemainingSec.value = 0
                        onPhaseCompleted(state)
                        break
                    } else {
                        _displayRemainingSec.value = (remMs / 1000L).toInt()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun startTimer(linkedSessionId: String? = null) {
        val current = _currentTimerState.value
        val now = System.currentTimeMillis()

        val nextState: TimerState
        val targetDurationMs: Long

        if (current.phase != "idle" && current.pausedRemainingMs != null) {
            // Resume from pause
            targetDurationMs = current.pausedRemainingMs
            nextState = current.copy(
                endsAt = now + targetDurationMs,
                pausedRemainingMs = null
            )
        } else {
            // Start fresh
            val phase = if (current.phase == "idle") "focus" else current.phase
            val durationSec = getPhaseDurationSec(phase)
            targetDurationMs = durationSec * 1000L
            nextState = TimerState(
                phase = phase,
                endsAt = now + targetDurationMs,
                pausedRemainingMs = null,
                sessionId = linkedSessionId ?: current.sessionId,
                cycleCount = current.cycleCount
            )
        }

        scheduleAlarm(nextState.endsAt, nextState.phase)
        scope.launch {
            dataStoreManager.saveTimerState(nextState)
        }
    }

    fun pauseTimer() {
        val current = _currentTimerState.value
        if (current.phase == "idle" || current.pausedRemainingMs != null) return

        val now = System.currentTimeMillis()
        val remainingMs = (current.endsAt - now).coerceAtLeast(0L)
        cancelAlarm()

        val nextState = current.copy(
            pausedRemainingMs = remainingMs
        )
        scope.launch {
            dataStoreManager.saveTimerState(nextState)
        }
    }

    fun resetTimer() {
        cancelAlarm()
        stopTicker()
        val nextState = TimerState(
            phase = "idle",
            endsAt = 0L,
            pausedRemainingMs = null,
            sessionId = null,
            cycleCount = 1
        )
        scope.launch {
            dataStoreManager.saveTimerState(nextState)
        }
    }

    fun skipPhase() {
        cancelAlarm()
        val current = _currentTimerState.value
        onPhaseCompleted(current, recordMinutes = false)
    }

    private fun onPhaseCompleted(completedState: TimerState, recordMinutes: Boolean = true) {
        val wasFocus = completedState.phase == "focus"
        val todayIso = DateUtil.epochDayToIso(DateUtil.todayEpochDay())

        if (wasFocus && recordMinutes) {
            // Log pomodoro
            scope.launch {
                repository.insertPomodoroLog(
                    PomodoroLog(
                        id = UUID.randomUUID().toString(),
                        date = todayIso,
                        minutes = cachedSettings.focusMin,
                        sessionId = completedState.sessionId
                    )
                )

                // If linked to session, check if done
                completedState.sessionId?.let { sid ->
                    val session = repository.getSessionById(sid)
                    if (session != null && session.status == "pending") {
                        _sessionDonePrompt.value = session
                    }
                }
            }
        }

        // Determine next phase
        val nextCycle = if (wasFocus) completedState.cycleCount else completedState.cycleCount + 1
        val isLongBreak = wasFocus && (completedState.cycleCount % cachedSettings.longBreakEvery == 0)
        val nextPhase = when {
            !wasFocus -> "focus"
            isLongBreak -> "longBreak"
            else -> "break"
        }

        val nextState = TimerState(
            phase = nextPhase,
            endsAt = 0L,
            pausedRemainingMs = null,
            sessionId = completedState.sessionId,
            cycleCount = nextCycle
        )

        scope.launch {
            dataStoreManager.saveTimerState(nextState)
        }
    }

    fun dismissSessionDonePrompt() {
        _sessionDonePrompt.value = null
    }

    fun markPromptedSessionDone(session: Session) {
        scope.launch {
            repository.updateSession(session.copy(status = "done"))
            _sessionDonePrompt.value = null
        }
    }

    private fun scheduleAlarm(endsAt: Long, phase: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            putExtra("phase", phase)
            putExtra("soundOn", cachedSettings.soundOn)
            putExtra("vibrateOn", cachedSettings.vibrateOn)
            putExtra(
                "title",
                if (phase == "focus") "Focus complete" else "Break over"
            )
            putExtra(
                "message",
                if (phase == "focus") "25 minutes done. Stand up for 5." else "Break's over. Ready for the next block."
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endsAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endsAt, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
