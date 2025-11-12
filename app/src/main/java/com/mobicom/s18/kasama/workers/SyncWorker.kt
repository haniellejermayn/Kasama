package com.mobicom.s18.kasama.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.toFirebaseModel
import kotlinx.coroutines.tasks.await

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = KasamaDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting sync...")

            syncChores()
            syncNotes()
            syncPendingDeletes()

            Log.d("SyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry() // WorkManager will retry with exponential backoff
        }
    }

    private suspend fun syncChores() {
        val unsyncedChores = database.choreDao().getUnsyncedChores()
        Log.d("SyncWorker", "Syncing ${unsyncedChores.size} chores")

        unsyncedChores.forEach { chore ->
            try {
                firestore.collection("households")
                    .document(chore.householdId)
                    .collection("chores")
                    .document(chore.id)
                    .set(chore.toFirebaseModel())
                    .await()

                database.choreDao().markAsSynced(chore.id)
                Log.d("SyncWorker", "Synced chore: ${chore.id}")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync chore ${chore.id}", e)
                throw e // Trigger retry
            }
        }
    }

    private suspend fun syncNotes() {
        val unsyncedNotes = database.noteDao().getUnsyncedNotes()
        Log.d("SyncWorker", "Syncing ${unsyncedNotes.size} notes")

        unsyncedNotes.forEach { note ->
            try {
                firestore.collection("households")
                    .document(note.householdId)
                    .collection("notes")
                    .document(note.id)
                    .set(note.toFirebaseModel())
                    .await()

                database.noteDao().markAsSynced(note.id)
                Log.d("SyncWorker", "Synced note: ${note.id}")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync note ${note.id}", e)
                throw e
            }
        }
    }

    private suspend fun syncPendingDeletes() {
        val pendingDeletes = database.pendingDeleteDao().getAllPendingDeletes()
        Log.d("SyncWorker", "Syncing ${pendingDeletes.size} deletions")

        pendingDeletes.forEach { delete ->
            try {
                val collection = when (delete.itemType) {
                    "chore" -> "chores"
                    "note" -> "notes"
                    else -> return@forEach
                }

                firestore.collection("households")
                    .document(delete.householdId)
                    .collection(collection)
                    .document(delete.itemId)
                    .delete()
                    .await()

                database.pendingDeleteDao().delete(delete)
                Log.d("SyncWorker", "Synced deletion: ${delete.itemId}")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync deletion ${delete.itemId}", e)
                throw e
            }
        }
    }
}