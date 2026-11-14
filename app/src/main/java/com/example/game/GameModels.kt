package com.example.game

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class ObstacleKind {
    BASALT_BOULDER,
    ACACIA_LOG,
    AKSUM_PILLAR
}

enum class SceneryKind {
    ACACIA_TREE,
    AKSUM_OBELISK,
    ETHIOPIAN_TUKUL,
    HIGHLAND_ROCK
}

data class ObstacleEntity(
    val id: Long,
    val lane: Int,
    val progress: Float,
    val kind: ObstacleKind
)

data class CoinEntity(
    val id: Long,
    val lane: Int,
    val progress: Float
)

data class SceneryEntity(
    val id: Long,
    val side: Int, // -1 = left, +1 = right
    val lateralFactor: Float,
    val progress: Float,
    val kind: SceneryKind
)

data class DustParticle(
    val id: Long,
    val lane: Float,
    val yOffset: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val isGold: Boolean,
    val age: Float,
    val maxAge: Float
)

data class FloatingEffect(
    val id: Long,
    val text: String,
    val lane: Float,
    val age: Float
)

data class RunnerGameState(
    val phase: GamePhase = GamePhase.MENU,
    val scoreTicks: Float = 0f,
    val runCoins: Int = 0,
    val highScore: Int = 0,
    val isNewRecord: Boolean = false,
    val speed: Float = 6f,
    val targetLane: Int = 1,
    val visualLane: Float = 1f,
    val laneTilt: Float = 0f,
    val isJumping: Boolean = false,
    val jumpY: Float = 0f,
    val jumpVelocity: Float = 0f,
    val roadScroll: Float = 0f,
    val runCycle: Float = 0f,
    val dayCycle: Float = 0f,
    val obstacles: List<ObstacleEntity> = emptyList(),
    val coins: List<CoinEntity> = emptyList(),
    val scenery: List<SceneryEntity> = emptyList(),
    val particles: List<DustParticle> = emptyList(),
    val floatingEffects: List<FloatingEffect> = emptyList(),
    val crashFlash: Float = 0f
) {
    val displayScore: Int
        get() = (scoreTicks / 5f).toInt()
}

sealed interface GameHapticEvent {
    data object LaneSwitch : GameHapticEvent
    data object Jump : GameHapticEvent
    data object CoinCollected : GameHapticEvent
    data object Crash : GameHapticEvent
}
