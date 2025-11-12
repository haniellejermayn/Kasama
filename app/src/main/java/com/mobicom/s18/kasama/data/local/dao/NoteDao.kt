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

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteByIdOnce(noteId: String): Note?

    @Query("SELECT * FROM notes WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getNotesByHousehold(householdId: String): Flow<List<Note>>

    @Delete
    suspend fun delete(note: Note)

    // Sync methods
    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<Note>

    @Query("UPDATE notes SET isSynced = 1 WHERE id = :noteId")
    suspend fun markAsSynced(noteId: String)
}