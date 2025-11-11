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

    @Query("SELECT * FROM chores where id = :choreId")
    suspend fun getChoreByIdOnce(choreId: String): Chore?

    @Query("SELECT * FROM chores WHERE householdId = :householdId ORDER BY dueDate ASC")
    fun getChoresByHousehold(householdId: String): Flow<List<Chore>>

    @Query("SELECT * FROM chores WHERE householdId = :householdId AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getIncompleteChores(householdId: String): Flow<List<Chore>>

    @Query("SELECT * FROM chores WHERE assignedTo = :userId AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getUserChores(userId: String): Flow<List<Chore>>

    @Delete
    suspend fun delete(chore: Chore)
}