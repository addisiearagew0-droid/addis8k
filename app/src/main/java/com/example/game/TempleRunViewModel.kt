package com.example.game

import android.app.Application
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GamePreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class TempleRunViewModel(
    private val repository: GamePreferencesRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(
        RunnerGameState(scenery = createInitialScenery())
    )
    val gameState: StateFlow<RunnerGameState> = _gameState.asStateFlow()

    private val _avatarBitmap = MutableStateFlow<ImageBitmap?>(null)
    val avatarBitmap: StateFlow<ImageBitmap?> = _avatarBitmap.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<GameHapticEvent>(extraBufferCapacity = 8)
    val hapticEvents: SharedFlow<GameHapticEvent> = _hapticEvents.asSharedFlow()

    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private var nextEntityId = 100L
    private var spawnAccumulator = 0f
    private var sceneryAccumulator = 0f
    private var dustAccumulator = 0f

    init {
        viewModelScope.launch {
            repository.highScoreFlow.collect { best ->
                _gameState.update { it.copy(highScore = best) }
            }
        }
        viewModelScope.launch {
            _avatarBitmap.value = repository.loadSavedAvatar()
        }
    }

    fun startGame() {
        spawnAccumulator = 0.6f
        sceneryAccumulator = 0.2f
        dustAccumulator = 0f
        _gameState.update { current ->
            RunnerGameState(
                phase = GamePhase.PLAYING,
                highScore = current.highScore,
                isNewRecord = false,
                speed = 6f,
                targetLane = 1,
                visualLane = 1f,
                scenery = createInitialScenery()
            )
        }
    }

    fun pauseGame() {
        _gameState.update { state ->
            if (state.phase == GamePhase.PLAYING) {
                state.copy(phase = GamePhase.PAUSED)
            } else {
                state
            }
        }
    }

    fun resumeGame() {
        _gameState.update { state ->
            if (state.phase == GamePhase.PAUSED) {
                state.copy(phase = GamePhase.PLAYING)
            } else {
                state
            }
        }
    }

    fun moveLeft() {
        val current = _gameState.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.targetLane > 0) {
            _gameState.update { it.copy(targetLane = (it.targetLane - 1).coerceAtLeast(0)) }
            _hapticEvents.tryEmit(GameHapticEvent.LaneSwitch)
        }
    }

    fun moveRight() {
        val current = _gameState.value
        if (current.phase != GamePhase.PLAYING) return
        if (current.targetLane < 2) {
            _gameState.update { it.copy(targetLane = (it.targetLane + 1).coerceAtMost(2)) }
            _hapticEvents.tryEmit(GameHapticEvent.LaneSwitch)
        }
    }

    fun jump() {
        val current = _gameState.value
        if (current.phase != GamePhase.PLAYING || current.isJumping) return
        _gameState.update {
            it.copy(
                isJumping = true,
                jumpVelocity = 19.5f
            )
        }
        _hapticEvents.tryEmit(GameHapticEvent.Jump)
    }

    fun fastDrop() {
        val current = _gameState.value
        if (current.phase != GamePhase.PLAYING || !current.isJumping) return
        if (current.jumpVelocity > -12f) {
            _gameState.update { it.copy(jumpVelocity = -16f) }
        }
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = repository.saveAvatarFromUri(uri)
            if (bitmap != null) {
                _avatarBitmap.value = bitmap
            }
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            repository.clearSavedAvatar()
            _avatarBitmap.value = null
        }
    }

    fun toggleHaptics() {
        viewModelScope.launch {
            repository.setHapticsEnabled(!hapticsEnabled.value)
        }
    }

    fun onFrame(deltaSeconds: Float) {
        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)
        val dtScale = dt * 60f

        val state = _gameState.value
        if (state.phase != GamePhase.PLAYING) {
            if (state.crashFlash > 0f) {
                _gameState.update {
                    it.copy(crashFlash = (it.crashFlash - dt * 2.5f).coerceAtLeast(0f))
                }
            }
            return
        }

        val newScoreTicks = state.scoreTicks + dtScale
        val newSpeed = (state.speed + dt * 0.12f).coerceAtMost(16f)
        val newRunCycle = (state.runCycle + dt * newSpeed * 2.15f) % (Math.PI.toFloat() * 2f)
        val newRoadScroll = (state.roadScroll + dt * newSpeed * 0.30f) % 1f
        val newDayCycle = (state.dayCycle + dt * 0.022f) % 1f

        val laneDelta = state.targetLane - state.visualLane
        val newVisualLane = if (abs(laneDelta) < 0.01f) {
            state.targetLane.toFloat()
        } else {
            state.visualLane + laneDelta * min(1f, dt * 16f)
        }
        val newLaneTilt = (laneDelta * 1.6f).coerceIn(-1f, 1f)

        var newIsJumping = state.isJumping
        var newJumpY = state.jumpY
        var newJumpVelocity = state.jumpVelocity
        var justLanded = false
        if (newIsJumping) {
            newJumpY += newJumpVelocity * dtScale
            newJumpVelocity -= 1.25f * dtScale
            if (newJumpY <= 0f) {
                newJumpY = 0f
                newJumpVelocity = 0f
                newIsJumping = false
                justLanded = true
            }
        }

        // Advance roadside Ethiopian scenery
        val updatedScenery = ArrayList<SceneryEntity>(state.scenery.size + 2)
        for (item in state.scenery) {
            val nextProgress = item.progress + (newSpeed * 0.002f) * (item.progress + 0.3f) * dtScale
            if (nextProgress <= 1.28f) {
                updatedScenery.add(item.copy(progress = nextProgress))
            }
        }
        sceneryAccumulator += dt
        val sceneryInterval = max(0.26f, 0.72f - (newSpeed - 6f) * 0.04f)
        if (sceneryAccumulator >= sceneryInterval) {
            sceneryAccumulator -= sceneryInterval
            val kinds = SceneryKind.entries
            updatedScenery.add(
                SceneryEntity(
                    id = nextEntityId++,
                    side = if (Random.nextBoolean()) -1 else 1,
                    lateralFactor = 1.22f + Random.nextFloat() * 0.65f,
                    progress = 0.03f,
                    kind = kinds[Random.nextInt(kinds.size)]
                )
            )
        }

        // Advance dust & coin sparkle particles
        val updatedParticles = ArrayList<DustParticle>(state.particles.size + 8)
        for (p in state.particles) {
            val nextAge = p.age + dt
            if (nextAge < p.maxAge) {
                updatedParticles.add(
                    p.copy(
                        lane = p.lane + p.vx * dt,
                        yOffset = p.yOffset + p.vy * dt,
                        age = nextAge
                    )
                )
            }
        }

        // Footstep dust puffs on ground
        if (!newIsJumping) {
            dustAccumulator += dt * newSpeed
            if (dustAccumulator >= 0.95f) {
                dustAccumulator = 0f
                updatedParticles.add(
                    DustParticle(
                        id = nextEntityId++,
                        lane = newVisualLane + (Random.nextFloat() - 0.5f) * 0.16f,
                        yOffset = 34f,
                        vx = (Random.nextFloat() - 0.5f) * 0.35f,
                        vy = -18f - Random.nextFloat() * 14f,
                        size = 7f + Random.nextFloat() * 5f,
                        isGold = false,
                        age = 0f,
                        maxAge = 0.38f
                    )
                )
            }
        }
        if (justLanded) {
            repeat(5) { idx ->
                val dir = if (idx % 2 == 0) -1f else 1f
                updatedParticles.add(
                    DustParticle(
                        id = nextEntityId++,
                        lane = newVisualLane,
                        yOffset = 34f,
                        vx = dir * (0.4f + Random.nextFloat() * 0.5f),
                        vy = -14f - Random.nextFloat() * 20f,
                        size = 8f + Random.nextFloat() * 6f,
                        isGold = false,
                        age = 0f,
                        maxAge = 0.42f
                    )
                )
            }
        }

        val updatedObstacles = mutableListOf<ObstacleEntity>()
        var hitObstacle = false
        for (obs in state.obstacles) {
            val nextProgress = obs.progress + (newSpeed * 0.002f) * (obs.progress + 0.3f) * dtScale
            if (nextProgress in 0.87f..0.97f &&
                abs(newVisualLane - obs.lane) < 0.42f &&
                newJumpY < 48f
            ) {
                hitObstacle = true
            }
            if (nextProgress <= 1.22f) {
                updatedObstacles.add(obs.copy(progress = nextProgress))
            }
        }

        val updatedCoins = mutableListOf<CoinEntity>()
        var collectedCoinsCount = 0
        val newEffects = state.floatingEffects
            .mapNotNull { effect ->
                val nextAge = effect.age + dt
                if (nextAge < 0.75f) effect.copy(age = nextAge) else null
            }
            .toMutableList()

        for (coin in state.coins) {
            val nextProgress = coin.progress + (newSpeed * 0.002f) * (coin.progress + 0.3f) * dtScale
            if (nextProgress in 0.85f..0.99f &&
                abs(newVisualLane - coin.lane) < 0.48f &&
                newJumpY < 105f
            ) {
                collectedCoinsCount++
                newEffects.add(
                    FloatingEffect(
                        id = nextEntityId++,
                        text = "+20",
                        lane = coin.lane.toFloat(),
                        age = 0f
                    )
                )
                repeat(6) {
                    updatedParticles.add(
                        DustParticle(
                            id = nextEntityId++,
                            lane = coin.lane.toFloat(),
                            yOffset = -10f - newJumpY,
                            vx = (Random.nextFloat() - 0.5f) * 1.4f,
                            vy = -45f - Random.nextFloat() * 45f,
                            size = 5f + Random.nextFloat() * 4f,
                            isGold = true,
                            age = 0f,
                            maxAge = 0.48f
                        )
                    )
                }
            } else if (nextProgress <= 1.22f) {
                updatedCoins.add(coin.copy(progress = nextProgress))
            }
        }

        if (collectedCoinsCount > 0) {
            _hapticEvents.tryEmit(GameHapticEvent.CoinCollected)
        }

        spawnAccumulator += dt
        val spawnInterval = max(0.45f, 1.38f - (newSpeed - 6f) * 0.085f)
        if (spawnAccumulator >= spawnInterval) {
            spawnAccumulator -= spawnInterval
            spawnWave(newSpeed, updatedObstacles, updatedCoins)
        }

        val bonusTicks = collectedCoinsCount * 100f
        val totalScoreTicks = newScoreTicks + bonusTicks

        if (hitObstacle) {
            val finalScore = (totalScoreTicks / 5f).toInt()
            val updatedHigh = max(state.highScore, finalScore)
            val isNewRecord = finalScore > state.highScore && finalScore > 0
            _hapticEvents.tryEmit(GameHapticEvent.Crash)
            _gameState.value = state.copy(
                phase = GamePhase.GAME_OVER,
                scoreTicks = totalScoreTicks,
                runCoins = state.runCoins + collectedCoinsCount,
                highScore = updatedHigh,
                isNewRecord = isNewRecord,
                speed = newSpeed,
                visualLane = newVisualLane,
                laneTilt = newLaneTilt,
                isJumping = newIsJumping,
                jumpY = newJumpY,
                jumpVelocity = newJumpVelocity,
                roadScroll = newRoadScroll,
                runCycle = newRunCycle,
                dayCycle = newDayCycle,
                obstacles = updatedObstacles,
                coins = updatedCoins,
                scenery = updatedScenery,
                particles = updatedParticles,
                floatingEffects = newEffects,
                crashFlash = 1f
            )
            viewModelScope.launch {
                repository.saveHighScoreIfHigher(finalScore)
            }
        } else {
            _gameState.value = state.copy(
                scoreTicks = totalScoreTicks,
                runCoins = state.runCoins + collectedCoinsCount,
                speed = newSpeed,
                visualLane = newVisualLane,
                laneTilt = newLaneTilt,
                isJumping = newIsJumping,
                jumpY = newJumpY,
                jumpVelocity = newJumpVelocity,
                roadScroll = newRoadScroll,
                runCycle = newRunCycle,
                dayCycle = newDayCycle,
                obstacles = updatedObstacles,
                coins = updatedCoins,
                scenery = updatedScenery,
                particles = updatedParticles,
                floatingEffects = newEffects
            )
        }
    }

    private fun spawnWave(
        speed: Float,
        obstacles: MutableList<ObstacleEntity>,
        coins: MutableList<CoinEntity>
    ) {
        val roll = Random.nextFloat()
        if (roll < 0.62f) {
            val primaryLane = Random.nextInt(3)
            val kind = ObstacleKind.entries[Random.nextInt(ObstacleKind.entries.size)]
            obstacles.add(
                ObstacleEntity(
                    id = nextEntityId++,
                    lane = primaryLane,
                    progress = 0.04f,
                    kind = kind
                )
            )
            if (speed > 8.5f && Random.nextFloat() < 0.35f) {
                val otherLanes = (0..2).filter { it != primaryLane }
                val secondLane = otherLanes.random()
                val secondKind = ObstacleKind.entries[Random.nextInt(ObstacleKind.entries.size)]
                obstacles.add(
                    ObstacleEntity(
                        id = nextEntityId++,
                        lane = secondLane,
                        progress = 0.04f,
                        kind = secondKind
                    )
                )
                val safeLane = (0..2).first { it != primaryLane && it != secondLane }
                if (Random.nextFloat() < 0.55f) {
                    coins.add(CoinEntity(id = nextEntityId++, lane = safeLane, progress = 0.04f))
                }
            }
        } else {
            val coinLane = Random.nextInt(3)
            coins.add(CoinEntity(id = nextEntityId++, lane = coinLane, progress = 0.04f))
            if (Random.nextFloat() < 0.45f) {
                coins.add(CoinEntity(id = nextEntityId++, lane = coinLane, progress = 0.11f))
            }
        }
    }

    private fun createInitialScenery(): List<SceneryEntity> = listOf(
        SceneryEntity(1L, -1, 1.35f, 0.14f, SceneryKind.AKSUM_OBELISK),
        SceneryEntity(2L, 1, 1.40f, 0.22f, SceneryKind.ACACIA_TREE),
        SceneryEntity(3L, -1, 1.50f, 0.42f, SceneryKind.ETHIOPIAN_TUKUL),
        SceneryEntity(4L, 1, 1.32f, 0.58f, SceneryKind.AKSUM_OBELISK),
        SceneryEntity(5L, -1, 1.38f, 0.78f, SceneryKind.ACACIA_TREE),
        SceneryEntity(6L, 1, 1.48f, 0.88f, SceneryKind.HIGHLAND_ROCK)
    )

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TempleRunViewModel(GamePreferencesRepository(application)) as T
        }
    }
}
