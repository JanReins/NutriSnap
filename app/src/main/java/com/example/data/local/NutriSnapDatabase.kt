package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MealEntity::class, MacroGoalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NutriSnapDatabase : RoomDatabase() {
    abstract fun nutriSnapDao(): NutriSnapDao

    companion object {
        @Volatile
        private var INSTANCE: NutriSnapDatabase? = null

        fun getDatabase(context: Context): NutriSnapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriSnapDatabase::class.java,
                    "nutrisnap_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
