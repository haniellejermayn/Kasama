package com.mobicom.s18.kasama.data.repository

import android.util.Log
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.PendingDelete
import com.mobicom.s18.kasama.data.remote.models.FirebaseNote
import com.mobicom.s18.kasama.workers.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

class NoteRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase,
    private val workManager: WorkManager
) {
    // Store active listener so we can remove it later
    private var notesListener: ListenerRegistration? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    suspend fun createNote(
        householdId: String,
        title: String,
        content: String,
        createdBy: String,
        profilePictureUrl: String?
    ): Result<FirebaseNote> {
        return try {
            val noteId = UUID.randomUUID().toString()

            val note = FirebaseNote(
                id = noteId,
                householdId = householdId,
                title = title,
                content = content,
                createdBy = createdBy,
                profilePictureUrl = profilePictureUrl
            )

            database.noteDao().insert(note.toEntity(isSynced = false))
            scheduleSyncWork()

            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNote(note: FirebaseNote): Result<Unit> {
        return try {
            database.noteDao().update(
                note.toEntity(isSynced = false).copy(
                    title = note.title,
                    content = note.content,
                    createdBy = note.createdBy,
                    profilePictureUrl = note.profilePictureUrl,
                    lastModified = System.currentTimeMillis()
                )
            )

            scheduleSyncWork()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(householdId: String, noteId: String): Result<Unit> {
        return try {
            val note = database.noteDao().getNoteByIdOnce(noteId)

            if (note != null) {
                database.noteDao().delete(note)

                database.pendingDeleteDao().insert(
                    PendingDelete(
                        itemId = noteId,
                        itemType = "note",
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

    fun getNotesByHousehold(householdId: String): Flow<List<com.mobicom.s18.kasama.data.local.entities.Note>> {
        return database.noteDao().getNotesByHousehold(householdId)
    }

    suspend fun syncNotesFromFirestore(householdId: String) {
        try {
            val snapshot = firestore.collection("households")
                .document(householdId)
                .collection("notes")
                .get()
                .await()

            val pendingDeleteIds = database.pendingDeleteDao()
                .getAllPendingDeletes()
                .filter { it.itemType == "note" }
                .map { it.itemId }
                .toSet()

            val notes = snapshot.toObjects(FirebaseNote::class.java)
            notes.forEach { note ->
                if (note.id in pendingDeleteIds) {
                    return@forEach
                }

                val localNote = database.noteDao().getNoteByIdOnce(note.id)
                if (localNote == null || localNote.isSynced) {
                    database.noteDao().insert(note.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Failed to sync notes from Firebase: ${e.message}")
        }
    }

    // NEW: Start real-time listening for notes
    fun startRealtimeSync(householdId: String) {
        // Remove any existing listener first
        stopRealtimeSync()

        Log.d("NoteRepository", "Starting realtime sync for household: $householdId")

        notesListener = firestore.collection("households")
            .document(householdId)
            .collection("notes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("NoteRepository", "Realtime sync error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    repositoryScope.launch {
                        try {
                            val pendingDeleteIds = database.pendingDeleteDao()
                                .getAllPendingDeletes()
                                .filter { it.itemType == "note" }
                                .map { it.itemId }
                                .toSet()

                            val remoteNoteIds = mutableSetOf<String>()

                            snapshot.documents.forEach { doc ->
                                val note = doc.toObject(FirebaseNote::class.java)
                                if (note != null && note.id !in pendingDeleteIds) {
                                    remoteNoteIds.add(note.id)
                                    val localNote = database.noteDao().getNoteByIdOnce(note.id)
                                    if (localNote == null || localNote.isSynced) {
                                        database.noteDao().insert(note.toEntity(isSynced = true))
                                    }
                                }
                            }

                            // Handle deletions
                            val localNotes = database.noteDao().getNotesByHousehold(householdId).first()
                            localNotes.forEach { localNote ->
                                if (localNote.id !in remoteNoteIds && 
                                    localNote.isSynced && 
                                    localNote.id !in pendingDeleteIds) {
                                    database.noteDao().delete(localNote)
                                    Log.d("NoteRepository", "Deleted note from remote: ${localNote.id}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("NoteRepository", "Error processing realtime update: ${e.message}")
                        }
                    }
                }
            }
    }

    // NEW: Stop real-time listening
    fun stopRealtimeSync() {
        notesListener?.remove()
        notesListener = null
        Log.d("NoteRepository", "Stopped realtime sync")
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