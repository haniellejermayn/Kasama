package com.mobicom.s18.kasama.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobicom.s18.kasama.data.local.dao.*
import com.mobicom.s18.kasama.data.local.entities.*

@Database(
    entities = [User::class, Household::class, Chore::class, Note::class, PendingDelete::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KasamaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun householdDao(): HouseholdDao
    abstract fun choreDao(): ChoreDao
    abstract fun noteDao(): NoteDao
    abstract fun pendingDeleteDao(): PendingDeleteDao

    companion object {
        @Volatile
        private var INSTANCE: KasamaDatabase? = null

        fun getDatabase(context: Context): KasamaDatabase {
            if (INSTANCE != null) {
                return INSTANCE!!
            } else {
                synchronized(this) {
                    if (INSTANCE == null) {
                        val instance = Room.databaseBuilder(
                            context.applicationContext,
                            KasamaDatabase::class.java,
                            "kasama_database"
                        )
                            .fallbackToDestructiveMigration()
                            .build()
                        INSTANCE = instance
                    }
                    return INSTANCE!!
                }
            }
        }
    }
}