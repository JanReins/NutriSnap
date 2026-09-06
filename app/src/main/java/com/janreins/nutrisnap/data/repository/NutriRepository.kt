package com.janreins.nutrisnap.data.repository

import android.graphics.Bitmap
import com.janreins.nutrisnap.data.ai.EstimatedMeal
import com.janreins.nutrisnap.data.ai.GeminiMealService
import com.janreins.nutrisnap.data.local.MacroGoalEntity
import com.janreins.nutrisnap.data.local.MealEntity
import com.janreins.nutrisnap.data.local.NutriSnapDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth repository managing meal history, macro goals, and AI analysis.
 */
class NutriRepository(
    private val dao: NutriSnapDao,
    private val geminiService: GeminiMealService = GeminiMealService()
) {

    fun getTodayMeals(startOfDay: Long, endOfDay: Long): Flow<List<MealEntity>> {
        return dao.getMealsForDate(startOfDay, endOfDay)
    }

    fun getMealsBetween(startMillis: Long, endMillis: Long): Flow<List<MealEntity>> {
        return dao.getMealsBetween(startMillis, endMillis)
    }

    fun getAllMeals(): Flow<List<MealEntity>> {
        return dao.getAllMeals()
    }

    suspend fun getAllMealsDirect(): List<MealEntity> {
        return dao.getAllMealsDirect()
    }

    suspend fun getMealById(id: Long): MealEntity? {
        return dao.getMealById(id)
    }

    suspend fun getMacroGoalsDirect(): MacroGoalEntity {
        return dao.getMacroGoalsDirect() ?: MacroGoalEntity(
            id = 1,
            targetCalories = 2000,
            targetProtein = 150f,
            targetCarbs = 200f,
            targetFats = 65f
        )
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

    suspend fun updateMeal(meal: MealEntity) {
        dao.updateMeal(meal)
    }

    suspend fun deleteMeal(mealId: Long) {
        dao.deleteMealById(mealId)
    }

    suspend fun updateGoals(goals: MacroGoalEntity) {
        dao.setMacroGoals(goals)
    }

    suspend fun overwriteAllData(meals: List<MealEntity>, goals: MacroGoalEntity) {
        dao.overwriteAllData(meals, goals)
    }

    suspend fun clearAllData() {
        dao.clearAllMeals()
        dao.setMacroGoals(
            MacroGoalEntity(
                id = 1,
                targetCalories = 2000,
                targetProtein = 150f,
                targetCarbs = 200f,
                targetFats = 65f
            )
        )
    }

    suspend fun analyzeMeal(
        text: String?,
        bitmap: Bitmap?,
        customApiKey: String? = null
    ): Result<EstimatedMeal> {
        return geminiService.analyzeMeal(text, bitmap, customApiKey)
    }
}
