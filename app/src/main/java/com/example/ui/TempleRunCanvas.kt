package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.game.CoinEntity
import com.example.game.DustParticle
import com.example.game.ObstacleEntity
import com.example.game.ObstacleKind
import com.example.game.RunnerGameState
import com.example.game.SceneryEntity
import com.example.game.SceneryKind
import com.example.ui.theme.CrimsonHazard
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TempleGold
import com.example.ui.theme.TempleGoldDeep
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class RunnerSpriteSet(
    val pose1: ImageBitmap,
    val pose2: ImageBitmap,
    val pose3: ImageBitmap,
    val jump: ImageBitmap,
    val highlandsBackdrop: ImageBitmap
)

private sealed interface DepthRenderable {
    val progress: Float

    data class Obstacle(val entity: ObstacleEntity) : DepthRenderable {
        override val progress: Float get() = entity.progress
    }

    data class Coin(val entity: CoinEntity) : DepthRenderable {
        override val progress: Float get() = entity.progress
    }

    data class Scenery(val entity: SceneryEntity) : DepthRenderable {
        override val progress: Float get() = entity.progress
    }
}

@Composable
fun TempleRunCanvas(
    gameState: RunnerGameState,
    avatarBitmap: ImageBitmap?,
    spriteSet: RunnerSpriteSet,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Authentic Temple Run low camera perspective
        val horizonY = height * 0.27f
        val playerBaseY = height * 0.84f
        val minDim = min(width, height)

        drawEthiopianEnvironment(
            width = width,
            height = height,
            horizonY = horizonY,
            roadScroll = gameState.roadScroll,
            dayCycle = gameState.dayCycle,
            highlandsBackdrop = spriteSet.highlandsBackdrop
        )

        val renderList = ArrayList<DepthRenderable>(
            gameState.obstacles.size + gameState.coins.size + gameState.scenery.size
        )
        for (item in gameState.scenery) renderList.add(DepthRenderable.Scenery(item))
        for (obs in gameState.obstacles) renderList.add(DepthRenderable.Obstacle(obs))
        for (coin in gameState.coins) renderList.add(DepthRenderable.Coin(coin))
        renderList.sortBy { it.progress }

        for (item in renderList) {
            when (item) {
                is DepthRenderable.Scenery -> drawScenery(
                    scenery = item.entity,
                    width = width,
                    height = height,
                    horizonY = horizonY
                )
                is DepthRenderable.Coin -> drawCoin(
                    coin = item.entity,
                    width = width,
                    height = height,
                    horizonY = horizonY,
                    runCycle = gameState.runCycle
                )
                is DepthRenderable.Obstacle -> drawObstacle(
                    obstacle = item.entity,
                    width = width,
                    height = height,
                    horizonY = horizonY
                )
            }
        }

        // Horizon mist veil so distant objects emerge softly from Simien mountain fog
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x55FFF1D6), Color.Transparent),
                startY = horizonY,
                endY = horizonY + (height - horizonY) * 0.14f
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(width, (height - horizonY) * 0.14f)
        )

        drawParticles(
            particles = gameState.particles,
            width = width,
            height = height,
            playerBaseY = playerBaseY
        )

        drawRealisticRunner(
            visualLane = gameState.visualLane,
            laneTilt = gameState.laneTilt,
            jumpY = gameState.jumpY,
            isJumping = gameState.isJumping,
            runCycle = gameState.runCycle,
            spriteSet = spriteSet,
            customFaceBitmap = avatarBitmap,
            width = width,
            height = height,
            playerBaseY = playerBaseY
        )

        val floatingTextSp = with(density) { (minDim * 0.068f).toSp() }
        val floatingStyle = TextStyle(
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = floatingTextSp,
            color = TempleGold
        )
        drawFloatingEffects(
            gameState = gameState,
            textMeasurer = textMeasurer,
            style = floatingStyle,
            width = width,
            height = height,
            playerBaseY = playerBaseY
        )

        if (gameState.crashFlash > 0f) {
            drawRect(
                color = CrimsonHazard.copy(alpha = gameState.crashFlash * 0.38f),
                size = size
            )
        }
    }
}

