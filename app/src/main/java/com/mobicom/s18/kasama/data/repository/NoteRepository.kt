package com.mobicom.s18.kasama.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseNote
import kotlinx.coroutines.flow.Flow
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

            // save to firestore
            firestore.collection("households")
                .document(householdId)
                .collection("notes")
                .document(noteId)
                .set(note)
                .await()

            // save to room
            database.noteDao().insert(note.toEntity())

            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNote(householdId: String, noteId: String): Result<Unit> {
        return try {
            // delete from firestore
            firestore.collection("households")
                .document(householdId)
                .collection("notes")
                .document(noteId)
                .delete()
                .await()

            // delete from room
            val note = database.noteDao().getNoteByIdOnce(noteId)
            if (note != null) {
                database.noteDao().delete(note)
            } else {
                throw Exception("Note not found")
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
            println(e.message)
        }
    }
}