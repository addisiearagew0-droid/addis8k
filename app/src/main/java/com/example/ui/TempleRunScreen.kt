package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.game.GameHapticEvent
import com.example.game.GamePhase
import com.example.game.RunnerGameState
import com.example.game.TempleRunViewModel
import com.example.ui.theme.AzurePhotoBlue
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.JungleLush
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.TempleGold
import com.example.ui.theme.TempleStoneCard
import com.example.ui.theme.TempleStoneDark
import com.example.ui.theme.WarmParchment
import kotlin.math.abs

@Composable
fun TempleRunScreen(
    viewModel: TempleRunViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val avatarBitmap by viewModel.avatarBitmap.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()

    var showSupportDialog by rememberSaveable { mutableStateOf(false) }
    val defaultRunnerBitmap = ImageBitmap.imageResource(id = R.drawable.img_default_runner_face)
    val pose1 = ImageBitmap.imageResource(id = R.drawable.img_runner_pose_1)
    val pose2 = ImageBitmap.imageResource(id = R.drawable.img_runner_pose_2)
    val pose3 = ImageBitmap.imageResource(id = R.drawable.img_runner_pose_3)
    val jumpPose = ImageBitmap.imageResource(id = R.drawable.img_runner_jump)
    val highlandsBackdrop = ImageBitmap.imageResource(id = R.drawable.img_ethiopia_highlands)
    val runnerSpriteSet = remember(pose1, pose2, pose3, jumpPose, highlandsBackdrop) {
        RunnerSpriteSet(
            pose1 = pose1,
            pose2 = pose2,
            pose3 = pose3,
            jump = jumpPose,
            highlandsBackdrop = highlandsBackdrop
        )
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 24.dp.toPx() }
    val focusRequester = remember { FocusRequester() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.onPhotoPicked(it) }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { event ->
            if (!hapticsEnabled) return@collect
            when (event) {
                GameHapticEvent.LaneSwitch,
                GameHapticEvent.Jump,
                GameHapticEvent.CoinCollected -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                GameHapticEvent.Crash -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    LaunchedEffect(gameState.phase) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val deltaSeconds = (frameNanos - lastNanos) / 1_000_000_000f
                    viewModel.onFrame(deltaSeconds)
                }
                lastNanos = frameNanos
            }
        }
    }

    BackHandler(enabled = gameState.phase == GamePhase.PLAYING || gameState.phase == GamePhase.PAUSED) {
        when (gameState.phase) {
            GamePhase.PLAYING -> viewModel.pauseGame()
            GamePhase.PAUSED -> viewModel.resumeGame()
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianNight)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || gameState.phase != GamePhase.PLAYING) {
                    return@onPreviewKeyEvent false
                }
                when (keyEvent.key) {
                    Key.DirectionLeft, Key.A -> {
                        viewModel.moveLeft()
                        true
                    }
                    Key.DirectionRight, Key.D -> {
                        viewModel.moveRight()
                        true
                    }
                    Key.DirectionUp, Key.W, Key.Spacebar -> {
                        viewModel.jump()
                        true
                    }
                    Key.DirectionDown, Key.S -> {
                        viewModel.fastDrop()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(gameState.phase, swipeThresholdPx) {
                if (gameState.phase != GamePhase.PLAYING) return@pointerInput
                var totalDx = 0f
                var totalDy = 0f
                var gestureTriggered = false
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                        gestureTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (gestureTriggered) return@detectDragGestures
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y
                        if (abs(totalDx) > swipeThresholdPx || abs(totalDy) > swipeThresholdPx) {
                            gestureTriggered = true
                            if (abs(totalDx) > abs(totalDy)) {
                                if (totalDx > 0f) viewModel.moveRight() else viewModel.moveLeft()
                            } else {
                                if (totalDy < 0f) viewModel.jump() else viewModel.fastDrop()
                            }
                        }
                    }
                )
            }
    ) {
        TempleRunCanvas(
            gameState = gameState,
            avatarBitmap = avatarBitmap,
            spriteSet = runnerSpriteSet,
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
        )

        TopGameHud(
            gameState = gameState,
            hapticsEnabled = hapticsEnabled,
            onToggleHaptics = viewModel::toggleHaptics,
            onPauseClick = viewModel::pauseGame,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        AnimatedVisibility(
            visible = gameState.phase == GamePhase.PLAYING && gameState.displayScore < 30,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            SwipeControlsHintPill()
        }

        AnimatedVisibility(
            visible = gameState.phase != GamePhase.PLAYING,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
            modifier = Modifier.fillMaxSize()
        ) {
            OverlayMenuLayer(
                gameState = gameState,
                avatarBitmap = avatarBitmap,
                defaultRunnerBitmap = defaultRunnerBitmap,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearPhoto = viewModel::clearAvatar,
                onStartOrRestart = viewModel::startGame,
                onResume = viewModel::resumeGame,
                onOpenSupport = { showSupportDialog = true }
            )
        }

        if (showSupportDialog) {
            SupportCreatorDialog(
                onDismiss = { showSupportDialog = false }
            )
        }
    }
}

