package com.janreins.nutrisnap

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.janreins.nutrisnap.data.local.MealEntity
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel
import com.janreins.nutrisnap.ui.viewmodel.SummaryMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SummaryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: NutriViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("nutrisnap_user_preferences", Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        viewModel = NutriViewModel(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getWeekRange calculates Monday to Sunday span`() {
        // Wednesday, Sep 16, 2026
        val anchorCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.SEPTEMBER, 16, 14, 30, 0)
        }
        val (start, end) = NutriViewModel.getWeekRange(anchorCal.timeInMillis)

        val startCal = Calendar.getInstance(Locale.US).apply { timeInMillis = start }
        val endCal = Calendar.getInstance(Locale.US).apply { timeInMillis = end }

        // Monday Sep 14, 2026 00:00:00
        assertEquals(Calendar.MONDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(14, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        // Sunday Sep 20, 2026 23:59:59
        assertEquals(Calendar.SUNDAY, endCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(20, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `getMonthRange calculates 1st to last day of month`() {
        // Sep 15, 2026
        val anchorCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.SEPTEMBER, 15, 10, 0, 0)
        }
        val (start, end) = NutriViewModel.getMonthRange(anchorCal.timeInMillis)

        val startCal = Calendar.getInstance(Locale.US).apply { timeInMillis = start }
        val endCal = Calendar.getInstance(Locale.US).apply { timeInMillis = end }

        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `computeSummaryUiState correctly sums macros and averages over period`() {
        // Wednesday, Sep 16, 2026
        val anchorCal = Calendar.getInstance(Locale.US).apply {
            set(2026, Calendar.SEPTEMBER, 16, 12, 0, 0)
        }
        val (startWeek, _) = NutriViewModel.getWeekRange(anchorCal.timeInMillis)

        // Day 1 (Mon Sep 14): Breakfast + Lunch (total 1000 cals, 60g P, 100g C, 30g F)
        val m1 = MealEntity(
            id = 1,
            mealName = "Oatmeal",
            calories = 400,
            proteinGrams = 20f,
            carbsGrams = 60f,
            fatsGrams = 10f,
            fiberGrams = 8f,
            sugarGrams = 5f,
            mealType = "Breakfast",
            timestamp = startWeek + 8 * 3600 * 1000L
        )
        val m2 = MealEntity(
            id = 2,
            mealName = "Chicken Rice",
            calories = 600,
            proteinGrams = 40f,
            carbsGrams = 40f,
            fatsGrams = 20f,
            fiberGrams = 2f,
            sugarGrams = 1f,
            mealType = "Lunch",
            timestamp = startWeek + 13 * 3600 * 1000L
        )

        // Day 3 (Wed Sep 16): Dinner (total 700 cals, 50g P, 50g C, 25g F)
        val m3 = MealEntity(
            id = 3,
            mealName = "Salmon Salad",
            calories = 700,
            proteinGrams = 50f,
            carbsGrams = 50f,
            fatsGrams = 25f,
            fiberGrams = 5f,
            sugarGrams = 2f,
            mealType = "Dinner",
            timestamp = startWeek + 2 * 24 * 3600 * 1000L + 19 * 3600 * 1000L
        )

        val meals = listOf(m1, m2, m3)

        val summary = NutriViewModel.computeSummaryUiState(SummaryMode.WEEK, anchorCal.timeInMillis, meals)

        assertFalse(summary.isEmpty)
        assertEquals(7, summary.daysInPeriod)
        assertEquals(2, summary.loggedDaysCount) // Mon and Wed

        // Totals
        assertEquals(1700, summary.totalCalories)
        assertEquals(110f, summary.totalProtein, 0.1f)
        assertEquals(150f, summary.totalCarbs, 0.1f)
        assertEquals(55f, summary.totalFats, 0.1f)

        // Daily Averages over 7 days
        assertEquals(243, summary.avgCalories) // 1700 / 7 = 242.857 -> 243
        assertEquals(15.71f, summary.avgProtein, 0.1f) // 110 / 7 = 15.71

        // Meal counts
        assertEquals(1, summary.breakfastCount)
        assertEquals(1, summary.lunchCount)
        assertEquals(1, summary.dinnerCount)
        assertEquals(0, summary.snackCount)

        // Daily bars
        assertEquals(7, summary.dailyCalorieBars.size)
        assertEquals(1000, summary.dailyCalorieBars[0].totalCalories) // Mon
        assertEquals(0, summary.dailyCalorieBars[1].totalCalories)    // Tue
        assertEquals(700, summary.dailyCalorieBars[2].totalCalories)  // Wed
    }

    @Test
    fun `empty meals yields friendly empty summary state`() {
        val summary = NutriViewModel.computeSummaryUiState(SummaryMode.WEEK, System.currentTimeMillis(), emptyList())
        assertTrue(summary.isEmpty)
        assertEquals(0, summary.totalCalories)
        assertEquals(0, summary.loggedDaysCount)
        assertEquals(7, summary.daysInPeriod)
    }

    @Test
    fun `switching mode and navigating period updates ViewModel state`() = runTest {
        assertEquals(SummaryMode.WEEK, viewModel.summaryMode.value)

        viewModel.setSummaryMode(SummaryMode.MONTH)
        assertEquals(SummaryMode.MONTH, viewModel.summaryMode.value)

        val initialAnchor = viewModel.summaryAnchorDateMillis.value
        viewModel.goToPreviousSummaryPeriod()
        val prevAnchor = viewModel.summaryAnchorDateMillis.value

        assertTrue(prevAnchor < initialAnchor)

        viewModel.resetToCurrentSummaryPeriod()
        assertEquals(viewModel.selectedDateMillis.value, viewModel.summaryAnchorDateMillis.value)
    }
}
