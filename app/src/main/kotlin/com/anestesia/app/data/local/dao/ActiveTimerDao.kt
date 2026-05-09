package com.anestesia.app.data.local.dao

import androidx.room.*
import com.anestesia.app.data.local.entity.ActiveTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveTimerDao {

    @Query("SELECT * FROM active_timers WHERE isExpired = 0 ORDER BY administeredAtMs DESC")
    fun getActiveTimersFlow(): Flow<List<ActiveTimerEntity>>

    @Query("SELECT * FROM active_timers WHERE isExpired = 0 ORDER BY administeredAtMs DESC")
    suspend fun getActiveTimers(): List<ActiveTimerEntity>

    @Query("SELECT * FROM active_timers WHERE id = :id")
    suspend fun getTimerById(id: Int): ActiveTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: ActiveTimerEntity): Long

    @Update
    suspend fun updateTimer(timer: ActiveTimerEntity)

    @Query("UPDATE active_timers SET alertAt80Sent = 1 WHERE id = :id")
    suspend fun markAlert80Sent(id: Int)

    @Query("UPDATE active_timers SET alertAt100Sent = 1 WHERE id = :id")
    suspend fun markAlert100Sent(id: Int)

    @Query("UPDATE active_timers SET isExpired = 1 WHERE id = :id")
    suspend fun markExpired(id: Int)

    @Delete
    suspend fun deleteTimer(timer: ActiveTimerEntity)

    @Query("DELETE FROM active_timers WHERE isExpired = 1")
    suspend fun clearExpiredTimers()

    @Query("DELETE FROM active_timers")
    suspend fun clearAll()
}
