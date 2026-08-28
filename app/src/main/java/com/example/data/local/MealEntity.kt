package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a logged meal entry in NutriSnap.
 * Stores comprehensive nutrition metrics (calories, protein, carbs, fats, fiber, sugar),
 * meal classification, timestamp, image storage path, notes, and AI estimate attribution.
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealName: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatsGrams: Float,
    val fiberGrams: Float = 0f,
    val sugarGrams: Float = 0f,
    val mealType: String = "Meal", // e.g., Breakfast, Lunch, Dinner, Snack
    val timestamp: Long = System.currentTimeMillis(),
    val imageUriOrBase64: String? = null,
    val notes: String = "",
    val isAiEstimated: Boolean = true
)
