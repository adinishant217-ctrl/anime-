package com.anistream.app.data.repository

import com.anistream.app.TestData
import com.anistream.app.data.remote.AnimeRemoteDataSource
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimeRepositoryTest {

    @Test
    fun `forwards page numbers and search queries to the data source`() = runTest {
        val remote = StubRemote()
        val repository = AnimeRepositoryImpl(remote)

        val page = repository.getOngoing(3).getOrThrow()
        repository.getCompleted(2)
        repository.getGenreAnime("action", 4)
        repository.search("naruto")
        repository.getSchedule()
        repository.getAnimeDetail("anime-a")
        repository.getEpisode("anime-a-episode-1")

        assertEquals(3, page.page)
        assertEquals(3, remote.lastPage)
        assertEquals(2, remote.lastCompletedPage)
        assertEquals("action", remote.lastGenreSlug)
        assertEquals(4, remote.lastGenrePage)
        assertEquals("naruto", remote.lastSearchQuery)
        assertEquals("anime-a", remote.lastDetailSlug)
        assertEquals("anime-a-episode-1", remote.lastEpisodeSlug)
    }

    @Test
    fun `propagates data source failures`() = runTest {
        val remote = StubRemote().apply { fail = true }
        val repository = AnimeRepositoryImpl(remote)

        val result = repository.getOngoing(1)

        assertEquals(true, result.isFailure)
    }

    private class StubRemote : AnimeRemoteDataSource {
        var lastPage: Int = 0
        var lastCompletedPage: Int = 0
        var lastGenreSlug: String = ""
        var lastGenrePage: Int = 0
        var lastSearchQuery: String = ""
        var lastDetailSlug: String = ""
        var lastEpisodeSlug: String = ""
        var fail: Boolean = false

        private fun <T> result(value: T): Result<T> =
            if (fail) Result.failure(IllegalStateException("offline")) else Result.success(value)

        override suspend fun fetchOngoing(page: Int): Result<PagedAnime> {
            lastPage = page
            return result(PagedAnime(emptyList(), page, false))
        }

        override suspend fun fetchCompleted(page: Int): Result<PagedAnime> {
            lastCompletedPage = page
            return result(PagedAnime(emptyList(), page, false))
        }

        override suspend fun fetchGenres(): Result<List<Genre>> =
            result(listOf(Genre("Action", "action")))

        override suspend fun fetchGenreAnime(slug: String, page: Int): Result<PagedAnime> {
            lastGenreSlug = slug
            lastGenrePage = page
            return result(PagedAnime(emptyList(), page, false))
        }

        override suspend fun search(query: String): Result<SearchResult> {
            lastSearchQuery = query
            return result(SearchResult(query, 0, emptyList()))
        }

        override suspend fun fetchSchedule(): Result<List<ScheduleDay>> = result(emptyList())

        override suspend fun fetchAnimeDetail(slug: String): Result<AnimeDetail> {
            lastDetailSlug = slug
            return result(TestData.detail())
        }

        override suspend fun fetchEpisode(slug: String): Result<EpisodeDetail> {
            lastEpisodeSlug = slug
            return result(TestData.directEpisode())
        }
    }
}
