package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChoreRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase
) {

    suspend fun createChore(
        householdId: String,
        title: String,
        dueDate: Long,
        assignedTo: String,
        frequency: String?,
        createdBy: String
    ): Result<FirebaseChore> {
        return try {
            val choreId = UUID.randomUUID().toString()

            val chore = FirebaseChore(
                id = choreId,
                householdId = householdId,
                title = title,
                dueDate = dueDate,
                assignedTo = assignedTo,
                frequency = frequency,
                createdBy = createdBy,
                isCompleted = false
            )

            // save to firestore
            firestore.collection("households")
                .document(householdId)
                .collection("chores")
                .document(choreId)
                .set(chore)
                .await()

            // save to room
            database.choreDao().insert(chore.toEntity())

            Result.success(chore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChore(chore: FirebaseChore): Result<Unit> {
        return try {
            // update firestore
            firestore.collection("households")
                .document(chore.householdId)
                .collection("chores")
                .document(chore.id)
                .set(chore)
                .await()

            // update room
            database.choreDao().update(chore.toEntity())

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChore(householdId: String, choreId: String): Result<Unit> {
        return try {
            // delete from Firestore
            firestore.collection("households")
                .document(householdId)
                .collection("chores")
                .document(choreId)
                .delete()
                .await()

            // delete from room
            val chore = database.choreDao().getChoreByIdOnce(choreId)
            if (chore != null) {
                database.choreDao().delete(chore)
            } else {
                throw Exception("Chore not found")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChoresByHousehold(householdId: String): Flow<List<com.mobicom.s18.kasama.data.local.entities.Chore>> {
        return database.choreDao().getChoresByHousehold(householdId)
    }

    suspend fun syncChoresFromFirestore(householdId: String) {
        try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("chores")
                .get()
                .await()

            val chores = snapshot.toObjects(FirebaseChore::class.java)

            chores.forEach { chore ->
                database.choreDao().insert(chore.toEntity())
            }
        } catch (e: Exception) {
            println(e.message)
        }
    }
}