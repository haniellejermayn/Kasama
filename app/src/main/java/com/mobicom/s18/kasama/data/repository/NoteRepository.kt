package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NoteRepository(
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase
) {

    suspend fun createNote(
        householdId: String,
        title: String,
        content: String,
        createdBy: String
    ): Result<FirebaseNote> {
        return try {
            val noteId = UUID.randomUUID().toString()

            val note = FirebaseNote(
                id = noteId,
                householdId = householdId,
                title = title,
                content = content,
                createdBy = createdBy
            )

            // OFFLINE-FIRST: Save to Room first
            database.noteDao().insert(note.toEntity())

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(householdId)
                        .collection("notes")
                        .document(noteId)
                        .set(note)
                        .await()
                } catch (e: Exception) {
                    println("Failed to sync note to Firebase: ${e.message}")
                }
            }

            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(householdId: String, noteId: String): Result<Unit> {
        return try {
            // OFFLINE-FIRST: Delete from Room first
            val note = database.noteDao().getNoteByIdOnce(noteId)
            note?.let { database.noteDao().delete(it) }

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(householdId)
                        .collection("notes")
                        .document(noteId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    println("Failed to sync note deletion to Firebase: ${e.message}")
                }
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
                database.noteDao().insert(note.toEntity())
            }
        } catch (e: Exception) {
            println("Failed to sync notes from Firebase: ${e.message}")
        }
    }

    suspend fun updateNote(note: FirebaseNote): Result<Unit> {
        return try {
            // OFFLINE-FIRST: Update Room first
            database.noteDao().update(note.toEntity())

            // Then sync to Firebase (background)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("households")
                        .document(note.householdId)
                        .collection("notes")
                        .document(note.id)
                        .set(note)
                        .await()
                } catch (e: Exception) {
                    println("Failed to sync note update to Firebase: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}