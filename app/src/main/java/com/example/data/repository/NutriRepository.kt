package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.ai.EstimatedMeal
import com.example.data.ai.GeminiMealService
import com.example.data.local.MacroGoalEntity
import com.example.data.local.MealEntity
import com.example.data.local.NutriSnapDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NutriRepository(
    private val dao: NutriSnapDao,
    private val geminiService: GeminiMealService = GeminiMealService()
) {

    fun getTodayMeals(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>> {
        return dao.getMealsForDate(startOfDay, endOfDay)
    }

    fun getAllMeals(): Flow<List<MealEntity>> {
        return dao.getAllMeals()
    }

    fun getMacroGoals(): Flow<MacroGoalEntity> {
        return dao.getMacroGoals().map { saved ->
            saved ?: MacroGoalEntity(
                id = 1,
                targetCalories = 2000,
                targetProtein = 150f,
                targetCarbs = 200f,
                targetFats = 65f
            )
        }
    }

    suspend fun saveMeal(meal: MealEntity): Long {
        return dao.insertMeal(meal)
    }

    suspend fun deleteMeal(mealId: Long) {
        dao.deleteMealById(mealId)
    }

    suspend fun updateGoals(goals: MacroGoalEntity) {
        dao.setMacroGoals(goals)
    }

    suspend fun analyzeMeal(
        text: String?,
        bitmap: Bitmap?,
        customApiKey: String? = null
    ): Result<EstimatedMeal> {
        return geminiService.analyzeMeal(text, bitmap, customApiKey)
    }
}
