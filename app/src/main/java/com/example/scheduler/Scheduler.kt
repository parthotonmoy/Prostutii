package com.example.scheduler

import com.example.util.DateUtil
import java.time.DayOfWeek
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

data class SchedulerSubject(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val examDateEpochDay: Long,
    val weight: Int = 2,
    val archived: Boolean = false
)

data class SchedulerTopic(
    val id: String,
    val subjectId: String,
    val title: String,
    val difficulty: Int, // 1..5
    val estBlocks: Int,
    val status: String, // new, learning, revised, solid
    val blocksDone: Int,
    val revisionCount: Int,
    val confidence: Int, // 0..100
    val needsHelp: Boolean,
    val order: Int
)

data class SchedulerSession(
    val id: String,
    val dateEpochDay: Long,
    val topicId: String,
    val subjectId: String,
    val kind: String, // learn, revise, practice
    val blocks: Int, // 1..4
    val status: String, // pending, done, missed, skipped
    val pinned: Boolean,
    val createdBy: String, // auto, manual
    val actualMinutes: Int? = null,
    val startedAt: Long? = null
)

data class SchedulerAvailability(
    val weekdayMinutes: List<Int>, // 7 values for Sat..Fri (default [180, 180, 180, 180, 180, 180, 120])
    val overrides: Map<Long, Int> = emptyMap(), // epochDay -> minutes (0 = off day)
    val focusMin: Int = 25,
    val bufferDays: Int = 2,
    val startDayOfWeek: DayOfWeek = DayOfWeek.SATURDAY
)

data class SchedulerOverload(
    val subjectId: String,
    val subjectName: String,
    val examDateEpochDay: Long,
    val shortByBlocks: Int,
    val neededExtraMinutesPerDay: Int
)

data class SchedulerPlanResult(
    val newSessions: List<SchedulerSession>,
    val overloads: List<SchedulerOverload>,
    val unplacedBlocks: Int
)

object Scheduler {

    fun defaultEstBlocks(difficulty: Int): Int {
        return when (difficulty) {
            1 -> 1
            2 -> 2
            3 -> 2
            4 -> 3
            5 -> 4
            else -> 2
        }
    }

