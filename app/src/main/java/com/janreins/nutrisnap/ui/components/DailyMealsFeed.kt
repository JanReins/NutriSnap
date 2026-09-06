package com.janreins.nutrisnap.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.nutrisnap.data.local.MealEntity
import com.janreins.nutrisnap.ui.theme.EmeraldContainer
import com.janreins.nutrisnap.ui.theme.EmeraldDark
import com.janreins.nutrisnap.ui.theme.EmeraldPrimary
import com.janreins.nutrisnap.ui.theme.MacroCaloriesColor
import com.janreins.nutrisnap.ui.theme.MacroCarbsColor
import com.janreins.nutrisnap.ui.theme.MacroExceededColor
import com.janreins.nutrisnap.ui.theme.MacroFatsColor
import com.janreins.nutrisnap.ui.theme.MacroProteinColor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DailyMealsFeed(
    meals: List<MealEntity>,
    onEditMeal: (MealEntity) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var mealToDelete by remember { mutableStateOf<MealEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_meals_feed"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title & count badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Logged Meals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${meals.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }

            if (meals.isNotEmpty()) {
                val totalCals = meals.sumOf { it.calories }
                Text(
                    text = "Total: $totalCals kcal",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (meals.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_meals_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(EmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Restaurant,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No meals logged for this day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Type what you ate or snap a food photo above to estimate your macros!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // List of meals
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                meals.forEach { meal ->
                    MealItemCard(
                        meal = meal,
                        onClick = { onEditMeal(meal) },
                        onDeleteClick = { mealToDelete = meal }
                    )
                }
            }
        }
    }

    // Confirmation dialog before deleting
    if (mealToDelete != null) {
        AlertDialog(
            onDismissRequest = { mealToDelete = null },
            title = { Text("Delete Meal Entry?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove \"${mealToDelete?.mealName}\"? This will update your daily macro totals and clean up associated image files.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mealToDelete?.let { onDeleteMeal(it.id) }
                        mealToDelete = null
                    }
                ) {
                    Text("Delete", color = MacroExceededColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mealToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MealItemCard(
    meal: MealEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val timeFormatted = remember(meal.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(meal.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("meal_item_card_${meal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail if photo exists, else food icon
            if (!meal.imageUriOrBase64.isNullOrBlank() && File(meal.imageUriOrBase64).exists()) {
                val bitmap = remember(meal.imageUriOrBase64) {
                    BitmapFactory.decodeFile(meal.imageUriOrBase64)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Meal photo thumbnail",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    FallbackMealIcon()
                }
            } else {
                FallbackMealIcon()
            }

            // Meal Details & Macro badges
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = meal.mealName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (meal.mealType.isNotBlank() && meal.mealType != "Meal") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = meal.mealType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Macro pill badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calories badge
                    MacroPill(
                        label = "${meal.calories} kcal",
                        color = MacroCaloriesColor,
                        bgColor = MacroCaloriesColor.copy(alpha = 0.15f)
                    )

                    // Protein
                    MacroPill(
                        label = "${meal.proteinGrams.roundToInt()}g P",
                        color = MacroProteinColor,
                        bgColor = MacroProteinColor.copy(alpha = 0.15f)
                    )

                    // Carbs
                    MacroPill(
                        label = "${meal.carbsGrams.roundToInt()}g C",
                        color = MacroCarbsColor,
                        bgColor = MacroCarbsColor.copy(alpha = 0.15f)
                    )

                    // Fats
                    MacroPill(
                        label = "${meal.fatsGrams.roundToInt()}g F",
                        color = MacroFatsColor,
                        bgColor = MacroFatsColor.copy(alpha = 0.15f)
                    )
                }

                if (meal.notes.isNotBlank()) {
                    Text(
                        text = meal.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Actions: Edit and Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_meal_button_${meal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit meal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_meal_button_${meal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete meal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackMealIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(EmeraldContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Fastfood,
            contentDescription = null,
            tint = EmeraldPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun MacroPill(
    label: String,
    color: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
