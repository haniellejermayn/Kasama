package com.mobicom.s18.kasama.data.local.dao

import androidx.room.*
import com.mobicom.s18.kasama.data.local.entities.PendingDelete

@Dao
interface PendingDeleteDao {
    @Insert
    suspend fun insert(pendingDelete: PendingDelete)

    @Query("SELECT * FROM pending_deletes")
    suspend fun getAllPendingDeletes(): List<PendingDelete>

    @Delete
    suspend fun delete(pendingDelete: PendingDelete)

    @Query("DELETE FROM pending_deletes WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)
}