package com.mobicom.s18.kasama.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseChore
import com.mobicom.s18.kasama.data.remote.models.FirebaseHousehold
import com.mobicom.s18.kasama.data.remote.models.FirebaseNote
import com.mobicom.s18.kasama.data.remote.models.FirebaseUser
import com.mobicom.s18.kasama.data.remote.models.toFirebaseModel
import kotlinx.coroutines.tasks.await

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = KasamaDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        return try {
            Log.d("SyncWorker", "Starting sync...")

            // Get current user's household
            val currentUserId = auth.currentUser?.uid
            if (currentUserId == null) {
                Log.d("SyncWorker", "No authenticated user, skipping sync")
                return Result.success()
            }

            // Get user's current household ID
            val householdId = getCurrentHouseholdId(currentUserId)

            // PUSH: Delete from Firestore first
            syncPendingDeletes()

            // PUSH: Sync local changes to Firestore
            syncChores()
            syncNotes()

            // PULL: Sync data FROM Firestore (new!)
            if (householdId != null) {
                pullChoresFromFirestore(householdId)
                pullNotesFromFirestore(householdId)
                pullHouseholdFromFirestore(householdId)
                pullHouseholdMembersFromFirestore(householdId)
            }

            Log.d("SyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    private suspend fun getCurrentHouseholdId(userId: String): String? {
        return try {
            // Try local first
            val localUser = database.userDao().getUserByIdOnce(userId)
            if (localUser?.householdId != null) {
                return localUser.householdId
            }

            // Fall back to Firestore
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getString("householdId")
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to get household ID", e)
            null
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
                throw e
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

    // Pull chores from Firestore
    private suspend fun pullChoresFromFirestore(householdId: String) {
        try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("chores")
                .get()
                .await()

            val remoteChores = snapshot.toObjects(FirebaseChore::class.java)
            Log.d("SyncWorker", "Pulling ${remoteChores.size} chores from Firestore")

            // Get pending deletes to skip
            val pendingDeleteIds = database.pendingDeleteDao()
                .getAllPendingDeletes()
                .filter { it.itemType == "chore" }
                .map { it.itemId }
                .toSet()

            remoteChores.forEach { chore ->
                if (chore.id in pendingDeleteIds) {
                    return@forEach
                }

                val localChore = database.choreDao().getChoreByIdOnce(chore.id)
                // Only update if local doesn't exist OR local is already synced (no pending local changes)
                if (localChore == null || localChore.isSynced) {
                    database.choreDao().insert(chore.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to pull chores from Firestore", e)
        }
    }

    // Pull notes from Firestore
    private suspend fun pullNotesFromFirestore(householdId: String) {
        try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("notes")
                .get()
                .await()

            val remoteNotes = snapshot.toObjects(FirebaseNote::class.java)
            Log.d("SyncWorker", "Pulling ${remoteNotes.size} notes from Firestore")

            // Get pending deletes to skip
            val pendingDeleteIds = database.pendingDeleteDao()
                .getAllPendingDeletes()
                .filter { it.itemType == "note" }
                .map { it.itemId }
                .toSet()

            remoteNotes.forEach { note ->
                if (note.id in pendingDeleteIds) {
                    return@forEach
                }

                val localNote = database.noteDao().getNoteByIdOnce(note.id)
                if (localNote == null || localNote.isSynced) {
                    database.noteDao().insert(note.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to pull notes from Firestore", e)
        }
    }

    // Pull household data from Firestore
    private suspend fun pullHouseholdFromFirestore(householdId: String) {
        try {
            val doc = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            val household = doc.toObject(FirebaseHousehold::class.java)
            if (household != null) {
                database.householdDao().insert(household.toEntity())
                Log.d("SyncWorker", "Pulled household: ${household.id}")
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to pull household from Firestore", e)
        }
    }

    // Pull household members from Firestore
    private suspend fun pullHouseholdMembersFromFirestore(householdId: String) {
        try {
            val householdDoc = firestore.collection("households")
                .document(householdId)
                .get()
                .await()

            val household = householdDoc.toObject(FirebaseHousehold::class.java)
            if (household != null) {
                household.memberIds.forEach { memberId ->
                    try {
                        val userDoc = firestore.collection("users")
                            .document(memberId)
                            .get()
                            .await()

                        val user = userDoc.toObject(FirebaseUser::class.java)
                        if (user != null) {
                            database.userDao().insert(user.toEntity())
                            Log.d("SyncWorker", "Pulled user: ${user.uid}")
                        }
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Failed to pull user $memberId", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Failed to pull household members", e)
        }
    }
}