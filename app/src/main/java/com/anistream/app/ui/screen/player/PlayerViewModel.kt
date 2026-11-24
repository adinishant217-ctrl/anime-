package com.anistream.app.ui.screen.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.anistream.app.core.common.EpisodeNumbers
import com.anistream.app.core.common.toUserMessage
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.StreamProvider
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.LibraryRepository
import com.anistream.app.domain.repository.SettingsRepository
import com.anistream.app.player.engine.PlaybackEngine
import com.anistream.app.player.source.EmbeddedPlaybackSource
import com.anistream.app.player.source.PlaybackKind
import com.anistream.app.player.source.PlaybackResolution
import com.anistream.app.player.source.PlaybackResolver
import com.anistream.app.player.state.PlaybackEngineState
import com.anistream.app.player.state.TrackKind
import com.anistream.app.player.state.TrackOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val animeTitle: String = "",
    val animeCoverUrl: String? = null,
    val detail: AnimeDetail? = null,
    val episode: Episode? = null,
    val episodeDetail: EpisodeDetail? = null,
    val sourceAvailable: Boolean = false,
    val unavailableReason: String? = null,
    val providers: List<StreamProvider> = emptyList(),
    val playbackKind: PlaybackKind = PlaybackKind.UNAVAILABLE,
    val embeddedSources: List<EmbeddedPlaybackSource> = emptyList(),
    val selectedEmbeddedSource: EmbeddedPlaybackSource? = null,
    val embeddedLoading: Boolean = false,
    val embeddedError: String? = null,
    val availableQualities: List<String> = emptyList(),
    val selectedQuality: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackError: String? = null,
    val ended: Boolean = false,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val autoplayNext: Boolean = true,
    val resumedFromMs: Long = 0L,
    val showResumeNotice: Boolean = false,
    val episodeSwitchLoading: Boolean = false
)

