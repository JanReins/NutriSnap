package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MacroGoalEntity
import com.example.data.local.MealEntity
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCaloriesColor
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroExceededColor
import com.example.ui.theme.MacroExceededSoft
import com.example.ui.theme.MacroFatsColor
import com.example.ui.theme.MacroProteinColor
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MacroDashboard(
    meals: List<MealEntity>,
    goals: MacroGoalEntity,
    modifier: Modifier = Modifier
) {
    val totalCalories = meals.sumOf { it.calories }
    val totalProtein = meals.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCarbs = meals.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val totalFats = meals.sumOf { it.fatsGrams.toDouble() }.toFloat()

    val calRemaining = goals.targetCalories - totalCalories
    val calOver = totalCalories > goals.targetCalories
    val calProgress = (totalCalories.toFloat() / goals.targetCalories.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("macro_dashboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header row with title and summary badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Macro Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Track your targets & real-time intake",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (calOver) MacroExceededSoft else EmeraldContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (calOver) "${abs(calRemaining)} kcal over" else "$calRemaining kcal left",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (calOver) MacroExceededColor else EmeraldDark
                    )
                }
            }

            // Hero Calories Circular Progress Section
            HeroCalorieRing(
                consumed = totalCalories,
                goal = goals.targetCalories,
                remaining = calRemaining,
                isOver = calOver,
                progress = calProgress,
                modifier = Modifier.testTag("calories_progress")
            )

            // Linear Nutrient Progress Bars (Protein, Carbs, Fats)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MacroProgressBar(
                    title = "Protein",
                    consumed = totalProtein,
                    goal = goals.targetProtein,
                    unit = "g",
                    baseColor = MacroProteinColor,
                    icon = Icons.Rounded.FitnessCenter,
                    testTag = "protein_progress"
                )

                MacroProgressBar(
                    title = "Carbs",
                    consumed = totalCarbs,
                    goal = goals.targetCarbs,
                    unit = "g",
                    baseColor = MacroCarbsColor,
                    icon = Icons.Rounded.Grain,
                    testTag = "carbs_progress"
                )

                MacroProgressBar(
                    title = "Fats",
                    consumed = totalFats,
                    goal = goals.targetFats,
                    unit = "g",
                    baseColor = MacroFatsColor,
                    icon = Icons.Rounded.Opacity,
                    testTag = "fats_progress"
                )
            }
        }
    }
}

@Composable
private fun HeroCalorieRing(
    consumed: Int,
    goal: Int,
    remaining: Int,
    isOver: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "calorie_progress"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isOver) MacroExceededColor else MacroCaloriesColor,
        label = "calorie_ring_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Circular Progress Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(92.dp)
        ) {
            // Track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(92.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round
            )
            // Progress
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(92.dp),
                color = ringColor,
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round
            )
            // Center icon & percent
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Calories stats breakdown
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Calories Intake",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$consumed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOver) MacroExceededColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / $goal kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }

            Text(
                text = if (isOver) "${abs(remaining)} kcal over goal" else "$remaining kcal remaining",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isOver) MacroExceededColor else EmeraldPrimary
            )
        }
    }
}

@Composable
private fun MacroProgressBar(
    title: String,
    consumed: Float,
    goal: Float,
    unit: String,
    baseColor: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isOver = consumed > goal
    val rawRatio = consumed / goal.coerceAtLeast(1f)
    val progress = rawRatio.coerceIn(0f, 1f)
    val remaining = goal - consumed

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "${title}_progress"
    )

    val barColor by animateColorAsState(
        targetValue = if (isOver) MacroExceededColor else baseColor,
        label = "${title}_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label & Values Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${consumed.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MacroExceededColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / ${goal.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Text(
                    text = if (isOver) " (+${abs(remaining).roundToInt()}$unit)" else " (${remaining.roundToInt()}$unit left)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOver) MacroExceededColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Linear Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            strokeCap = StrokeCap.Round
        )
    }
}
