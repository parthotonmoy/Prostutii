package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {

    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayOfWeekDayMonthFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun epochDayToIso(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).toString()
    }

    fun isoToEpochDay(iso: String): Long {
        return LocalDate.parse(iso).toEpochDay()
    }

    fun formatDayMonth(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(dayMonthFormatter)
    }

    fun formatDayOfWeekDayMonth(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(dayOfWeekDayMonthFormatter)
    }

    fun formatDuration(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /**
     * Maps an epochDay to day-of-week index 0..6 based on weekStart.
     * Default weekStart = DayOfWeek.SATURDAY (Sat=0, Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6).
     */
    fun dayOfWeekIndex(epochDay: Long, startDay: DayOfWeek = DayOfWeek.SATURDAY): Int {
        val date = LocalDate.ofEpochDay(epochDay)
        val currentDayValue = date.dayOfWeek.value // Mon=1..Sun=7
        val startDayValue = startDay.value // Mon=1..Sun=7
        return (currentDayValue - startDayValue + 7) % 7
    }

    fun dayOfWeekShortName(index: Int, startDay: DayOfWeek = DayOfWeek.SATURDAY): String {
        val dayValue = ((startDay.value - 1 + index) % 7) + 1
        return DayOfWeek.of(dayValue).name.take(3).lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    }
}
