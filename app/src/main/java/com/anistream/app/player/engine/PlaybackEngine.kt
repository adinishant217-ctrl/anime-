package com.anistream.app.player.engine

import androidx.media3.common.Player
import com.anistream.app.player.state.PlaybackEngineState
import com.anistream.app.player.state.TrackKind
import com.anistream.app.player.state.TrackOption
import kotlinx.coroutines.flow.StateFlow

interface PlaybackEngine {
    val state: StateFlow<PlaybackEngineState>

    /** Exposed for the Media3 PlayerView only. Null until a source is prepared. */
    val player: Player?

    fun prepare(
        url: String,
        startPositionMs: Long = 0L,
        headers: Map<String, String> = emptyMap(),
        mimeType: String? = null
    )
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
    fun retry()
    fun selectTrack(option: TrackOption)
    fun disableTracks(kind: TrackKind)
    fun release()
}
