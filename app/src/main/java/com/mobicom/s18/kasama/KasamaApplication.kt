package com.mobicom.s18.kasama

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.repository.AuthRepository
import com.mobicom.s18.kasama.data.repository.ChoreRepository
import com.mobicom.s18.kasama.data.repository.HouseholdRepository
import com.mobicom.s18.kasama.data.repository.NoteRepository
import com.mobicom.s18.kasama.data.repository.UserRepository

class KasamaApplication : Application() {

    // lazy initialization
    val database: KasamaDatabase by lazy {
        KasamaDatabase.getDatabase(this)
    }

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(firebaseAuth, firestore, database)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(firestore, database)
    }

    val householdRepository: HouseholdRepository by lazy {
        HouseholdRepository(firestore, database)
    }

    val choreRepository: ChoreRepository by lazy {
        ChoreRepository(firestore, database)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(firestore, database)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}