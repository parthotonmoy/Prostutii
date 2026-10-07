package com.example.scheduler

import com.example.util.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SchedulerTest {

    private val defaultAvail = SchedulerAvailability(
        weekdayMinutes = listOf(180, 180, 180, 180, 180, 180, 120), // 7 blocks, 7, 7, 7, 7, 7, 4
        focusMin = 25,
        bufferDays = 2
    )

    @Test
    fun neverSchedulesOnOrAfterExamDay() {
        val today = 1000L
        val examDay = 1005L
        val subject = SchedulerSubject("s1", "DSP", 0, examDateEpochDay = examDay)
        val topics = listOf(
            SchedulerTopic("t1", "s1", "Filters", difficulty = 3, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)
        for (session in plan.newSessions) {
            assertTrue("Session scheduled on or after exam day: ${session.dateEpochDay}", session.dateEpochDay < examDay)
        }
    }

    @Test
    fun neverExceedsADaysCapacity() {
        val today = 1000L
        val examDay = 1008L
        val subject = SchedulerSubject("s1", "Math", 0, examDateEpochDay = examDay)
        val topics = (1..10).map { i ->
            SchedulerTopic("t$i", "s1", "Topic $i", difficulty = 4, estBlocks = 5, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = i)
        }

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)
        val groupedByDay = plan.newSessions.groupBy { it.dateEpochDay }
        for ((day, sessions) in groupedByDay) {
            val totalBlocks = sessions.sumOf { it.blocks }
            val dayOfWeekIdx = DateUtil.dayOfWeekIndex(day, defaultAvail.startDayOfWeek)
            val maxBlocks = defaultAvail.weekdayMinutes[dayOfWeekIdx] / defaultAvail.focusMin
            assertTrue("Day $day exceeded capacity: $totalBlocks > $maxBlocks", totalBlocks <= maxBlocks)
        }
    }

    @Test
    fun keepsHistoryAndPinnedSessionsUntouched() {
        val today = 1000L
        val pastSession = SchedulerSession("p1", dateEpochDay = 998L, topicId = "t1", subjectId = "s1", kind = "learn", blocks = 2, status = "done", pinned = false, createdBy = "auto")
        val pinnedFuture = SchedulerSession("pinned1", dateEpochDay = 1002L, topicId = "t1", subjectId = "s1", kind = "learn", blocks = 2, status = "pending", pinned = true, createdBy = "manual")

        val subject = SchedulerSubject("s1", "DSP", 0, examDateEpochDay = 1010L)
        val topics = listOf(
            SchedulerTopic("t1", "s1", "Z-Transform", difficulty = 3, estBlocks = 4, status = "learning", blocksDone = 2, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, listOf(pastSession, pinnedFuture), defaultAvail)

        // Neither p1 nor pinned1 should be replaced or duplicated in generated new sessions
        assertFalse(plan.newSessions.any { it.id == "p1" })
        assertFalse(plan.newSessions.any { it.id == "pinned1" })
    }

    @Test
    fun revisionZoneContainsOnlyReviseAndPractice() {
        val today = 1000L
        val examDay = 1006L // Window: 1000..1005 (6 days). Zone = last 2 days: 1004, 1005
        val subject = SchedulerSubject("s1", "Physics", 0, examDateEpochDay = examDay)
        val topics = listOf(
            SchedulerTopic("t1", "s1", "Optics", difficulty = 3, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)
        val zoneSessions = plan.newSessions.filter { it.dateEpochDay in 1004L..1005L }
        for (session in zoneSessions) {
            assertTrue("Zone contains non-revision work: ${session.kind}", session.kind == "revise" || session.kind == "practice")
        }
    }

    @Test
    fun spacedRevisionsLandAt137AndSkipTheZone() {
        val today = 1000L
        val examDay = 1015L // Large window: 1000..1014. Zone: 1013, 1014.
        val pastDone = SchedulerSession("done1", dateEpochDay = 998L, topicId = "t1", subjectId = "s1", kind = "learn", blocks = 3, status = "done", pinned = false, createdBy = "auto")

        val subject = SchedulerSubject("s1", "Signals", 0, examDateEpochDay = examDay)
        val topic = SchedulerTopic("t1", "s1", "FFT", difficulty = 3, estBlocks = 3, status = "learning", blocksDone = 3, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)

        val plan = Scheduler.generatePlan(today, listOf(subject), listOf(topic), listOf(pastDone), defaultAvail)
        // Past done was on 998. Offsets: +1 (999, past), +3 (1001), +7 (1005).
        // On 1001 and 1005 there should be spaced revision sessions
        val reviseDays = plan.newSessions.filter { it.topicId == "t1" && it.kind == "revise" }.map { it.dateEpochDay }
        assertTrue("Expected revise on day 1001", reviseDays.contains(1001L))
        assertTrue("Expected revise on day 1005", reviseDays.contains(1005L))
    }

    @Test
    fun missedWorkLandsBeforeNewMaterial() {
        val today = 1000L
        val missedSession = SchedulerSession("m1", dateEpochDay = 999L, topicId = "t_missed", subjectId = "s1", kind = "learn", blocks = 2, status = "missed", pinned = false, createdBy = "auto")

        val subject = SchedulerSubject("s1", "Circuits", 0, examDateEpochDay = 1010L)
        val topics = listOf(
            SchedulerTopic("t_missed", "s1", "Op-Amps", difficulty = 3, estBlocks = 4, status = "learning", blocksDone = 2, revisionCount = 0, confidence = 50, needsHelp = false, order = 1),
            SchedulerTopic("t_new", "s1", "Filters", difficulty = 2, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 2)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, listOf(missedSession), defaultAvail)
        val firstLearnSession = plan.newSessions.firstOrNull { it.kind == "learn" }
        assertEquals("Missed work should be scheduled first", "t_missed", firstLearnSession?.topicId)
    }

    @Test
    fun examTomorrowGivesOnlyRevision() {
        val today = 1000L
        val examDay = 1001L // Exam is tomorrow!
        val subject = SchedulerSubject("s1", "Chemistry", 0, examDateEpochDay = examDay)
        val topics = listOf(
            SchedulerTopic("t1", "s1", "Thermodynamics", difficulty = 4, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)
        for (session in plan.newSessions) {
            assertTrue("Tomorrow exam must only have revise or practice", session.kind == "revise" || session.kind == "practice")
        }
    }

    @Test
    fun overloadReportsTheExactShortfallInBlocksAndMinutesPerDay() {
        val today = 1000L
        val examDay = 1002L // 2 study days: 1000 and 1001
        val tinyAvail = SchedulerAvailability(
            weekdayMinutes = listOf(25, 25, 25, 25, 25, 25, 25), // 1 block per day = 2 blocks total
            focusMin = 25,
            bufferDays = 0
        )
        val subject = SchedulerSubject("s1", "DSP", 0, examDateEpochDay = examDay)
        // 10 blocks needed, only 2 capacity available
        val topics = listOf(
            SchedulerTopic("t1", "s1", "BigTopic", difficulty = 5, estBlocks = 10, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        )

        val plan = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), tinyAvail)
        assertFalse("Plan should report overload", plan.overloads.isEmpty())
        val ov = plan.overloads.first()
        assertTrue("Shortfall should be reported", ov.shortByBlocks >= 8)
        assertTrue("Needed extra minutes should be calculated", ov.neededExtraMinutesPerDay > 0)
    }

    @Test
    fun sameInputTwiceGivesIdenticalOutput() {
        val today = 1000L
        val subject = SchedulerSubject("s1", "Algorithms", 0, examDateEpochDay = 1010L)
        val topics = listOf(
            SchedulerTopic("t1", "s1", "Graphs", difficulty = 4, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1),
            SchedulerTopic("t2", "s1", "Dynamic Prog", difficulty = 5, estBlocks = 5, status = "new", blocksDone = 0, revisionCount = 0, confidence = 40, needsHelp = true, order = 2)
        )

        val plan1 = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)
        val plan2 = Scheduler.generatePlan(today, listOf(subject), topics, emptyList(), defaultAvail)

        assertEquals(plan1.newSessions.size, plan2.newSessions.size)
        for (i in plan1.newSessions.indices) {
            val s1 = plan1.newSessions[i]
            val s2 = plan2.newSessions[i]
            assertEquals(s1.dateEpochDay, s2.dateEpochDay)
            assertEquals(s1.topicId, s2.topicId)
            assertEquals(s1.blocks, s2.blocks)
            assertEquals(s1.kind, s2.kind)
        }
    }

    @Test
    fun dstAndLeapDayBoundaries() {
        // Leap year 2024: Feb 28 (epoch 19781), Feb 29 (19782), Mar 1 (19783)
        val feb28 = LocalDate.of(2024, 2, 28).toEpochDay()
        val mar2 = LocalDate.of(2024, 3, 2).toEpochDay()
        val subject = SchedulerSubject("s1", "Math", 0, examDateEpochDay = mar2)
        val topic = SchedulerTopic("t1", "s1", "Calculus", difficulty = 3, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)

        val oneBlockAvail = SchedulerAvailability(
            weekdayMinutes = listOf(25, 25, 25, 25, 25, 25, 25),
            focusMin = 25,
            bufferDays = 0
        )
        val plan = Scheduler.generatePlan(feb28, listOf(subject), listOf(topic), emptyList(), oneBlockAvail)
        val days = plan.newSessions.map { it.dateEpochDay }.distinct()
        val feb29 = LocalDate.of(2024, 2, 29).toEpochDay()
        assertEquals(feb28 + 1, feb29)
        assertTrue("Must include leap day Feb 29", days.contains(feb29))
    }

    @Test
    fun twoExamsOnOneDay() {
        val today = 1000L
        val examDay = 1005L
        val s1 = SchedulerSubject("s1", "Subj A", 0, examDateEpochDay = examDay, weight = 2)
        val s2 = SchedulerSubject("s2", "Subj B", 1, examDateEpochDay = examDay, weight = 2)
        val t1 = SchedulerTopic("t1", "s1", "Topic A", difficulty = 3, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)
        val t2 = SchedulerTopic("t2", "s2", "Topic B", difficulty = 3, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)

        val plan = Scheduler.generatePlan(today, listOf(s1, s2), listOf(t1, t2), emptyList(), defaultAvail)
        assertTrue(plan.newSessions.any { it.subjectId == "s1" })
        assertTrue(plan.newSessions.any { it.subjectId == "s2" })
    }

    @Test
    fun offDayOverrides() {
        val today = 1000L
        val examDay = 1005L
        val offDay = 1001L
        val availWithOff = defaultAvail.copy(overrides = mapOf(offDay to 0))
        val subject = SchedulerSubject("s1", "Electromagnetics", 0, examDateEpochDay = examDay)
        val topic = SchedulerTopic("t1", "s1", "Maxwell", difficulty = 4, estBlocks = 6, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)

        val plan = Scheduler.generatePlan(today, listOf(subject), listOf(topic), emptyList(), availWithOff)
        assertFalse("Off-day must not have generated sessions", plan.newSessions.any { it.dateEpochDay == offDay })
    }

    @Test
    fun pinnedSessionOnAnOffDayIsKept() {
        val today = 1000L
        val offDay = 1002L
        val pinnedSession = SchedulerSession("pin1", dateEpochDay = offDay, topicId = "t1", subjectId = "s1", kind = "learn", blocks = 2, status = "pending", pinned = true, createdBy = "manual")
        val availWithOff = defaultAvail.copy(overrides = mapOf(offDay to 0))

        val subject = SchedulerSubject("s1", "Power", 0, examDateEpochDay = 1006L)
        val topic = SchedulerTopic("t1", "s1", "Transmission", difficulty = 3, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, needsHelp = false, order = 1)

        val plan = Scheduler.generatePlan(today, listOf(subject), listOf(topic), listOf(pinnedSession), availWithOff)
        // Pinned session is untouched in input list; new sessions don't schedule onto day 1002
        assertFalse("New sessions should not be added on off day", plan.newSessions.any { it.dateEpochDay == offDay })
    }
}
