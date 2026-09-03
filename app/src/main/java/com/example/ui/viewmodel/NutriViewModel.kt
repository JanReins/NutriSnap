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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

/**
 * State object representing an active meal review or edit dialog.
 * Holds all macro & micronutrient inputs, meal category, photo data, and provenance.
 */
data class ReviewMealState(
    val id: Long? = null, // null for new logs; contains database ID when editing existing entry
    val mealName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val mealType: String = "Meal", // Breakfast, Lunch, Dinner, Snack
    val notes: String = "",
    val photoBitmap: Bitmap? = null,
    val imagePath: String? = null,
    val isAiEstimate: Boolean = true,
    val originalTimestamp: Long? = null
)

/**
 * Preview model displayed before confirming a backup import.
 */
data class BackupImportPreview(
    val mealsCount: Int,
    val meals: List<MealEntity>,
    val goals: MacroGoalEntity
)

/**
 * Main ViewModel for NutriSnap.
 * Manages daily meal state, macro goals, AI & local heuristic estimation, date navigation,
 * photo disk storage lifecycle, and JSON backup export/import.
 */
class NutriViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("nutrisnap_user_preferences", Context.MODE_PRIVATE)
    private val repository: NutriRepository

    init {
        val db = NutriSnapDatabase.getDatabase(application)
        repository = NutriRepository(db.nutriSnapDao())
    }

    // Individual state flows
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

    private var infoBannerJob: Job? = null

    private val _reviewState = MutableStateFlow<ReviewMealState?>(null)
    val reviewState: StateFlow<ReviewMealState?> = _reviewState.asStateFlow()

    private val _pendingImport = MutableStateFlow<BackupImportPreview?>(null)
    val pendingImport: StateFlow<BackupImportPreview?> = _pendingImport.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_gemini_api_key", null))
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean("pin_lock_enabled", false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isUnlocked = MutableStateFlow(!_isPinEnabled.value)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Combined NutriUiState Flow
    val uiState: StateFlow<NutriUiState> = combine(
        _isDarkMode,
        _selectedDateMillis,
        _isAnalyzing,
        _analysisStatusText,
        _errorMessage,
        _infoMessage,
        _reviewState,
        _pendingImport,
        _customApiKey,
        _isPinEnabled,
        _isUnlocked
    ) { flows: Array<Any?> ->
        NutriUiState(
            isDarkMode = flows[0] as Boolean,
            selectedDateMillis = flows[1] as Long,
            isAnalyzing = flows[2] as Boolean,
            analysisStatusText = flows[3] as String,
            errorMessage = flows[4] as? String,
            infoMessage = flows[5] as? String,
            reviewState = flows[6] as? ReviewMealState,
            pendingImport = flows[7] as? BackupImportPreview,
            customApiKey = flows[8] as? String,
            isPinEnabled = flows[9] as Boolean,
            isUnlocked = flows[10] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NutriUiState(
            isDarkMode = _isDarkMode.value,
            selectedDateMillis = _selectedDateMillis.value,
            customApiKey = _customApiKey.value,
            isPinEnabled = _isPinEnabled.value,
            isUnlocked = _isUnlocked.value
        )
    )

    // Macro goals flow
    val macroGoals: StateFlow<MacroGoalEntity> = repository.getMacroGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MacroGoalEntity(1, 2000, 150f, 200f, 65f)
        )

    // Meals for the selected date
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

    // ==========================================
    // DATE NAVIGATION LOGIC
    // ==========================================

    fun goToPreviousDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDateMillis.value
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun goToNextDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDateMillis.value
            add(Calendar.DAY_OF_YEAR, 1)
        }
        _selectedDateMillis.value = cal.timeInMillis
    }

    fun goToToday() {
        _selectedDateMillis.value = System.currentTimeMillis()
    }

    fun selectDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    fun logForYesterday() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.DAY_OF_YEAR, -1)
        }
        _selectedDateMillis.value = cal.timeInMillis
        showInfo("Viewing yesterday's log. New entries will be saved to yesterday.")
    }

    // ==========================================
    // THEME & SETTINGS LOGIC
    // ==========================================

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

    // ==========================================
    // MEAL ANALYSIS (AI & HONEST FALLBACK)
    // ==========================================

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
                if (!estimated.isAiEstimate) {
                    showInfo("AI unavailable. Showing a rough local estimate. Please review and edit before saving.")
                }
                _reviewState.value = ReviewMealState(
                    id = null,
                    mealName = estimated.mealName,
                    calories = estimated.calories,
                    protein = estimated.protein,
                    carbs = estimated.carbs,
                    fats = estimated.fats,
                    fiber = estimated.fiber,
                    sugar = estimated.sugar,
                    mealType = estimated.mealType.ifBlank { guessMealTypeFromTime() },
                    notes = estimated.notes,
                    photoBitmap = null,
                    imagePath = null,
                    isAiEstimate = estimated.isAiEstimate
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
                if (!estimated.isAiEstimate) {
                    showInfo("AI unavailable. Showing a rough local estimate. Please review and edit before saving.")
                }
                _reviewState.value = ReviewMealState(
                    id = null,
                    mealName = estimated.mealName,
                    calories = estimated.calories,
                    protein = estimated.protein,
                    carbs = estimated.carbs,
                    fats = estimated.fats,
                    fiber = estimated.fiber,
                    sugar = estimated.sugar,
                    mealType = estimated.mealType.ifBlank { guessMealTypeFromTime() },
                    notes = estimated.notes,
                    photoBitmap = bitmap,
                    imagePath = savedPath,
                    isAiEstimate = estimated.isAiEstimate
                )
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to analyze photo"
            }
        }
    }

    // ==========================================
    // MANUAL LOGGING & EDIT FLOW
    // ==========================================

    fun openEditMeal(meal: MealEntity) {
        _reviewState.value = ReviewMealState(
            id = meal.id,
            mealName = meal.mealName,
            calories = meal.calories,
            protein = meal.proteinGrams,
            carbs = meal.carbsGrams,
            fats = meal.fatsGrams,
            fiber = meal.fiberGrams,
            sugar = meal.sugarGrams,
            mealType = meal.mealType,
            notes = meal.notes,
            photoBitmap = null,
            imagePath = meal.imageUriOrBase64,
            isAiEstimate = meal.isAiEstimated,
            originalTimestamp = meal.timestamp
        )
    }

    fun logMealManually(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fats: Float,
        fiber: Float = 0f,
        sugar: Float = 0f,
        mealType: String = "Meal",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val timestamp = calculateTimestampForSelectedDate(null)
            val meal = MealEntity(
                id = 0,
                mealName = name.ifBlank { "Logged Meal" },
                calories = calories.coerceAtLeast(0),
                proteinGrams = protein.coerceAtLeast(0f),
                carbsGrams = carbs.coerceAtLeast(0f),
                fatsGrams = fats.coerceAtLeast(0f),
                fiberGrams = fiber.coerceAtLeast(0f),
                sugarGrams = sugar.coerceAtLeast(0f),
                mealType = mealType.ifBlank { "Meal" },
                timestamp = timestamp,
                imageUriOrBase64 = null,
                notes = notes,
                isAiEstimated = false
            )
            repository.saveMeal(meal)
            showInfo("Saved \"${meal.mealName}\" to your log")
        }
    }

    fun saveReviewedMeal(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fats: Float,
        fiber: Float,
        sugar: Float,
        mealType: String,
        notes: String
    ) {
        val currentReview = _reviewState.value ?: return
        viewModelScope.launch {
            val isEditing = currentReview.id != null
            val timestamp = calculateTimestampForSelectedDate(currentReview.originalTimestamp)

            val meal = MealEntity(
                id = currentReview.id ?: 0L,
                mealName = name.ifBlank { "Logged Meal" },
                calories = calories.coerceAtLeast(0),
                proteinGrams = protein.coerceAtLeast(0f),
                carbsGrams = carbs.coerceAtLeast(0f),
                fatsGrams = fats.coerceAtLeast(0f),
                fiberGrams = fiber.coerceAtLeast(0f),
                sugarGrams = sugar.coerceAtLeast(0f),
                mealType = mealType.ifBlank { "Meal" },
                timestamp = timestamp,
                imageUriOrBase64 = currentReview.imagePath,
                notes = notes,
                isAiEstimated = currentReview.isAiEstimate
            )

            if (isEditing) {
                repository.updateMeal(meal)
                showInfo("Updated \"${meal.mealName}\"")
            } else {
                repository.saveMeal(meal)
                showInfo("Saved \"${meal.mealName}\" to your daily log")
            }

            _reviewState.value = null
        }
    }

    private fun calculateTimestampForSelectedDate(originalTimestamp: Long?): Long {
        if (originalTimestamp != null) {
            return originalTimestamp
        }
        val nowCal = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { timeInMillis = _selectedDateMillis.value }

        val isSameDay = nowCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)

        return if (isSameDay) {
            System.currentTimeMillis()
        } else {
            // Apply current hour & minute to selected calendar day
            selectedCal.apply {
                set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, nowCal.get(Calendar.SECOND))
            }.timeInMillis
        }
    }

    private fun guessMealTypeFromTime(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "Breakfast"
            in 11..14 -> "Lunch"
            in 15..17 -> "Snack"
            in 18..22 -> "Dinner"
            else -> "Snack"
        }
    }

    fun dismissReview() {
        _reviewState.value = null
    }

    // ==========================================
    // MEAL DELETION WITH PHOTO DISK CLEANUP
    // ==========================================

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            val meal = repository.getMealById(mealId)
            if (meal?.imageUriOrBase64 != null) {
                deletePhotoFile(meal.imageUriOrBase64)
            }
            repository.deleteMeal(mealId)
            showInfo("Meal deleted")
        }
    }

    private fun deletePhotoFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // non-fatal cleanup
        }
    }

    // ==========================================
    // GOALS & NOTIFICATIONS
    // ==========================================

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
            showInfo("Macro targets updated")
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearInfo() {
        _infoMessage.value = null
    }

    private fun showInfo(msg: String) {
        _infoMessage.value = msg
        infoBannerJob?.cancel()
        infoBannerJob = viewModelScope.launch {
            delay(4500)
            if (_infoMessage.value == msg) {
                _infoMessage.value = null
            }
        }
    }

    // ==========================================
    // DATA EXPORT & IMPORT BACKUP LOGIC
    // ==========================================

    suspend fun generateBackupJsonString(): String = withContext(Dispatchers.IO) {
        val meals = repository.getAllMealsDirect()
        val goals = repository.getMacroGoalsDirect()

        val root = JSONObject()
        root.put("appName", "NutriSnap")
        root.put("version", 2)
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
            mObj.put("fiberGrams", m.fiberGrams.toDouble())
            mObj.put("sugarGrams", m.sugarGrams.toDouble())
            mObj.put("mealType", m.mealType)
            mObj.put("timestamp", m.timestamp)
            mObj.put("notes", m.notes)
            mObj.put("isAiEstimated", m.isAiEstimated)
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
                showInfo("Backup successfully exported!")
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

    fun parseBackupJsonContent(jsonStr: String): BackupImportPreview {
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
                        fiberGrams = mObj.optDouble("fiberGrams", 0.0).toFloat(),
                        sugarGrams = mObj.optDouble("sugarGrams", 0.0).toFloat(),
                        mealType = mObj.optString("mealType", "Meal"),
                        timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                        imageUriOrBase64 = mObj.optString("imageUriOrBase64").takeIf { it.isNotBlank() },
                        notes = mObj.optString("notes", ""),
                        isAiEstimated = mObj.optBoolean("isAiEstimated", true)
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
                showInfo("Data restored successfully! (${pending.mealsCount} meals imported)")
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
            val photosDir = File(context.filesDir, "meal_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val filename = "meal_${System.currentTimeMillis()}.jpg"
            val file = File(photosDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            ""
        }
    }

    // ==========================================
    // PIN LOCK & SECURITY LOGIC
    // ==========================================

    private fun hashPin(pin: String, salt: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((pin + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String): Boolean {
        if (!_isPinEnabled.value) {
            _isUnlocked.value = true
            return true
        }
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val storedSalt = prefs.getString("pin_salt", "") ?: ""
        val hash = hashPin(inputPin, storedSalt)
        val matches = hash == storedHash
        if (matches) {
            _isUnlocked.value = true
        }
        return matches
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
            return false
        }
        val salt = java.util.UUID.randomUUID().toString()
        val hash = hashPin(newPin, salt)
        prefs.edit()
            .putBoolean("pin_lock_enabled", true)
            .putString("pin_hash", hash)
            .putString("pin_salt", salt)
            .apply()

        _isPinEnabled.value = true
        _isUnlocked.value = true
        showInfo("PIN lock enabled successfully")
        return true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val storedSalt = prefs.getString("pin_salt", "") ?: ""
        if (hashPin(currentPin, storedSalt) != storedHash) {
            return false
        }
        if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
            return false
        }
        val newSalt = java.util.UUID.randomUUID().toString()
        val newHash = hashPin(newPin, newSalt)
        prefs.edit()
            .putString("pin_hash", newHash)
            .putString("pin_salt", newSalt)
            .apply()

        _isUnlocked.value = true
        showInfo("PIN updated successfully")
        return true
    }

    fun removePin(currentPin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val storedSalt = prefs.getString("pin_salt", "") ?: ""
        if (hashPin(currentPin, storedSalt) != storedHash) {
            return false
        }
        prefs.edit()
            .putBoolean("pin_lock_enabled", false)
            .remove("pin_hash")
            .remove("pin_salt")
            .apply()

        _isPinEnabled.value = false
        _isUnlocked.value = true
        showInfo("PIN lock removed")
        return true
    }

    fun lockApp() {
        if (_isPinEnabled.value) {
            _isUnlocked.value = false
        }
    }

    fun checkBackgroundTimeoutAndLock(backgroundTimestampMillis: Long) {
        if (_isPinEnabled.value && backgroundTimestampMillis > 0) {
            val elapsed = System.currentTimeMillis() - backgroundTimestampMillis
            if (elapsed >= 15_000L) {
                _isUnlocked.value = false
            }
        }
    }
}
