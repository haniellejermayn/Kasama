package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
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
        } while (database.householdDao().getHouseholdByInviteCode(code) != null)

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

            // save to room
            database.householdDao().insert(household.toEntity())

            // update local user
            val user = database.userDao().getUserByIdOnce(createdBy)
            if (user != null) {
                database.userDao().update(user.copy(householdId = householdId))
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

            // save to Room
            val updatedHousehold = household.copy(memberIds = updatedMembers)
            database.householdDao().insert(updatedHousehold.toEntity())

            // update local user
            val user = database.userDao().getUserByIdOnce(userId)
            if (user != null) {
                database.userDao().update(user.copy(householdId = household.id))
            } else {
                throw Exception("User not found")
            }

            Result.success(updatedHousehold)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseholdById(householdId: String): Result<FirebaseHousehold> {
        return try {
            val doc = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            val household = doc.toObject(FirebaseHousehold::class.java)
                ?: throw Exception("Household not found")

            Result.success(household)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}