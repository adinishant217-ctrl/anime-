package com.anistream.app.player.engine

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.anistream.app.player.state.PlaybackEngineState
import com.anistream.app.player.state.TrackKind
import com.anistream.app.player.state.TrackOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
class Media3PlaybackEngine(context: Context) : PlaybackEngine {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackEngineState())
    override val state: StateFlow<PlaybackEngineState> = _state.asStateFlow()
    override val player: Player get() = exoPlayer

    private val trackLookup = mutableMapOf<String, Pair<Tracks.Group, Int>>()
    private var lastUrl: String? = null
    private var released = false

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    publishPosition(ended = true)
                } else {
                    publishPosition()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publishPosition()
            }

            override fun onTracksChanged(tracks: Tracks) {
                publishTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        error = error.toPlaybackMessage(),
                        isBuffering = false
                    )
                }
            }
        })

        scope.launch {
            while (isActive) {
                publishPosition()
                delay(POSITION_INTERVAL_MS)
            }
        }
    }

    override fun prepare(url: String, startPositionMs: Long) {
        if (released) return
        lastUrl = url
        _state.update {
            it.copy(
                error = null,
                ended = false,
                isBuffering = true,
                positionMs = startPositionMs.coerceAtLeast(0L),
                durationMs = 0L
            )
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        if (startPositionMs > 0L) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = true
    }

    override fun play() {
        if (!released) exoPlayer.play()
    }

    override fun pause() {
        if (!released) exoPlayer.pause()
    }

    override fun seekTo(positionMs: Long) {
        if (!released) exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        publishPosition()
    }

    override fun stop() {
        if (released) return
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        lastUrl = null
        _state.value = PlaybackEngineState()
    }

    override fun retry() {
        val url = lastUrl ?: return
        val resumeAt = exoPlayer.currentPosition.coerceAtLeast(0L)
        prepare(url, resumeAt)
    }

    override fun selectTrack(option: TrackOption) {
        if (released) return
        val entry = trackLookup[option.id] ?: return
        val type = if (option.kind == TrackKind.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
        val parameters = exoPlayer.trackSelectionParameters.buildUpon()
        parameters.clearOverridesOfType(type)
        parameters.setTrackTypeDisabled(type, false)
        parameters.setOverrideForType(
            TrackSelectionOverride(entry.first.mediaTrackGroup, entry.second)
        )
        exoPlayer.trackSelectionParameters = parameters.build()
    }

    override fun disableTracks(kind: TrackKind) {
        if (released) return
        val type = if (kind == TrackKind.AUDIO) C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT
        val parameters = exoPlayer.trackSelectionParameters.buildUpon()
        parameters.clearOverridesOfType(type)
        parameters.setTrackTypeDisabled(type, true)
        exoPlayer.trackSelectionParameters = parameters.build()
    }

    override fun release() {
        if (released) return
        released = true
        scope.cancel()
        exoPlayer.release()
        trackLookup.clear()
        _state.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    private fun publishPosition(ended: Boolean = false) {
        if (released) return
        val playbackState = exoPlayer.playbackState
        val duration = exoPlayer.duration
        _state.update {
            it.copy(
                isReady = playbackState == Player.STATE_READY,
                isPlaying = exoPlayer.isPlaying,
                isBuffering = playbackState == Player.STATE_BUFFERING,
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                durationMs = if (duration > 0L) duration else 0L,
                ended = ended || playbackState == Player.STATE_ENDED
            )
        }
    }

    private fun publishTracks(tracks: Tracks) {
        if (released) return
        trackLookup.clear()
        val audio = mutableListOf<TrackOption>()
        val subtitles = mutableListOf<TrackOption>()

        tracks.groups.forEach { group ->
            val kind = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackKind.AUDIO
                C.TRACK_TYPE_TEXT -> TrackKind.SUBTITLE
                else -> null
            } ?: return@forEach

            for (index in 0 until group.length) {
                if (!group.isTrackSupported(index)) continue
                val format = group.getTrackFormat(index)
                val id = "${group.mediaTrackGroup.id}-$index"
                trackLookup[id] = group to index
                val fallback = if (kind == TrackKind.AUDIO) "Audio" else "Subtitle"
                val label = format.label
                    ?: format.language
                    ?: "$fallback ${index + 1}"
                val option = TrackOption(
                    id = id,
                    label = label,
                    selected = group.isTrackSelected(index),
                    kind = kind
                )
                if (kind == TrackKind.AUDIO) audio.add(option) else subtitles.add(option)
            }
        }

        _state.update {
            it.copy(audioTracks = audio, subtitleTracks = subtitles)
        }
    }

    private fun PlaybackException.toPlaybackMessage(): String = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Cannot reach this video source. Check your connection, then retry."

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "The video source is offline or rejected the request."

        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
            "This video format is not supported by the device decoder."

        else -> "This source could not be played ($errorCodeName)."
    }

    private companion object {
        const val POSITION_INTERVAL_MS = 500L
    }
}
