package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferencesRepository
import com.example.game.GamePhase
import com.example.game.TempleRunViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `app name and creator support info match request`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Temple Run", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.developer_credit).contains("አዲሴ አራጋው"))
        assertEquals("0942320888", context.getString(R.string.support_telebirr_number))
        assertEquals("66833787", context.getString(R.string.support_abyssinia_number))
        assertEquals("1000282540634", context.getString(R.string.support_cbe_number))
    }

    @Test
    fun `gameplay lane switching jumping scenery and pause transitions work`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TempleRunViewModel(GamePreferencesRepository(app))

        assertEquals(GamePhase.MENU, viewModel.gameState.value.phase)
        assertTrue(viewModel.gameState.value.scenery.isNotEmpty())

        viewModel.startGame()
        assertEquals(GamePhase.PLAYING, viewModel.gameState.value.phase)
        assertEquals(1, viewModel.gameState.value.targetLane)

        viewModel.moveLeft()
        assertEquals(0, viewModel.gameState.value.targetLane)
        viewModel.moveLeft()
        assertEquals(0, viewModel.gameState.value.targetLane)

        viewModel.moveRight()
        viewModel.moveRight()
        assertEquals(2, viewModel.gameState.value.targetLane)

        viewModel.jump()
        assertTrue(viewModel.gameState.value.isJumping)
        viewModel.onFrame(0.016f)
        assertTrue(viewModel.gameState.value.jumpY > 0f)

        viewModel.pauseGame()
        assertEquals(GamePhase.PAUSED, viewModel.gameState.value.phase)
        viewModel.resumeGame()
        assertEquals(GamePhase.PLAYING, viewModel.gameState.value.phase)
    }
}
