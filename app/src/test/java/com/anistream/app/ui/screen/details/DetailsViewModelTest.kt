package com.anistream.app.ui.screen.details

import com.anistream.app.FakeAnimeRepository
import com.anistream.app.FakeLibraryRepository
import com.anistream.app.MainDispatcherRule
import com.anistream.app.domain.model.HistoryEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads details, watchlist state and resume entry`() = runTest(mainDispatcherRule.testDispatcher) {
        val entry = HistoryEntry(
            animeSlug = "anime-a",
            animeTitle = "Anime A",
            coverUrl = null,
            episodeSlug = "anime-a-episode-1",
            episodeNumber = 1,
            episodeTitle = "Anime A",
            positionMs = 60_000L,
            durationMs = 1_440_000L,
            completed = false,
            updatedAt = 10L
        )
        val library = FakeLibraryRepository().apply {
            savedFlow.value = true
            historyFlow.value = listOf(entry)
            resumeEntry = entry
        }

        val viewModel = DetailsViewModel("anime-a", FakeAnimeRepository(), library)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Anime A", state.detail?.title)
        assertEquals(2, state.detail?.episodes?.size)
        assertTrue(state.isInWatchlist)
        assertEquals(1, state.resumeEntry?.episodeNumber)
    }

    @Test
    fun `watchlist toggle removes an existing save`() = runTest(mainDispatcherRule.testDispatcher) {
        val library = FakeLibraryRepository().apply { savedFlow.value = true }
        val viewModel = DetailsViewModel("anime-a", FakeAnimeRepository(), library)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isInWatchlist)

        viewModel.toggleWatchlist()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isInWatchlist)
        assertFalse(library.savedFlow.value)
    }
}
