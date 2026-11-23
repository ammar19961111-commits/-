package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Dhikr
import kotlinx.coroutines.flow.Flow

@Dao
interface DhikrDao {

    @Query("SELECT * FROM dhikr ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr WHERE category = :category ORDER BY sortOrder ASC, id ASC")
    fun observeByCategory(category: String): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr WHERE category IN (:categories) ORDER BY category ASC, sortOrder ASC, id ASC")
    fun observeByCategories(categories: List<String>): Flow<List<Dhikr>>

    @Query(
        """
        SELECT * FROM dhikr
        WHERE category IN ('morning', 'evening')
        ORDER BY CASE category WHEN 'morning' THEN 0 ELSE 1 END, sortOrder ASC, id ASC
        """
    )
    fun observeMorningAndEvening(): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr WHERE category = :category ORDER BY sortOrder ASC, id ASC")
    suspend fun getByCategory(category: String): List<Dhikr>

    @Query("SELECT * FROM dhikr WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Dhikr?

    @Query("SELECT * FROM dhikr WHERE isFavorite = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeFavorites(): Flow<List<Dhikr>>

    @Query("SELECT COUNT(*) FROM dhikr")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dhikr: Dhikr): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Dhikr>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Dhikr>)

    @Update
    suspend fun update(dhikr: Dhikr)

    @Delete
    suspend fun delete(dhikr: Dhikr)

    @Query("UPDATE dhikr SET currentCount = :count WHERE id = :id")
    suspend fun updateProgress(id: Int, count: Int)

    @Query("UPDATE dhikr SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE dhikr SET currentCount = 0 WHERE category = :category")
    suspend fun resetCategory(category: String)

    @Query("UPDATE dhikr SET currentCount = 0")
    suspend fun resetAllProgress()

    @Query("UPDATE dhikr SET currentCount = 0, isFavorite = 0")
    suspend fun resetAllProgressAndFavorites()

    @Query("UPDATE dhikr SET currentCount = :count, isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateProgressAndFavorite(id: Int, count: Int, isFavorite: Boolean)
}
