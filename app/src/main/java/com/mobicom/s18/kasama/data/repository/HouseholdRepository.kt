package com.mobicom.s18.kasama.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.Household
import com.mobicom.s18.kasama.data.remote.models.FirebaseUser
import com.mobicom.s18.kasama.data.remote.models.FirebaseHousehold
import kotlinx.coroutines.tasks.await
import java.util.UUID


class HouseholdRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase
) {

    private suspend fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var code: String
        do {
            code = (1..6)
                .map { chars.random() }
                .joinToString("")
            val existing = getHouseholdByInviteCode(code)
        } while (existing.isSuccess)

        return code
    }

    suspend fun createHousehold(
        name: String,
        createdBy: String
    ): Result<FirebaseHousehold> {
        return try {
            val householdId = UUID.randomUUID().toString()
            val inviteCode = generateInviteCode()

            val household = FirebaseHousehold(
                id = householdId,
                name = name,
                inviteCode = inviteCode,
                createdBy = createdBy,
                memberIds = listOf(createdBy)
            )

            // save to Firestore
            firestore.collection("households")
                .document(householdId)
                .set(household)
                .await()

            // update user's householdId
            firestore.collection("users")
                .document(createdBy)
                .update("householdId", householdId)
                .await()

            val userDoc = firestore.collection("users").document(createdBy)
            val snapshot = userDoc.get().await()
            val currentIDs = snapshot.get("householdIDs")

            val updatedIDs = when (currentIDs) {
                is List<*> -> currentIDs + household.id
                is String -> listOf(currentIDs, household.id)
                else -> listOf(household.id)
            }

            userDoc.update("householdIDs", updatedIDs).await()

            // ADDED: update user household ids
            firestore.collection("users")
                .document(createdBy)
                .update("householdIDs", FieldValue.arrayUnion(household.id))
                .await()

            // save to room
            database.householdDao().insert(household.toEntity())

            // update local user
            val user = database.userDao().getUserByIdOnce(createdBy)
            if (user != null) {
                val updatedIDs = (user.householdIDs + household.id).distinct()
                database.userDao().update(
                    user.copy(householdIDs = updatedIDs, householdId = household.id)
                )
            } else {
                throw Exception("User not found")
            }

            Result.success(household)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinHousehold(
        inviteCode: String,
        userId: String
    ): Result<FirebaseHousehold> {
        return try {
            // find household by invite code
            val querySnapshot = firestore.collection("households")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                throw Exception("Invalid invite code")
            }

            val doc = querySnapshot.documents.first()
            val household = doc.toObject(FirebaseHousehold::class.java)
                ?: throw Exception("Household not found")

            // check if user is already a member
            if (household.memberIds.contains(userId)) {
                throw Exception("You're already a member of this household")
            }

            // add user to household
            val updatedMembers = household.memberIds + userId
            firestore.collection("households")
                .document(household.id)
                .update("memberIds", updatedMembers)
                .await()

            // update user's householdId
            firestore.collection("users")
                .document(userId)
                .update("householdId", household.id)
                .await()

            val userDoc = firestore.collection("users").document(userId)
            val snapshot = userDoc.get().await()
            val currentIDs = snapshot.get("householdIDs")

            val updatedIDs = when (currentIDs) {
                is List<*> -> currentIDs + household.id
                is String -> listOf(currentIDs, household.id)
                else -> listOf(household.id)
            }

            userDoc.update("householdIDs", updatedIDs).await()

            // ADDED: update user household ids
            firestore.collection("users")
                .document(userId)
                .update("householdIDs", FieldValue.arrayUnion(household.id))
                .await()

            // save to Room
            val updatedHousehold = household.copy(memberIds = updatedMembers)
            database.householdDao().insert(updatedHousehold.toEntity())

            // update local user
            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                val updatedIDs = (user.householdIDs + household.id).distinct()
                database.userDao().update(
                    user.copy(householdIDs = updatedIDs, householdId = household.id)
                )
            }

            Result.success(updatedHousehold)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCurrentHousehold(
        householdId: String,
        userId: String
    ): Result<FirebaseHousehold> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("householdId", householdId)
                .await()

            val householdResult = getHouseholdById(householdId)
            if (householdResult.isFailure) {
                return Result.failure(householdResult.exceptionOrNull()!!)
            }
            val household = householdResult.getOrNull()!!

            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                database.userDao().update(
                    user.copy(householdId = householdId)
                )
            } else {
                return Result.failure(Exception("User not found locally"))
            }

            Result.success(household)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHousehold(
        householdId: String,
        newName: String,
        createdBy: String
    ): Result<FirebaseHousehold> {
        return try {
            firestore.collection("households")
                .document(householdId)
                .update(
                    mapOf(
                        "name" to newName,
                        "createdBy" to createdBy
                    )
                )
                .await()

            val firestoreResult = getHouseholdById(householdId)
            if (firestoreResult.isFailure) {
                Log.e("HH-UPDATE", "Failed to fetch updated household: ${firestoreResult.exceptionOrNull()}")
                return Result.failure(firestoreResult.exceptionOrNull()!!)
            }
            val updatedHousehold = firestoreResult.getOrNull()!!

            // Update Room
            val localHousehold = database.householdDao().getHouseholdByIdOnce(householdId)
            if (localHousehold == null) {
                return Result.failure(Exception("Household not found locally"))
            }
            database.householdDao().update(localHousehold.copy(name = newName))

            Result.success(updatedHousehold)
        } catch (e: Exception) {
            Log.e("HH-UPDATE", "Update failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteHousehold(
        householdId: String
    ): Result<FirebaseHousehold> {
        return try {
            // Fetch household from Firestore
            val householdSnapshot = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            if (!householdSnapshot.exists()) {
                return Result.failure(Exception("Household not found"))
            }

            val household = householdSnapshot.toObject(FirebaseHousehold::class.java)
                ?: return Result.failure(Exception("Failed to parse household"))

            val creatorId = household.createdBy

            // Check if there are other members besides the creator
            val otherMembers = household.memberIds.filter { it != creatorId }
            if (otherMembers.isNotEmpty()) {
                return Result.failure(Exception("Cannot delete household while other members exist."))
            }

            // Update the creator user locally and in Firestore
            val user = database.userDao().getUserByIdOnce(creatorId)
            if (user != null) {
                val updatedHouseholdIDs = user.householdIDs.toMutableList().apply { remove(householdId) }
                val newCurrentHouseholdId = if (user.householdId == householdId) updatedHouseholdIDs.firstOrNull() else user.householdId

                // Firestore update
                firestore.collection("users")
                    .document(creatorId)
                    .update(
                        mapOf(
                            "householdIDs" to updatedHouseholdIDs,
                            "householdId" to newCurrentHouseholdId
                        )
                    )
                    .await()

                // Room update
                database.userDao().update(
                    user.copy(
                        householdId = newCurrentHouseholdId,
                        householdIDs = updatedHouseholdIDs
                    )
                )
            }

            // Delete household in Firestore
            firestore.collection("households")
                .document(householdId)
                .delete()
                .await()

            // Delete household in local Room database
            val localHousehold = database.householdDao().getHouseholdByIdOnce(householdId)
            if (localHousehold != null) {
                database.householdDao().delete(localHousehold)
            }

            Result.success(household)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveHousehold(
        householdId: String,
        userId: String
    ): Result<FirebaseHousehold> {
        return try {
            val householdSnapshot = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            if (!householdSnapshot.exists()) {
                return Result.failure(Exception("Household not found"))
            }

            val household = householdSnapshot.toObject(FirebaseHousehold::class.java)
                ?: return Result.failure(Exception("Failed to parse household"))

            val updatedMemberIds = household.memberIds.toMutableList().apply { remove(userId) }
            firestore.collection("households")
                .document(householdId)
                .update("memberIds", updatedMemberIds)
                .await()

            val localHousehold = database.householdDao().getHouseholdByIdOnce(householdId)
            if (localHousehold != null) {
                database.householdDao().update(localHousehold.copy(memberIds = updatedMemberIds))
            }

            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                val updatedHouseholdIDs = user.householdIDs.toMutableList().apply { remove(householdId) }
                val newCurrentHouseholdId = if (user.householdId == householdId) {
                    updatedHouseholdIDs.firstOrNull()
                } else user.householdId

                firestore.collection("users")
                    .document(user.uid)
                    .update(
                        mapOf(
                            "householdId" to newCurrentHouseholdId,
                            "householdIDs" to updatedHouseholdIDs
                        )
                    )
                    .await()

                database.userDao().update(
                    user.copy(
                        householdId = newCurrentHouseholdId,
                        householdIDs = updatedHouseholdIDs
                    )
                )
            }

            val updatedHouseholdSnapshot = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            val updatedHousehold = updatedHouseholdSnapshot.toObject(FirebaseHousehold::class.java)
                ?: return Result.failure(Exception("Failed to fetch updated household"))

            Result.success(updatedHousehold)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseholdById(
        householdId: String
    ): Result<FirebaseHousehold> {
        return try {
            // Try local cache first
            val localHousehold = database.householdDao().getHouseholdByIdOnce(householdId)
            if (localHousehold != null) {
                return Result.success(
                    FirebaseHousehold(
                        id = localHousehold.id,
                        name = localHousehold.name,
                        inviteCode = localHousehold.inviteCode,
                        createdBy = localHousehold.createdBy,
                        memberIds = localHousehold.memberIds
                    )
                )
            }

            // Fall back to Firestore
            val doc = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            val household = doc.toObject(FirebaseHousehold::class.java)
                ?: throw Exception("Household not found")

            // Cache locally
            database.householdDao().insert(household.toEntity())

            Result.success(household)
        } catch (e: Exception) {
            // If Firestore fails, try local one more time
            val localHousehold = database.householdDao().getHouseholdByIdOnce(householdId)
            if (localHousehold != null) {
                return Result.success(
                    FirebaseHousehold(
                        id = localHousehold.id,
                        name = localHousehold.name,
                        inviteCode = localHousehold.inviteCode,
                        createdBy = localHousehold.createdBy,
                        memberIds = localHousehold.memberIds
                    )
                )
            }
            Result.failure(e)
        }
    }

    suspend fun getHouseholdByInviteCode(
        inviteCode: String
    ): Result<FirebaseHousehold> {
        return try {
            val querySnapshot = firestore.collection("households")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                throw Exception("Invalid invite code")
            }

            val doc = querySnapshot.documents.first()
            val household = doc.toObject(FirebaseHousehold::class.java)
                ?: throw Exception("Household not found")

            Result.success(household)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}