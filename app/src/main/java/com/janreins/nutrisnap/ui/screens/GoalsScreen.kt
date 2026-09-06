package com.janreins.nutrisnap.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.nutrisnap.ui.theme.EmeraldPrimary
import com.janreins.nutrisnap.ui.theme.MacroCaloriesColor
import com.janreins.nutrisnap.ui.theme.MacroCarbsColor
import com.janreins.nutrisnap.ui.theme.MacroFatsColor
import com.janreins.nutrisnap.ui.theme.MacroProteinColor
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: NutriViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val macroGoals by viewModel.macroGoals.collectAsStateWithLifecycle()

    var caloriesText by remember(macroGoals) { mutableStateOf(macroGoals.targetCalories.toString()) }
    var proteinText by remember(macroGoals) { mutableStateOf(macroGoals.targetProtein.roundToInt().toString()) }
    var carbsText by remember(macroGoals) { mutableStateOf(macroGoals.targetCarbs.roundToInt().toString()) }
    var fatsText by remember(macroGoals) { mutableStateOf(macroGoals.targetFats.roundToInt().toString()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Set Daily Targets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure your Cronometer-style goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Quick Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            caloriesText = "2000"
                            proteinText = "150"
                            carbsText = "200"
                            fatsText = "65"
                        },
                        label = { Text("Balanced 2000") },
                        colors = FilterChipDefaults.filterChipColors()
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            caloriesText = "2200"
                            proteinText = "190"
                            carbsText = "210"
                            fatsText = "60"
                        },
                        label = { Text("High Protein 2200") },
                        colors = FilterChipDefaults.filterChipColors()
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            caloriesText = "1800"
                            proteinText = "140"
                            carbsText = "50"
                            fatsText = "115"
                        },
                        label = { Text("Low Carb 1800") },
                        colors = FilterChipDefaults.filterChipColors()
                    )

                    FilterChip(
                        selected = false,
                        onClick = {
                            caloriesText = "1650"
                            proteinText = "160"
                            carbsText = "140"
                            fatsText = "50"
                        },
                        label = { Text("Cut / Loss 1650") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            // Inputs
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Calories
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it.filter { c -> c.isDigit() } },
                    label = { Text("Daily Calories Target (kcal)") },
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
                        .testTag("target_calories_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MacroCaloriesColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // 3 Macros in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .testTag("target_protein_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroProteinColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                            .testTag("target_carbs_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroCarbsColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                            .testTag("target_fats_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MacroFatsColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val cal = caloriesText.toIntOrNull() ?: macroGoals.targetCalories
                        val p = proteinText.toFloatOrNull() ?: macroGoals.targetProtein
                        val c = carbsText.toFloatOrNull() ?: macroGoals.targetCarbs
                        val f = fatsText.toFloatOrNull() ?: macroGoals.targetFats
                        viewModel.updateGoals(cal, p, c, f)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_goals_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Save Goals",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
