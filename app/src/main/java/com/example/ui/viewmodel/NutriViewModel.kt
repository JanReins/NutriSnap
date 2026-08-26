package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.EstimatedMeal
import com.example.data.local.MacroGoalEntity
import com.example.data.local.MealEntity
import com.example.data.local.NutriSnapDatabase
import com.example.data.repository.NutriRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReviewMealState(
    val mealName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val notes: String = "",
    val photoBitmap: Bitmap? = null,
    val imagePath: String? = null
)

data class BackupImportPreview(
    val mealsCount: Int,
    val meals: List<MealEntity>,
    val goals: MacroGoalEntity
)

class NutriViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("nutrisnap_user_preferences", Context.MODE_PRIVATE)
    private val repository: NutriRepository

    init {
        val db = NutriSnapDatabase.getDatabase(application)
        repository = NutriRepository(db.nutriSnapDao())
    }

    // Theme state (persisted locally, default = light theme)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_theme_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStatusText = MutableStateFlow("Analyzing your meal with Gemini AI...")
    val analysisStatusText: StateFlow<String> = _analysisStatusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewMealState?>(null)
    val reviewState: StateFlow<ReviewMealState?> = _reviewState.asStateFlow()

    private val _showGoalsDialog = MutableStateFlow(false)
    val showGoalsDialog: StateFlow<Boolean> = _showGoalsDialog.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _pendingImport = MutableStateFlow<BackupImportPreview?>(null)
    val pendingImport: StateFlow<BackupImportPreview?> = _pendingImport.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", null))
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    val macroGoals: StateFlow<MacroGoalEntity> = repository.getMacroGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MacroGoalEntity(1, 2000, 150f, 200f, 65f)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayMeals: StateFlow<List<MealEntity>> = _selectedDateMillis
        .flatMapLatest { dateMillis ->
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val (startOfDay, endOfDay) = getDayRange(calendar)
            repository.getTodayMeals(startOfDay, endOfDay)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun getDayRange(cal: Calendar): Pair<Long, Long> {
        val start = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return Pair(start, end)
    }

    // Theme Toggling & Local Persistence
    fun toggleTheme() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("dark_theme_enabled", newMode).apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_theme_enabled", enabled).apply()
    }

    fun setCustomApiKey(key: String?) {
        val sanitized = key?.trim()?.takeIf { it.isNotBlank() }
        _customApiKey.value = sanitized
        if (sanitized != null) {
            prefs.edit().putString("custom_gemini_api_key", sanitized).apply()
        } else {
            prefs.edit().remove("custom_gemini_api_key").apply()
        }
    }

    // Meal AI Analysis
    fun analyzeTextMeal(description: String) {
        if (description.isBlank()) {
            _errorMessage.value = "Please enter food or meal details"
            return
        }
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            _analysisStatusText.value = "Estimating portion sizes and macros..."

            val result = repository.analyzeMeal(
                text = description,
                bitmap = null,
                customApiKey = _customApiKey.value
            )

            _isAnalyzing.value = false

            result.onSuccess { estimated ->
                _reviewState.value = ReviewMealState(
                    mealName = estimated.mealName,
                    calories = estimated.calories,
                    protein = estimated.protein,
                    carbs = estimated.carbs,
                    fats = estimated.fats,
                    notes = estimated.notes,
                    photoBitmap = null,
                    imagePath = null
                )
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to analyze meal"
            }
        }
    }

    fun analyzePhotoMeal(bitmap: Bitmap, promptAddition: String? = null) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _errorMessage.value = null
            _analysisStatusText.value = "Recognizing food items in photo with AI..."

            val result = repository.analyzeMeal(
                text = promptAddition?.takeIf { it.isNotBlank() },
                bitmap = bitmap,
                customApiKey = _customApiKey.value
            )

            _isAnalyzing.value = false

            result.onSuccess { estimated ->
                val savedPath = saveImageToInternalStorage(bitmap)
                _reviewState.value = ReviewMealState(
                    mealName = estimated.mealName,
                    calories = estimated.calories,
                    protein = estimated.protein,
                    carbs = estimated.carbs,
                    fats = estimated.fats,
                    notes = estimated.notes,
                    photoBitmap = bitmap,
                    imagePath = savedPath
                )
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to analyze photo"
            }
        }
    }

    fun saveReviewedMeal(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fats: Float,
        notes: String
    ) {
        val currentReview = _reviewState.value
        viewModelScope.launch {
            val meal = MealEntity(
                mealName = name.ifBlank { "Logged Meal" },
                calories = calories.coerceAtLeast(0),
                proteinGrams = protein.coerceAtLeast(0f),
                carbsGrams = carbs.coerceAtLeast(0f),
                fatsGrams = fats.coerceAtLeast(0f),
                timestamp = System.currentTimeMillis(),
                imageUriOrBase64 = currentReview?.imagePath,
                notes = notes
            )
            repository.saveMeal(meal)
            _reviewState.value = null
            _infoMessage.value = "Saved \"${meal.mealName}\" to your daily log"
        }
    }

    fun dismissReview() {
        _reviewState.value = null
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
            _infoMessage.value = "Meal deleted"
        }
    }

    fun updateGoals(calories: Int, protein: Float, carbs: Float, fats: Float) {
        viewModelScope.launch {
            val updated = MacroGoalEntity(
                id = 1,
                targetCalories = calories.coerceAtLeast(500),
                targetProtein = protein.coerceAtLeast(10f),
                targetCarbs = carbs.coerceAtLeast(10f),
                targetFats = fats.coerceAtLeast(5f)
            )
            repository.updateGoals(updated)
            _showGoalsDialog.value = false
            _infoMessage.value = "Macro targets updated"
        }
    }

    fun openGoalsDialog() {
        _showGoalsDialog.value = true
    }

    fun closeGoalsDialog() {
        _showGoalsDialog.value = false
    }

    fun openSettings() {
        _showSettingsDialog.value = true
    }

    fun closeSettings() {
        _showSettingsDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInfo() {
        _infoMessage.value = null
    }

    // ==========================================
    // DATA EXPORT & IMPORT BACKUP LOGIC
    // ==========================================

    suspend fun generateBackupJsonString(): String = withContext(Dispatchers.IO) {
        val meals = repository.getAllMealsDirect()
        val goals = repository.getMacroGoalsDirect()

        val root = JSONObject()
        root.put("appName", "NutriSnap")
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        val goalsObj = JSONObject()
        goalsObj.put("targetCalories", goals.targetCalories)
        goalsObj.put("targetProtein", goals.targetProtein.toDouble())
        goalsObj.put("targetCarbs", goals.targetCarbs.toDouble())
        goalsObj.put("targetFats", goals.targetFats.toDouble())
        root.put("macroGoals", goalsObj)

        val mealsArray = JSONArray()
        for (m in meals) {
            val mObj = JSONObject()
            mObj.put("mealName", m.mealName)
            mObj.put("calories", m.calories)
            mObj.put("proteinGrams", m.proteinGrams.toDouble())
            mObj.put("carbsGrams", m.carbsGrams.toDouble())
            mObj.put("fatsGrams", m.fatsGrams.toDouble())
            mObj.put("timestamp", m.timestamp)
            mObj.put("notes", m.notes)
            mObj.put("imageUriOrBase64", m.imageUriOrBase64 ?: "")
            mealsArray.put(mObj)
        }
        root.put("meals", mealsArray)
        root.toString(2)
    }

    fun exportBackupToUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val jsonString = generateBackupJsonString()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                }
                _infoMessage.value = "Backup successfully exported!"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to export backup: ${e.localizedMessage}"
            }
        }
    }

    fun prepareImportFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val jsonContent = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    } ?: throw IllegalStateException("Could not open file")
                }

                val preview = parseBackupJsonContent(jsonContent)
                _pendingImport.value = preview
            } catch (e: Exception) {
                _errorMessage.value = "Failed to parse backup file: ${e.localizedMessage}"
            }
        }
    }

    private fun parseBackupJsonContent(jsonStr: String): BackupImportPreview {
        val root = JSONObject(jsonStr)
        val goalsObj = root.optJSONObject("macroGoals")
        val goals = if (goalsObj != null) {
            MacroGoalEntity(
                id = 1,
                targetCalories = goalsObj.optInt("targetCalories", 2000),
                targetProtein = goalsObj.optDouble("targetProtein", 150.0).toFloat(),
                targetCarbs = goalsObj.optDouble("targetCarbs", 200.0).toFloat(),
                targetFats = goalsObj.optDouble("targetFats", 65.0).toFloat()
            )
        } else {
            MacroGoalEntity(1, 2000, 150f, 200f, 65f)
        }

        val mealsList = mutableListOf<MealEntity>()
        val mealsArray = root.optJSONArray("meals")
        if (mealsArray != null) {
            for (i in 0 until mealsArray.length()) {
                val mObj = mealsArray.getJSONObject(i)
                mealsList.add(
                    MealEntity(
                        id = 0,
                        mealName = mObj.optString("mealName", "Logged Meal"),
                        calories = mObj.optInt("calories", 0),
                        proteinGrams = mObj.optDouble("proteinGrams", 0.0).toFloat(),
                        carbsGrams = mObj.optDouble("carbsGrams", 0.0).toFloat(),
                        fatsGrams = mObj.optDouble("fatsGrams", 0.0).toFloat(),
                        timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                        imageUriOrBase64 = mObj.optString("imageUriOrBase64").takeIf { it.isNotBlank() },
                        notes = mObj.optString("notes", "")
                    )
                )
            }
        }
        return BackupImportPreview(
            mealsCount = mealsList.size,
            meals = mealsList,
            goals = goals
        )
    }

    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        viewModelScope.launch {
            try {
                repository.overwriteAllData(pending.meals, pending.goals)
                _pendingImport.value = null
                _infoMessage.value = "Data restored successfully! (${pending.mealsCount} meals imported)"
                // Refresh today's date timestamp trigger
                _selectedDateMillis.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore data: ${e.localizedMessage}"
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    private suspend fun saveImageToInternalStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>().applicationContext
            val filename = "meal_photo_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
