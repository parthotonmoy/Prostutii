package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.DateUtil
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String,
    val name: String,
    val colorIndex: Int,
    val examDate: String, // ISO "YYYY-MM-DD"
    val examTime: String? = null,
    val weight: Int = 2, // 1-3, default 2
    val archived: Boolean = false,
    val isSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val examDateEpochDay: Long get() = DateUtil.isoToEpochDay(examDate)
}

@Serializable
@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val difficulty: Int = 3, // 1-5
    val estBlocks: Int = 2,
    val status: String = "new", // new | learning | revised | solid
    val blocksDone: Int = 0,
    val revisionCount: Int = 0,
    val confidence: Int = 50, // 0-100, default 50
    val needsHelp: Boolean = false,
    val notes: String = "",
    val formulaSheet: String = "",
    val order: Int = 0,
    val isSample: Boolean = false
)

@Serializable
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val date: String, // ISO "YYYY-MM-DD"
    val topicId: String,
    val kind: String = "learn", // learn | revise | practice
    val blocks: Int = 1, // 1-4
    val status: String = "pending", // pending | done | missed | skipped
    val actualMinutes: Int? = null,
    val pinned: Boolean = false,
    val createdBy: String = "auto", // auto | manual
    val startedAt: Long? = null
) {
    val dateEpochDay: Long get() = DateUtil.isoToEpochDay(date)
}

@Serializable
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey val id: String,
    val topicId: String,
    val front: String,
    val back: String,
    val ease: Double = 2.5,
    val intervalDays: Int = 1,
    val reps: Int = 0,
    val lapses: Int = 0,
    val due: String, // ISO "YYYY-MM-DD"
    val createdAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
) {
    val dueEpochDay: Long get() = DateUtil.isoToEpochDay(due)
}

@Serializable
@Entity(tableName = "problems")
data class Problem(
    @PrimaryKey val id: String,
    val topicId: String,
    val prompt: String,
    val solution: String,
    val marks: Int? = null,
    val source: String? = null,
    val attempts: Int = 0,
    val lastResult: String? = null, // got | partly | missed | null
    val isSample: Boolean = false
)

@Serializable
@Entity(tableName = "past_papers")
data class PastPaper(
    @PrimaryKey val id: String,
    val subjectId: String,
    val label: String,
    val done: Boolean = false,
    val scorePercent: Int? = null
)

@Serializable
@Entity(tableName = "pomodoro_logs")
data class PomodoroLog(
    @PrimaryKey val id: String,
    val date: String, // ISO "YYYY-MM-DD"
    val minutes: Int,
    val sessionId: String? = null,
    val topicId: String? = null
)

@Serializable
@Entity(tableName = "snapshots")
data class Snapshot(
    @PrimaryKey val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reason: String,
    val payloadJson: String // serialized snapshot of subjects, topics, sessions, cards, problems
)
