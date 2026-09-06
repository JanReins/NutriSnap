package com.janreins.nutrisnap

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.janreins.nutrisnap.data.local.MacroGoalEntity
import com.janreins.nutrisnap.data.local.MealEntity
import com.janreins.nutrisnap.ui.components.MacroDashboard
import com.janreins.nutrisnap.ui.theme.NutriSnapTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      NutriSnapTheme {
        MacroDashboard(
          meals = listOf(
            MealEntity(
              id = 1,
              mealName = "Oatmeal with Berries",
              calories = 350,
              proteinGrams = 12f,
              carbsGrams = 55f,
              fatsGrams = 6f
            )
          ),
          goals = MacroGoalEntity(1, 2000, 150f, 200f, 65f)
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
