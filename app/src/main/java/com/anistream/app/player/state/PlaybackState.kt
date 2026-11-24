package com.anistream.app.player.state

enum class TrackKind {
    AUDIO,
    SUBTITLE
}

data class TrackOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val kind: TrackKind
)

data class PlaybackEngineState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val ended: Boolean = false,
    val error: String? = null,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList()
)
