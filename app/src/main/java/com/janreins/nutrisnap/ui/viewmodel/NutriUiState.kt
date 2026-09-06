package com.janreins.nutrisnap.ui.viewmodel

/**
 * UI State data class consolidating UI flags and ephemeral states for NutriSnap screens.
 */
data class NutriUiState(
    val isDarkMode: Boolean = false,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val isAnalyzing: Boolean = false,
    val analysisStatusText: String = "Analyzing your meal with Gemini AI...",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val reviewState: ReviewMealState? = null,
    val pendingImport: BackupImportPreview? = null,
    val customApiKey: String? = null,
    val isPinEnabled: Boolean = false,
    val isUnlocked: Boolean = true
)
