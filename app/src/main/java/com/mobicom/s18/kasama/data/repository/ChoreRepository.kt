package com.mobicom.s18.kasama.data.repository

import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.PendingDelete
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import com.mobicom.s18.kasama.utils.RecurringChoreHelper
import com.mobicom.s18.kasama.workers.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
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

            database.choreDao().insert(chore.toEntity(isSynced = false))
            scheduleSyncWork()

            Result.success(chore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChore(chore: FirebaseChore): Result<Unit> {
        return try {
            database.choreDao().update(
                chore.toEntity(isSynced = false).copy(
                    lastModified = System.currentTimeMillis()
                )
            )
            scheduleSyncWork()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // handles recurring logic
    suspend fun completeChore(choreId: String): Result<Unit> {
        return try {
            val chore = database.choreDao().getChoreByIdOnce(choreId)
                ?: return Result.failure(Exception("Chore not found"))

            // mark as completed
            val completedChore = chore.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            database.choreDao().update(completedChore)

            // if recurring, create next instance
            if (RecurringChoreHelper.shouldCreateNextInstance(chore.frequency)) {
                val nextDueDate = RecurringChoreHelper.calculateNextDueDate(
                    chore.dueDate,
                    chore.frequency
                )

                if (nextDueDate != null) {
                    val nextChore = FirebaseChore(
                        id = UUID.randomUUID().toString(),
                        householdId = chore.householdId,
                        title = chore.title,
                        dueDate = nextDueDate,
                        assignedTo = chore.assignedTo,
                        frequency = chore.frequency,
                        createdBy = chore.createdBy,
                        isCompleted = false
                    )

                    database.choreDao().insert(nextChore.toEntity(isSynced = false))
                }
            }

            scheduleSyncWork()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // also handles recurring chores
    suspend fun uncompleteChore(choreId: String): Result<Unit> {
        return try {
            val chore = database.choreDao().getChoreByIdOnce(choreId)
                ?: return Result.failure(Exception("Chore not found"))

            // mark as not completed
            val uncompletedChore = chore.copy(
                isCompleted = false,
                completedAt = null,
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            database.choreDao().update(uncompletedChore)

            // if recurring, delete next instance
            if (RecurringChoreHelper.shouldCreateNextInstance(chore.frequency)) {
                val nextDueDate = RecurringChoreHelper.calculateNextDueDate(
                    chore.dueDate,
                    chore.frequency
                )

                if (nextDueDate != null) {
                    val allChores = database.choreDao()
                        .getChoresByHousehold(chore.householdId)
                        .first()

                    val nextInstance = allChores.find {
                        it.title == chore.title &&
                                it.assignedTo == chore.assignedTo &&
                                it.dueDate == nextDueDate &&
                                !it.isCompleted
                    }

                    nextInstance?.let {
                        database.choreDao().delete(it)
                        database.pendingDeleteDao().insert(
                            PendingDelete(
                                itemId = it.id,
                                itemType = "chore",
                                householdId = it.householdId
                            )
                        )
                    }
                }
            }

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
                database.choreDao().delete(chore)
                database.pendingDeleteDao().insert(
                    PendingDelete(
                        itemId = choreId,
                        itemType = "chore",
                        householdId = householdId
                    )
                )
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

    fun getActiveChoresByHousehold(householdId: String): Flow<List<com.mobicom.s18.kasama.data.local.entities.Chore>> {
        return database.choreDao().getActiveChoresByHousehold(householdId)
    }

    fun getActiveChoresWithRecentCompleted(householdId: String): Flow<List<com.mobicom.s18.kasama.data.local.entities.Chore>> {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        return database.choreDao().getActiveChoresWithRecentCompleted(householdId, cutoffTime)
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