package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NutriSnapDao {

    @Query("SELECT * FROM meals ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getMealsForDate(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Query("DELETE FROM meals")
    suspend fun clearAllMeals()

    @Query("SELECT * FROM macro_goals WHERE id = 1 LIMIT 1")
    fun getMacroGoals(): Flow<MacroGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMacroGoals(goals: MacroGoalEntity)
}
