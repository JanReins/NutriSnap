package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReviewMealState
import kotlin.math.roundToInt

@Composable
fun MealReviewDialog(
    reviewState: ReviewMealState,
    onConfirm: (name: String, calories: Int, protein: Float, carbs: Float, fats: Float, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var mealName by remember { mutableStateOf(reviewState.mealName) }
    var caloriesText by remember { mutableStateOf(reviewState.calories.toString()) }
    var proteinText by remember { mutableStateOf(reviewState.protein.roundToInt().toString()) }
    var carbsText by remember { mutableStateOf(reviewState.carbs.roundToInt().toString()) }
    var fatsText by remember { mutableStateOf(reviewState.fats.roundToInt().toString()) }
    var notesText by remember { mutableStateOf(reviewState.notes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("meal_review_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with AI Sparkle badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI Meal Breakdown",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Review & adjust macros before saving",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500
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
                            tint = Slate500
                        )
                    }
                }

                // Optional Photo Preview
                if (reviewState.photoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Image(
                            bitmap = reviewState.photoBitmap.asImageBitmap(),
                            contentDescription = "Meal Photo",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Meal Name Field
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meal_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate300,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )

                // Editable Macro Grid
                Text(
                    text = "Nutritional Breakdown",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )

                // Calories (Hero input)
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Calories (kcal)") },
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
                        .testTag("calories_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroCaloriesColor,
                        unfocusedBorderColor = Slate300,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )

                // 3-Macro Row (Protein, Carbs, Fats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Protein
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Protein (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MacroProteinColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("protein_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    // Carbs
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Carbs (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Grain,
                                contentDescription = null,
                                tint = MacroCarbsColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("carbs_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )

                    // Fats
                    OutlinedTextField(
                        value = fatsText,
                        onValueChange = { fatsText = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Fats (g)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Opacity,
                                contentDescription = null,
                                tint = MacroFatsColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fats_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatsColor,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Slate50,
                            unfocusedContainerColor = Slate50
                        )
                    )
                }

                // AI Notes / Explanation
                if (notesText.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "AI Portion Notes:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notesText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                    }
                }

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
                        Text("Discard")
                    }

                    Button(
                        onClick = {
                            val cal = caloriesText.toIntOrNull() ?: reviewState.calories
                            val p = proteinText.toFloatOrNull() ?: reviewState.protein
                            val c = carbsText.toFloatOrNull() ?: reviewState.carbs
                            val f = fatsText.toFloatOrNull() ?: reviewState.fats
                            onConfirm(mealName, cal, p, c, f, notesText)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_meal_review_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Log",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
