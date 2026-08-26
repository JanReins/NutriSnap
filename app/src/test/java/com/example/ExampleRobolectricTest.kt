package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiMealService
import com.example.data.local.MacroGoalEntity
import com.example.data.local.MealEntity
import com.example.data.local.NutriSnapDao
import com.example.data.local.NutriSnapDatabase
import com.example.data.repository.NutriRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
  fun `test meal insertion and daily query`() = runBlocking {
    val now = System.currentTimeMillis()
    val meal = MealEntity(
      mealName = "Oatmeal with Blueberries",
      calories = 320,
      proteinGrams = 12f,
      carbsGrams = 52f,
      fatsGrams = 6f,
      timestamp = now,
      notes = "Fresh berries and rolled oats"
    )

    val id = dao.insertMeal(meal)
    assertTrue(id > 0)

    val todayMeals = dao.getMealsForDate(now - 1000, now + 1000).first()
    assertEquals(1, todayMeals.size)
    assertEquals("Oatmeal with Blueberries", todayMeals[0].mealName)
    assertEquals(320, todayMeals[0].calories)

    // Test delete
    dao.deleteMealById(id)
    val afterDelete = dao.getMealsForDate(now - 1000, now + 1000).first()
    assertEquals(0, afterDelete.size)
  }

  @Test
  fun `test gemini meal local fallback estimation`() = runBlocking {
    val service = GeminiMealService()
    val result = service.analyzeMeal("2 eggs and whole wheat toast", null)

    assertTrue(result.isSuccess)
    val estimated = result.getOrNull()
    assertNotNull(estimated)
    assertEquals("Eggs & Toast", estimated?.mealName)
    assertTrue((estimated?.calories ?: 0) > 0)
    assertTrue((estimated?.protein ?: 0f) > 0f)
  }
}
