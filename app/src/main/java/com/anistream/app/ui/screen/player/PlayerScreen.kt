package com.anistream.app.ui.screen.player

import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.anistream.app.player.state.TrackOption
import com.anistream.app.ui.common.episodeLabel
import com.anistream.app.ui.common.findActivity
import com.anistream.app.ui.common.formatClock
import com.anistream.app.ui.component.ErrorState

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isLandscape by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLandscape) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val toggleFullscreen: () -> Unit = {
        val next = !isLandscape
        isLandscape = next
        activity?.requestedOrientation = if (next) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    when {
        state.loading -> PlayerLoading(
            title = state.animeTitle,
            onBack = onBack
        )

        state.error != null -> PlayerError(
            title = state.animeTitle,
            message = state.error.orEmpty(),
            onBack = onBack,
            onRetry = viewModel::retryPlayback
        )

        !state.sourceAvailable -> UnavailableSourceScreen(
            state = state,
            onBack = onBack,
            onRetry = viewModel::retryPlayback,
            onPrevious = viewModel::previousEpisode,
            onNext = viewModel::nextEpisode
        )

        else -> PlaybackSurface(
            state = state,
            viewModel = viewModel,
            isLandscape = isLandscape,
            onToggleFullscreen = toggleFullscreen,
            onBack = onBack
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlaybackSurface(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    isLandscape: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = viewModel.player
                }
            },
            update = { view ->
                view.player = viewModel.player
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            PlayerTopBar(
                title = state.animeTitle,
                subtitle = state.episode?.let { episode ->
                    "${episodeLabel(episode.number)} · ${episode.displayTitle}"
                },
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onToggleFullscreen) {
                        Icon(
                            imageVector = if (isLandscape) {
                                Icons.Filled.FullscreenExit
                            } else {
                                Icons.Filled.Fullscreen
                            },
                            contentDescription = "Toggle fullscreen",
                            tint = Color.White
                        )
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerControls(
                state = state,
                onSeekFraction = viewModel::seekToFraction,
                onTogglePlay = viewModel::togglePlayPause,
                onPrevious = viewModel::previousEpisode,
                onNext = viewModel::nextEpisode,
                onAudio = { showAudioDialog = true },
                onSubtitles = { showSubtitleDialog = true },
                onQuality = { showQualityDialog = true },
                onFullscreen = onToggleFullscreen,
                isLandscape = isLandscape
            )
        }

        if (state.isBuffering && state.playbackError == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
            )
        }

        if (state.showResumeNotice) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 74.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                ) {
                    Text(
                        text = "Resumed from ${formatClock(state.resumedFromMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = viewModel::dismissResumeNotice,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        state.playbackError?.let { message ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Playback error", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = viewModel::retryPlayback) { Text("Retry") }
                }
            }
        }

        if (state.episodeSwitchLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Loading episode…",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showAudioDialog) {
        TrackDialog(
            title = "Audio track",
            options = state.audioTracks,
            emptyText = "This source does not report alternative audio tracks.",
            onSelect = {
                viewModel.selectAudioTrack(it)
                showAudioDialog = false
            },
            onDismiss = { showAudioDialog = false }
        )
    }

    if (showSubtitleDialog) {
        TrackDialog(
            title = "Subtitles",
            options = state.subtitleTracks,
            emptyText = "This source does not report subtitle tracks.",
            onSelect = {
                viewModel.selectSubtitleTrack(it)
                showSubtitleDialog = false
            },
            onOff = {
                viewModel.disableSubtitles()
                showSubtitleDialog = false
            },
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Quality") },
            text = {
                Column {
                    if (state.availableQualities.isEmpty()) {
                        Text(
                            text = "This source reports a single quality.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.availableQualities.forEach { quality ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectQuality(quality)
                                        showQualityDialog = false
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.selectedQuality == quality,
                                    onClick = {
                                        viewModel.selectQuality(quality)
                                        showQualityDialog = false
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(quality)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    onSeekFraction: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    onQuality: () -> Unit,
    onFullscreen: () -> Unit,
    isLandscape: Boolean
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val duration = state.durationMs.coerceAtLeast(0L)
    val fraction = if (duration > 0L) {
        (state.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(state.positionMs, dragging) {
        if (!dragging) dragValue = fraction
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Slider(
            value = dragValue,
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeekFraction(dragValue)
                dragging = false
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${formatClock(if (dragging) (dragValue * duration).toLong() else state.positionMs)} / ${formatClock(duration)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            TextButton(onClick = onQuality) {
                Text(
                    text = state.selectedQuality ?: "Quality",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous episode",
                    tint = if (state.hasPrevious) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = onNext, enabled = state.hasNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next episode",
                    tint = if (state.hasNext) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAudio) {
                Icon(
                    imageVector = Icons.Filled.Audiotrack,
                    contentDescription = "Audio track",
                    tint = Color.White
                )
            }
            IconButton(onClick = onSubtitles) {
                Icon(
                    imageVector = Icons.Filled.Subtitles,
                    contentDescription = "Subtitles",
                    tint = Color.White
                )
            }
            IconButton(onClick = onFullscreen) {
                Icon(
                    imageVector = if (isLandscape) {
                        Icons.Filled.FullscreenExit
                    } else {
                        Icons.Filled.Fullscreen
                    },
                    contentDescription = "Toggle fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Player" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
private fun PlayerLoading(title: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PlayerTopBar(title = title, subtitle = null, onBack = onBack)
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Preparing episode…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlayerError(
    title: String,
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PlayerTopBar(title = title, subtitle = null, onBack = onBack)
        ErrorState(message = message, onRetry = onRetry)
    }
}

@Composable
private fun UnavailableSourceScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PlayerTopBar(
            title = state.animeTitle,
            subtitle = state.episode?.let { "${episodeLabel(it.number)} · ${it.displayTitle}" },
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LinkOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No native source available",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.unavailableReason
                    ?: "This episode does not expose a direct source the native player can use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            val qualities = state.episodeDetail?.qualities.orEmpty()
            if (qualities.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Qualities reported by the catalogue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = qualities.joinToString(" · ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            val providerNames = state.providers.map { it.name }.distinct()
            if (providerNames.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Providers: ${providerNames.take(8).joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "AniStream only plays direct HLS, DASH or progressive sources that " +
                        "Media3 can handle. These providers are third-party embed players, so " +
                        "playing them inside the app would require extracting or bypassing them, " +
                        "which AniStream does not do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry) { Text("Check again") }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPrevious, enabled = state.hasPrevious) {
                    Text("Previous episode")
                }
                TextButton(onClick = onNext, enabled = state.hasNext) {
                    Text("Next episode")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TrackDialog(
    title: String,
    options: List<TrackOption>,
    emptyText: String,
    onSelect: (TrackOption) -> Unit,
    onDismiss: () -> Unit,
    onOff: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (options.isEmpty() && onOff == null) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onOff != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOff)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = options.none { it.selected },
                            onClick = onOff
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Off")
                    }
                }
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.selected,
                            onClick = { onSelect(option) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
