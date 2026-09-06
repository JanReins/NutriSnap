package com.janreins.nutrisnap.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.nutrisnap.data.local.MacroGoalEntity
import com.janreins.nutrisnap.ui.theme.EmeraldContainer
import com.janreins.nutrisnap.ui.theme.EmeraldDark
import com.janreins.nutrisnap.ui.theme.EmeraldPrimary
import com.janreins.nutrisnap.ui.theme.MacroCaloriesColor
import com.janreins.nutrisnap.ui.theme.MacroCarbsColor
import com.janreins.nutrisnap.ui.theme.MacroExceededColor
import com.janreins.nutrisnap.ui.theme.MacroExceededSoft
import com.janreins.nutrisnap.ui.theme.MacroFatsColor
import com.janreins.nutrisnap.ui.theme.MacroProteinColor
import com.janreins.nutrisnap.ui.viewmodel.DailyCalorieBar
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel
import com.janreins.nutrisnap.ui.viewmodel.SummaryMode
import com.janreins.nutrisnap.ui.viewmodel.SummaryUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: NutriViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summaryState by viewModel.summaryUiState.collectAsStateWithLifecycle()
    val macroGoals by viewModel.macroGoals.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("summary_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Insights & Trends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Macro averages & period breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("summary_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Navigate back",
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle Row (Week | Month FilterChips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = summaryState.mode == SummaryMode.WEEK,
                    onClick = { viewModel.setSummaryMode(SummaryMode.WEEK) },
                    label = { Text("Week", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldContainer,
                        selectedLabelColor = EmeraldDark,
                        selectedLeadingIconColor = EmeraldDark
                    ),
                    modifier = Modifier.testTag("summary_week_chip")
                )

                FilterChip(
                    selected = summaryState.mode == SummaryMode.MONTH,
                    onClick = { viewModel.setSummaryMode(SummaryMode.MONTH) },
                    label = { Text("Month", fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldContainer,
                        selectedLabelColor = EmeraldDark,
                        selectedLeadingIconColor = EmeraldDark
                    ),
                    modifier = Modifier.testTag("summary_month_chip")
                )
            }

            // Period Navigation Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("period_navigation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.goToPreviousSummaryPeriod() },
                        modifier = Modifier.testTag("prev_period_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Previous Period",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = summaryState.periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summaryState.mode == SummaryMode.WEEK) {
                            Text(
                                text = "Monday start",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.resetToCurrentSummaryPeriod() },
                            modifier = Modifier.testTag("jump_period_button")
                        ) {
                            Text(
                                text = if (summaryState.mode == SummaryMode.WEEK) "This Week" else "This Month",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.goToNextSummaryPeriod() },
                            modifier = Modifier.testTag("next_period_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Next Period",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Empty State Banner if no meals in period
            if (summaryState.isEmpty) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No meals logged for this period",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Log food entries on the Home screen to see your progress and averages here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Summary Overview Card: Logged Days + Totals
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_overview_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Period Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Consistency & total intake",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${summaryState.loggedDaysCount} of ${summaryState.daysInPeriod} days logged",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricTile(
                            label = "Total Calories",
                            value = "${summaryState.totalCalories} kcal",
                            color = MacroCaloriesColor,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        MetricTile(
                            label = "Daily Avg Calories",
                            value = "${summaryState.avgCalories} kcal",
                            color = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Daily Average vs Daily Goal Progress Section
            SummaryMacroSection(
                summaryState = summaryState,
                goals = macroGoals
            )

            // Daily Calorie Bar Breakdown (Small bar chart showing each day)
            DailyCalorieDistributionBarChart(
                bars = summaryState.dailyCalorieBars,
                dailyGoal = macroGoals.targetCalories,
                mode = summaryState.mode
            )

            // Meal Type Mix Card
            MealTypeMixSection(
                breakfastCount = summaryState.breakfastCount,
                lunchCount = summaryState.lunchCount,
                dinnerCount = summaryState.dinnerCount,
                snackCount = summaryState.snackCount
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun SummaryMacroSection(
    summaryState: SummaryUiState,
    goals: MacroGoalEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_macro_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Daily Average vs Target Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Average daily intake across period",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Calories Progress
            SummaryProgressBar(
                title = "Calories",
                average = summaryState.avgCalories.toFloat(),
                goal = goals.targetCalories.toFloat(),
                unit = "kcal",
                baseColor = MacroCaloriesColor,
                icon = Icons.Default.LocalFireDepartment,
                testTag = "summary_calories_progress"
            )

            // Protein Progress
            SummaryProgressBar(
                title = "Protein",
                average = summaryState.avgProtein,
                goal = goals.targetProtein,
                unit = "g",
                baseColor = MacroProteinColor,
                icon = Icons.Rounded.FitnessCenter,
                testTag = "summary_protein_progress"
            )

            // Carbs Progress
            SummaryProgressBar(
                title = "Carbs",
                average = summaryState.avgCarbs,
                goal = goals.targetCarbs,
                unit = "g",
                baseColor = MacroCarbsColor,
                icon = Icons.Rounded.Grain,
                testTag = "summary_carbs_progress"
            )

            // Fats Progress
            SummaryProgressBar(
                title = "Fats",
                average = summaryState.avgFats,
                goal = goals.targetFats,
                unit = "g",
                baseColor = MacroFatsColor,
                icon = Icons.Rounded.Opacity,
                testTag = "summary_fats_progress"
            )

            // Micronutrients row (Fiber & Sugar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avg Fiber: ${summaryState.avgFiber.roundToInt()}g (Total: ${summaryState.totalFiber.roundToInt()}g)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Avg Sugar: ${summaryState.avgSugar.roundToInt()}g (Total: ${summaryState.totalSugar.roundToInt()}g)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryProgressBar(
    title: String,
    average: Float,
    goal: Float,
    unit: String,
    baseColor: Color,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isOver = average > goal
    val rawRatio = average / goal.coerceAtLeast(1f)
    val progress = rawRatio.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "summary_${title}_progress"
    )

    val barColor by animateColorAsState(
        targetValue = if (isOver) MacroExceededColor else baseColor,
        label = "summary_${title}_color"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
                    text = "Avg: ${average.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MacroExceededColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / target ${goal.roundToInt()}$unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = " (${(rawRatio * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) MacroExceededColor else EmeraldDark,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

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

@Composable
private fun DailyCalorieDistributionBarChart(
    bars: List<DailyCalorieBar>,
    dailyGoal: Int,
    mode: SummaryMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_calorie_distribution_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Daily Calorie Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (mode == SummaryMode.WEEK) "7-day view (empty days highlighted)" else "Monthly daily view (1-${bars.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Row of mini vertical progress bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_bars_row")
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val ratio = (bar.totalCalories.toFloat() / dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    val isOver = bar.totalCalories > dailyGoal
                    val barColor = when {
                        bar.totalCalories == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        isOver -> MacroExceededColor
                        else -> EmeraldPrimary
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Visual vertical bar
                        Box(
                            modifier = Modifier
                                .height(64.dp)
                                .width(if (mode == SummaryMode.WEEK) 16.dp else 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((64 * ratio).dp.coerceAtLeast(if (bar.totalCalories > 0) 4.dp else 0.dp))
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(barColor)
                            )
                        }

                        // Day Label below bar
                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bar.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (bar.isToday) EmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = if (mode == SummaryMode.WEEK) 10.sp else 8.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealTypeMixSection(
    breakfastCount: Int,
    lunchCount: Int,
    dinnerCount: Int,
    snackCount: Int,
    modifier: Modifier = Modifier
) {
    val totalMeals = breakfastCount + lunchCount + dinnerCount + snackCount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("meal_type_mix_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Meal Type Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category breakdown for logged items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalMeals meals total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MealTypeChip(label = "Breakfast", count = breakfastCount, color = MacroCaloriesColor)
                MealTypeChip(label = "Lunch", count = lunchCount, color = MacroProteinColor)
                MealTypeChip(label = "Dinner", count = dinnerCount, color = MacroCarbsColor)
                MealTypeChip(label = "Snack", count = snackCount, color = MacroFatsColor)
            }
        }
    }
}

@Composable
private fun MealTypeChip(
    label: String,
    count: Int,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Restaurant,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}