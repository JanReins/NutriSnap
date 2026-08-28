package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroCaloriesColor
import com.example.ui.theme.MacroCarbsColor
import com.example.ui.theme.MacroFatsColor
import com.example.ui.theme.MacroProteinColor
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate700
import com.example.ui.viewmodel.ReviewMealState
import kotlin.math.roundToInt

@Composable
fun MealReviewDialog(
    reviewState: ReviewMealState,
    onSave: (
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fats: Float,
        fiber: Float,
        sugar: Float,
        mealType: String,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var mealName by remember { mutableStateOf(reviewState.mealName) }
    var mealType by remember { mutableStateOf(reviewState.mealType.ifBlank { "Meal" }) }
    var caloriesText by remember { mutableStateOf(reviewState.calories.toString()) }
    var proteinText by remember { mutableStateOf(reviewState.protein.roundToInt().toString()) }
    var carbsText by remember { mutableStateOf(reviewState.carbs.roundToInt().toString()) }
    var fatsText by remember { mutableStateOf(reviewState.fats.roundToInt().toString()) }
    var fiberText by remember { mutableStateOf(if (reviewState.fiber > 0) reviewState.fiber.roundToInt().toString() else "0") }
    var sugarText by remember { mutableStateOf(if (reviewState.sugar > 0) reviewState.sugar.roundToInt().toString() else "0") }
    var notesText by remember { mutableStateOf(reviewState.notes) }

    val isEditingExisting = reviewState.id != null
    val mealTypeOptions = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("meal_review_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (reviewState.isAiEstimate) Icons.Rounded.AutoAwesome else Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isEditingExisting) "Edit Meal Entry" else "Review & Save Meal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (reviewState.isAiEstimate) "Estimated with Gemini AI" else "Local estimate / Manual entry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Provenance Badge Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (reviewState.isAiEstimate) EmeraldContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (reviewState.isAiEstimate) Icons.Rounded.AutoAwesome else Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (reviewState.isAiEstimate) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (reviewState.isAiEstimate) "AI Nutritional Estimate — You can tweak values before saving"
                            else "Local/Heuristic Estimate — Please verify portions and macros",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reviewState.isAiEstimate) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Optional Photo Preview
                if (reviewState.photoBitmap != null) {
                    Image(
                        bitmap = reviewState.photoBitmap.asImageBitmap(),
                        contentDescription = "Logged food photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Meal Name
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_meal_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // Meal Category Chips (Breakfast, Lunch, Dinner, Snack)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Meal Type:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mealTypeOptions.forEach { type ->
                            FilterChip(
                                selected = mealType.equals(type, ignoreCase = true),
                                onClick = { mealType = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldDark
                                )
                            )
                        }
                    }
                }

                // Total Calories
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter { c -> c.isDigit() } },
                    label = { Text("Calories (kcal)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = MacroCaloriesColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_calories_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroCaloriesColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // 3 Core Macros (Protein, Carbs, Fats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it.filter { c -> c.isDigit() } },
                        label = { Text("Protein (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MacroProteinColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("review_protein_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Carbs (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Grain,
                                contentDescription = null,
                                tint = MacroCarbsColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("review_carbs_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedTextField(
                        value = fatsText,
                        onValueChange = { fatsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Fats (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Opacity,
                                contentDescription = null,
                                tint = MacroFatsColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("review_fats_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatsColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )
                }

                // Optional Fiber & Sugar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it.filter { c -> c.isDigit() } },
                        label = { Text("Fiber (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = sugarText,
                        onValueChange = { sugarText = it.filter { c -> c.isDigit() } },
                        label = { Text("Sugar (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // AI Notes & Portion Breakdown
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Portion Notes & Ingredients") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    ),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cals = caloriesText.toIntOrNull() ?: reviewState.calories
                            val p = proteinText.toFloatOrNull() ?: reviewState.protein
                            val c = carbsText.toFloatOrNull() ?: reviewState.carbs
                            val f = fatsText.toFloatOrNull() ?: reviewState.fats
                            val fib = fiberText.toFloatOrNull() ?: reviewState.fiber
                            val sug = sugarText.toFloatOrNull() ?: reviewState.sugar

                            onSave(
                                mealName.ifBlank { "Logged Meal" },
                                cals,
                                p,
                                c,
                                f,
                                fib,
                                sug,
                                mealType,
                                notesText
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("confirm_save_meal_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (isEditingExisting) "Save Changes" else "Add to Daily Log",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
