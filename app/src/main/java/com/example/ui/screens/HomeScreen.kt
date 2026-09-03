package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DailyMealsFeed
import com.example.ui.components.MacroDashboard
import com.example.ui.components.MealInputSection
import com.example.ui.components.MealReviewDialog
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroExceededColor
import com.example.ui.theme.MacroExceededSoft
import com.example.ui.viewmodel.NutriViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NutriViewModel,
    onNavigateToGoals: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val macroGoals by viewModel.macroGoals.collectAsStateWithLifecycle()

    val formattedDateText = remember(uiState.selectedDateMillis) {
        val selectedCal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDateMillis }
        val nowCal = Calendar.getInstance()

        val isToday = selectedCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = selectedCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        when {
            isToday -> "Today, ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(uiState.selectedDateMillis))}"
            isYesterday -> "Yesterday, ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(uiState.selectedDateMillis))}"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(uiState.selectedDateMillis))
        }
    }

    val isCurrentDayToday = remember(uiState.selectedDateMillis) {
        val selectedCal = Calendar.getInstance().apply { timeInMillis = uiState.selectedDateMillis }
        val nowCal = Calendar.getInstance()
        selectedCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                selectedCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestaurantMenu,
                                contentDescription = "NutriSnap Logo",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "NutriSnap",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AI Macro & Food Tracker",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Summary & Insights Button
                    IconButton(
                        onClick = onNavigateToSummary,
                        modifier = Modifier.testTag("open_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "View Insights and Summary",
                            tint = EmeraldPrimary
                        )
                    }

                    // Set Goals Target Button
                    IconButton(
                        onClick = onNavigateToGoals,
                        modifier = Modifier.testTag("open_goals_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Flag,
                            contentDescription = "Set Macro Goals",
                            tint = EmeraldPrimary
                        )
                    }

                    // Theme Toggle Button (Light / Dark)
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (uiState.isDarkMode) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Settings & Backup Menu Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings and Data Backup",
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
            // Notification / Info Banner
            AnimatedVisibility(
                visible = uiState.infoMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.infoMessage?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("info_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = info,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldDark
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearInfo() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                uiState.errorMessage?.let { err ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MacroExceededSoft)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MacroExceededColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MacroExceededColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MacroExceededColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Date Navigation Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_navigation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous day button
                    IconButton(
                        onClick = { viewModel.goToPreviousDay() },
                        modifier = Modifier.testTag("prev_day_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Current Selected Date Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formattedDateText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Next day button & Jump to Today
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isCurrentDayToday) {
                            TextButton(
                                onClick = { viewModel.goToToday() },
                                modifier = Modifier.testTag("jump_to_today_button")
                            ) {
                                Text(
                                    text = "Today",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.goToNextDay() },
                            modifier = Modifier.testTag("next_day_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Next Day",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick date shortcut if on Today: "Log for Yesterday"
            if (isCurrentDayToday) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.logForYesterday() },
                        label = { Text("Log for Yesterday") },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("log_yesterday_chip")
                    )
                }
            }

            // Macro Dashboard (Calories, Protein, Carbs, Fats targets)
            MacroDashboard(
                meals = todayMeals,
                goals = macroGoals
            )

            // Meal Input Section (Describe / Photo Snap / Manual Entry)
            MealInputSection(
                isAnalyzing = uiState.isAnalyzing,
                analysisStatusText = uiState.analysisStatusText,
                onAnalyzeText = { text -> viewModel.analyzeTextMeal(text) },
                onAnalyzePhoto = { bitmap, prompt -> viewModel.analyzePhotoMeal(bitmap, prompt) },
                onLogManually = { name, cals, p, c, f, fib, sug, type, notes ->
                    viewModel.logMealManually(name, cals, p, c, f, fib, sug, type, notes)
                }
            )

            // Daily Meals Feed (List of logged meals, edit triggers, and deletion)
            DailyMealsFeed(
                meals = todayMeals,
                onEditMeal = { meal -> viewModel.openEditMeal(meal) },
                onDeleteMeal = { mealId -> viewModel.deleteMeal(mealId) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs
    if (uiState.reviewState != null) {
        MealReviewDialog(
            reviewState = uiState.reviewState!!,
            onSave = { name, cals, p, c, f, fib, sug, type, notes ->
                viewModel.saveReviewedMeal(name, cals, p, c, f, fib, sug, type, notes)
            },
            onDismiss = { viewModel.dismissReview() }
        )
    }
}