private fun getLaneX(lane: Float, progress: Float, width: Float): Float {
    val centerX = width / 2f
    val spread = (width * 0.34f) * progress
    return centerX + (lane - 1f) * spread
}

private fun getSceneryX(side: Int, lateralFactor: Float, progress: Float, width: Float): Float {
    val centerX = width / 2f
    val roadHalfTop = width * 0.11f
    val roadHalfBottom = width * 0.54f
    val roadHalfAtProgress = roadHalfTop + (roadHalfBottom - roadHalfTop) * progress
    val offset = roadHalfAtProgress * lateralFactor
    return centerX + side * offset
}

private fun DrawScope.drawEthiopianEnvironment(
    width: Float,
    height: Float,
    horizonY: Float,
    roadScroll: Float,
    dayCycle: Float,
    highlandsBackdrop: ImageBitmap
) {
    // 1. Photorealistic painted Ethiopian Simien Mountains & golden sky across the upper horizon
    val srcCropHeight = (highlandsBackdrop.height * 0.56f).toInt().coerceAtLeast(1)
    drawImage(
        image = highlandsBackdrop,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(highlandsBackdrop.width, srcCropHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width.toInt(), (horizonY + height * 0.04f).toInt())
    )

    // Dynamic atmospheric lighting tint across dayCycle (Morning -> Midday -> Sunset -> Night)
    val phase = dayCycle * 4f
    val atmosphereTint: Color
    when {
        phase < 1f -> atmosphereTint = lerp(Color(0x28FFB347), Color(0x08000000), phase)
        phase < 2f -> atmosphereTint = lerp(Color(0x08000000), Color(0x40FF6B35), phase - 1f)
        phase < 3f -> atmosphereTint = lerp(Color(0x40FF6B35), Color(0x880B132B), phase - 2f)
        else -> atmosphereTint = lerp(Color(0x880B132B), Color(0x28FFB347), phase - 3f)
    }
    drawRect(
        color = atmosphereTint,
        size = Size(width, horizonY + height * 0.04f)
    )

    val nightAmount = when {
        phase in 2f..3f -> (phase - 2f) * 0.85f
        phase > 3f -> (1f - (phase - 3f)) * 0.85f
        else -> 0f
    }

    // 2. Lush textured Ethiopian highland valleys flanking the stone causeway
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0xFF3E6E3B), Color(0xFF172D1C), nightAmount),
                lerp(Color(0xFF234723), Color(0xFF0E1D12), nightAmount),
                lerp(Color(0xFF132915), Color(0xFF071009), nightAmount)
            ),
            startY = horizonY,
            endY = height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, height - horizonY)
    )

    // 3. Perspective Temple Stone & Red-Earth Causeway
    val centerX = width / 2f
    val topHalfWidth = width * 0.11f
    val bottomHalfWidth = width * 0.54f

    val roadPath = Path().apply {
        moveTo(centerX - topHalfWidth, horizonY)
        lineTo(centerX + topHalfWidth, horizonY)
        lineTo(centerX + bottomHalfWidth, height)
        lineTo(centerX - bottomHalfWidth, height)
        close()
    }
    drawPath(
        path = roadPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                lerp(Color(0xFF6B4229), Color(0xFF382215), nightAmount),
                lerp(Color(0xFF835234), Color(0xFF452919), nightAmount),
                lerp(Color(0xFF5C3721), Color(0xFF2B180E), nightAmount)
            ),
            startY = horizonY,
            endY = height
        )
    )

    // Perspective stone masonry courses & alternating shaded stone tiles
    val numRows = 11
    for (i in 0 until numRows) {
        val rawT0 = (i.toFloat() / numRows + roadScroll / numRows) % 1f
        val rawT1 = ((i + 0.88f) / numRows + roadScroll / numRows) % 1f
        if (rawT1 <= rawT0) continue

        val t0 = rawT0 * rawT0
        val t1 = rawT1 * rawT1
        val y0 = horizonY + (height - horizonY) * t0
        val y1 = horizonY + (height - horizonY) * t1
        val hw0 = topHalfWidth + (bottomHalfWidth - topHalfWidth) * t0
        val hw1 = topHalfWidth + (bottomHalfWidth - topHalfWidth) * t1

        // Draw 3 lane stone pavers with subtle bevel shading
        for (col in 0..2) {
            val fLeft = -1f + col * (2f / 3f) + 0.03f
            val fRight = -1f + (col + 1) * (2f / 3f) - 0.03f
            val tilePath = Path().apply {
                moveTo(centerX + fLeft * hw0, y0)
                lineTo(centerX + fRight * hw0, y0)
                lineTo(centerX + fRight * hw1, y1)
                lineTo(centerX + fLeft * hw1, y1)
                close()
            }
            val tileAlpha = 0.14f + 0.16f * t1
            val tileColor = if ((i + col) % 2 == 0) {
                Color(0xFFD6B999).copy(alpha = tileAlpha * 0.7f)
            } else {
                Color(0xFF2B170C).copy(alpha = tileAlpha)
            }
            drawPath(path = tilePath, color = tileColor)
        }

        // Mortar seam line across causeway
        drawLine(
            color = Color(0x3D1A0D06),
            start = Offset(centerX - hw1, y1),
            end = Offset(centerX + hw1, y1),
            strokeWidth = width * (0.002f + t1 * 0.005f)
        )
    }

    // Heavy 3D carved Lalibela stone parapets along left and right edges
    val wallTopW = width * 0.025f
    val wallBottomW = width * 0.082f
    val curbLeftPath = Path().apply {
        moveTo(centerX - topHalfWidth - wallTopW, horizonY)
        lineTo(centerX - topHalfWidth + wallTopW * 0.35f, horizonY)
        lineTo(centerX - bottomHalfWidth + wallBottomW * 0.35f, height)
        lineTo(centerX - bottomHalfWidth - wallBottomW, height)
        close()
    }
    val curbRightPath = Path().apply {
        moveTo(centerX + topHalfWidth - wallTopW * 0.35f, horizonY)
        lineTo(centerX + topHalfWidth + wallTopW, horizonY)
        lineTo(centerX + bottomHalfWidth + wallBottomW, height)
        lineTo(centerX + bottomHalfWidth - wallBottomW * 0.35f, height)
        close()
    }
    val wallHighlight = lerp(Color(0xFFC4A484), Color(0xFF665544), nightAmount)
    val wallShadow = lerp(Color(0xFF6E553D), Color(0xFF332920), nightAmount)
    drawPath(
        curbLeftPath,
        brush = Brush.horizontalGradient(
            colors = listOf(wallShadow, wallHighlight),
            startX = centerX - bottomHalfWidth - wallBottomW,
            endX = centerX - bottomHalfWidth + wallBottomW
        )
    )
    drawPath(
        curbRightPath,
        brush = Brush.horizontalGradient(
            colors = listOf(wallHighlight, wallShadow),
            startX = centerX + bottomHalfWidth - wallBottomW,
            endX = centerX + bottomHalfWidth + wallBottomW
        )
    )

    // Subtle 3-lane track seams
    for (laneSep in listOf(-0.5f, 0.5f)) {
        val topX = centerX + laneSep * topHalfWidth * 1.15f
        val bottomX = centerX + laneSep * (width * 0.68f)
        drawLine(
            color = Color(0x40F6EAD7),
            start = Offset(topX, horizonY),
            end = Offset(bottomX, height),
            strokeWidth = width * 0.0042f
        )
    }
}

