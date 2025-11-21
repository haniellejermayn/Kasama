package com.mobicom.s18.kasama.data.repository

import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.local.entities.PendingDelete
import com.mobicom.s18.kasama.data.remote.models.FirebaseNote
import com.mobicom.s18.kasama.workers.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.lang.System
import java.util.UUID
import java.util.concurrent.TimeUnit

class NoteRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase,
    private val workManager: WorkManager
) {

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

            // Save to Room with isSynced = false
            database.noteDao().insert(note.toEntity(isSynced = false))

            // Schedule sync
            scheduleSyncWork()

            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNote(note: FirebaseNote): Result<Unit> {
        return try {
            // Update Room with isSynced = false
            database.noteDao().update(
                note.toEntity(isSynced = false).copy(
                    title = note.title,
                    content = note.content,
                    createdBy = note.createdBy,
                    profilePictureUrl = note.profilePictureUrl,
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

    suspend fun deleteNote(householdId: String, noteId: String): Result<Unit> {
        return try {
            val note = database.noteDao().getNoteByIdOnce(noteId)

            if (note != null) {
                // Delete from Room immediately
                database.noteDao().delete(note)

                // Track pending delete
                database.pendingDeleteDao().insert(
                    PendingDelete(
                        itemId = noteId,
                        itemType = "note",
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

            val notes = snapshot.toObjects(FirebaseNote::class.java)
            notes.forEach { note ->
                val localNote = database.noteDao().getNoteByIdOnce(note.id)
                if (localNote == null || localNote.isSynced) {
                    database.noteDao().insert(note.toEntity(isSynced = true))
                }
            }
        } catch (e: Exception) {
            println("Failed to sync notes from Firebase: ${e.message}")
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