package com.mobicom.s18.kasama.data.local.dao

import androidx.room.*
import com.mobicom.s18.kasama.data.local.entities.Household
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(household: Household)

    @Update
    suspend fun update(household: Household)

    @Query("SELECT * FROM households")
    fun getAllHouseholds(): Flow<List<Household>>

    @Query("SELECT * FROM households WHERE id = :householdId")
    fun getHouseholdById(householdId: String): Flow<Household?>

    @Query("SELECT * FROM households WHERE inviteCode = :code")
    suspend fun getHouseholdByInviteCode(code: String): Household?

    @Delete
    suspend fun delete(household: Household)
}