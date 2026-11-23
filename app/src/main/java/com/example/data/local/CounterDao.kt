package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Counter
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {

    @Query("SELECT * FROM counter ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Counter>>

    @Query("SELECT * FROM counter WHERE id = :id")
    fun observeById(id: Int): Flow<Counter?>

    @Query("SELECT COUNT(*) FROM counter")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(counter: Counter): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(counters: List<Counter>)

    @Query("SELECT * FROM counter WHERE id = :id")
    suspend fun getById(id: Int): Counter?

    @Query(
        """
        UPDATE counter
        SET currentCount = CASE WHEN targetCount > 0 AND currentCount >= targetCount THEN 1 ELSE currentCount + 1 END,
            todayCount = todayCount + 1,
            totalCount = totalCount + 1,
            lastUpdatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun incrementAtomically(id: Int, updatedAt: Long = System.currentTimeMillis())

    @Query(
        "UPDATE counter SET currentCount = :currentCount, todayCount = :todayCount, totalCount = :totalCount, lastUpdatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Int,
        currentCount: Int,
        todayCount: Int,
        totalCount: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE counter SET targetCount = :targetCount, currentCount = 0 WHERE id = :id")
    suspend fun updateTargetAndReset(id: Int, targetCount: Int)

    @Query("UPDATE counter SET currentCount = 0 WHERE id = :id")
    suspend fun resetCycle(id: Int)

    @Query("UPDATE counter SET currentCount = 0, todayCount = 0")
    suspend fun resetDailyCounts()

    @Update
    suspend fun update(counter: Counter)

    @Delete
    suspend fun delete(counter: Counter)

    @Query("DELETE FROM counter WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM counter")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(counters: List<Counter>)
}
