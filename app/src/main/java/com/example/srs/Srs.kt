package com.example.srs

import kotlin.math.roundToInt

enum class ReviewRating {
    AGAIN, HARD, GOOD, EASY
}

data class SrsCardState(
    val ease: Double = 2.5,
    val intervalDays: Int = 1,
    val reps: Int = 0,
    val lapses: Int = 0,
    val due: Long = 0L
)

data class SrsResult(
    val ease: Double,
    val intervalDays: Int,
    val reps: Int,
    val lapses: Int,
    val due: Long,
    val shouldRequeue: Boolean
)

object Srs {
    const val MIN_EASE = 1.3

    fun review(state: SrsCardState, rating: ReviewRating, todayEpochDay: Long): SrsResult {
        return when (rating) {
            ReviewRating.AGAIN -> {
                val newEase = maxOf(MIN_EASE, ((state.ease - 0.2) * 100.0).roundToInt() / 100.0)
                SrsResult(
                    ease = newEase,
                    intervalDays = 1,
                    reps = 0,
                    lapses = state.lapses + 1,
                    due = todayEpochDay + 1,
                    shouldRequeue = true
                )
            }
            ReviewRating.HARD -> {
                val newEase = maxOf(MIN_EASE, ((state.ease - 0.15) * 100.0).roundToInt() / 100.0)
                val base = if (state.intervalDays <= 0) 1 else state.intervalDays
                val newInterval = maxOf(1, (base * 1.2).roundToInt())
                SrsResult(
                    ease = newEase,
                    intervalDays = newInterval,
                    reps = state.reps + 1,
                    lapses = state.lapses,
                    due = todayEpochDay + newInterval,
                    shouldRequeue = false
                )
            }
            ReviewRating.GOOD -> {
                val newInterval = when (state.reps) {
                    0 -> 1
                    1 -> 3
                    else -> {
                        val base = if (state.intervalDays <= 0) 1 else state.intervalDays
                        maxOf(1, (base * state.ease).roundToInt())
                    }
                }
                SrsResult(
                    ease = state.ease,
                    intervalDays = newInterval,
                    reps = state.reps + 1,
                    lapses = state.lapses,
                    due = todayEpochDay + newInterval,
                    shouldRequeue = false
                )
            }
            ReviewRating.EASY -> {
                val newEase = ((state.ease + 0.15) * 100.0).roundToInt() / 100.0
                val goodInterval = when (state.reps) {
                    0 -> 1
                    1 -> 3
                    else -> {
                        val base = if (state.intervalDays <= 0) 1 else state.intervalDays
                        maxOf(1, (base * state.ease).roundToInt())
                    }
                }
                val newInterval = maxOf(1, (goodInterval * 1.3).roundToInt())
                SrsResult(
                    ease = newEase,
                    intervalDays = newInterval,
                    reps = state.reps + 1,
                    lapses = state.lapses,
                    due = todayEpochDay + newInterval,
                    shouldRequeue = false
                )
            }
        }
    }

    fun previewIntervalText(state: SrsCardState, rating: ReviewRating, todayEpochDay: Long): String {
        val result = review(state, rating, todayEpochDay)
        return "${result.intervalDays}d"
    }
}
