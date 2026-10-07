package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Card
import com.example.model.PastPaper
import com.example.model.PomodoroLog
import com.example.model.Problem
import com.example.model.Session
import com.example.model.Snapshot
import com.example.model.Subject
import com.example.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY archived ASC, examDate ASC, id ASC")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY archived ASC, examDate ASC, id ASC")
    suspend fun getAllSubjects(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("DELETE FROM subjects WHERE isSample = 1")
    suspend fun deleteSampleSubjects()

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY `order` ASC, id ASC")
    fun getTopicsBySubjectFlow(subjectId: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics ORDER BY subjectId ASC, `order` ASC, id ASC")
    fun getAllTopicsFlow(): Flow<List<Topic>>

    @Query("SELECT * FROM topics ORDER BY subjectId ASC, `order` ASC, id ASC")
    suspend fun getAllTopics(): List<Topic>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: String): Topic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsBySubject(subjectId: String)

    @Query("DELETE FROM topics WHERE isSample = 1")
    suspend fun deleteSampleTopics()

    @Query("DELETE FROM topics")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date ASC, id ASC")
    fun getAllSessionsFlow(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY id ASC")
    fun getSessionsForDateFlow(date: String): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY date ASC, id ASC")
    suspend fun getAllSessions(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: String): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: Session)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<Session>)

    @Update
    suspend fun updateSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("DELETE FROM sessions WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<String>)

    @Query("DELETE FROM sessions WHERE date >= :today AND status = 'pending' AND pinned = 0 AND createdBy = 'auto'")
    suspend fun deleteFuturePendingAutoSessions(today: String)

    @Query("DELETE FROM sessions WHERE topicId IN (SELECT id FROM topics WHERE subjectId = :subjectId)")
    suspend fun deleteSessionsBySubject(subjectId: String)

    @Query("DELETE FROM sessions WHERE topicId = :topicId")
    suspend fun deleteSessionsByTopic(topicId: String)

    @Query("DELETE FROM sessions WHERE topicId IN (SELECT id FROM topics WHERE isSample = 1)")
    suspend fun deleteSampleSessions()

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY createdAt ASC")
    fun getAllCardsFlow(): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE topicId = :topicId ORDER BY createdAt ASC")
    fun getCardsByTopicFlow(topicId: String): Flow<List<Card>>

    @Query("SELECT * FROM cards ORDER BY createdAt ASC")
    suspend fun getAllCards(): List<Card>

    @Query("SELECT * FROM cards WHERE due <= :today ORDER BY due ASC, createdAt ASC")
    suspend fun getDueCards(today: String): List<Card>

    @Query("SELECT * FROM cards WHERE topicId = :topicId AND due <= :today ORDER BY due ASC, createdAt ASC")
    suspend fun getDueCardsForTopic(topicId: String, today: String): List<Card>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Card)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Card>)

    @Update
    suspend fun updateCard(card: Card)

    @Delete
    suspend fun deleteCard(card: Card)

    @Query("DELETE FROM cards WHERE topicId = :topicId")
    suspend fun deleteCardsByTopic(topicId: String)

    @Query("DELETE FROM cards WHERE topicId IN (SELECT id FROM topics WHERE subjectId = :subjectId)")
    suspend fun deleteCardsBySubject(subjectId: String)

    @Query("DELETE FROM cards WHERE isSample = 1")
    suspend fun deleteSampleCards()

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}

@Dao
interface ProblemDao {
    @Query("SELECT * FROM problems WHERE topicId = :topicId ORDER BY id ASC")
    fun getProblemsByTopicFlow(topicId: String): Flow<List<Problem>>

    @Query("SELECT * FROM problems ORDER BY id ASC")
    fun getAllProblemsFlow(): Flow<List<Problem>>

    @Query("SELECT * FROM problems ORDER BY id ASC")
    suspend fun getAllProblems(): List<Problem>

    @Query("SELECT * FROM problems WHERE topicId = :topicId ORDER BY id ASC")
    suspend fun getProblemsByTopic(topicId: String): List<Problem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: Problem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<Problem>)

    @Update
    suspend fun updateProblem(problem: Problem)

    @Delete
    suspend fun deleteProblem(problem: Problem)

    @Query("DELETE FROM problems WHERE topicId = :topicId")
    suspend fun deleteProblemsByTopic(topicId: String)

    @Query("DELETE FROM problems WHERE topicId IN (SELECT id FROM topics WHERE subjectId = :subjectId)")
    suspend fun deleteProblemsBySubject(subjectId: String)

    @Query("DELETE FROM problems WHERE isSample = 1")
    suspend fun deleteSampleProblems()

    @Query("DELETE FROM problems")
    suspend fun deleteAll()
}

@Dao
interface PastPaperDao {
    @Query("SELECT * FROM past_papers WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getPastPapersBySubjectFlow(subjectId: String): Flow<List<PastPaper>>

    @Query("SELECT * FROM past_papers ORDER BY id ASC")
    fun getAllPastPapersFlow(): Flow<List<PastPaper>>

    @Query("SELECT * FROM past_papers ORDER BY id ASC")
    suspend fun getAllPastPapers(): List<PastPaper>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastPaper(pastPaper: PastPaper)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPastPapers(pastPapers: List<PastPaper>)

    @Update
    suspend fun updatePastPaper(pastPaper: PastPaper)

    @Delete
    suspend fun deletePastPaper(pastPaper: PastPaper)

    @Query("DELETE FROM past_papers WHERE subjectId = :subjectId")
    suspend fun deletePastPapersBySubject(subjectId: String)

    @Query("DELETE FROM past_papers")
    suspend fun deleteAll()
}

@Dao
interface PomodoroLogDao {
    @Query("SELECT * FROM pomodoro_logs ORDER BY date DESC, id DESC")
    fun getAllLogsFlow(): Flow<List<PomodoroLog>>

    @Query("SELECT * FROM pomodoro_logs WHERE date = :date")
    fun getLogsForDateFlow(date: String): Flow<List<PomodoroLog>>

    @Query("SELECT * FROM pomodoro_logs ORDER BY date DESC")
    suspend fun getAllLogs(): List<PomodoroLog>

    @Query("SELECT * FROM pomodoro_logs WHERE date = :date")
    suspend fun getLogsForDate(date: String): List<PomodoroLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: PomodoroLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<PomodoroLog>)

    @Query("DELETE FROM pomodoro_logs")
    suspend fun deleteAll()
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC")
    fun getAllSnapshotsFlow(): Flow<List<Snapshot>>

    @Query("SELECT * FROM snapshots ORDER BY createdAt DESC")
    suspend fun getAllSnapshots(): List<Snapshot>

    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshotById(id: String): Snapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: Snapshot)

    @Delete
    suspend fun deleteSnapshot(snapshot: Snapshot)

    @Query("DELETE FROM snapshots WHERE id NOT IN (SELECT id FROM snapshots ORDER BY createdAt DESC LIMIT 5)")
    suspend fun pruneOldSnapshots()

    @Query("DELETE FROM snapshots")
    suspend fun deleteAll()
}
