package com.janreins.nutrisnap.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macro_goals")
data class MacroGoalEntity(
    @PrimaryKey
    val id: Int = 1,
    val targetCalories: Int = 2000,
    val targetProtein: Float = 150f,
    val targetCarbs: Float = 200f,
    val targetFats: Float = 65f
)