private fun DrawScope.drawScenery(
    scenery: SceneryEntity,
    width: Float,
    height: Float,
    horizonY: Float
) {
    val p = scenery.progress.coerceAtLeast(0.02f)
    val x = getSceneryX(scenery.side, scenery.lateralFactor, p, width)
    val baseY = horizonY + (height - horizonY) * p
    val scale = (min(width, height) / 230f) * p

    when (scenery.kind) {
        SceneryKind.ACACIA_TREE -> {
            val trunkH = 95f * scale
            val trunkW = 14f * scale
            drawOval(
                color = Color.Black.copy(alpha = 0.28f * p),
                topLeft = Offset(x - 44f * scale, baseY - 6f * scale),
                size = Size(88f * scale, 18f * scale)
            )
            drawLine(
                color = Color(0xFF4A3121),
                start = Offset(x, baseY),
                end = Offset(x, baseY - trunkH * 0.72f),
                strokeWidth = trunkW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF4A3121),
                start = Offset(x, baseY - trunkH * 0.55f),
                end = Offset(x - 28f * scale, baseY - trunkH * 0.85f),
                strokeWidth = trunkW * 0.65f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF4A3121),
                start = Offset(x, baseY - trunkH * 0.62f),
                end = Offset(x + 26f * scale, baseY - trunkH * 0.88f),
                strokeWidth = trunkW * 0.65f,
                cap = StrokeCap.Round
            )
            drawOval(
                color = Color(0xFF1F4E29),
                topLeft = Offset(x - 62f * scale, baseY - trunkH - 12f * scale),
                size = Size(124f * scale, 32f * scale)
            )
            drawOval(
                color = Color(0xFF377A42),
                topLeft = Offset(x - 48f * scale, baseY - trunkH - 22f * scale),
                size = Size(96f * scale, 26f * scale)
            )
        }
        SceneryKind.AKSUM_OBELISK -> {
            val obH = 132f * scale
            val obW = 30f * scale
            drawOval(
                color = Color.Black.copy(alpha = 0.28f * p),
                topLeft = Offset(x - obW * 0.9f, baseY - 5f * scale),
                size = Size(obW * 1.8f, 14f * scale)
            )
            val obeliskPath = Path().apply {
                moveTo(x - obW * 0.5f, baseY)
                lineTo(x - obW * 0.36f, baseY - obH * 0.86f)
                lineTo(x, baseY - obH)
                lineTo(x + obW * 0.36f, baseY - obH * 0.86f)
                lineTo(x + obW * 0.5f, baseY)
                close()
            }
            drawPath(
                path = obeliskPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFA89683), Color(0xFF6B5D50)),
                    startX = x - obW * 0.5f,
                    endX = x + obW * 0.5f
                )
            )
            for (tier in listOf(0.22f, 0.44f, 0.66f)) {
                drawRect(
                    color = Color(0xFF473E35),
                    topLeft = Offset(x - obW * 0.22f, baseY - obH * tier),
                    size = Size(obW * 0.44f, 6f * scale)
                )
            }
        }
        SceneryKind.ETHIOPIAN_TUKUL -> {
            val wallW = 70f * scale
            val wallH = 40f * scale
            val roofH = 44f * scale
            drawOval(
                color = Color.Black.copy(alpha = 0.28f * p),
                topLeft = Offset(x - wallW * 0.65f, baseY - 6f * scale),
                size = Size(wallW * 1.3f, 16f * scale)
            )
            drawRoundRect(
                color = Color(0xFFD8C3A5),
                topLeft = Offset(x - wallW / 2f, baseY - wallH),
                size = Size(wallW, wallH),
                cornerRadius = CornerRadius(4f * scale, 4f * scale)
            )
            drawRoundRect(
                color = Color(0xFF3E2723),
                topLeft = Offset(x - 8f * scale, baseY - wallH * 0.68f),
                size = Size(16f * scale, wallH * 0.68f),
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            val roofPath = Path().apply {
                moveTo(x - wallW * 0.65f, baseY - wallH + 4f * scale)
                lineTo(x, baseY - wallH - roofH)
                lineTo(x + wallW * 0.65f, baseY - wallH + 4f * scale)
                close()
            }
            drawPath(
                path = roofPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFC29B61), Color(0xFF8C6B3B)),
                    startY = baseY - wallH - roofH,
                    endY = baseY - wallH
                )
            )
        }
        SceneryKind.HIGHLAND_ROCK -> {
            val rW = 48f * scale
            val rH = 30f * scale
            val rockPath = Path().apply {
                moveTo(x - rW * 0.55f, baseY)
                lineTo(x - rW * 0.32f, baseY - rH * 0.85f)
                lineTo(x + rW * 0.12f, baseY - rH)
                lineTo(x + rW * 0.52f, baseY - rH * 0.35f)
                lineTo(x + rW * 0.48f, baseY)
                close()
            }
            drawPath(path = rockPath, color = Color(0xFF5A5652))
        }
    }
}