    fun generatePlan(
        todayEpochDay: Long,
        subjects: List<SchedulerSubject>,
        topics: List<SchedulerTopic>,
        existingSessions: List<SchedulerSession>,
        availability: SchedulerAvailability,
        dueCardsCountByTopic: Map<String, Int> = emptyMap()
    ): SchedulerPlanResult {
        val activeSubjects = subjects
            .filter { !it.archived && it.examDateEpochDay > todayEpochDay }
            .sortedWith(compareBy({ it.examDateEpochDay }, { it.id }))

        if (activeSubjects.isEmpty()) {
            return SchedulerPlanResult(emptyList(), emptyList(), 0)
        }

        val topicsBySubject = topics.groupBy { it.subjectId }
        val topicMap = topics.associateBy { it.id }

        // Sessions that must be preserved
        val preservedSessions = existingSessions.filter { session ->
            session.dateEpochDay < todayEpochDay ||
                session.status != "pending" ||
                session.pinned ||
                session.createdBy == "manual"
        }

        // Occupied blocks by date from preserved sessions
        val preservedBlocksByDate = preservedSessions
            .groupBy { it.dateEpochDay }
            .mapValues { entry -> entry.value.sumOf { it.blocks } }
            .toMutableMap()

        // Track how many blocks of each topic are already accounted for in preserved sessions
        val preservedDoneOrScheduledBlocks = preservedSessions
            .filter { it.status == "done" || it.status == "pending" }
            .groupBy { it.topicId }
            .mapValues { entry -> entry.value.sumOf { it.blocks } }

        fun getDayCapacity(day: Long): Int {
            val override = availability.overrides[day]
            val minutes = override ?: run {
                val idx = DateUtil.dayOfWeekIndex(day, availability.startDayOfWeek)
                if (idx in availability.weekdayMinutes.indices) availability.weekdayMinutes[idx] else 180
            }
            val totalBlocks = floor(minutes.toDouble() / availability.focusMin.toDouble()).toInt()
            val used = preservedBlocksByDate[day] ?: 0
            return max(0, totalBlocks - used)
        }

        val generatedSessions = mutableListOf<SchedulerSession>()
        val dayRemainingCapacity = mutableMapOf<Long, Int>()

        // Find overall scheduling window
        val maxExamDay = activeSubjects.maxOf { it.examDateEpochDay }
        val allDays = (todayEpochDay until maxExamDay).toList()
        for (day in allDays) {
            dayRemainingCapacity[day] = getDayCapacity(day)
        }

        val overloads = mutableListOf<SchedulerOverload>()
        var totalUnplacedBlocks = 0

        // Helper to generate deterministic session ID
        var sessionCounter = 0
        fun makeSessionId(subjectId: String, topicId: String, kind: String, day: Long): String {
            sessionCounter++
            return "gen_${day}_${topicId}_${kind}_${sessionCounter}"
        }

        // For each subject, calculate revision zone window
        val revisionZones = mutableMapOf<String, Set<Long>>()
        for (subj in activeSubjects) {
            val windowStart = todayEpochDay
            val windowEnd = subj.examDateEpochDay - 1
            if (windowEnd >= windowStart) {
                val totalWindowDays = (windowEnd - windowStart + 1).toInt()
                val zoneLen = if (totalWindowDays <= availability.bufferDays + 1) {
                    totalWindowDays
                } else {
                    availability.bufferDays
                }
                val zoneStart = windowEnd - zoneLen + 1
                val zoneDays = (zoneStart..windowEnd).toSet()
                revisionZones[subj.id] = zoneDays
            } else {
                revisionZones[subj.id] = emptySet()
            }
        }

        // 1. COLLECT WORK ITEMS
        // Item types:
        // A) Spaced Revisions / Revision Zone Revise
        // B) Carried-over Missed Learn Blocks
        // C) Revision Zone Practice Blocks
        // D) New Learn Blocks

        data class ScheduledWork(
            val topicId: String,
            val subjectId: String,
            val kind: String,
            val blocks: Int,
            val priority: Int, // 1: Revise, 2: Missed learn, 3: Zone practice, 4: New learn
            val targetDay: Long? = null, // for fixed spaced revision
            val maxDay: Long,
            val minDay: Long = todayEpochDay
        )

        val fixedDayWork = mutableListOf<ScheduledWork>()
        val flexibleReviseWork = mutableListOf<ScheduledWork>()
        val carriedMissedWork = mutableListOf<ScheduledWork>()
        val flexibleLearnWork = mutableListOf<ScheduledWork>()
        val zonePracticeWork = mutableListOf<ScheduledWork>()

        // Find missed sessions from before or today to carry over
        val missedPendingSessions = existingSessions.filter {
            it.status == "missed" && it.kind == "learn" && it.dateEpochDay <= todayEpochDay
        }
        for (missed in missedPendingSessions) {
            val topic = topicMap[missed.topicId] ?: continue
            val subj = activeSubjects.find { it.id == topic.subjectId } ?: continue
            val zoneDays = revisionZones[subj.id] ?: emptySet()
            val maxLearnDay = if (zoneDays.isNotEmpty()) (zoneDays.minOrNull() ?: subj.examDateEpochDay) - 1 else subj.examDateEpochDay - 1
            if (maxLearnDay >= todayEpochDay) {
                carriedMissedWork.add(
                    ScheduledWork(
                        topicId = topic.id,
                        subjectId = subj.id,
                        kind = "learn",
                        blocks = missed.blocks,
                        priority = 2,
                        maxDay = maxLearnDay
                    )
                )
            }
        }

        // Process topics per subject
        for (subj in activeSubjects) {
            val subjTopics = topicsBySubject[subj.id]?.sortedWith(
                compareByDescending<SchedulerTopic> { it.difficulty }
                    .thenBy { it.order }
                    .thenBy { it.id }
            ) ?: emptyList()

            val zoneDays = revisionZones[subj.id] ?: emptySet()
            val maxLearnDay = if (zoneDays.isNotEmpty()) {
                (zoneDays.minOrNull() ?: subj.examDateEpochDay) - 1
            } else {
                subj.examDateEpochDay - 1
            }

            for (topic in subjTopics) {
                val effectiveEst = topic.estBlocks + (if (topic.needsHelp) 1 else 0)
                val accountedBlocks = max(topic.blocksDone, preservedDoneOrScheduledBlocks[topic.id] ?: 0)
                val remainingLearnBlocks = max(0, effectiveEst - accountedBlocks)

                if (remainingLearnBlocks > 0 && maxLearnDay >= todayEpochDay) {
                    flexibleLearnWork.add(
                        ScheduledWork(
                            topicId = topic.id,
                            subjectId = subj.id,
                            kind = "learn",
                            blocks = remainingLearnBlocks,
                            priority = 4,
                            maxDay = maxLearnDay
                        )
                    )
                }

                // Check spaced revisions from past completed learn work
                if (topic.blocksDone > 0 && topic.revisionCount < 3) {
                    val completedLearn = preservedSessions.filter {
                        it.topicId == topic.id && it.kind == "learn" && it.status == "done"
                    }.maxByOrNull { it.dateEpochDay }
                    if (completedLearn != null) {
                        val offsets = listOf(1, 3, 7)
                        val reviseBlocks = max(1, ceil(0.4 * completedLearn.blocks).toInt())
                        for (i in topic.revisionCount until offsets.size) {
                            val revDay = completedLearn.dateEpochDay + offsets[i]
                            if (revDay >= todayEpochDay && revDay < subj.examDateEpochDay && !zoneDays.contains(revDay)) {
                                fixedDayWork.add(
                                    ScheduledWork(
                                        topicId = topic.id,
                                        subjectId = subj.id,
                                        kind = "revise",
                                        blocks = reviseBlocks,
                                        priority = 1,
                                        targetDay = revDay,
                                        maxDay = revDay,
                                        minDay = revDay
                                    )
                                )
                            }
                        }
                    }
                }

                // Revision zone work:
                // One revise pass per topic
                if (zoneDays.isNotEmpty()) {
                    val minZoneDay = zoneDays.minOrNull() ?: todayEpochDay
                    val maxZoneDay = zoneDays.maxOrNull() ?: (subj.examDateEpochDay - 1)
                    val reviseLength = if (topic.status == "solid" && topic.confidence >= 80) 1 else max(1, ceil(0.4 * effectiveEst).toInt())
                    flexibleReviseWork.add(
                        ScheduledWork(
                            topicId = topic.id,
                            subjectId = subj.id,
                            kind = "revise",
                            blocks = reviseLength,
                            priority = 1,
                            minDay = minZoneDay,
                            maxDay = maxZoneDay
                        )
                    )

                    // Practice block: 1 block, or 2 blocks if difficulty is 4 or 5
                    val practiceBlocks = if (topic.difficulty >= 4) 2 else 1
                    zonePracticeWork.add(
                        ScheduledWork(
                            topicId = topic.id,
                            subjectId = subj.id,
                            kind = "practice",
                            blocks = practiceBlocks,
                            priority = 3,
                            minDay = minZoneDay,
                            maxDay = maxZoneDay
                        )
                    )
                }
            }
        }

        // Sort flexible revision items:
        // Due cards >= 5 first, then confidence asc, difficulty desc, id
        flexibleReviseWork.sortWith(
            compareByDescending<ScheduledWork> { (dueCardsCountByTopic[it.topicId] ?: 0) >= 5 }
                .thenBy { topicMap[it.topicId]?.confidence ?: 50 }
                .thenByDescending { topicMap[it.topicId]?.difficulty ?: 1 }
                .thenBy { it.topicId }
        )

        // Helper to place blocks on a day
        fun tryPlaceSession(work: ScheduledWork, day: Long, blocksToPlace: Int): Boolean {
            val rem = dayRemainingCapacity[day] ?: 0
            if (rem < blocksToPlace) return false
            dayRemainingCapacity[day] = rem - blocksToPlace
            generatedSessions.add(
                SchedulerSession(
                    id = makeSessionId(work.subjectId, work.topicId, work.kind, day),
                    dateEpochDay = day,
                    topicId = work.topicId,
                    subjectId = work.subjectId,
                    kind = work.kind,
                    blocks = blocksToPlace,
                    status = "pending",
                    pinned = false,
                    createdBy = "auto"
                )
            )
            return true
        }

        // 2. PLACE FIXED SPACED REVISIONS FIRST
        for (item in fixedDayWork) {
            val targetDay = item.targetDay ?: continue
            val placed = tryPlaceSession(item, targetDay, item.blocks)
            if (!placed) {
                // If capacity is 0 on target day, try to place on nearest next day before maxDay
                for (d in (targetDay + 1)..item.maxDay) {
                    if (tryPlaceSession(item, d, item.blocks)) break
                }
            }
        }

        // 3. PLACE REVISION ZONE REVISE & PRACTICE WORK
        // Schedule into zone days
        for (work in flexibleReviseWork) {
            var needed = work.blocks
            for (day in work.minDay..work.maxDay) {
                if (needed <= 0) break
                val cap = dayRemainingCapacity[day] ?: 0
                if (cap > 0) {
                    val placeCount = minOf(cap, needed)
                    tryPlaceSession(work.copy(blocks = placeCount), day, placeCount)
                    needed -= placeCount
                }
            }
            if (needed > 0) totalUnplacedBlocks += needed
        }

        for (work in zonePracticeWork) {
            var needed = work.blocks
            for (day in work.minDay..work.maxDay) {
                if (needed <= 0) break
                val cap = dayRemainingCapacity[day] ?: 0
                if (cap > 0) {
                    val placeCount = minOf(cap, needed)
                    tryPlaceSession(work.copy(blocks = placeCount), day, placeCount)
                    needed -= placeCount
                }
            }
            if (needed > 0) totalUnplacedBlocks += needed
        }

        // 4. PLACE CARRIED-OVER MISSED WORK
        for (work in carriedMissedWork) {
            var needed = work.blocks
            for (day in todayEpochDay..work.maxDay) {
                if (needed <= 0) break
                val cap = dayRemainingCapacity[day] ?: 0
                if (cap > 0) {
                    val placeCount = minOf(cap, minOf(4, needed))
                    tryPlaceSession(work.copy(blocks = placeCount), day, placeCount)
                    needed -= placeCount
                }
            }
            if (needed > 0) totalUnplacedBlocks += needed
        }

        // 5. PLACE NEW LEARNING WORK
        // Share across subjects in proportion to pressure = remainingBlocks / daysLeftToExam * weight
        // Chunk sizes 1..4 blocks. Interleave: never more than 4 consecutive blocks of same subject if another has work.
        data class SubjectLearnQueue(
            val subject: SchedulerSubject,
            val works: MutableList<ScheduledWork>,
            var consecutiveBlocks: Int = 0
        )

        val subjectQueues = activeSubjects.mapNotNull { subj ->
            val works = flexibleLearnWork.filter { it.subjectId == subj.id }.toMutableList()
            if (works.isNotEmpty()) SubjectLearnQueue(subj, works) else null
        }.toMutableList()

        for (day in allDays) {
            var cap = dayRemainingCapacity[day] ?: 0
            if (cap <= 0) continue

            // Filter active queues that can study on this day (day <= maxLearnDay)
            while (cap > 0 && subjectQueues.any { it.works.isNotEmpty() && day <= (it.works.firstOrNull()?.maxDay ?: -1) }) {
                // Compute pressure for eligible subjects
                val eligible = subjectQueues.filter { q ->
                    q.works.isNotEmpty() && day <= q.works.first().maxDay
                }
                if (eligible.isEmpty()) break

                // Pick subject: avoid >4 consecutive blocks if another eligible subject exists
                val candidate = eligible
                    .filter { if (eligible.size > 1) it.consecutiveBlocks < 4 else true }
                    .maxWithOrNull(
                        compareBy<SubjectLearnQueue> { q ->
                            val remBlocks = q.works.sumOf { it.blocks }
                            val daysLeft = max(1, (q.subject.examDateEpochDay - day).toInt())
                            (remBlocks.toDouble() / daysLeft) * q.subject.weight
                        }.thenBy { it.subject.id }
                    ) ?: eligible.minByOrNull { it.consecutiveBlocks } ?: eligible.first()

                val work = candidate.works.first()
                val chunk = minOf(cap, minOf(4, work.blocks))
                val placed = tryPlaceSession(work.copy(blocks = chunk), day, chunk)
                if (placed) {
                    cap -= chunk
                    candidate.consecutiveBlocks += chunk
                    // Reset other queues consecutive counts
                    for (other in subjectQueues) {
                        if (other != candidate) other.consecutiveBlocks = 0
                    }

                    if (work.blocks > chunk) {
                        candidate.works[0] = work.copy(blocks = work.blocks - chunk)
                    } else {
                        candidate.works.removeAt(0)
                    }

                    // Spaced revisions triggered by new learn session
                    val topic = topicMap[work.topicId]
                    if (topic != null && topic.revisionCount == 0) {
                        val zoneDays = revisionZones[candidate.subject.id] ?: emptySet()
                        val offsets = listOf(1, 3, 7)
                        val revBlocks = max(1, ceil(0.4 * chunk).toInt())
                        for (off in offsets) {
                            val revDay = day + off
                            if (revDay < candidate.subject.examDateEpochDay && !zoneDays.contains(revDay)) {
                                val revCap = dayRemainingCapacity[revDay] ?: 0
                                if (revCap >= revBlocks) {
                                    tryPlaceSession(
                                        ScheduledWork(
                                            topicId = topic.id,
                                            subjectId = candidate.subject.id,
                                            kind = "revise",
                                            blocks = revBlocks,
                                            priority = 1,
                                            maxDay = revDay
                                        ),
                                        revDay,
                                        revBlocks
                                    )
                                }
                            }
                        }
                    }
                } else {
                    break
                }
            }
        }

        // 6. FEASIBILITY & OVERLOAD CHECK
        for (q in subjectQueues) {
            val unplaced = q.works.sumOf { it.blocks }
            if (unplaced > 0) {
                totalUnplacedBlocks += unplaced
                // Calculate remaining study days
                val studyDays = allDays.filter { d ->
                    d < q.subject.examDateEpochDay && (dayRemainingCapacity[d] != null)
                }.size
                val extraMin = if (studyDays > 0) {
                    ceil((unplaced * availability.focusMin).toDouble() / studyDays).toInt()
                } else {
                    unplaced * availability.focusMin
                }
                overloads.add(
                    SchedulerOverload(
                        subjectId = q.subject.id,
                        subjectName = q.subject.name,
                        examDateEpochDay = q.subject.examDateEpochDay,
                        shortByBlocks = unplaced,
                        neededExtraMinutesPerDay = extraMin
                    )
                )
            }
        }

        // Return sorted generated sessions
        val sortedSessions = generatedSessions.sortedWith(
            compareBy({ it.dateEpochDay }, { it.subjectId }, { it.id })
        )

        return SchedulerPlanResult(
            newSessions = sortedSessions,
            overloads = overloads,
            unplacedBlocks = totalUnplacedBlocks
        )
    }

