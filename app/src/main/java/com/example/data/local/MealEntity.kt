package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealName: String,
    val calories: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatsGrams: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUriOrBase64: String? = null,
    val notes: String = ""
)
