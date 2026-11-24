package com.anistream.app.data.remote

import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult

interface AnimeRemoteDataSource {
    suspend fun fetchOngoing(page: Int): Result<PagedAnime>
    suspend fun fetchCompleted(page: Int): Result<PagedAnime>
    suspend fun fetchGenres(): Result<List<Genre>>
    suspend fun fetchGenreAnime(slug: String, page: Int): Result<PagedAnime>
    suspend fun search(query: String): Result<SearchResult>
    suspend fun fetchSchedule(): Result<List<ScheduleDay>>
    suspend fun fetchAnimeDetail(slug: String): Result<AnimeDetail>
    suspend fun fetchEpisode(slug: String): Result<EpisodeDetail>
}