class PlayerViewModel(
    private val animeSlug: String,
    initialEpisodeSlug: String,
    private val animeRepository: AnimeRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val sourceResolver: PlaybackResolver,
    private val engine: PlaybackEngine,
    private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val player: Player? get() = engine.player

    private var currentDetail: AnimeDetail? = null
    private var currentEpisode: Episode? = null
    private var currentEpisodeDetail: EpisodeDetail? = null
    private var currentResolution: PlaybackResolution? = null
    private var preferredQuality: String? = null
    private var preferHighestQuality = true
    private var autoplayNext = true
    private var lastSavedAt = 0L
    private var wasPlaying = false
    private var handledEnd = false
    private var loadJob: Job? = null
    private var requestedEpisodeSlug: String = initialEpisodeSlug
    private var autoRefreshUsed = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                preferHighestQuality = settings.preferHighestQuality
                autoplayNext = settings.autoplayNext
                _state.update { it.copy(autoplayNext = settings.autoplayNext) }
            }
        }
        viewModelScope.launch {
            engine.state.collect { engineState -> onEngineState(engineState) }
        }
        loadEpisode(initialEpisodeSlug, allowResume = true)
    }

    fun loadEpisode(episodeSlug: String, allowResume: Boolean = true) {
        autoRefreshUsed = false
        loadEpisodeInternal(episodeSlug, allowResume, isAutoRefresh = false)
    }

    private fun loadEpisodeInternal(
        episodeSlug: String,
        allowResume: Boolean,
        isAutoRefresh: Boolean
    ) {
        loadJob?.cancel()
        requestedEpisodeSlug = episodeSlug
        handledEnd = false
        wasPlaying = false
        _state.update {
            it.copy(
                loading = true,
                error = null,
                episodeSwitchLoading = !isAutoRefresh,
                playbackError = null,
                sourceAvailable = false,
                unavailableReason = null,
                playbackKind = PlaybackKind.UNAVAILABLE,
                embeddedSources = emptyList(),
                selectedEmbeddedSource = null,
                embeddedLoading = false,
                embeddedError = null,
                showResumeNotice = false
            )
        }
        loadJob = viewModelScope.launch {
            if (currentEpisode != null) {
                saveProgress(engine.state.value.positionMs, engine.state.value.durationMs)
                engine.stop()
            }
            val cachedDetail = currentDetail
            val detailResult = if (cachedDetail != null && cachedDetail.slug == animeSlug) {
                Result.success(cachedDetail)
            } else {
                animeRepository.getAnimeDetail(animeSlug)
            }
            val episodeResult = animeRepository.getEpisode(episodeSlug)
            val detail = detailResult.getOrNull()
            val episodeDetail = episodeResult.getOrNull()

            if (detail == null || episodeDetail == null) {
                engine.stop()
                _state.update {
                    it.copy(
                        loading = false,
                        episodeSwitchLoading = false,
                        error = (detailResult.exceptionOrNull() ?: episodeResult.exceptionOrNull())
                            ?.toUserMessage()
                            ?: "This episode could not be loaded."
                    )
                }
                return@launch
            }

            val number = EpisodeNumbers.extract(episodeDetail.title)
            val episode = detail.episodes.firstOrNull { it.slug == episodeSlug }
                ?: Episode(
                    slug = episodeSlug,
                    title = episodeDetail.title,
                    number = number,
                    date = episodeDetail.releaseTime,
                    url = episodeDetail.url,
                    displayTitle = EpisodeNumbers.cleanTitle(episodeDetail.title, number)
                )

            currentDetail = detail
            currentEpisode = episode
            currentEpisodeDetail = episodeDetail

            val resolution = sourceResolver.resolve(
                episode = episodeDetail,
                preferredQuality = preferredQuality,
                preferHighestQuality = preferHighestQuality
            )
            currentResolution = resolution

            libraryRepository.touchEpisode(detail, episode)
            val saved = libraryRepository.getHistoryEntry(detail.slug, episode.number)
            val resumeAt = if (
                allowResume &&
                saved != null &&
                saved.positionMs > RESUME_MIN_MS &&
                !saved.completed &&
                (saved.durationMs <= 0L || saved.positionMs < saved.durationMs - RESUME_END_GUARD_MS)
            ) {
                saved.positionMs
            } else {
                0L
            }

            val listIndex = detail.episodes.indexOfFirst { it.slug == episode.slug }
            val nextSlug = episodeDetail.nextEpisodeSlug
                ?: if (listIndex in 0 until detail.episodes.lastIndex) {
                    detail.episodes[listIndex + 1].slug
                } else {
                    null
                }
            val previousSlug = episodeDetail.previousEpisodeSlug
                ?: if (listIndex > 0) {
                    detail.episodes[listIndex - 1].slug
                } else {
                    null
                }

            _state.update {
                it.copy(
                    loading = false,
                    episodeSwitchLoading = false,
                    error = null,
                    animeTitle = detail.title,
                    animeCoverUrl = detail.coverUrl,
                    detail = detail,
                    episode = episode,
                    episodeDetail = episodeDetail,
                    sourceAvailable = resolution.isNative,
                    unavailableReason = resolution.unavailableReason,
                    providers = resolution.providers,
                    playbackKind = resolution.kind,
                    embeddedSources = resolution.embeddedSources,
                    selectedEmbeddedSource = resolution.embeddedSources.firstOrNull(),
                    embeddedLoading = resolution.isEmbedded,
                    embeddedError = null,
                    availableQualities = resolution.sources.map { source -> source.qualityLabel }.distinct(),
                    selectedQuality = resolution.sources.firstOrNull()?.qualityLabel
                        ?: resolution.embeddedSources.firstOrNull()?.qualityLabel,
                    positionMs = resumeAt,
                    durationMs = saved?.durationMs ?: 0L,
                    resumedFromMs = resumeAt,
                    showResumeNotice = resumeAt > 0L,
                    playbackError = null,
                    ended = false,
                    hasNext = !nextSlug.isNullOrBlank(),
                    hasPrevious = !previousSlug.isNullOrBlank()
                )
            }

            if (resolution.isNative) {
                val source = resolution.sources.first()
                lastSavedAt = 0L
                engine.prepare(
                    url = source.url,
                    startPositionMs = resumeAt,
                    headers = source.headers,
                    mimeType = source.mimeType
                )
            } else {
                engine.stop()
            }
        }
    }

    fun togglePlayPause() {
        if (engine.state.value.isPlaying) engine.pause() else engine.play()
    }

    fun seekToFraction(fraction: Float) {
        val duration = _state.value.durationMs
        if (duration > 0L) {
            engine.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
        }
    }

    fun selectQuality(label: String) {
        val resolution = currentResolution ?: return
        val source = resolution.sources.firstOrNull {
            it.qualityLabel.equals(label, ignoreCase = true)
        } ?: return
        val position = engine.state.value.positionMs
        preferredQuality = source.qualityLabel
        _state.update {
            it.copy(
                selectedQuality = source.qualityLabel,
                playbackError = null,
                showResumeNotice = false
            )
        }
        lastSavedAt = 0L
        engine.prepare(
            url = source.url,
            startPositionMs = position,
            headers = source.headers,
            mimeType = source.mimeType
        )
    }

    fun selectEmbeddedSource(source: EmbeddedPlaybackSource) {
        val available = currentResolution?.embeddedSources.orEmpty()
        if (available.none { it.url == source.url }) return
        _state.update {
            it.copy(
                selectedEmbeddedSource = source,
                embeddedLoading = true,
                embeddedError = null,
                selectedQuality = source.qualityLabel
            )
        }
    }

    fun onEmbeddedPageStarted() {
        _state.update { it.copy(embeddedLoading = true, embeddedError = null) }
    }

    fun onEmbeddedPageFinished() {
        _state.update { it.copy(embeddedLoading = false) }
    }

    fun onEmbeddedPageError(message: String) {
        _state.update {
            it.copy(
                embeddedLoading = false,
                embeddedError = message.ifBlank { "The provider player could not be loaded." }
            )
        }
    }

    fun selectAudioTrack(option: TrackOption) {
        if (option.kind == TrackKind.AUDIO) engine.selectTrack(option)
    }

    fun selectSubtitleTrack(option: TrackOption) {
        if (option.kind == TrackKind.SUBTITLE) engine.selectTrack(option)
    }

    fun disableSubtitles() {
        engine.disableTracks(TrackKind.SUBTITLE)
    }

    fun retryPlayback() {
        autoRefreshUsed = false
        loadEpisodeInternal(requestedEpisodeSlug, allowResume = true, isAutoRefresh = false)
    }

    fun nextEpisode() {
        val detail = currentDetail ?: return
        val episode = currentEpisode ?: return
        val explicitSlug = currentEpisodeDetail?.nextEpisodeSlug
        val index = detail.episodes.indexOfFirst { it.slug == episode.slug }
        val fallbackSlug = if (index in 0 until detail.episodes.lastIndex) {
            detail.episodes[index + 1].slug
        } else {
            null
        }
        val slug = explicitSlug ?: fallbackSlug
        if (!slug.isNullOrBlank()) loadEpisode(slug)
    }

    fun previousEpisode() {
        val detail = currentDetail ?: return
        val episode = currentEpisode ?: return
        val explicitSlug = currentEpisodeDetail?.previousEpisodeSlug
        val index = detail.episodes.indexOfFirst { it.slug == episode.slug }
        val fallbackSlug = if (index > 0) detail.episodes[index - 1].slug else null
        val slug = explicitSlug ?: fallbackSlug
        if (!slug.isNullOrBlank()) loadEpisode(slug)
    }

    fun dismissResumeNotice() {
        _state.update { it.copy(showResumeNotice = false) }
    }

    fun saveNow() {
        if (!_state.value.sourceAvailable) return
        val anime = currentDetail ?: return
        val episode = currentEpisode ?: return
        val engineState = engine.state.value
        if (engineState.positionMs <= 0L) return
        applicationScope.launch {
            libraryRepository.saveProgress(
                animeSlug = anime.slug,
                animeTitle = anime.title,
                coverUrl = anime.coverUrl,
                episode = episode,
                positionMs = engineState.positionMs,
                durationMs = engineState.durationMs
            )
        }
    }

    override fun onCleared() {
        saveNow()
        engine.release()
        super.onCleared()
    }

    private suspend fun onEngineState(engineState: PlaybackEngineState) {
        _state.update { current ->
            current.copy(
                isPlaying = engineState.isPlaying,
                isBuffering = engineState.isBuffering,
                positionMs = engineState.positionMs,
                durationMs = if (engineState.durationMs > 0L) {
                    engineState.durationMs
                } else {
                    current.durationMs
                },
                playbackError = engineState.error,
                ended = engineState.ended,
                audioTracks = engineState.audioTracks,
                subtitleTracks = engineState.subtitleTracks
            )
        }

        if (
            engineState.error != null &&
            currentResolution?.isNative == true &&
            !autoRefreshUsed &&
            !_state.value.loading &&
            !_state.value.episodeSwitchLoading
        ) {
            // A temporary CDN URL commonly expires with a 403/404. Resolve the
            // episode once more, then let the normal error UI handle a repeat.
            autoRefreshUsed = true
            loadEpisodeInternal(requestedEpisodeSlug, allowResume = true, isAutoRefresh = true)
            return
        }

        val anime = currentDetail
        val episode = currentEpisode
        if (anime == null || episode == null) return

        if (engineState.isPlaying) {
            val now = System.currentTimeMillis()
            if (now - lastSavedAt >= PROGRESS_SAVE_INTERVAL_MS && engineState.positionMs > 0L) {
                lastSavedAt = now
                saveProgress(engineState.positionMs, engineState.durationMs)
            }
        } else if (wasPlaying && engineState.positionMs > 0L && !engineState.ended) {
            saveProgress(engineState.positionMs, engineState.durationMs)
        }

        if (engineState.ended && !handledEnd) {
            handledEnd = true
            saveProgress(engineState.durationMs, engineState.durationMs)
            if (autoplayNext) nextEpisode()
        }

        wasPlaying = engineState.isPlaying
    }

    private suspend fun saveProgress(positionMs: Long, durationMs: Long) {
        if (positionMs <= PROGRESS_MIN_MS) return
        val anime = currentDetail ?: return
        val episode = currentEpisode ?: return
        libraryRepository.saveProgress(
            animeSlug = anime.slug,
            animeTitle = anime.title,
            coverUrl = anime.coverUrl,
            episode = episode,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }

    private companion object {
        const val RESUME_MIN_MS = 10_000L
        const val RESUME_END_GUARD_MS = 5_000L
        const val PROGRESS_SAVE_INTERVAL_MS = 5_000L
        const val PROGRESS_MIN_MS = 1_000L
    }
}
