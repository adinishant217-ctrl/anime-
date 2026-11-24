package com.anistream.app.data.repository

import com.anistream.app.data.remote.AnimeRemoteDataSource
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult
import com.anistream.app.domain.repository.AnimeRepository

class AnimeRepositoryImpl(
    private val remote: AnimeRemoteDataSource
) : AnimeRepository {

    override suspend fun getOngoing(page: Int): Result<PagedAnime> = remote.fetchOngoing(page)

    override suspend fun getCompleted(page: Int): Result<PagedAnime> = remote.fetchCompleted(page)

    override suspend fun getGenres(): Result<List<Genre>> = remote.fetchGenres()

    override suspend fun getGenreAnime(slug: String, page: Int): Result<PagedAnime> =
        remote.fetchGenreAnime(slug, page)

    override suspend fun search(query: String): Result<SearchResult> = remote.search(query)

    override suspend fun getSchedule(): Result<List<ScheduleDay>> = remote.fetchSchedule()

    override suspend fun getAnimeDetail(slug: String): Result<AnimeDetail> = remote.fetchAnimeDetail(slug)

    override suspend fun getEpisode(slug: String): Result<EpisodeDetail> = remote.fetchEpisode(slug)
}
