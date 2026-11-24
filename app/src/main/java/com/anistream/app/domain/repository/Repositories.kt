package com.anistream.app.domain.repository

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
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {
    suspend fun getOngoing(page: Int): Result<PagedAnime>
    suspend fun getCompleted(page: Int): Result<PagedAnime>
    suspend fun getGenres(): Result<List<Genre>>
    suspend fun getGenreAnime(slug: String, page: Int): Result<PagedAnime>
    suspend fun search(query: String): Result<SearchResult>
    suspend fun getSchedule(): Result<List<ScheduleDay>>
    suspend fun getAnimeDetail(slug: String): Result<AnimeDetail>
    suspend fun getEpisode(slug: String): Result<EpisodeDetail>
}

interface LibraryRepository {
    fun observeWatchlist(): Flow<List<SavedAnime>>
    fun observeHistory(): Flow<List<HistoryEntry>>
    fun observeContinueWatching(): Flow<List<HistoryEntry>>
    fun observeHistoryForAnime(animeSlug: String): Flow<List<HistoryEntry>>
    fun observeIsInWatchlist(animeSlug: String): Flow<Boolean>

    suspend fun setWatchlistSaved(
        saved: Boolean,
        slug: String,
        title: String,
        coverUrl: String?,
        status: String?,
        type: String?
    )

    suspend fun saveAnimeToWatchlist(anime: AnimeSummary)
    suspend fun saveAnimeToWatchlist(anime: AnimeDetail)

    /** Records that an episode was opened, without claiming playback progress. */
    suspend fun touchEpisode(anime: AnimeDetail, episode: Episode)

    suspend fun getHistoryEntry(animeSlug: String, episodeNumber: Int): HistoryEntry?

    suspend fun saveProgress(
        animeSlug: String,
        animeTitle: String,
        coverUrl: String?,
        episode: Episode,
        positionMs: Long,
        durationMs: Long
    )

    suspend fun removeHistoryEntry(animeSlug: String, episodeNumber: Int)
    suspend fun clearHistory()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAutoplayNext(enabled: Boolean)
    suspend fun setPreferHighestQuality(enabled: Boolean)
    suspend fun addSearchQuery(query: String)
    suspend fun clearSearchHistory()
}