@Composable
private fun TopGameHud(
    gameState: RunnerGameState,
    hapticsEnabled: Boolean,
    onToggleHaptics: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = TempleStoneCard,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, TempleGold.copy(alpha = 0.65f)),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.score_format, gameState.displayScore),
                        style = MaterialTheme.typography.titleLarge,
                        color = TempleGold,
                        modifier = Modifier.testTag("score_text")
                    )
                    if (gameState.runCoins > 0 || gameState.phase == GamePhase.PLAYING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonetizationOn,
                                contentDescription = null,
                                tint = TempleGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.coins_format, gameState.runCoins),
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmParchment
                            )
                        }
                    }
                }
                if (gameState.highScore > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MutedSand,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.high_score_format, gameState.highScore),
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSand,
                            modifier = Modifier.testTag("high_score_text")
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onToggleHaptics,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = TempleStoneCard,
                    contentColor = if (hapticsEnabled) TempleGold else MutedSand.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("haptics_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = stringResource(R.string.cd_sound_toggle)
                )
            }

            if (gameState.phase == GamePhase.PLAYING) {
                FilledTonalIconButton(
                    onClick = onPauseClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = TempleStoneCard,
                        contentColor = WarmParchment
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(R.string.cd_pause_game)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeControlsHintPill() {
    Surface(
        color = TempleStoneCard,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, TempleGold.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_swipe_left),
                    tint = TempleGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.hint_swipe_left),
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmParchment
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_swipe_up),
                    tint = TempleGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.hint_swipe_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmParchment
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.hint_swipe_right),
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmParchment
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_swipe_right),
                    tint = TempleGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlayMenuLayer(
    gameState: RunnerGameState,
    avatarBitmap: ImageBitmap?,
    defaultRunnerBitmap: ImageBitmap,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onStartOrRestart: () -> Unit,
    onResume: () -> Unit,
    onOpenSupport: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (gameState.phase == GamePhase.MENU) {
            Image(
                painter = painterResource(id = R.drawable.img_ethiopia_highlands),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x99140E0B),
                                Color(0xC81A1412),
                                Color(0xDD140E0B)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB80E0A08))
            )
        }

        Card(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = TempleStoneCard),
            border = BorderStroke(1.5.dp, TempleGold.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Subtle Ethiopian flag accent ribbon at top of card
                Row(
                    modifier = Modifier
                        .width(84.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFF009A44)))
                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(TempleGold))
                    Box(modifier = Modifier.weight(1f).fillMaxSize().background(Color(0xFFDA121A)))
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (gameState.phase) {
                    GamePhase.MENU -> {
                        Text(
                            text = stringResource(R.string.game_title),
                            style = MaterialTheme.typography.displayLarge,
                            color = TempleGold,
                            textAlign = TextAlign.Center
                        )
                    }
                    GamePhase.GAME_OVER -> {
                        Text(
                            text = stringResource(R.string.game_over_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TempleGold,
                            textAlign = TextAlign.Center
                        )
                    }
                    GamePhase.PAUSED -> {
                        Text(
                            text = stringResource(R.string.game_paused_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TempleGold,
                            textAlign = TextAlign.Center
                        )
                    }
                    GamePhase.PLAYING -> Unit
                }

                // Developer Attribution Badge ("አፑን የሰራው: አዲሴ አራጋው")
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0x33FFD700),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, TempleGold.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = stringResource(R.string.developer_credit),
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmParchment,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("developer_credit_text")
                    )
                }

                if (gameState.phase == GamePhase.GAME_OVER && gameState.isNewRecord) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = TempleGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, TempleGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = TempleGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.new_record_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = TempleGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A2920))
                        .border(3.dp, TempleGold, CircleShape)
                        .testTag("preview_avatar_image"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = avatarBitmap ?: defaultRunnerBitmap,
                        contentDescription = stringResource(R.string.cd_player_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val subtitle = when (gameState.phase) {
                    GamePhase.GAME_OVER -> stringResource(R.string.final_score_format, gameState.displayScore)
                    GamePhase.PAUSED -> stringResource(R.string.score_format, gameState.displayScore)
                    else -> stringResource(R.string.game_subtitle_amharic)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmParchment,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPickPhoto,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AzurePhotoBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pick_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (avatarBitmap == null) {
                            stringResource(R.string.btn_choose_photo)
                        } else {
                            stringResource(R.string.btn_change_photo)
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (avatarBitmap != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onClearPhoto,
                        modifier = Modifier.testTag("clear_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MutedSand,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_clear_photo),
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedSand
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (gameState.phase == GamePhase.PAUSED) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlameOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("resume_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_resume_game),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onStartOrRestart,
                        border = BorderStroke(1.dp, TempleGold.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = TempleGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_play_again),
                            style = MaterialTheme.typography.labelLarge,
                            color = TempleGold
                        )
                    }
                } else {
                    Button(
                        onClick = onStartOrRestart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlameOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_button")
                    ) {
                        Icon(
                            imageVector = if (gameState.phase == GamePhase.GAME_OVER) {
                                Icons.Rounded.Refresh
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (gameState.phase == GamePhase.GAME_OVER) {
                                stringResource(R.string.btn_play_again)
                            } else {
                                stringResource(R.string.btn_start_game)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Support / Tip Addisie Aragaw Button
                OutlinedButton(
                    onClick = onOpenSupport,
                    border = BorderStroke(1.2.dp, TempleGold.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = JungleLush.copy(alpha = 0.32f),
                        contentColor = TempleGold
                    ),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("support_creator_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = TempleGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_support_creator),
                        style = MaterialTheme.typography.labelLarge,
                        color = TempleGold
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportCreatorDialog(
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val telebirrLabel = stringResource(R.string.support_telebirr_label)
    val telebirrNumber = stringResource(R.string.support_telebirr_number)
    val abyssiniaLabel = stringResource(R.string.support_abyssinia_label)
    val abyssiniaNumber = stringResource(R.string.support_abyssinia_number)
    val cbeLabel = stringResource(R.string.support_cbe_label)
    val cbeNumber = stringResource(R.string.support_cbe_number)
    val holderName = stringResource(R.string.support_account_holder)

    fun copyAccount(label: String, number: String) {
        clipboard.setText(AnnotatedString(number))
        Toast.makeText(
            context,
            context.getString(R.string.copied_feedback, label, number),
            Toast.LENGTH_SHORT
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TempleStoneDark,
        titleContentColor = TempleGold,
        textContentColor = WarmParchment,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = TempleGold
                )
                Text(
                    text = stringResource(R.string.support_sheet_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.support_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand
                )

                PaymentAccountCard(
                    icon = Icons.Rounded.PhoneAndroid,
                    bankLabel = telebirrLabel,
                    accountNumber = telebirrNumber,
                    holderName = holderName,
                    accentColor = Color(0xFF009A44),
                    onCopy = { copyAccount(telebirrLabel, telebirrNumber) },
                    testTag = "copy_telebirr_button"
                )

                PaymentAccountCard(
                    icon = Icons.Rounded.AccountBalance,
                    bankLabel = abyssiniaLabel,
                    accountNumber = abyssiniaNumber,
                    holderName = holderName,
                    accentColor = Color(0xFFE09F3E),
                    onCopy = { copyAccount(abyssiniaLabel, abyssiniaNumber) },
                    testTag = "copy_abyssinia_button"
                )

                PaymentAccountCard(
                    icon = Icons.Rounded.AccountBalance,
                    bankLabel = cbeLabel,
                    accountNumber = cbeNumber,
                    holderName = holderName,
                    accentColor = Color(0xFF9E5BD6),
                    onCopy = { copyAccount(cbeLabel, cbeNumber) },
                    testTag = "copy_cbe_button"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlameOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("close_support_dialog_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_close),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun PaymentAccountCard(
    icon: ImageVector,
    bankLabel: String,
    accountNumber: String,
    holderName: String,
    accentColor: Color,
    onCopy: () -> Unit,
    testTag: String
) {
    Surface(
        color = Color(0xFF1E1713),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = bankLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedSand
                    )
                    Text(
                        text = accountNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = TempleGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = holderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmParchment
                    )
                }
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(R.string.btn_copy_number),
                    tint = TempleGold
                )
            }
        }
    }
}
