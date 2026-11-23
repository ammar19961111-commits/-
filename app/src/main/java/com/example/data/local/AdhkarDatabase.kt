package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Counter
import com.example.data.model.DailyActivity
import com.example.data.model.Dhikr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [Dhikr::class, Counter::class, DailyActivity::class, ChatMessage::class],
    version = 5,
    exportSchema = false
)
abstract class AdhkarDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun counterDao(): CounterDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AdhkarDatabase? = null

        private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_activity` (
                        `dateKey` TEXT NOT NULL,
                        `dhikrCount` INTEGER NOT NULL DEFAULT 0,
                        `tasbihCount` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`dateKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `daily_activity` ADD COLUMN `sessionsCount` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestampMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AdhkarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdhkarDatabase::class.java,
                    "adhkar_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            databaseScope.launch {
                                INSTANCE?.let { database ->
                                    if (database.dhikrDao().getCount() == 0) {
                                        database.dhikrDao().insertAll(AdhkarSeedData.getInitialAdhkar())
                                    }
                                    if (database.counterDao().getCount() == 0) {
                                        database.counterDao().insertAll(AdhkarSeedData.getInitialCounters())
                                    }
                                }
                            }
                        }
                    })
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