private fun DrawScope.drawCoin(
    coin: CoinEntity,
    width: Float,
    height: Float,
    horizonY: Float,
    runCycle: Float
) {
    val p = coin.progress.coerceAtLeast(0.02f)
    val x = getLaneX(coin.lane.toFloat(), p, width)
    val y = horizonY + (height - horizonY) * p - (height * 0.026f * p)
    val radius = max(5f, (min(width, height) * 0.068f) * p)
    val spinWidthFactor = 0.35f + 0.65f * abs(cos(runCycle + coin.id * 0.8f))

    drawOval(
        color = Color.Black.copy(alpha = 0.32f * p),
        topLeft = Offset(x - radius * 0.85f, y + radius * 0.95f),
        size = Size(radius * 1.7f, radius * 0.45f)
    )

    val coinW = radius * 2f * spinWidthFactor
    val coinH = radius * 2f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF5AD), TempleGold, TempleGoldDeep),
            center = Offset(x - coinW * 0.15f, y - coinH * 0.15f),
            radius = radius * 1.4f
        ),
        topLeft = Offset(x - coinW / 2f, y - coinH / 2f),
        size = Size(coinW, coinH)
    )
    drawOval(
        color = TempleGoldDeep,
        topLeft = Offset(x - coinW / 2f, y - coinH / 2f),
        size = Size(coinW, coinH),
        style = Stroke(width = max(2f, radius * 0.14f))
    )
    if (coinW > 12f) {
        drawLine(
            color = Color(0xFFFFFCE0),
            start = Offset(x, y - coinH * 0.25f),
            end = Offset(x, y + coinH * 0.25f),
            strokeWidth = max(2f, radius * 0.12f)
        )
        drawLine(
            color = Color(0xFFFFFCE0),
            start = Offset(x - coinW * 0.25f, y),
            end = Offset(x + coinW * 0.25f, y),
            strokeWidth = max(2f, radius * 0.12f)
        )
    }
}

