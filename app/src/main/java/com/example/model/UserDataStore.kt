package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAvailability(
    val weekdayMinutes: List<Int> = listOf(180, 180, 180, 180, 180, 180, 120), // Sat..Fri
    val overrides: Map<String, Int> = emptyMap(), // ISO date "YYYY-MM-DD" -> minutes (0 = off day)
    val dayStart: String = "16:00"
)

@Serializable
data class UserSettings(
    val theme: String = "system", // system | light | dark
    val weekStart: String = "SATURDAY",
    val focusMin: Int = 25,
    val breakMin: Int = 5,
    val longBreakMin: Int = 15,
    val longBreakEvery: Int = 4,
    val bufferDays: Int = 2,
    val reminderTime: String? = null, // e.g. "09:00"
    val soundOn: Boolean = true,
    val vibrateOn: Boolean = true,
    val onboardingDone: Boolean = false
)

@Serializable
data class TimerState(
    val phase: String = "idle", // idle | focus | break | longBreak
    val endsAt: Long = 0L, // epoch ms
    val pausedRemainingMs: Long? = null,
    val sessionId: String? = null,
    val cycleCount: Int = 1
)
