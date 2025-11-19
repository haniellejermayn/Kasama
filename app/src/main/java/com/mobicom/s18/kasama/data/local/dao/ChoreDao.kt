package com.mobicom.s18.kasama.data.local.dao

import androidx.room.*
import com.mobicom.s18.kasama.data.local.entities.Chore
import kotlinx.coroutines.flow.Flow

@Dao
interface ChoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chore: Chore)

    @Update
    suspend fun update(chore: Chore)

    @Query("SELECT * FROM chores WHERE id = :choreId")
    suspend fun getChoreByIdOnce(choreId: String): Chore?

    @Query("SELECT * FROM chores WHERE householdId = :householdId ORDER BY dueDate ASC")
    fun getChoresByHousehold(householdId: String): Flow<List<Chore>>

    @Query("""
        SELECT * FROM chores 
        WHERE householdId = :householdId
        AND (isCompleted = 0 OR frequency IS NULL OR frequency = 'never')
        ORDER BY dueDate ASC
    """)
    fun getActiveChoresByHousehold(householdId: String): Flow<List<Chore>>

    @Query("""
        SELECT * FROM chores 
        WHERE householdId = :householdId
        AND (isCompleted = 0 OR frequency IS NULL OR frequency = 'never' 
             OR (isCompleted = 1 AND completedAt > :cutoffTime))
        ORDER BY 
            CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getActiveChoresWithRecentCompleted(
        householdId: String,
        cutoffTime: Long = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
    ): Flow<List<Chore>>

    @Query("SELECT * FROM chores WHERE householdId = :householdId AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getIncompleteChores(householdId: String): Flow<List<Chore>>

    @Query("SELECT * FROM chores WHERE assignedTo = :userId AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getUserChores(userId: String): Flow<List<Chore>>

    @Delete
    suspend fun delete(chore: Chore)

    // Sync methods
    @Query("SELECT * FROM chores WHERE isSynced = 0")
    suspend fun getUnsyncedChores(): List<Chore>

    @Query("UPDATE chores SET isSynced = 1 WHERE id = :choreId")
    suspend fun markAsSynced(choreId: String)
}