package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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

            // OFFLINE-FIRST: Save to Room first (immediate)
            database.choreDao().insert(chore.toEntity())

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(householdId)
                        .collection("chores")
                        .document(choreId)
                        .set(chore)
                        .await()
                } catch (e: Exception) {
                    // Log error but don't fail - will sync later
                    println("Failed to sync chore to Firebase: ${e.message}")
                }
            }

            Result.success(chore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChore(chore: FirebaseChore): Result<Unit> {
        return try {
            // OFFLINE-FIRST: Update Room first
            database.choreDao().update(chore.toEntity())

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(chore.householdId)
                        .collection("chores")
                        .document(chore.id)
                        .set(chore)
                        .await()
                } catch (e: Exception) {
                    println("Failed to sync chore update to Firebase: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChore(householdId: String, choreId: String): Result<Unit> {
        return try {
            // OFFLINE-FIRST: Delete from Room first
            val chore = database.choreDao().getChoreByIdOnce(choreId)
            chore?.let { database.choreDao().delete(it) }

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(householdId)
                        .collection("chores")
                        .document(choreId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    println("Failed to sync chore deletion to Firebase: ${e.message}")
                }
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
            println("Failed to sync chores from Firebase: ${e.message}")
        }
    }
}