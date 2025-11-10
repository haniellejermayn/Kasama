package com.mobicom.s18.kasama.data.local.dao

import androidx.room.*
import com.mobicom.s18.kasama.data.local.entities.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Update
    suspend fun update(note: Note)

    @Query("SELECT * FROM notes WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getNotesByHousehold(householdId: String): Flow<List<Note>>

    @Delete
    suspend fun delete(note: Note)
}