private fun DrawScope.drawObstacle(
    obstacle: ObstacleEntity,
    width: Float,
    height: Float,
    horizonY: Float
) {
    val p = obstacle.progress.coerceAtLeast(0.02f)
    val x = getLaneX(obstacle.lane.toFloat(), p, width)
    val baseY = horizonY + (height - horizonY) * p
    val objSize = max(12f, (min(width, height) * 0.25f) * p)
    val objHeight = objSize * 0.78f
    val strokeW = max(2f, objSize * 0.032f)

    drawOval(
        color = Color.Black.copy(alpha = 0.42f * p),
        topLeft = Offset(x - objSize * 0.58f, baseY - objHeight * 0.10f),
        size = Size(objSize * 1.16f, objHeight * 0.36f)
    )

    val corner = CornerRadius(objSize * 0.08f, objSize * 0.08f)

    when (obstacle.kind) {
        ObstacleKind.BASALT_BOULDER -> {
            val boulderPath = Path().apply {
                moveTo(x - objSize * 0.50f, baseY)
                lineTo(x - objSize * 0.44f, baseY - objHeight * 0.65f)
                lineTo(x - objSize * 0.12f, baseY - objHeight * 1.02f)
                lineTo(x + objSize * 0.34f, baseY - objHeight * 0.88f)
                lineTo(x + objSize * 0.52f, baseY - objHeight * 0.35f)
                lineTo(x + objSize * 0.46f, baseY)
                close()
            }
            drawPath(
                path = boulderPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6B6E78), Color(0xFF3B3D47), Color(0xFF23252C)),
                    startY = baseY - objHeight,
                    endY = baseY
                )
            )
            val topFacet = Path().apply {
                moveTo(x - objSize * 0.44f, baseY - objHeight * 0.65f)
                lineTo(x - objSize * 0.12f, baseY - objHeight * 1.02f)
                lineTo(x + objSize * 0.34f, baseY - objHeight * 0.88f)
                lineTo(x + objSize * 0.05f, baseY - objHeight * 0.45f)
                close()
            }
            drawPath(path = topFacet, color = Color(0xFF8D919E).copy(alpha = 0.45f))
            drawPath(
                path = boulderPath,
                color = CrimsonHazard.copy(alpha = 0.88f),
                style = Stroke(width = strokeW)
            )
        }
        ObstacleKind.ACACIA_LOG -> {
            val logLeft = x - objSize * 0.56f
            val logTop = baseY - objHeight * 0.82f
            val logW = objSize * 1.12f
            val logH = objHeight * 0.82f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF7F4F24), Color(0xFF582F0E), Color(0xFF3B1F09)),
                    startY = logTop,
                    endY = baseY
                ),
                topLeft = Offset(logLeft, logTop),
                size = Size(logW, logH),
                cornerRadius = CornerRadius(logH * 0.45f, logH * 0.45f)
            )
            drawOval(
                color = Color(0xFFDDB892),
                topLeft = Offset(logLeft + logW - logH * 0.72f, logTop + logH * 0.10f),
                size = Size(logH * 0.64f, logH * 0.80f)
            )
            drawRoundRect(
                color = CrimsonHazard,
                topLeft = Offset(logLeft, logTop),
                size = Size(logW, logH),
                cornerRadius = CornerRadius(logH * 0.45f, logH * 0.45f),
                style = Stroke(width = strokeW)
            )
        }
        ObstacleKind.AKSUM_PILLAR -> {
            val pilW = objSize * 0.86f
            val pilH = objHeight * 1.08f
            val pilLeft = x - pilW / 2f
            val pilTop = baseY - pilH
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF7D5A44), Color(0xFF52392A)),
                    startX = pilLeft,
                    endX = pilLeft + pilW
                ),
                topLeft = Offset(pilLeft, pilTop),
                size = Size(pilW, pilH),
                cornerRadius = corner
            )
            drawRect(
                color = TempleGold.copy(alpha = 0.90f),
                topLeft = Offset(pilLeft + pilW * 0.14f, pilTop + pilH * 0.30f),
                size = Size(pilW * 0.72f, pilH * 0.14f)
            )
            drawRect(
                color = TempleGold.copy(alpha = 0.90f),
                topLeft = Offset(x - pilW * 0.08f, pilTop + pilH * 0.16f),
                size = Size(pilW * 0.16f, pilH * 0.44f)
            )
            drawRoundRect(
                color = CrimsonHazard,
                topLeft = Offset(pilLeft, pilTop),
                size = Size(pilW, pilH),
                cornerRadius = corner,
                style = Stroke(width = strokeW)
            )
        }
    }
}

