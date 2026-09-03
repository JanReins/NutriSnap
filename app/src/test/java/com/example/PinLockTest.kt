package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.NutriViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinLockTest {

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
    fun `default state when no PIN is set - PIN lock disabled and app unlocked`() {
        assertFalse(viewModel.isPinEnabled.value)
        assertTrue(viewModel.isUnlocked.value)
    }

    @Test
    fun `setting valid PIN enables PIN lock`() {
        val success = viewModel.setPin("1234")
        assertTrue(success)

        assertTrue(viewModel.isPinEnabled.value)
        assertTrue(viewModel.isUnlocked.value)
    }

    @Test
    fun `setting invalid PIN fails`() {
        assertFalse(viewModel.setPin("12"))
        assertFalse(viewModel.setPin("abcd"))
        assertFalse(viewModel.setPin("1234567"))

        assertFalse(viewModel.isPinEnabled.value)
    }

    @Test
    fun `locking app requires PIN verification to unlock`() {
        viewModel.setPin("4321")
        viewModel.lockApp()

        assertFalse(viewModel.isUnlocked.value)

        // Wrong PIN
        assertFalse(viewModel.verifyPin("0000"))
        assertFalse(viewModel.isUnlocked.value)

        // Correct PIN
        assertTrue(viewModel.verifyPin("4321"))
        assertTrue(viewModel.isUnlocked.value)
    }

    @Test
    fun `changing PIN with correct current PIN succeeds`() {
        viewModel.setPin("1111")

        // Wrong current PIN
        assertFalse(viewModel.changePin("9999", "2222"))

        // Correct current PIN
        assertTrue(viewModel.changePin("1111", "2222"))

        // Lock and verify new PIN
        viewModel.lockApp()
        assertFalse(viewModel.verifyPin("1111"))
        assertTrue(viewModel.verifyPin("2222"))
    }

    @Test
    fun `removing PIN requires correct current PIN`() {
        viewModel.setPin("5555")

        // Wrong current PIN
        assertFalse(viewModel.removePin("0000"))
        assertTrue(viewModel.isPinEnabled.value)

        // Correct current PIN
        assertTrue(viewModel.removePin("5555"))
        assertFalse(viewModel.isPinEnabled.value)
        assertTrue(viewModel.isUnlocked.value)
    }

    @Test
    fun `background timeout triggers re-lock when PIN is enabled`() {
        viewModel.setPin("1234")
        assertTrue(viewModel.isUnlocked.value)

        // Backgrounded for 5 seconds -> should remain unlocked
        val fiveSecsAgo = System.currentTimeMillis() - 5_000L
        viewModel.checkBackgroundTimeoutAndLock(fiveSecsAgo)
        assertTrue(viewModel.isUnlocked.value)

        // Backgrounded for 16 seconds -> should lock
        val sixteenSecsAgo = System.currentTimeMillis() - 16_000L
        viewModel.checkBackgroundTimeoutAndLock(sixteenSecsAgo)
        assertFalse(viewModel.isUnlocked.value)
    }
}
