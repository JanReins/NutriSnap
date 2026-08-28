package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Data model for meal analysis output.
 * Tracks estimated macros, optional micronutrients (fiber/sugar), meal type, and AI vs local provenance.
 */
data class EstimatedMeal(
    val mealName: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val mealType: String = "Meal", // Breakfast, Lunch, Dinner, Snack
    val notes: String = "",
    val isAiEstimate: Boolean = true
)

/**
 * Service for analyzing meal descriptions and food photos using Gemini 2.5 Flash.
 * Falls back gracefully to intelligent local heuristics if offline or without an API key,
 * clearly marking whether the estimate came from AI or local heuristic.
 */
class GeminiMealService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyzeMeal(
        textDescription: String?,
        imageBitmap: Bitmap?,
        customApiKey: String? = null
    ): Result<EstimatedMeal> = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.takeIf {
                it.isNotBlank() && !it.startsWith("MY_GEMINI_API_KEY")
            }

        // If no Gemini API key configured, use transparent local fallback
        if (apiKey == null) {
            val fallback = localFallbackEstimate(textDescription, imageBitmap != null)
            return@withContext Result.success(fallback)
        }

        try {
            val systemPrompt = """
                You are an expert nutritionist and meal analyzer for the NutriSnap app.
                Analyze the food description or image provided.
                Estimate realistic portion sizes, total calories (kcal), protein (g), carbohydrates (g), fats (g), dietary fiber (g), and sugar (g).
                Determine the most appropriate meal type: "Breakfast", "Lunch", "Dinner", or "Snack".
                In the notes, provide the assumed portion breakdown and a brief confidence comment.
                
                You MUST return ONLY a JSON object matching this exact schema:
                {
                  "mealName": "Short descriptive meal name (e.g., Grilled Salmon Bowl, Chicken Adobo with Rice)",
                  "mealType": "Lunch",
                  "calories": 480,
                  "protein": 36.0,
                  "carbs": 45.0,
                  "fats": 14.0,
                  "fiber": 4.0,
                  "sugar": 3.5,
                  "notes": "Assumed 150g grilled chicken, 1 cup steamed rice. High confidence estimate."
                }
            """.trimIndent()

            val partsArray = JSONArray()

            if (!textDescription.isNullOrBlank()) {
                val textPart = JSONObject().apply {
                    put("text", "Food or meal to analyze: $textDescription")
                }
                partsArray.put(textPart)
            }

            if (imageBitmap != null) {
                val base64Image = bitmapToBase64(imageBitmap)
                val imagePart = JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    })
                }
                partsArray.put(imagePart)
            }

            if (partsArray.length() == 0) {
                return@withContext Result.failure(IllegalArgumentException("Please provide a food description or photo"))
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.w("GeminiMealService", "API call unsuccessful: code ${response.code}")
                val fallback = localFallbackEstimate(textDescription, imageBitmap != null)
                return@withContext Result.success(fallback.copy(
                    notes = "AI unavailable (HTTP ${response.code}). Showing rough local estimate."
                ))
            }

            val parsedMeal = parseGeminiResponse(responseBody)
            Result.success(parsedMeal)
        } catch (e: Exception) {
            Log.e("GeminiMealService", "Error during Gemini meal analysis", e)
            val fallback = localFallbackEstimate(textDescription, imageBitmap != null)
            Result.success(fallback.copy(
                notes = "AI unavailable. Showing rough local estimate: ${e.localizedMessage ?: "Offline"}"
            ))
        }
    }

    private fun parseGeminiResponse(responseBody: String): EstimatedMeal {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

        val cleanedJson = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val mealJson = JSONObject(cleanedJson)
        val mealName = mealJson.optString("mealName", "Logged Meal")
        val mealType = mealJson.optString("mealType", "Meal")
        val calories = mealJson.optInt("calories", 350)
        val protein = mealJson.optDouble("protein", 20.0).toFloat()
        val carbs = mealJson.optDouble("carbs", 35.0).toFloat()
        val fats = mealJson.optDouble("fats", 10.0).toFloat()
        val fiber = mealJson.optDouble("fiber", 2.0).toFloat()
        val sugar = mealJson.optDouble("sugar", 2.0).toFloat()
        val notes = mealJson.optString("notes", "Estimated via Gemini AI with portion recognition.")

        return EstimatedMeal(
            mealName = mealName,
            mealType = mealType,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats,
            fiber = fiber,
            sugar = sugar,
            notes = notes,
            isAiEstimate = true
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height
        val resized = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Transparent, honest local fallback estimator with rich Asian/Filipino and Western home meals.
     * Clearly flagged with isAiEstimate = false.
     */
    fun localFallbackEstimate(description: String?, hasPhoto: Boolean): EstimatedMeal {
        val text = description?.lowercase() ?: ""

        return when {
            // Filipino / Asian Home Meals
            text.contains("adobo") -> EstimatedMeal(
                mealName = if (text.contains("pork")) "Pork Adobo with Rice" else "Chicken Adobo with Rice",
                mealType = "Lunch",
                calories = 510,
                protein = 36f,
                carbs = 48f,
                fats = 20f,
                fiber = 1.5f,
                sugar = 3f,
                notes = "Estimated 1 serving adobo with soy-vinegar sauce and 1 cup rice",
                isAiEstimate = false
            )
            text.contains("sinigang") -> EstimatedMeal(
                mealName = "Sinigang with Rice",
                mealType = "Dinner",
                calories = 420,
                protein = 30f,
                carbs = 46f,
                fats = 12f,
                fiber = 3.5f,
                sugar = 4f,
                notes = "Estimated tamarind sour soup with kangkong, radish, meat, and rice",
                isAiEstimate = false
            )
            text.contains("silog") || text.contains("tapsilog") || text.contains("tocilog") || text.contains("longsilog") -> EstimatedMeal(
                mealName = if (text.contains("tapsilog")) "Tapsilog" else if (text.contains("tocilog")) "Tocilog" else "Filipino Silog Plate",
                mealType = "Breakfast",
                calories = 580,
                protein = 26f,
                carbs = 62f,
                fats = 26f,
                fiber = 1.5f,
                sugar = 4f,
                notes = "Estimated garlic fried rice (sinangag), fried egg, and cured meat",
                isAiEstimate = false
            )
            text.contains("tinola") -> EstimatedMeal(
                mealName = "Chicken Tinola with Rice",
                mealType = "Dinner",
                calories = 390,
                protein = 32f,
                carbs = 44f,
                fats = 9f,
                fiber = 2.5f,
                sugar = 2f,
                notes = "Estimated ginger chicken soup with green papaya/sayote and 1 cup rice",
                isAiEstimate = false
            )
            text.contains("inihaw") || text.contains("tilapia") || text.contains("bangus") || text.contains("grilled fish") -> EstimatedMeal(
                mealName = "Grilled Fish with Rice & Veggies",
                mealType = "Lunch",
                calories = 380,
                protein = 34f,
                carbs = 45f,
                fats = 7f,
                fiber = 2f,
                sugar = 1f,
                notes = "Estimated 1 whole medium grilled fish with rice and dipping sauce",
                isAiEstimate = false
            )
            text.contains("pancit") || text.contains("bihon") || text.contains("canton") || text.contains("noodles") -> EstimatedMeal(
                mealName = "Pancit Stir-fry Noodles",
                mealType = "Lunch",
                calories = 440,
                protein = 18f,
                carbs = 64f,
                fats = 13f,
                fiber = 3f,
                sugar = 2.5f,
                notes = "Estimated 1.5 cups stir-fried noodles with mixed vegetables and pork/chicken",
                isAiEstimate = false
            )
            text.contains("chop suey") || text.contains("chopsuey") -> EstimatedMeal(
                mealName = "Chop Suey with Rice",
                mealType = "Lunch",
                calories = 390,
                protein = 22f,
                carbs = 52f,
                fats = 11f,
                fiber = 5f,
                sugar = 4f,
                notes = "Estimated mixed stir-fry vegetables with meat slices and rice",
                isAiEstimate = false
            )
            text.contains("rice") && (text.contains("cup") || text.contains("white") || text.contains("kanin")) -> EstimatedMeal(
                mealName = "Steamed White Rice",
                mealType = "Meal",
                calories = 205,
                protein = 4.2f,
                carbs = 45f,
                fats = 0.4f,
                fiber = 0.6f,
                sugar = 0.1f,
                notes = "Estimated 1 cup standard cooked white rice",
                isAiEstimate = false
            )

            // Western & Everyday Staples
            text.contains("egg") && (text.contains("toast") || text.contains("bread")) -> EstimatedMeal(
                mealName = "Eggs & Whole Grain Toast",
                mealType = "Breakfast",
                calories = 340,
                protein = 18f,
                carbs = 28f,
                fats = 16f,
                fiber = 3f,
                sugar = 2f,
                notes = "Estimated 2 eggs with whole grain toast and butter",
                isAiEstimate = false
            )
            text.contains("oat") || text.contains("oatmeal") -> EstimatedMeal(
                mealName = "Oatmeal with Toppings",
                mealType = "Breakfast",
                calories = 310,
                protein = 11f,
                carbs = 54f,
                fats = 6f,
                fiber = 6f,
                sugar = 12f,
                notes = "Estimated 1 cup cooked oats with fruit and honey",
                isAiEstimate = false
            )
            text.contains("chicken") && (text.contains("rice") || text.contains("bowl")) -> EstimatedMeal(
                mealName = "Grilled Chicken Rice Bowl",
                mealType = "Lunch",
                calories = 520,
                protein = 42f,
                carbs = 58f,
                fats = 12f,
                fiber = 3f,
                sugar = 2f,
                notes = "Estimated 150g grilled chicken breast with 1 cup rice and veggies",
                isAiEstimate = false
            )
            text.contains("salmon") || text.contains("fish") -> EstimatedMeal(
                mealName = "Salmon Fillet with Sides",
                mealType = "Dinner",
                calories = 480,
                protein = 38f,
                carbs = 20f,
                fats = 26f,
                fiber = 3f,
                sugar = 1f,
                notes = "Estimated pan-seared salmon with vegetables",
                isAiEstimate = false
            )
            text.contains("salad") -> EstimatedMeal(
                mealName = "Fresh Garden Salad",
                mealType = "Lunch",
                calories = 260,
                protein = 9f,
                carbs = 18f,
                fats = 18f,
                fiber = 4.5f,
                sugar = 3.5f,
                notes = "Estimated mixed greens, vinaigrette, seeds, and light cheese",
                isAiEstimate = false
            )
            text.contains("shake") || text.contains("smoothie") || text.contains("protein") -> EstimatedMeal(
                mealName = "Protein Smoothie",
                mealType = "Snack",
                calories = 290,
                protein = 30f,
                carbs = 28f,
                fats = 5f,
                fiber = 4f,
                sugar = 14f,
                notes = "Estimated 1 scoop protein powder, banana, and milk",
                isAiEstimate = false
            )
            text.contains("pasta") || text.contains("spaghetti") -> EstimatedMeal(
                mealName = "Pasta with Sauce",
                mealType = "Dinner",
                calories = 560,
                protein = 22f,
                carbs = 76f,
                fats = 17f,
                fiber = 4f,
                sugar = 6f,
                notes = "Estimated 1.5 cups pasta with sauce and parmesan",
                isAiEstimate = false
            )
            text.contains("pizza") || text.contains("burger") -> EstimatedMeal(
                mealName = if (text.contains("pizza")) "Pizza Slices" else "Classic Burger",
                mealType = "Dinner",
                calories = 650,
                protein = 28f,
                carbs = 62f,
                fats = 32f,
                fiber = 2.5f,
                sugar = 5f,
                notes = "Estimated standard restaurant single serving",
                isAiEstimate = false
            )
            hasPhoto -> EstimatedMeal(
                mealName = "Photo Logged Meal",
                mealType = "Meal",
                calories = 420,
                protein = 24f,
                carbs = 44f,
                fats = 16f,
                fiber = 3f,
                sugar = 3f,
                notes = "Local balanced portion estimate from photo. Please adjust numbers if needed.",
                isAiEstimate = false
            )
            else -> EstimatedMeal(
                mealName = if (description.isNullOrBlank()) "Logged Meal" else description.trim().replaceFirstChar { it.uppercase() },
                mealType = "Meal",
                calories = 400,
                protein = 20f,
                carbs = 45f,
                fats = 14f,
                fiber = 3f,
                sugar = 4f,
                notes = "Local general estimate. Please review and edit before saving.",
                isAiEstimate = false
            )
        }
    }
}
