package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiMealService
import com.example.data.local.MacroGoalEntity
import com.example.data.local.MealEntity
import com.example.data.local.NutriSnapDao
import com.example.data.local.NutriSnapDatabase
import com.example.data.repository.NutriRepository
import com.example.ui.viewmodel.NutriViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: NutriSnapDatabase
  private lateinit var dao: NutriSnapDao
  private lateinit var repository: NutriRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, NutriSnapDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    dao = db.nutriSnapDao()
    repository = NutriRepository(dao)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("NutriSnap", appName)
  }

  @Test
  fun `test default macro goals insertion and retrieval`() = runBlocking {
    dao.setMacroGoals(MacroGoalEntity(id = 1, targetCalories = 2200, targetProtein = 160f, targetCarbs = 210f, targetFats = 65f))
    val retrieved = dao.getMacroGoals().first()
    assertNotNull(retrieved)
    assertEquals(2200, retrieved?.targetCalories)
    assertEquals(160f, retrieved?.targetProtein)
  }

  @Test
  fun `test meal insertion update and daily query`() = runBlocking {
    val now = System.currentTimeMillis()
    val meal = MealEntity(
      mealName = "Oatmeal with Blueberries",
      calories = 320,
      proteinGrams = 12f,
      carbsGrams = 52f,
      fatsGrams = 6f,
      fiberGrams = 5f,
      sugarGrams = 10f,
      mealType = "Breakfast",
      timestamp = now,
      notes = "Fresh berries and rolled oats"
    )

    val id = dao.insertMeal(meal)
    assertTrue(id > 0)

    val todayMeals = dao.getMealsForDate(now - 1000, now + 1000).first()
    assertEquals(1, todayMeals.size)
    assertEquals("Oatmeal with Blueberries", todayMeals[0].mealName)
    assertEquals(320, todayMeals[0].calories)
    assertEquals("Breakfast", todayMeals[0].mealType)

    // Test update
    val updatedMeal = todayMeals[0].copy(mealName = "Oatmeal with Extra Almonds", calories = 390)
    dao.updateMeal(updatedMeal)

    val afterUpdate = dao.getMealById(id)
    assertNotNull(afterUpdate)
    assertEquals("Oatmeal with Extra Almonds", afterUpdate?.mealName)
    assertEquals(390, afterUpdate?.calories)

    // Test delete
    dao.deleteMealById(id)
    val afterDelete = dao.getMealsForDate(now - 1000, now + 1000).first()
    assertEquals(0, afterDelete.size)
  }

  @Test
  fun `test gemini meal local fallback estimation with honest provenance and filipino dishes`() = runBlocking {
    val service = GeminiMealService()

    // Western staple fallback
    val resultToast = service.analyzeMeal("2 eggs and whole wheat toast", null)
    assertTrue(resultToast.isSuccess)
    val estimatedToast = resultToast.getOrNull()
    assertNotNull(estimatedToast)
    assertFalse(estimatedToast!!.isAiEstimate) // Honest local provenance
    assertTrue(estimatedToast.calories > 0)
    assertTrue(estimatedToast.protein > 0f)

    // Filipino staple fallback (Chicken Adobo)
    val resultAdobo = service.analyzeMeal("Chicken adobo with 1 cup rice", null)
    assertTrue(resultAdobo.isSuccess)
    val estimatedAdobo = resultAdobo.getOrNull()
    assertNotNull(estimatedAdobo)
    assertEquals("Chicken Adobo with Rice", estimatedAdobo!!.mealName)
    assertEquals("Lunch", estimatedAdobo.mealType)
    assertEquals(510, estimatedAdobo.calories)
    assertEquals(36f, estimatedAdobo.protein)
    assertFalse(estimatedAdobo.isAiEstimate)
  }

  @Test
  fun `test backup json parsing and import preview`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = NutriViewModel(app)

    val jsonString = """
      {
        "appName": "NutriSnap",
        "version": 2,
        "exportedAt": "2026-08-28T12:00:00Z",
        "macroGoals": {
          "targetCalories": 2400,
          "targetProtein": 180.0,
          "targetCarbs": 220.0,
          "targetFats": 70.0
        },
        "meals": [
          {
            "mealName": "Grilled Chicken Rice Bowl",
            "calories": 520,
            "proteinGrams": 42.0,
            "carbsGrams": 58.0,
            "fatsGrams": 12.0,
            "fiberGrams": 3.0,
            "sugarGrams": 2.0,
            "mealType": "Lunch",
            "timestamp": 1724800000000,
            "notes": "150g chicken breast",
            "isAiEstimated": true,
            "imageUriOrBase64": ""
          }
        ]
      }
    """.trimIndent()

    val preview = vm.parseBackupJsonContent(jsonString)
    assertEquals(1, preview.mealsCount)
    assertEquals(2400, preview.goals.targetCalories)
    assertEquals(180f, preview.goals.targetProtein)
    assertEquals("Grilled Chicken Rice Bowl", preview.meals[0].mealName)
    assertEquals(520, preview.meals[0].calories)
    assertEquals("Lunch", preview.meals[0].mealType)
  }
}