private fun DrawScope.drawParticles(
    particles: List<DustParticle>,
    width: Float,
    height: Float,
    playerBaseY: Float
) {
    val scale = min(width, height) / 230f
    val jumpUnitToPx = height * 0.00145f
    for (p in particles) {
        val alpha = (1f - p.age / p.maxAge).coerceIn(0f, 1f)
        val x = getLaneX(p.lane, 0.92f, width)
        val y = playerBaseY + p.yOffset * jumpUnitToPx
        val color = if (p.isGold) {
            TempleGold.copy(alpha = alpha)
        } else {
            Color(0xFFB57C56).copy(alpha = alpha * 0.65f)
        }
        drawCircle(
            color = color,
            radius = p.size * scale * 0.55f * (0.6f + 0.5f * (p.age / p.maxAge)),
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawRealisticRunner(
    visualLane: Float,
    laneTilt: Float,
    jumpY: Float,
    isJumping: Boolean,
    runCycle: Float,
    spriteSet: RunnerSpriteSet,
    customFaceBitmap: ImageBitmap?,
    width: Float,
    height: Float,
    playerBaseY: Float
) {
    val minDim = min(width, height)
    val x = getLaneX(visualLane, 0.92f, width)
    val jumpPx = jumpY * (height * 0.0015f)
    val strideBob = if (isJumping) 0f else abs(sin(runCycle)) * (height * 0.012f)

    // Choose realistic 3D-rendered full-body frame based on jump/stride cycle
    val activeSprite: ImageBitmap = if (isJumping) {
        spriteSet.jump
    } else {
        val normCycle = ((runCycle % (2f * PI.toFloat())) + 2f * PI.toFloat()) % (2f * PI.toFloat())
        val frameIndex = ((normCycle / (2f * PI.toFloat())) * 4f).toInt().coerceIn(0, 3)
        when (frameIndex) {
            0 -> spriteSet.pose1
            1 -> spriteSet.pose2
            2 -> spriteSet.pose3
            else -> spriteSet.pose2
        }
    }

    // Heroic Temple Run character height (~33% of screen height!)
    val spriteHeight = min(height * 0.34f, minDim * 0.72f)
    val spriteWidth = spriteHeight * (activeSprite.width.toFloat() / activeSprite.height.toFloat())
    val feetY = playerBaseY + height * 0.055f - jumpPx - strideBob

    // Soft ground shadow directly beneath the runner's feet
    val shadowScale = (1f - (jumpY / 220f)).coerceIn(0.35f, 1f)
    val shadowW = spriteWidth * 0.58f * shadowScale
    val shadowH = spriteHeight * 0.085f * shadowScale
    val groundShadowY = playerBaseY + height * 0.055f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.52f * shadowScale), Color.Transparent),
            center = Offset(x, groundShadowY),
            radius = max(1f, shadowW * 0.6f)
        ),
        topLeft = Offset(x - shadowW / 2f, groundShadowY - shadowH / 2f),
        size = Size(shadowW, shadowH)
    )

    val dstTop = (feetY - spriteHeight).toInt()
    val dstLeft = (x - spriteWidth / 2f).toInt()

    rotate(degrees = laneTilt * 8f, pivot = Offset(x, feetY - spriteHeight * 0.35f)) {
        drawImage(
            image = activeSprite,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(activeSprite.width, activeSprite.height),
            dstOffset = IntOffset(dstLeft, dstTop),
            dstSize = IntSize(spriteWidth.toInt().coerceAtLeast(1), spriteHeight.toInt().coerceAtLeast(1))
        )

        // If user selected their own personal photo, blend it smoothly as a crisp runner head portrait
        if (customFaceBitmap != null) {
            val headCenterX = x - spriteWidth * 0.06f
            val headCenterY = feetY - spriteHeight * 0.875f
            val headR = spriteHeight * 0.095f
            val headRect = Rect(
                left = headCenterX - headR,
                top = headCenterY - headR,
                right = headCenterX + headR,
                bottom = headCenterY + headR
            )
            val clip = Path().apply { addOval(headRect) }
            clipPath(clip) {
                drawImage(
                    image = customFaceBitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(customFaceBitmap.width, customFaceBitmap.height),
                    dstOffset = IntOffset(headRect.left.toInt(), headRect.top.toInt()),
                    dstSize = IntSize((headR * 2f).toInt(), (headR * 2f).toInt())
                )
            }
            drawOval(
                color = TempleGold,
                topLeft = headRect.topLeft,
                size = headRect.size,
                style = Stroke(width = max(2f, headR * 0.12f))
            )
        }
    }
}

private fun DrawScope.drawFloatingEffects(
    gameState: RunnerGameState,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    width: Float,
    height: Float,
    playerBaseY: Float
) {
    for (effect in gameState.floatingEffects) {
        val alpha = (1f - effect.age / 0.75f).coerceIn(0f, 1f)
        val x = getLaneX(effect.lane, 0.92f, width) - width * 0.045f
        val y = playerBaseY - height * 0.24f - effect.age * (height * 0.12f)
        val measured = textMeasurer.measure(
            text = effect.text,
            style = style.copy(color = TempleGold.copy(alpha = alpha))
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(x, y)
        )
    }
}
