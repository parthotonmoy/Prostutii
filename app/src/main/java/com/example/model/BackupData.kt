package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseBackupPayload(
    val subjects: List<Subject> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val cards: List<Card> = emptyList(),
    val problems: List<Problem> = emptyList(),
    val pastPapers: List<PastPaper> = emptyList(),
    val pomodoroLogs: List<PomodoroLog> = emptyList()
)

@Serializable
data class ProstutiBackupFile(
    val app: String = "Prostuti",
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val data: DatabaseBackupPayload
)
