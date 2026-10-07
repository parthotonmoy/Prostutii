package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CardDao
import com.example.data.dao.PastPaperDao
import com.example.data.dao.PomodoroLogDao
import com.example.data.dao.ProblemDao
import com.example.data.dao.SessionDao
import com.example.data.dao.SnapshotDao
import com.example.data.dao.SubjectDao
import com.example.data.dao.TopicDao
import com.example.model.Card
import com.example.model.PastPaper
import com.example.model.PomodoroLog
import com.example.model.Problem
import com.example.model.Session
import com.example.model.Snapshot
import com.example.model.Subject
import com.example.model.Topic

@Database(
    entities = [
        Subject::class,
        Topic::class,
        Session::class,
        Card::class,
        Problem::class,
        PastPaper::class,
        PomodoroLog::class,
        Snapshot::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun sessionDao(): SessionDao
    abstract fun cardDao(): CardDao
    abstract fun problemDao(): ProblemDao
    abstract fun pastPaperDao(): PastPaperDao
    abstract fun pomodoroLogDao(): PomodoroLogDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATIONS: Array<Migration> = arrayOf(
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Placeholder for future schema migrations
                }
            }
        )

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prostuti.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
