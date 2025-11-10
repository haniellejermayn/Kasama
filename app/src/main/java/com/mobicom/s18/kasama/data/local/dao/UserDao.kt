package com.mobicom.s18.kasama.data.local.dao

import androidx.room.*
import com.mobicom.s18.kasama.data.local.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE uid = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE uid = :userId")
    suspend fun getUserByIdOnce(userId: String): User?

    @Delete
    suspend fun delete(user: User)
}