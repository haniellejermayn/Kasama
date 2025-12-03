package com.mobicom.s18.kasama.data.repository

import android.util.Log
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.PendingDelete
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import com.mobicom.s18.kasama.utils.RecurringChoreHelper
import com.mobicom.s18.kasama.workers.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChoreRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase,
    private val workManager: WorkManager
) {
    // Store active listener so we can remove it later
    private var choresListener: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

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

    suspend fun completeChore(choreId: String): Result<Unit> {
        return try {
            val chore = database.choreDao().getChoreByIdOnce(choreId)
                ?: return Result.failure(Exception("Chore not found"))

            val completedChore = chore.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            database.choreDao().update(completedChore)

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

    suspend fun uncompleteChore(choreId: String): Result<Unit> {
        return try {
            val chore = database.choreDao().getChoreByIdOnce(choreId)
                ?: return Result.failure(Exception("Chore not found"))

            val uncompletedChore = chore.copy(
                isCompleted = false,
                completedAt = null,
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            database.choreDao().update(uncompletedChore)

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
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
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

            val pendingDeleteIds = database.pendingDeleteDao()
                .getAllPendingDeletes()
                .filter { it.itemType == "chore" }
                .map { it.itemId }
                .toSet()

            chores.forEach { chore ->
                if (chore.id in pendingDeleteIds) {
                    return@forEach
                }

                val localChore = database.choreDao().getChoreByIdOnce(chore.id)
                if (localChore == null || localChore.isSynced) {
                    database.choreDao().insert(chore.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("ChoreRepository", "Failed to sync chores from Firebase: ${e.message}")
        }
    }

    // Start real-time listening for chores
    fun startRealtimeSync(householdId: String) {
        // Remove any existing listener first
        stopRealtimeSync()

        Log.d("ChoreRepository", "Starting realtime sync for household: $householdId")

        choresListener = firestore.collection("households")
            .document(householdId)
            .collection("chores")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChoreRepository", "Realtime sync error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    repositoryScope.launch {
                        try {
                            val pendingDeleteIds = database.pendingDeleteDao()
                                .getAllPendingDeletes()
                                .filter { it.itemType == "chore" }
                                .map { it.itemId }
                                .toSet()

                            val remoteChoreIds = mutableSetOf<String>()

                            snapshot.documents.forEach { doc ->
                                val chore = doc.toObject(FirebaseChore::class.java)
                                if (chore != null && chore.id !in pendingDeleteIds) {
                                    remoteChoreIds.add(chore.id)
                                    val localChore = database.choreDao().getChoreByIdOnce(chore.id)
                                    // Only update if local doesn't exist OR local is already synced
                                    if (localChore == null || localChore.isSynced) {
                                        database.choreDao().insert(chore.toEntity(isSynced = true))
                                    }
                                }
                            }

                            // Handle deletions - remove local chores that no longer exist in Firebase
                            val localChores = database.choreDao().getChoresByHousehold(householdId).first()
                            localChores.forEach { localChore ->
                                if (localChore.id !in remoteChoreIds && 
                                    localChore.isSynced && 
                                    localChore.id !in pendingDeleteIds) {
                                    // This chore was deleted by another user
                                    database.choreDao().delete(localChore)
                                    Log.d("ChoreRepository", "Deleted chore from remote: ${localChore.id}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ChoreRepository", "Error processing realtime update: ${e.message}")
                        }
                    }
                }
            }
    }

    // Stop real-time listening
    fun stopRealtimeSync() {
        choresListener?.remove()
        choresListener = null
        Log.d("ChoreRepository", "Stopped realtime sync")
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