    /**
     * Computes a before/after diff summary for reschedule confirmation.
     * e.g., "Moved. Power Systems gains 25 min on Thu and Fri. Nothing else changed."
     */
    fun computeRescheduleDiffSummary(
        oldSessions: List<SchedulerSession>,
        newSessions: List<SchedulerSession>,
        subjects: Map<String, SchedulerSubject>,
        focusMin: Int
    ): String {
        val oldMinutes = oldSessions.groupBy { Pair(it.subjectId, it.dateEpochDay) }
            .mapValues { entry -> entry.value.sumOf { it.blocks } * focusMin }
        val newMinutes = newSessions.groupBy { Pair(it.subjectId, it.dateEpochDay) }
            .mapValues { entry -> entry.value.sumOf { it.blocks } * focusMin }

        val allKeys = oldMinutes.keys + newMinutes.keys
        val changes = mutableMapOf<String, MutableList<Pair<Long, Int>>>() // subjectId -> list of (day, delta)

        for (k in allKeys) {
            val oldM = oldMinutes[k] ?: 0
            val newM = newMinutes[k] ?: 0
            val delta = newM - oldM
            if (delta != 0) {
                val list = changes.getOrPut(k.first) { mutableListOf() }
                list.add(Pair(k.second, delta))
            }
        }

        if (changes.isEmpty()) {
            return "Moved. Plan updated. Nothing else changed."
        }

        // Pick most prominent change
        val mostChangedSubjId = changes.maxByOrNull { it.value.sumOf { p -> kotlin.math.abs(p.second) } }?.key ?: changes.keys.first()
        val subjName = subjects[mostChangedSubjId]?.name ?: "Subject"
        val subjectGains = changes[mostChangedSubjId]?.filter { it.second > 0 } ?: emptyList()

        return if (subjectGains.isNotEmpty()) {
            val daysStr = subjectGains.take(2).joinToString(" and ") {
                DateUtil.formatDayOfWeekDayMonth(it.first).take(3)
            }
            val gainMin = subjectGains.first().second
            "Moved. $subjName gains $gainMin min on $daysStr. Nothing else changed."
        } else {
            "Moved. Plan adjusted for remaining days. Nothing else changed."
        }
    }
}
