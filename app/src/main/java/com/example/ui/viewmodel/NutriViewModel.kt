package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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

class NutriViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NutriRepository

    init {
        val db = NutriSnapDatabase.getDatabase(application)
        repository = NutriRepository(db.nutriSnapDao())
    }

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStatusText = MutableStateFlow("Analyzing your meal with Gemini AI...")
    val analysisStatusText: StateFlow<String> = _analysisStatusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewMealState?>(null)
    val reviewState: StateFlow<ReviewMealState?> = _reviewState.asStateFlow()

    private val _showGoalsDialog = MutableStateFlow(false)
    val showGoalsDialog: StateFlow<Boolean> = _showGoalsDialog.asStateFlow()

    private val _customApiKey = MutableStateFlow<String?>(null)
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
        }
    }

    fun dismissReview() {
        _reviewState.value = null
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
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
        }
    }

    fun openGoalsDialog() {
        _showGoalsDialog.value = true
    }

    fun closeGoalsDialog() {
        _showGoalsDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
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
