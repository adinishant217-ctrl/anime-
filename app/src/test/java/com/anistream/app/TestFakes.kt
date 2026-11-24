package com.anistream.app

import androidx.media3.common.Player
import com.anistream.app.core.common.ApiException
import com.anistream.app.data.local.dao.WatchHistoryDao
import com.anistream.app.data.local.dao.WatchlistDao
import com.anistream.app.data.local.entity.WatchHistoryEntity
import com.anistream.app.data.local.entity.WatchlistEntity
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.AppSettings
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.SavedAnime
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult
import com.anistream.app.domain.model.ThemeMode
import com.anistream.app.domain.repository.AnimeRepository
import com.anistream.app.domain.repository.LibraryRepository
import com.anistream.app.domain.repository.SettingsRepository
import com.anistream.app.player.engine.PlaybackEngine
import com.anistream.app.player.state.PlaybackEngineState
import com.anistream.app.player.state.TrackKind
import com.anistream.app.player.state.TrackOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAnimeRepository : AnimeRepository {
    var ongoing: PagedAnime = PagedAnime(emptyList(), 1, false)
    var completed: PagedAnime = PagedAnime(emptyList(), 1, false)
    var genres: List<Genre> = listOf(Genre("Action", "action"))
    var genreAnime: PagedAnime = ongoing
    var searchResult: SearchResult = SearchResult("", 0, emptyList())
    var schedule: List<ScheduleDay> = emptyList()
    var detail: AnimeDetail = TestData.detail()
    var episodeDetail: EpisodeDetail = TestData.embedEpisode()
    var failAll: Boolean = false
    var episodeCalls: Int = 0

    private fun <T> result(value: T): Result<T> =
        if (failAll) Result.failure(ApiException("Test failure")) else Result.success(value)

    override suspend fun getOngoing(page: Int): Result<PagedAnime> = result(ongoing)

    override suspend fun getCompleted(page: Int): Result<PagedAnime> = result(completed)

    override suspend fun getGenres(): Result<List<Genre>> = result(genres)

    override suspend fun getGenreAnime(slug: String, page: Int): Result<PagedAnime> = result(genreAnime)

    override suspend fun search(query: String): Result<SearchResult> = result(searchResult)

    override suspend fun getSchedule(): Result<List<ScheduleDay>> = result(schedule)

    override suspend fun getAnimeDetail(slug: String): Result<AnimeDetail> = result(detail)

    override suspend fun getEpisode(slug: String): Result<EpisodeDetail> {
        episodeCalls++
        return result(episodeDetail)
    }
}

class FakeLibraryRepository : LibraryRepository {
    val watchlistFlow = MutableStateFlow<List<SavedAnime>>(emptyList())
    val historyFlow = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val continueFlow = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val savedFlow = MutableStateFlow(false)

    var touchedEpisode: Pair<AnimeDetail, Episode>? = null
    var savedProgress: HistoryEntry? = null
    var resumeEntry: HistoryEntry? = null

    override fun observeWatchlist(): Flow<List<SavedAnime>> = watchlistFlow

    override fun observeHistory(): Flow<List<HistoryEntry>> = historyFlow

    override fun observeContinueWatching(): Flow<List<HistoryEntry>> = continueFlow

    override fun observeHistoryForAnime(animeSlug: String): Flow<List<HistoryEntry>> =
        historyFlow.map { entries -> entries.filter { it.animeSlug == animeSlug } }

    override fun observeIsInWatchlist(animeSlug: String): Flow<Boolean> = savedFlow

    override suspend fun setWatchlistSaved(
        saved: Boolean,
        slug: String,
        title: String,
        coverUrl: String?,
        status: String?,
        type: String?
    ) {
        savedFlow.value = saved
    }

    override suspend fun saveAnimeToWatchlist(anime: AnimeSummary) {
        savedFlow.value = true
    }

    override suspend fun saveAnimeToWatchlist(anime: AnimeDetail) {
        savedFlow.value = true
    }

    override suspend fun touchEpisode(anime: AnimeDetail, episode: Episode) {
        touchedEpisode = anime to episode
    }

    override suspend fun getHistoryEntry(animeSlug: String, episodeNumber: Int): HistoryEntry? =
        resumeEntry

    override suspend fun saveProgress(
        animeSlug: String,
        animeTitle: String,
        coverUrl: String?,
        episode: Episode,
        positionMs: Long,
        durationMs: Long
    ) {
        savedProgress = HistoryEntry(
            animeSlug = animeSlug,
            animeTitle = animeTitle,
            coverUrl = coverUrl,
            episodeSlug = episode.slug,
            episodeNumber = episode.number,
            episodeTitle = episode.displayTitle,
            positionMs = positionMs,
            durationMs = durationMs,
            completed = durationMs > 0 && positionMs >= (durationMs * 0.9f).toLong(),
            updatedAt = 0L
        )
    }

