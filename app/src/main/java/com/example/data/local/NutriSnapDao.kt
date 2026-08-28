package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for NutriSnap local database operations.
 */
@Dao
interface NutriSnapDao {

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    suspend fun getAllMealsDirect(): List<MealEntity>

    @Query("SELECT * FROM meals WHERE id = :mealId LIMIT 1")
    suspend fun getMealById(mealId: Long): MealEntity?

    @Query("SELECT * FROM meals WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getMealsForDate(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Query("DELETE FROM meals")
    suspend fun clearAllMeals()

    @Query("SELECT * FROM macro_goals WHERE id = 1 LIMIT 1")
    fun getMacroGoals(): Flow<MacroGoalEntity?>

    @Query("SELECT * FROM macro_goals WHERE id = 1 LIMIT 1")
    suspend fun getMacroGoalsDirect(): MacroGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMacroGoals(goals: MacroGoalEntity)

    @Transaction
    suspend fun overwriteAllData(meals: List<MealEntity>, goals: MacroGoalEntity) {
        clearAllMeals()
        if (meals.isNotEmpty()) {
            insertMeals(meals)
        }
        setMacroGoals(goals)
    }
}
