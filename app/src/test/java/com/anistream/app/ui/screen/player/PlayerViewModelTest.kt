package com.anistream.app.ui.screen.player

import com.anistream.app.FakeAnimeRepository
import com.anistream.app.FakeLibraryRepository
import com.anistream.app.FakePlaybackEngine
import com.anistream.app.FakeSettingsRepository
import com.anistream.app.MainDispatcherRule
import com.anistream.app.TestData
import com.anistream.app.player.source.PlaybackKind
import com.anistream.app.player.source.PlaybackResolver
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
    fun `embed-only episode opens the provider fallback and is still recorded`() =
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
                sourceResolver = PlaybackResolver(),
                engine = engine,
                applicationScope = this
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.sourceAvailable)
            assertEquals(PlaybackKind.EMBEDDED, state.playbackKind)
            assertEquals(3, state.embeddedSources.size)
            assertNotNull(state.selectedEmbeddedSource)
            assertNull(state.unavailableReason)
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
                sourceResolver = PlaybackResolver(),
                engine = engine,
                applicationScope = this
            )
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.sourceAvailable)
            assertEquals("720p", state.selectedQuality)
            assertEquals("https://cdn.example.com/720.m3u8", engine.preparedUrl)
            assertEquals(0L, engine.preparedPosition)
            assertEquals("application/x-mpegURL", engine.preparedMimeType)
            assertTrue(engine.preparedHeaders["User-Agent"].orEmpty().isNotBlank())
        }

    @Test
    fun `native playback error resolves the source once more`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val animeRepository = FakeAnimeRepository().apply {
                episodeDetail = TestData.directEpisode()
            }
            val engine = FakePlaybackEngine()
            val viewModel = PlayerViewModel(
                animeSlug = "anime-a",
                initialEpisodeSlug = "anime-a-episode-1",
                animeRepository = animeRepository,
                libraryRepository = FakeLibraryRepository(),
                settingsRepository = FakeSettingsRepository(),
                sourceResolver = PlaybackResolver(),
                engine = engine,
                applicationScope = this
            )
            advanceUntilIdle()
            assertEquals(1, animeRepository.episodeCalls)

            engine.fail("The video source is offline or rejected the request.")
            advanceUntilIdle()

            assertEquals(2, animeRepository.episodeCalls)
            assertEquals(2, engine.prepareCount)
            assertEquals("https://cdn.example.com/720.m3u8", engine.preparedUrl)

            engine.fail("The video source is offline or rejected the request.")
            advanceUntilIdle()
            assertEquals(2, animeRepository.episodeCalls)
            assertEquals("The video source is offline or rejected the request.", viewModel.state.value.playbackError)
        }

    @Test
    fun `embedded provider errors are exposed for the fallback UI`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = PlayerViewModel(
                animeSlug = "anime-a",
                initialEpisodeSlug = "anime-a-episode-1",
                animeRepository = FakeAnimeRepository(),
                libraryRepository = FakeLibraryRepository(),
                settingsRepository = FakeSettingsRepository(),
                sourceResolver = PlaybackResolver(),
                engine = FakePlaybackEngine(),
                applicationScope = this
            )
            advanceUntilIdle()

            viewModel.onEmbeddedPageError("The provider returned HTTP 403.")

            assertEquals("The provider returned HTTP 403.", viewModel.state.value.embeddedError)
            assertFalse(viewModel.state.value.embeddedLoading)
        }
}
