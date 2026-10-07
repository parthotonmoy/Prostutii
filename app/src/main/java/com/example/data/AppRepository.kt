package com.example.data

import androidx.room.withTransaction
import com.example.data.dao.CardDao
import com.example.data.dao.PastPaperDao
import com.example.data.dao.PomodoroLogDao
import com.example.data.dao.ProblemDao
import com.example.data.dao.SessionDao
import com.example.data.dao.SnapshotDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TopicDao
import com.example.model.Card
import com.example.model.DatabaseBackupPayload
import com.example.model.PastPaper
import com.example.model.PomodoroLog
import com.example.model.Problem
import com.example.model.ProstutiBackupFile
import com.example.model.Session
import com.example.model.Snapshot
import com.example.model.Subject
import com.example.model.Topic
import com.example.scheduler.Scheduler
import com.example.scheduler.SchedulerAvailability
import com.example.scheduler.SchedulerPlanResult
import com.example.scheduler.SchedulerSession
import com.example.scheduler.SchedulerSubject
import com.example.scheduler.SchedulerTopic
import com.example.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.util.UUID

class AppRepository(
    private val database: AppDatabase,
    private val dataStoreManager: DataStoreManager
) {
    private val subjectDao: SubjectDao = database.subjectDao()
    private val topicDao: TopicDao = database.topicDao()
    private val sessionDao: SessionDao = database.sessionDao()
    private val cardDao: CardDao = database.cardDao()
    private val problemDao: ProblemDao = database.problemDao()
    private val pastPaperDao: PastPaperDao = database.pastPaperDao()
    private val pomodoroLogDao: PomodoroLogDao = database.pomodoroLogDao()
    private val snapshotDao: SnapshotDao = database.snapshotDao()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // Flows
    val allSubjectsFlow: Flow<List<Subject>> = subjectDao.getAllSubjectsFlow()
    val allTopicsFlow: Flow<List<Topic>> = topicDao.getAllTopicsFlow()
    val allSessionsFlow: Flow<List<Session>> = sessionDao.getAllSessionsFlow()
    val allCardsFlow: Flow<List<Card>> = cardDao.getAllCardsFlow()
    val allProblemsFlow: Flow<List<Problem>> = problemDao.getAllProblemsFlow()
    val allPastPapersFlow: Flow<List<PastPaper>> = pastPaperDao.getAllPastPapersFlow()
    val allPomodoroLogsFlow: Flow<List<PomodoroLog>> = pomodoroLogDao.getAllLogsFlow()
    val allSnapshotsFlow: Flow<List<Snapshot>> = snapshotDao.getAllSnapshotsFlow()

    fun getTopicsForSubjectFlow(subjectId: String): Flow<List<Topic>> = topicDao.getTopicsBySubjectFlow(subjectId)
    fun getSessionsForDateFlow(date: String): Flow<List<Session>> = sessionDao.getSessionsForDateFlow(date)
    fun getCardsForTopicFlow(topicId: String): Flow<List<Card>> = cardDao.getCardsByTopicFlow(topicId)
    fun getProblemsForTopicFlow(topicId: String): Flow<List<Problem>> = problemDao.getProblemsByTopicFlow(topicId)
    fun getPastPapersForSubjectFlow(subjectId: String): Flow<List<PastPaper>> = pastPaperDao.getPastPapersBySubjectFlow(subjectId)

    // Direct Access
    suspend fun getAllSubjects(): List<Subject> = subjectDao.getAllSubjects()
    suspend fun getSubjectById(id: String): Subject? = subjectDao.getSubjectById(id)
    suspend fun getAllTopics(): List<Topic> = topicDao.getAllTopics()
    suspend fun getTopicById(id: String): Topic? = topicDao.getTopicById(id)
    suspend fun getAllSessions(): List<Session> = sessionDao.getAllSessions()
    suspend fun getSessionById(id: String): Session? = sessionDao.getSessionById(id)
    suspend fun getAllCards(): List<Card> = cardDao.getAllCards()
    suspend fun getDueCards(today: String): List<Card> = cardDao.getDueCards(today)
    suspend fun getDueCardsForTopic(topicId: String, today: String): List<Card> = cardDao.getDueCardsForTopic(topicId, today)
    suspend fun getProblemsForTopic(topicId: String): List<Problem> = problemDao.getProblemsByTopic(topicId)

    // Snapshots
    suspend fun createSnapshot(reason: String) {
        val payload = DatabaseBackupPayload(
            subjects = subjectDao.getAllSubjects(),
            topics = topicDao.getAllTopics(),
            sessions = sessionDao.getAllSessions(),
            cards = cardDao.getAllCards(),
            problems = problemDao.getAllProblems(),
            pastPapers = pastPaperDao.getAllPastPapers(),
            pomodoroLogs = pomodoroLogDao.getAllLogs()
        )
        val snapshot = Snapshot(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            reason = reason,
            payloadJson = json.encodeToString(payload)
        )
        snapshotDao.insertSnapshot(snapshot)
        snapshotDao.pruneOldSnapshots()
    }

    suspend fun restoreSnapshot(snapshotId: String) {
        val snapshot = snapshotDao.getSnapshotById(snapshotId) ?: return
        val payload = json.decodeFromString<DatabaseBackupPayload>(snapshot.payloadJson)
        database.withTransaction {
            subjectDao.deleteAll()
            topicDao.deleteAll()
            sessionDao.deleteAll()
            cardDao.deleteAll()
            problemDao.deleteAll()
            pastPaperDao.deleteAll()
            pomodoroLogDao.deleteAll()

            subjectDao.insertSubjects(payload.subjects)
            topicDao.insertTopics(payload.topics)
            sessionDao.insertSessions(payload.sessions)
            cardDao.insertCards(payload.cards)
            problemDao.insertProblems(payload.problems)
            pastPaperDao.insertPastPapers(payload.pastPapers)
            pomodoroLogDao.insertLogs(payload.pomodoroLogs)
        }
    }

    // Subjects and Topics
    suspend fun insertSubject(subject: Subject) {
        database.withTransaction {
            subjectDao.insertSubject(subject)
        }
    }

    suspend fun updateSubject(subject: Subject) {
        database.withTransaction {
            subjectDao.updateSubject(subject)
        }
    }

    suspend fun deleteSubject(subjectId: String) {
        createSnapshot("Before deleting subject")
        database.withTransaction {
            subjectDao.deleteSubjectById(subjectId)
            topicDao.deleteTopicsBySubject(subjectId)
            sessionDao.deleteSessionsBySubject(subjectId)
            cardDao.deleteCardsBySubject(subjectId)
            problemDao.deleteProblemsBySubject(subjectId)
            pastPaperDao.deletePastPapersBySubject(subjectId)
        }
    }

    suspend fun insertTopic(topic: Topic) {
        database.withTransaction {
            topicDao.insertTopic(topic)
        }
    }

    suspend fun insertTopics(topics: List<Topic>) {
        database.withTransaction {
            topicDao.insertTopics(topics)
        }
    }

    suspend fun updateTopic(topic: Topic) {
        database.withTransaction {
            topicDao.updateTopic(topic)
        }
    }

    suspend fun deleteTopic(topic: Topic) {
        database.withTransaction {
            topicDao.deleteTopic(topic)
            sessionDao.deleteSessionsByTopic(topic.id)
            cardDao.deleteCardsByTopic(topic.id)
            problemDao.deleteProblemsByTopic(topic.id)
        }
    }

    // Sessions
    suspend fun updateSession(session: Session) {
        database.withTransaction {
            sessionDao.updateSession(session)
        }
    }

    suspend fun insertSession(session: Session) {
        database.withTransaction {
            sessionDao.insertSession(session)
        }
    }

    // Cards & SRS
    suspend fun insertCard(card: Card) {
        database.withTransaction {
            cardDao.insertCard(card)
        }
    }

    suspend fun updateCard(card: Card) {
        database.withTransaction {
            cardDao.updateCard(card)
        }
    }

    suspend fun deleteCard(card: Card) {
        database.withTransaction {
            cardDao.deleteCard(card)
        }
    }

    // Problems
    suspend fun insertProblem(problem: Problem) {
        database.withTransaction {
            problemDao.insertProblem(problem)
        }
    }

    suspend fun updateProblem(problem: Problem) {
        database.withTransaction {
            problemDao.updateProblem(problem)
        }
    }

    suspend fun deleteProblem(problem: Problem) {
        database.withTransaction {
            problemDao.deleteProblem(problem)
        }
    }

    // Past Papers
    suspend fun insertPastPaper(pastPaper: PastPaper) {
        database.withTransaction {
            pastPaperDao.insertPastPaper(pastPaper)
        }
    }

    suspend fun updatePastPaper(pastPaper: PastPaper) {
        database.withTransaction {
            pastPaperDao.updatePastPaper(pastPaper)
        }
    }

    suspend fun deletePastPaper(pastPaper: PastPaper) {
        database.withTransaction {
            pastPaperDao.deletePastPaper(pastPaper)
        }
    }

    // Pomodoro Logs
    suspend fun insertPomodoroLog(log: PomodoroLog) {
        database.withTransaction {
            pomodoroLogDao.insertLog(log)
        }
    }

    // Regenerate Plan
    suspend fun regeneratePlan(
        todayEpochDay: Long = DateUtil.todayEpochDay(),
        availability: SchedulerAvailability,
        reason: String = "Regenerate plan"
    ): SchedulerPlanResult {
        createSnapshot(reason)
        val subjects = subjectDao.getAllSubjects()
        val topics = topicDao.getAllTopics()
        val existingSessions = sessionDao.getAllSessions()
        val cards = cardDao.getAllCards()

        val todayIso = DateUtil.epochDayToIso(todayEpochDay)
        val dueCardsByTopic = cards
            .filter { it.due <= todayIso }
            .groupBy { it.topicId }
            .mapValues { it.value.size }

        val schedulerSubjects = subjects.map {
            SchedulerSubject(
                id = it.id,
                name = it.name,
                colorIndex = it.colorIndex,
                examDateEpochDay = it.examDateEpochDay,
                weight = it.weight,
                archived = it.archived
            )
        }

        val schedulerTopics = topics.map {
            SchedulerTopic(
                id = it.id,
                subjectId = it.subjectId,
                title = it.title,
                difficulty = it.difficulty,
                estBlocks = it.estBlocks,
                status = it.status,
                blocksDone = it.blocksDone,
                revisionCount = it.revisionCount,
                confidence = it.confidence,
                needsHelp = it.needsHelp,
                order = it.order
            )
        }

        val schedulerSessions = existingSessions.map {
            SchedulerSession(
                id = it.id,
                dateEpochDay = it.dateEpochDay,
                topicId = it.topicId,
                subjectId = topics.find { t -> t.id == it.topicId }?.subjectId ?: "",
                kind = it.kind,
                blocks = it.blocks,
                status = it.status,
                pinned = it.pinned,
                createdBy = it.createdBy,
                actualMinutes = it.actualMinutes,
                startedAt = it.startedAt
            )
        }

        val result = Scheduler.generatePlan(
            todayEpochDay = todayEpochDay,
            subjects = schedulerSubjects,
            topics = schedulerTopics,
            existingSessions = schedulerSessions,
            availability = availability,
            dueCardsCountByTopic = dueCardsByTopic
        )

        val newDbSessions = result.newSessions.map {
            Session(
                id = it.id,
                date = DateUtil.epochDayToIso(it.dateEpochDay),
                topicId = it.topicId,
                kind = it.kind,
                blocks = it.blocks,
                status = it.status,
                pinned = it.pinned,
                createdBy = it.createdBy
            )
        }

        database.withTransaction {
            sessionDao.deleteFuturePendingAutoSessions(todayIso)
            sessionDao.insertSessions(newDbSessions)
        }

        return result
    }

    // Sample Data Loader
    suspend fun loadSampleData(todayEpochDay: Long = DateUtil.todayEpochDay()) {
        createSnapshot("Before loading sample data")
        val s1Id = "sample_power_systems"
        val s2Id = "sample_dsp"
        val s3Id = "sample_eng_math"

        val s1 = Subject(
            id = s1Id,
            name = "Power Systems",
            colorIndex = 0, // Indigo
            examDate = DateUtil.epochDayToIso(todayEpochDay + 18),
            weight = 3,
            isSample = true
        )
        val s2 = Subject(
            id = s2Id,
            name = "Digital Signal Processing",
            colorIndex = 1, // Teal
            examDate = DateUtil.epochDayToIso(todayEpochDay + 11),
            weight = 2,
            isSample = true
        )
        val s3 = Subject(
            id = s3Id,
            name = "Engineering Mathematics",
            colorIndex = 2, // Ochre
            examDate = DateUtil.epochDayToIso(todayEpochDay + 25),
            weight = 2,
            isSample = true
        )

        val sampleTopics = listOf(
            // Power Systems topics (9)
            Topic("t_ps_1", s1Id, "Transmission Line Parameters", difficulty = 3, estBlocks = 2, status = "solid", blocksDone = 2, revisionCount = 1, confidence = 85, order = 1, isSample = true),
            Topic("t_ps_2", s1Id, "Line Model and Performance", difficulty = 4, estBlocks = 3, status = "learning", blocksDone = 1, revisionCount = 0, confidence = 60, order = 2, isSample = true),
            Topic("t_ps_3", s1Id, "Admittance Model and Network Calc", difficulty = 3, estBlocks = 2, status = "learning", blocksDone = 0, revisionCount = 0, confidence = 50, order = 3, isSample = true),
            Topic("t_ps_4", s1Id, "Power Flow Analysis (Gauss-Seidel)", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 45, order = 4, isSample = true),
            Topic("t_ps_5", s1Id, "Newton-Raphson Power Flow", difficulty = 5, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 35, needsHelp = true, order = 5, isSample = true),
            Topic("t_ps_6", s1Id, "Symmetrical Fault Analysis", difficulty = 3, estBlocks = 2, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, order = 6, isSample = true),
            Topic("t_ps_7", s1Id, "Symmetrical Components", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 40, order = 7, isSample = true),
            Topic("t_ps_8", s1Id, "Unsymmetrical Faults", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 40, order = 8, isSample = true),
            Topic("t_ps_9", s1Id, "Power System Stability", difficulty = 5, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 30, needsHelp = true, order = 9, isSample = true),

            // DSP topics (8)
            Topic("t_dsp_1", s2Id, "Discrete-Time Signals & Systems", difficulty = 2, estBlocks = 2, status = "solid", blocksDone = 2, revisionCount = 2, confidence = 90, order = 1, isSample = true),
            Topic("t_dsp_2", s2Id, "Z-Transform and ROC", difficulty = 3, estBlocks = 2, status = "revised", blocksDone = 2, revisionCount = 1, confidence = 75, order = 2, isSample = true),
            Topic("t_dsp_3", s2Id, "Discrete Fourier Transform (DFT)", difficulty = 4, estBlocks = 3, status = "learning", blocksDone = 1, revisionCount = 0, confidence = 55, order = 3, isSample = true),
            Topic("t_dsp_4", s2Id, "Fast Fourier Transform (FFT)", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 45, order = 4, isSample = true),
            Topic("t_dsp_5", s2Id, "FIR Filter Design (Window Method)", difficulty = 3, estBlocks = 2, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, order = 5, isSample = true),
            Topic("t_dsp_6", s2Id, "IIR Filter Design (Bilinear Trans)", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 40, order = 6, isSample = true),
            Topic("t_dsp_7", s2Id, "Filter Realization and Structures", difficulty = 3, estBlocks = 2, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, order = 7, isSample = true),
            Topic("t_dsp_8", s2Id, "Multirate Signal Processing", difficulty = 5, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 35, needsHelp = true, order = 8, isSample = true),

            // Engineering Math topics (10)
            Topic("t_em_1", s3Id, "First-Order ODEs", difficulty = 2, estBlocks = 2, status = "solid", blocksDone = 2, revisionCount = 1, confidence = 85, order = 1, isSample = true),
            Topic("t_em_2", s3Id, "Higher-Order Linear ODEs", difficulty = 3, estBlocks = 2, status = "solid", blocksDone = 2, revisionCount = 1, confidence = 80, order = 2, isSample = true),
            Topic("t_em_3", s3Id, "Laplace Transforms & Inverses", difficulty = 3, estBlocks = 2, status = "revised", blocksDone = 2, revisionCount = 1, confidence = 75, order = 3, isSample = true),
            Topic("t_em_4", s3Id, "Fourier Series and Integrals", difficulty = 4, estBlocks = 3, status = "learning", blocksDone = 1, revisionCount = 0, confidence = 60, order = 4, isSample = true),
            Topic("t_em_5", s3Id, "Partial Differential Equations", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, order = 5, isSample = true),
            Topic("t_em_6", s3Id, "Matrices and Eigenvalues", difficulty = 3, estBlocks = 2, status = "new", blocksDone = 0, revisionCount = 0, confidence = 55, order = 6, isSample = true),
            Topic("t_em_7", s3Id, "Vector Calculus (Div, Grad, Curl)", difficulty = 4, estBlocks = 3, status = "new", blocksDone = 0, revisionCount = 0, confidence = 45, order = 7, isSample = true),
            Topic("t_em_8", s3Id, "Complex Integration & Residues", difficulty = 5, estBlocks = 4, status = "new", blocksDone = 0, revisionCount = 0, confidence = 40, needsHelp = true, order = 8, isSample = true),
            Topic("t_em_9", s3Id, "Probability and Random Variables", difficulty = 3, estBlocks = 2, status = "new", blocksDone = 0, revisionCount = 0, confidence = 50, order = 9, isSample = true),
            Topic("t_em_10", s3Id, "Numerical Integration and Root Finding", difficulty = 2, estBlocks = 1, status = "new", blocksDone = 0, revisionCount = 0, confidence = 60, order = 10, isSample = true)
        )

        val sampleCards = listOf(
            Card("c_1", "t_ps_1", "What is the Ferranti effect?", "The voltage at the receiving end of a lightly loaded or open transmission line becomes higher than at the sending end due to line charging capacitance.", 2.5, 3, 2, 0, DateUtil.epochDayToIso(todayEpochDay), isSample = true),
            Card("c_2", "t_ps_1", "Formula for characteristic impedance \$Z_0\$ of a lossless line", "\$Z_0 = \\sqrt{\\frac{L}{C}}\$", 2.6, 5, 3, 0, DateUtil.epochDayToIso(todayEpochDay), isSample = true),
            Card("c_3", "t_dsp_2", "What is the ROC for a causal, stable LTI system in the z-plane?", "The region of convergence extends outside the outermost pole and includes the unit circle |z| = 1.", 2.5, 1, 1, 0, DateUtil.epochDayToIso(todayEpochDay), isSample = true),
            Card("c_4", "t_dsp_2", "Z-transform of unit step \$u[n]\$", "\$X(z) = \\frac{1}{1 - z^{-1}}\$ with ROC |z| > 1", 2.4, 2, 1, 0, DateUtil.epochDayToIso(todayEpochDay), isSample = true),
            Card("c_5", "t_em_3", "Laplace transform of \$e^{at} \\cos(\\omega t)\$", "\$\\frac{s - a}{(s - a)^2 + \\omega^2}\$", 2.5, 4, 2, 0, DateUtil.epochDayToIso(todayEpochDay), isSample = true)
        )

        val sampleProblems = listOf(
            Problem("p_1", "t_ps_1", "A 220 kV, 3-phase, 50 Hz, 200 km line has \$L = 1.25\$ mH/km and \$C = 0.009\$ \$\\mu\$F/km. Find the surge impedance \$Z_c\$ and surge impedance loading (SIL).", "\$Z_c = \\sqrt{L/C} = \\sqrt{1.25 \\times 10^{-3} / 0.009 \\times 10^{-6}} \\approx 372.7\\ \\Omega\$. SIL = \$V_L^2 / Z_c = 220^2 / 372.7 \\approx 129.86\$ MW.", 10, "Textbook Ch 5, Ex 4", 1, "got", isSample = true),
            Problem("p_2", "t_dsp_2", "Determine the causal inverse z-transform of \$X(z) = \\frac{1}{(1 - 0.5z^{-1})(1 - 0.25z^{-1})}\$.", "Partial fraction expansion: \$X(z) = \\frac{2}{1 - 0.5z^{-1}} - \\frac{1}{1 - 0.25z^{-1}}\$. Inverse: \$x[n] = (2(0.5)^n - (0.25)^n)u[n]\$.", 8, "Midterm 2023", 2, "partly", isSample = true)
        )

        val samplePastPapers = listOf(
            PastPaper("pp_1", s1Id, "Final Exam 2023", done = true, scorePercent = 82),
            PastPaper("pp_2", s1Id, "Final Exam 2022", done = false, scorePercent = null),
            PastPaper("pp_3", s2Id, "Final Exam 2023", done = false, scorePercent = null)
        )

        database.withTransaction {
            subjectDao.insertSubjects(listOf(s1, s2, s3))
            topicDao.insertTopics(sampleTopics)
            cardDao.insertCards(sampleCards)
            problemDao.insertProblems(sampleProblems)
            pastPaperDao.insertPastPapers(samplePastPapers)
        }

        // Generate plan for sample data
        val avail = SchedulerAvailability(
            weekdayMinutes = listOf(180, 180, 180, 180, 180, 180, 120),
            focusMin = 25,
            bufferDays = 2
        )
        regeneratePlan(todayEpochDay, avail, "Sample data plan generation")
    }

    suspend fun removeSampleData(todayEpochDay: Long = DateUtil.todayEpochDay(), availability: SchedulerAvailability) {
        createSnapshot("Before removing sample data")
        database.withTransaction {
            sessionDao.deleteSampleSessions()
            cardDao.deleteSampleCards()
            problemDao.deleteSampleProblems()
            topicDao.deleteSampleTopics()
            subjectDao.deleteSampleSubjects()
        }
        regeneratePlan(todayEpochDay, availability, "After removing sample data")
    }

    suspend fun resetAllData() {
        createSnapshot("Before factory reset")
        database.withTransaction {
            subjectDao.deleteAll()
            topicDao.deleteAll()
            sessionDao.deleteAll()
            cardDao.deleteAll()
            problemDao.deleteAll()
            pastPaperDao.deleteAll()
            pomodoroLogDao.deleteAll()
        }
    }

    // Export & Import
    suspend fun exportToJson(): String {
        val payload = DatabaseBackupPayload(
            subjects = subjectDao.getAllSubjects(),
            topics = topicDao.getAllTopics(),
            sessions = sessionDao.getAllSessions(),
            cards = cardDao.getAllCards(),
            problems = problemDao.getAllProblems(),
            pastPapers = pastPaperDao.getAllPastPapers(),
            pomodoroLogs = pomodoroLogDao.getAllLogs()
        )
        val backupFile = ProstutiBackupFile(
            app = "Prostuti",
            schemaVersion = 1,
            exportedAt = System.currentTimeMillis(),
            data = payload
        )
        return json.encodeToString(backupFile)
    }

    suspend fun importBackup(
        backupJson: String,
        mode: String, // "replace" or "merge"
        todayEpochDay: Long,
        availability: SchedulerAvailability
    ): Result<String> {
        return try {
            val backupFile = json.decodeFromString<ProstutiBackupFile>(backupJson)
            if (backupFile.app != "Prostuti") {
                return Result.failure(IllegalArgumentException("Invalid file: not a Prostuti backup."))
            }
            val payload = backupFile.data

            createSnapshot("Before import backup ($mode)")

            database.withTransaction {
                if (mode == "replace") {
                    subjectDao.deleteAll()
                    topicDao.deleteAll()
                    sessionDao.deleteAll()
                    cardDao.deleteAll()
                    problemDao.deleteAll()
                    pastPaperDao.deleteAll()
                    pomodoroLogDao.deleteAll()

                    subjectDao.insertSubjects(payload.subjects)
                    topicDao.insertTopics(payload.topics)
                    sessionDao.insertSessions(payload.sessions)
                    cardDao.insertCards(payload.cards)
                    problemDao.insertProblems(payload.problems)
                    pastPaperDao.insertPastPapers(payload.pastPapers)
                    pomodoroLogDao.insertLogs(payload.pomodoroLogs)
                } else {
                    // Merge mode: newer createdAt wins
                    val existingSubjects = subjectDao.getAllSubjects().associateBy { it.id }
                    val mergedSubjects = payload.subjects.filter { newSubj ->
                        val ex = existingSubjects[newSubj.id]
                        ex == null || newSubj.createdAt >= ex.createdAt
                    }
                    subjectDao.insertSubjects(mergedSubjects)
                    topicDao.insertTopics(payload.topics)
                    sessionDao.insertSessions(payload.sessions)
                    cardDao.insertCards(payload.cards)
                    problemDao.insertProblems(payload.problems)
                    pastPaperDao.insertPastPapers(payload.pastPapers)
                    pomodoroLogDao.insertLogs(payload.pomodoroLogs)
                }
            }

            regeneratePlan(todayEpochDay, availability, "After importing backup")
            val summary = "${payload.subjects.size} subjects, ${payload.topics.size} topics, ${payload.cards.size} cards, ${payload.sessions.size} sessions imported."
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to import backup: ${e.message}"))
        }
    }
}