    override suspend fun removeHistoryEntry(animeSlug: String, episodeNumber: Int) {
        historyFlow.value = historyFlow.value.filterNot {
            it.animeSlug == animeSlug && it.episodeNumber == episodeNumber
        }
    }

    override suspend fun clearHistory() {
        historyFlow.value = emptyList()
    }
}

class FakeSettingsRepository : SettingsRepository {
    val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setAutoplayNext(enabled: Boolean) {
        state.value = state.value.copy(autoplayNext = enabled)
    }

    override suspend fun setPreferHighestQuality(enabled: Boolean) {
        state.value = state.value.copy(preferHighestQuality = enabled)
    }

    override suspend fun addSearchQuery(query: String) {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return
        state.value = state.value.copy(
            searchHistory = (listOf(cleaned) + state.value.searchHistory.filterNot {
                it.equals(cleaned, ignoreCase = true)
            }).take(10)
        )
    }

    override suspend fun clearSearchHistory() {
        state.value = state.value.copy(searchHistory = emptyList())
    }
}

class FakePlaybackEngine : PlaybackEngine {
    override val state = MutableStateFlow(PlaybackEngineState())
    override val player: Player? = null

    var preparedUrl: String? = null
    var preparedPosition: Long = 0L
    var preparedHeaders: Map<String, String> = emptyMap()
    var preparedMimeType: String? = null
    var prepareCount: Int = 0
    var released: Boolean = false
    var stopped: Boolean = false

    override fun prepare(
        url: String,
        startPositionMs: Long,
        headers: Map<String, String>,
        mimeType: String?
    ) {
        preparedUrl = url
        preparedPosition = startPositionMs
        preparedHeaders = headers
        preparedMimeType = mimeType
        prepareCount++
        state.value = state.value.copy(isReady = true, error = null, ended = false)
    }

    fun fail(message: String) {
        state.value = state.value.copy(error = message, isBuffering = false)
    }

    override fun play() {
        state.value = state.value.copy(isPlaying = true)
    }

    override fun pause() {
        state.value = state.value.copy(isPlaying = false)
    }

    override fun seekTo(positionMs: Long) {
        state.value = state.value.copy(positionMs = positionMs)
    }

    override fun stop() {
        stopped = true
        state.value = PlaybackEngineState()
    }

    override fun retry() {
        preparedUrl?.let { prepare(it, state.value.positionMs, preparedHeaders, preparedMimeType) }
    }

    override fun selectTrack(option: TrackOption) = Unit

    override fun disableTracks(kind: TrackKind) = Unit

    override fun release() {
        released = true
    }
}

class FakeHistoryDao : WatchHistoryDao {
    val entries = MutableStateFlow<List<WatchHistoryEntity>>(emptyList())

    override fun observeAll(): Flow<List<WatchHistoryEntity>> = entries

    override fun observeContinueWatching(): Flow<List<WatchHistoryEntity>> =
        entries.map { list ->
            list.filter { it.positionMs > 0 && !it.completed }
                .sortedByDescending { it.updatedAt }
                .take(20)
        }

    override fun observeForAnime(slug: String): Flow<List<WatchHistoryEntity>> =
        entries.map { list ->
            list.filter { it.animeSlug == slug }.sortedBy { it.episodeNumber }
        }

    override suspend fun find(slug: String, number: Int): WatchHistoryEntity? =
        entries.value.firstOrNull { it.animeSlug == slug && it.episodeNumber == number }

    override suspend fun latestResumeForAnime(slug: String): WatchHistoryEntity? =
        entries.value.firstOrNull {
            it.animeSlug == slug && it.positionMs > 0 && !it.completed
        }

    override suspend fun upsert(entity: WatchHistoryEntity) {
        entries.value = entries.value.filterNot {
            it.animeSlug == entity.animeSlug && it.episodeNumber == entity.episodeNumber
        } + entity
    }

    override suspend fun deleteEpisode(slug: String, number: Int) {
        entries.value = entries.value.filterNot {
            it.animeSlug == slug && it.episodeNumber == number
        }
    }

    override suspend fun deleteForAnime(slug: String) {
        entries.value = entries.value.filterNot { it.animeSlug == slug }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }
}

class FakeWatchlistDao : WatchlistDao {
    val entries = MutableStateFlow<List<WatchlistEntity>>(emptyList())

    override fun observeAll(): Flow<List<WatchlistEntity>> = entries

    override fun observeIsSaved(slug: String): Flow<Boolean> =
        entries.map { list -> list.any { it.slug == slug } }

    override suspend fun isSaved(slug: String): Boolean =
        entries.value.any { it.slug == slug }

    override suspend fun insert(entity: WatchlistEntity) {
        entries.value = entries.value.filterNot { it.slug == entity.slug } + entity
    }

    override suspend fun delete(slug: String) {
        entries.value = entries.value.filterNot { it.slug == slug }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }
}
