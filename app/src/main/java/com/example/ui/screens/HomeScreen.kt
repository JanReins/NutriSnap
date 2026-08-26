package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DailyMealsFeed
import com.example.ui.components.MacroDashboard
import com.example.ui.components.MealInputSection
import com.example.ui.components.MealReviewDialog
import com.example.ui.components.SetGoalsDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MacroExceededColor
import com.example.ui.theme.MacroExceededSoft
import com.example.ui.viewmodel.NutriViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: NutriViewModel,
    modifier: Modifier = Modifier
) {
    val meals by viewModel.todayMeals.collectAsStateWithLifecycle()
    val macroGoals by viewModel.macroGoals.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisStatusText by viewModel.analysisStatusText.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewState.collectAsStateWithLifecycle()
    val showGoalsDialog by viewModel.showGoalsDialog.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsStateWithLifecycle()

    val formattedDate = remember(selectedDateMillis) {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date(selectedDateMillis))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = statusBarPadding + 16.dp,
                    bottom = navBarPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Top Header Section with Brand, Theme Toggle & Actions
                item {
                    HeaderSection(
                        todayFormattedDate = formattedDate,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onOpenSettings = { viewModel.openSettings() },
                        onOpenGoals = { viewModel.openGoalsDialog() }
                    )
                }

                // Info Message banner (if any)
                if (infoMessage != null) {
                    item {
                        InfoMessageBanner(
                            message = infoMessage ?: "",
                            onDismiss = { viewModel.clearInfo() }
                        )
                    }
                }

                // Error Message banner (if any)
                if (errorMessage != null) {
                    item {
                        ErrorMessageBanner(
                            message = errorMessage ?: "",
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }

                // Daily Macro Dashboard
                item {
                    MacroDashboard(
                        meals = meals,
                        goals = macroGoals
                    )
                }

                // AI Meal Input (Natural Language or Food Photo)
                item {
                    MealInputSection(
                        isAnalyzing = isAnalyzing,
                        analysisStatusText = analysisStatusText,
                        onAnalyzeText = { text -> viewModel.analyzeTextMeal(text) },
                        onAnalyzePhoto = { bitmap, prompt -> viewModel.analyzePhotoMeal(bitmap, prompt) }
                    )
                }

                // Daily Logged Meals Feed
                item {
                    DailyMealsFeed(
                        meals = meals,
                        onDeleteMeal = { id -> viewModel.deleteMeal(id) }
                    )
                }
            }

            // Set Goals Pop-up Modal
            if (showGoalsDialog) {
                SetGoalsDialog(
                    currentGoals = macroGoals,
                    onSaveGoals = { cal, p, c, f ->
                        viewModel.updateGoals(cal, p, c, f)
                    },
                    onDismiss = { viewModel.closeGoalsDialog() }
                )
            }

            // Settings & Backup Modal
            if (showSettingsDialog) {
                SettingsDialog(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onDismiss = { viewModel.closeSettings() }
                )
            }

            // Editable Meal Review Pop-up Modal
            reviewState?.let { currentReview ->
                MealReviewDialog(
                    reviewState = currentReview,
                    onConfirm = { name, cal, p, c, f, notes ->
                        viewModel.saveReviewedMeal(name, cal, p, c, f, notes)
                    },
                    onDismiss = { viewModel.dismissReview() }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    todayFormattedDate: String,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_header"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App brand & Date
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = "NutriSnap Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "NutriSnap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = todayFormattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Header Action Buttons: Theme Toggle, Settings, Set Macros
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sun / Moon Theme Toggle Icon Button
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = if (isDarkMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings & Backup Icon Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings & Backup",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // "Set My Macros" Button
                Button(
                    onClick = onOpenGoals,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldContainer,
                        contentColor = EmeraldDark
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("set_macros_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Macros",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoMessageBanner(
    message: String,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onDismiss,
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

@Composable
private fun ErrorMessageBanner(
    message: String,
    onDismiss: () -> Unit
) {
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MacroExceededColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
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
