package com.mobicom.s18.kasama.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.remote.models.FirebaseUser as FirebaseUserModel
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: KasamaDatabase // for local storage
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<String> {
        return try {
            // create firebase auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("User creation failed")

            // create user document in firestore
            val firebaseUser = FirebaseUserModel(
                uid = user.uid,
                email = email,
                displayName = displayName
            )

            firestore.collection("users")
                .document(user.uid)
                .set(firebaseUser)
                .await()

            // save to local Room database
            database.userDao().insert(firebaseUser.toEntity())

            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Login failed")

            // fetch user data from Firestore
            val userDoc = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            val firebaseUser = userDoc.toObject(FirebaseUserModel::class.java)
                ?: throw Exception("User data not found")

            // save to local Room database
            database.userDao().insert(firebaseUser.toEntity())

            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logOut() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null
}