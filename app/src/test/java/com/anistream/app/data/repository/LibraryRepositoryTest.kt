package com.anistream.app.data.repository

import com.anistream.app.FakeHistoryDao
import com.anistream.app.FakeWatchlistDao
import com.anistream.app.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {

    @Test
    fun `saveProgress stores position and marks completion at ninety percent`() = runTest {
        val repository = LibraryRepositoryImpl(FakeHistoryDao(), FakeWatchlistDao())
        val episode = TestData.episode(1)

        repository.saveProgress(
            animeSlug = "anime-a",
            animeTitle = "Anime A",
            coverUrl = "https://example.com/a.jpg",
            episode = episode,
            positionMs = 10_000L,
            durationMs = 100_000L
        )

        val partial = repository.getHistoryEntry("anime-a", 1)
        assertNotNull(partial)
        assertEquals(10_000L, partial?.positionMs)
        assertFalse(partial?.completed == true)

        repository.saveProgress(
            animeSlug = "anime-a",
            animeTitle = "Anime A",
            coverUrl = "https://example.com/a.jpg",
            episode = episode,
            positionMs = 95_000L,
            durationMs = 100_000L
        )

        assertTrue(repository.getHistoryEntry("anime-a", 1)?.completed == true)
        assertEquals(1, repository.observeHistory().first().size)
    }

    @Test
    fun `touching an episode keeps existing resume progress`() = runTest {
        val repository = LibraryRepositoryImpl(FakeHistoryDao(), FakeWatchlistDao())
        val episode = TestData.episode(1)
        repository.saveProgress(
            animeSlug = "anime-a",
            animeTitle = "Anime A",
            coverUrl = null,
            episode = episode,
            positionMs = 42_000L,
            durationMs = 1_440_000L
        )

        repository.touchEpisode(TestData.detail(), episode)

        assertEquals(42_000L, repository.getHistoryEntry("anime-a", 1)?.positionMs)
    }

    @Test
    fun `continue watching only exposes episodes with progress`() = runTest {
        val repository = LibraryRepositoryImpl(FakeHistoryDao(), FakeWatchlistDao())
        val episode = TestData.episode(1)

        repository.touchEpisode(TestData.detail(), episode)
        assertTrue(repository.observeContinueWatching().first().isEmpty())

        repository.saveProgress(
            animeSlug = "anime-a",
            animeTitle = "Anime A",
            coverUrl = null,
            episode = episode,
            positionMs = 5_000L,
            durationMs = 1_440_000L
        )

        assertEquals(1, repository.observeContinueWatching().first().size)
    }

    @Test
    fun `watchlist add and remove updates persisted state`() = runTest {
        val repository = LibraryRepositoryImpl(FakeHistoryDao(), FakeWatchlistDao())

        repository.setWatchlistSaved(
            saved = true,
            slug = "anime-a",
            title = "Anime A",
            coverUrl = null,
            status = "Ongoing",
            type = "TV"
        )

        assertTrue(repository.observeIsInWatchlist("anime-a").first())
        assertEquals(1, repository.observeWatchlist().first().size)

        repository.setWatchlistSaved(
            saved = false,
            slug = "anime-a",
            title = "",
            coverUrl = null,
            status = null,
            type = null
        )

        assertFalse(repository.observeIsInWatchlist("anime-a").first())
        assertTrue(repository.observeWatchlist().first().isEmpty())
    }
}
