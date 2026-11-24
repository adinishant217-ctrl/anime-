package com.anistream.app.data.repository

import com.anistream.app.data.local.dao.WatchHistoryDao
import com.anistream.app.data.local.dao.WatchlistDao
import com.anistream.app.data.local.entity.WatchHistoryEntity
import com.anistream.app.data.local.entity.WatchlistEntity
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.HistoryEntry
import com.anistream.app.domain.model.SavedAnime
import com.anistream.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepositoryImpl(
    private val historyDao: WatchHistoryDao,
    private val watchlistDao: WatchlistDao
) : LibraryRepository {

    override fun observeWatchlist(): Flow<List<SavedAnime>> =
        watchlistDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeHistory(): Flow<List<HistoryEntry>> =
        historyDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeContinueWatching(): Flow<List<HistoryEntry>> =
        historyDao.observeContinueWatching().map { entities -> entities.map { it.toDomain() } }

    override fun observeHistoryForAnime(animeSlug: String): Flow<List<HistoryEntry>> =
        historyDao.observeForAnime(animeSlug).map { entities -> entities.map { it.toDomain() } }

    override fun observeIsInWatchlist(animeSlug: String): Flow<Boolean> =
        watchlistDao.observeIsSaved(animeSlug)

    override suspend fun setWatchlistSaved(
        saved: Boolean,
        slug: String,
        title: String,
        coverUrl: String?,
        status: String?,
        type: String?
    ) {
        if (saved) {
            watchlistDao.insert(
                WatchlistEntity(
                    slug = slug,
                    title = title,
                    coverUrl = coverUrl,
                    status = status,
                    type = type,
                    addedAt = System.currentTimeMillis()
                )
            )
        } else {
            watchlistDao.delete(slug)
        }
    }

    override suspend fun saveAnimeToWatchlist(anime: AnimeSummary) {
        setWatchlistSaved(
            saved = true,
            slug = anime.slug,
            title = anime.title,
            coverUrl = anime.coverUrl,
            status = anime.status,
            type = null
        )
    }

    override suspend fun saveAnimeToWatchlist(anime: AnimeDetail) {
        setWatchlistSaved(
            saved = true,
            slug = anime.slug,
            title = anime.title,
            coverUrl = anime.coverUrl,
            status = anime.status,
            type = anime.type
        )
    }

    override suspend fun touchEpisode(anime: AnimeDetail, episode: Episode) {
        val existing = historyDao.find(anime.slug, episode.number)
        val entity = if (existing == null) {
            WatchHistoryEntity(
                animeSlug = anime.slug,
                episodeNumber = episode.number,
                animeTitle = anime.title,
                coverUrl = anime.coverUrl,
                episodeSlug = episode.slug,
                episodeTitle = episode.displayTitle,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            existing.copy(
                animeTitle = anime.title,
                coverUrl = anime.coverUrl,
                episodeSlug = episode.slug,
                episodeTitle = episode.displayTitle,
                updatedAt = System.currentTimeMillis()
            )
        }
        historyDao.upsert(entity)
    }

    override suspend fun getHistoryEntry(animeSlug: String, episodeNumber: Int): HistoryEntry? =
        historyDao.find(animeSlug, episodeNumber)?.toDomain()

    override suspend fun saveProgress(
        animeSlug: String,
        animeTitle: String,
        coverUrl: String?,
        episode: Episode,
        positionMs: Long,
        durationMs: Long
    ) {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val watched = duration > 0L && position >= (duration * COMPLETION_FRACTION).toLong()
        val existing = historyDao.find(animeSlug, episode.number)
        val entity = WatchHistoryEntity(
            animeSlug = animeSlug,
            episodeNumber = episode.number,
            animeTitle = animeTitle,
            coverUrl = coverUrl,
            episodeSlug = episode.slug,
            episodeTitle = episode.displayTitle,
            positionMs = position,
            durationMs = duration,
            completed = watched || existing?.completed == true,
            updatedAt = System.currentTimeMillis()
        )
        historyDao.upsert(entity)
    }

    override suspend fun removeHistoryEntry(animeSlug: String, episodeNumber: Int) {
        historyDao.deleteEpisode(animeSlug, episodeNumber)
    }

    override suspend fun clearHistory() {
        historyDao.clear()
    }

    private fun WatchHistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
        animeSlug = animeSlug,
        animeTitle = animeTitle,
        coverUrl = coverUrl,
        episodeSlug = episodeSlug,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        positionMs = positionMs,
        durationMs = durationMs,
        completed = completed,
        updatedAt = updatedAt
    )

    private fun WatchlistEntity.toDomain(): SavedAnime = SavedAnime(
        slug = slug,
        title = title,
        coverUrl = coverUrl,
        status = status,
        type = type,
        addedAt = addedAt
    )

    private companion object {
        const val COMPLETION_FRACTION = 0.9f
    }
}
