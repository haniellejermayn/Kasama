package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseUser
import kotlinx.coroutines.tasks.await

class UserRepository (
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase
) {
    suspend fun updateUserProfile(
        userId: String,
        phoneNumber: String?,
        birthdate: Long?,
        profilePictureUrl: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()

            if (phoneNumber != null) {
                updates["phoneNumber"] = phoneNumber
            }
            if (birthdate != null) {
                updates["birthdate"] = birthdate
            }
            if (profilePictureUrl != null) {
                updates["profilePictureUrl"] = profilePictureUrl
            }

            if (updates.isEmpty()) return Result.success(Unit)

            // update firestore
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            // update local db
            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    phoneNumber = phoneNumber ?: user.phoneNumber,
                    birthdate = birthdate ?: user.birthdate,
                    profilePictureUrl = profilePictureUrl ?: user.profilePictureUrl
                )
                database.userDao().update(updatedUser)
            } else {
                throw Exception("User not found")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        name: String,
        phoneNumber: String?,
        birthdate: Long?,
        profilePictureUrl: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()

            updates["displayName"] = name

            if (phoneNumber != null) {
                updates["phoneNumber"] = phoneNumber
            }
            if (birthdate != null) {
                updates["birthdate"] = birthdate
            }
            if (profilePictureUrl != null) {
                updates["profilePictureUrl"] = profilePictureUrl
            }

            if (updates.isEmpty()) return Result.success(Unit)

            // update firestore
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()

            // update local db
            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    displayName = name,
                    phoneNumber = phoneNumber ?: user.phoneNumber,
                    birthdate = birthdate ?: user.birthdate,
                    profilePictureUrl = profilePictureUrl ?: user.profilePictureUrl
                )
                database.userDao().update(updatedUser)
            } else {
                throw Exception("User not found")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check local cache first, then try Firebase
    suspend fun getUserById(userId: String): Result<FirebaseUser> {
        return try {
            // Try local cache first
            val localUser = database.userDao().getUserByIdOnce(userId)
            if (localUser != null) {
                return Result.success(localUser.toFirebaseModel())
            }

            // Fall back to Firestore
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            val user = doc.toObject(FirebaseUser::class.java)
                ?: throw Exception("User not found")

            // Cache locally for future use
            database.userDao().insert(user.toEntity())

            Result.success(user)
        } catch (e: Exception) {
            // If Firestore fails, try local one more time
            val localUser = database.userDao().getUserByIdOnce(userId)
            if (localUser != null) {
                return Result.success(localUser.toFirebaseModel())
            }
            Result.failure(e)
        }
    }
}