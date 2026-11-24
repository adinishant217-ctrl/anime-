package com.anistream.app.ui.screen.player

import com.anistream.app.FakeAnimeRepository
import com.anistream.app.FakeLibraryRepository
import com.anistream.app.FakePlaybackEngine
import com.anistream.app.FakeSettingsRepository
import com.anistream.app.MainDispatcherRule
import com.anistream.app.TestData
import com.anistream.app.player.source.PlaybackSourceResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `embed-only episode is unavailable and still recorded as opened`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val animeRepository = FakeAnimeRepository().apply {
                episodeDetail = TestData.embedEpisode()
            }
            val library = FakeLibraryRepository()
            val engine = FakePlaybackEngine()

            val viewModel = PlayerViewModel(
                animeSlug = "anime-a",
                initialEpisodeSlug = "anime-a-episode-1",
                animeRepository = animeRepository,
                libraryRepository = library,
                settingsRepository = FakeSettingsRepository(),
                sourceResolver = PlaybackSourceResolver(),
                engine = engine,
                applicationScope = this
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.sourceAvailable)
            assertNotNull(state.unavailableReason)
            assertNull(engine.preparedUrl)
            assertTrue(engine.stopped)
            assertNotNull(library.touchedEpisode)
            assertEquals(1, library.touchedEpisode?.second?.number)
        }

    @Test
    fun `direct source is prepared at the preferred quality`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val animeRepository = FakeAnimeRepository().apply {
                episodeDetail = TestData.directEpisode()
            }
            val library = FakeLibraryRepository()
            val engine = FakePlaybackEngine()

            val viewModel = PlayerViewModel(
                animeSlug = "anime-a",
                initialEpisodeSlug = "anime-a-episode-1",
                animeRepository = animeRepository,
                libraryRepository = library,
                settingsRepository = FakeSettingsRepository(),
                sourceResolver = PlaybackSourceResolver(),
                engine = engine,
                applicationScope = this
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.sourceAvailable)
            assertEquals("720p", state.selectedQuality)
            assertEquals("https://cdn.example.com/720.m3u8", engine.preparedUrl)
            assertEquals(0L, engine.preparedPosition)
        }
}
