package com.janreins.nutrisnap.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database instance for NutriSnap meals and user macro targets.
 */
@Database(
    entities = [MealEntity::class, MacroGoalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NutriSnapDatabase : RoomDatabase() {
    abstract fun nutriSnapDao(): NutriSnapDao

    companion object {
        @Volatile
        private var INSTANCE: NutriSnapDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meals ADD COLUMN fiberGrams REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meals ADD COLUMN sugarGrams REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meals ADD COLUMN mealType TEXT NOT NULL DEFAULT 'Meal'")
                db.execSQL("ALTER TABLE meals ADD COLUMN isAiEstimated INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): NutriSnapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NutriSnapDatabase::class.java,
                    "nutrisnap_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
