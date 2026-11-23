package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.DailyActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Query("SELECT * FROM daily_activity ORDER BY dateKey ASC")
    fun observeAll(): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE dateKey >= :startDateKey ORDER BY dateKey ASC")
    fun observeFromDate(startDateKey: String): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE dateKey = :dateKey")
    suspend fun getByDate(dateKey: String): DailyActivity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(activity: DailyActivity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(activity: DailyActivity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<DailyActivity>)

    @Query("DELETE FROM daily_activity")
    suspend fun deleteAll()

    @Query(
        "UPDATE daily_activity SET dhikrCount = dhikrCount + :delta, updatedAt = :updatedAt WHERE dateKey = :dateKey"
    )
    suspend fun addDhikrCount(
        dateKey: String,
        delta: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE daily_activity SET tasbihCount = tasbihCount + :delta, updatedAt = :updatedAt WHERE dateKey = :dateKey"
    )
    suspend fun addTasbihCount(
        dateKey: String,
        delta: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE daily_activity SET sessionsCount = sessionsCount + :delta, updatedAt = :updatedAt WHERE dateKey = :dateKey"
    )
    suspend fun addSessionsCount(
        dateKey: String,
        delta: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE daily_activity SET dhikrCount = 0, tasbihCount = 0, sessionsCount = 0, updatedAt = :updatedAt WHERE dateKey = :dateKey"
    )
    suspend fun resetDate(dateKey: String, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    suspend fun recordDhikrIncrement(
        dateKey: String,
        delta: Int = 1,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        insertIgnore(DailyActivity(dateKey = dateKey, dhikrCount = 0, tasbihCount = 0, sessionsCount = 0, updatedAt = updatedAt))
        addDhikrCount(dateKey = dateKey, delta = delta, updatedAt = updatedAt)
    }

    @Transaction
    suspend fun recordTasbihIncrement(
        dateKey: String,
        delta: Int = 1,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        insertIgnore(DailyActivity(dateKey = dateKey, dhikrCount = 0, tasbihCount = 0, sessionsCount = 0, updatedAt = updatedAt))
        addTasbihCount(dateKey = dateKey, delta = delta, updatedAt = updatedAt)
    }

    @Transaction
    suspend fun recordSessionCompleted(
        dateKey: String,
        delta: Int = 1,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        if (delta <= 0) return
        insertIgnore(DailyActivity(dateKey = dateKey, dhikrCount = 0, tasbihCount = 0, sessionsCount = 0, updatedAt = updatedAt))
        addSessionsCount(dateKey = dateKey, delta = delta, updatedAt = updatedAt)
    }
}
