package com.janreins.nutrisnap

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.janreins.nutrisnap.data.ai.GeminiMealService
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for GeminiMealService local fallback estimation logic
 * and NutriViewModel backup JSON parsing/import preview helpers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiAndBackupUnitTest {

  @Test
  fun `test gemini meal local fallback with filipino and western dishes`() = runBlocking {
    val service = GeminiMealService()

    // Filipino Sinigang
    val resultSinigang = service.analyzeMeal("Pork sinigang with rice and kangkong", null)
    assertTrue(resultSinigang.isSuccess)
    val mealSinigang = resultSinigang.getOrNull()
    assertNotNull(mealSinigang)
    assertEquals("Sinigang with Rice", mealSinigang!!.mealName)
    assertEquals("Dinner", mealSinigang.mealType)
    assertEquals(420, mealSinigang.calories)
    assertFalse(mealSinigang.isAiEstimate)

    // Western Oatmeal
    val resultOatmeal = service.analyzeMeal("1 cup oatmeal with banana and honey", null)
    assertTrue(resultOatmeal.isSuccess)
    val mealOatmeal = resultOatmeal.getOrNull()
    assertNotNull(mealOatmeal)
    assertEquals("Oatmeal with Toppings", mealOatmeal!!.mealName)
    assertEquals("Breakfast", mealOatmeal.mealType)
    assertEquals(310, mealOatmeal.calories)
    assertFalse(mealOatmeal.isAiEstimate)
  }

  @Test
  fun `test backup JSON parsing and preview helper`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = NutriViewModel(app)

    val validJson = """
      {
        "appName": "NutriSnap",
        "version": 2,
        "exportedAt": "2026-08-28T12:00:00Z",
        "macroGoals": {
          "targetCalories": 2200,
          "targetProtein": 150.0,
          "targetCarbs": 200.0,
          "targetFats": 60.0
        },
        "meals": [
          {
            "mealName": "Chicken Adobo",
            "calories": 510,
            "proteinGrams": 36.0,
            "carbsGrams": 48.0,
            "fatsGrams": 20.0,
            "fiberGrams": 1.5,
            "sugarGrams": 3.0,
            "mealType": "Lunch",
            "timestamp": 1724800000000,
            "notes": "1 serving with rice",
            "isAiEstimated": false,
            "imageUriOrBase64": ""
          }
        ]
      }
    """.trimIndent()

    val preview = vm.parseBackupJsonContent(validJson)
    assertEquals(1, preview.mealsCount)
    assertEquals(2200, preview.goals.targetCalories)
    assertEquals(150f, preview.goals.targetProtein)
    assertEquals("Chicken Adobo", preview.meals[0].mealName)
    assertEquals(510, preview.meals[0].calories)
  }
}
