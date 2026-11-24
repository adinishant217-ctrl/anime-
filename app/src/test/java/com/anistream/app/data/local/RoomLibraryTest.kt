package com.anistream.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.anistream.app.data.local.entity.WatchHistoryEntity
import com.anistream.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoomLibraryTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `history dao persists progress and resume query`() = runTest {
        val dao = database.watchHistoryDao()
        dao.upsert(
            WatchHistoryEntity(
                animeSlug = "anime-a",
                episodeNumber = 1,
                animeTitle = "Anime A",
                coverUrl = null,
                episodeSlug = "anime-a-episode-1",
                episodeTitle = "Anime A",
                positionMs = 30_000L,
                durationMs = 1_440_000L,
                updatedAt = 10L
            )
        )

        assertEquals(1, dao.observeAll().first().size)
        assertEquals(30_000L, dao.find("anime-a", 1)?.positionMs)
        assertEquals(1, dao.observeContinueWatching().first().size)
        assertEquals("anime-a-episode-1", dao.latestResumeForAnime("anime-a")?.episodeSlug)
    }

    @Test
    fun `completed history is not offered as continue watching`() = runTest {
        val dao = database.watchHistoryDao()
        dao.upsert(
            WatchHistoryEntity(
                animeSlug = "anime-b",
                episodeNumber = 12,
                animeTitle = "Anime B",
                coverUrl = null,
                episodeSlug = "anime-b-episode-12",
                episodeTitle = "Anime B",
                positionMs = 1_440_000L,
                durationMs = 1_440_000L,
                completed = true,
                updatedAt = 20L
            )
        )

        assertTrue(dao.observeContinueWatching().first().isEmpty())
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun `watchlist dao inserts replaces and deletes`() = runTest {
        val dao = database.watchlistDao()
        dao.insert(
            WatchlistEntity(
                slug = "anime-a",
                title = "Anime A",
                coverUrl = null,
                status = "Ongoing",
                type = "TV",
                addedAt = 1L
            )
        )
        assertTrue(dao.observeIsSaved("anime-a").first())
        assertEquals(1, dao.observeAll().first().size)

        dao.insert(
            WatchlistEntity(
                slug = "anime-a",
                title = "Anime A Updated",
                coverUrl = null,
                status = "Completed",
                type = "TV",
                addedAt = 2L
            )
        )
        assertEquals("Anime A Updated", dao.observeAll().first().single().title)

        dao.delete("anime-a")
        assertTrue(dao.observeAll().first().isEmpty())
    }
}
