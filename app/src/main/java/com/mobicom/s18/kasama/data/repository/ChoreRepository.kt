package com.mobicom.s18.kasama.data.repository

import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.PendingDelete
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import com.mobicom.s18.kasama.workers.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChoreRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase,
    private val workManager: WorkManager
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

            // Save to Room with isSynced = false
            database.choreDao().insert(chore.toEntity(isSynced = false))

            // Schedule sync
            scheduleSyncWork()

            Result.success(chore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChore(chore: FirebaseChore): Result<Unit> {
        return try {
            // Update Room with isSynced = false
            database.choreDao().update(
                chore.toEntity(isSynced = false).copy(
                    lastModified = System.currentTimeMillis()
                )
            )

            // Schedule sync
            scheduleSyncWork()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChore(householdId: String, choreId: String): Result<Unit> {
        return try {
            val chore = database.choreDao().getChoreByIdOnce(choreId)

            if (chore != null) {
                // Delete from Room immediately
                database.choreDao().delete(chore)

                // Track pending delete
                database.pendingDeleteDao().insert(
                    PendingDelete(
                        itemId = choreId,
                        itemType = "chore",
                        householdId = householdId
                    )
                )

                // Schedule sync
                scheduleSyncWork()
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
                // Only insert/update if not locally modified
                val localChore = database.choreDao().getChoreByIdOnce(chore.id)
                if (localChore == null || localChore.isSynced) {
                    database.choreDao().insert(chore.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            println("Failed to sync chores from Firebase: ${e.message}")
        }
    }

    private fun scheduleSyncWork() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "sync_data",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}