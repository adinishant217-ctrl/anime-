package com.anistream.app.domain.model

data class Genre(
    val name: String,
    val slug: String
)

data class AnimeSummary(
    val slug: String,
    val title: String,
    val coverUrl: String? = null,
    val episodeLabel: String? = null,
    val totalEpisodes: String? = null,
    val rating: String? = null,
    val day: String? = null,
    val date: String? = null,
    val status: String? = null,
    val studio: String? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList()
)

data class Episode(
    val slug: String,
    val title: String,
    val number: Int,
    val date: String? = null,
    val url: String? = null,
    val displayTitle: String = title
)

data class AnimeDetail(
    val slug: String,
    val title: String,
    val japaneseTitle: String? = null,
    val coverUrl: String? = null,
    val score: String? = null,
    val producers: String? = null,
    val type: String? = null,
    val status: String? = null,
    val totalEpisodes: String? = null,
    val duration: String? = null,
    val releaseDate: String? = null,
    val studio: String? = null,
    val synopsis: String? = null,
    val genres: List<Genre> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val hasBatch: Boolean = false
)

data class StreamProvider(
    val name: String,
    val url: String
)

data class StreamQuality(
    val label: String,
    val providers: List<StreamProvider> = emptyList()
)

data class EpisodeDetail(
    val slug: String,
    val title: String,
    val url: String? = null,
    val releaseTime: String? = null,
    val previousEpisodeSlug: String? = null,
    val nextEpisodeSlug: String? = null,
    val defaultStreamUrl: String? = null,
    val qualities: List<StreamQuality> = emptyList()
) {
    val providerCount: Int get() = qualities.sumOf { it.providers.size }
}

data class PagedAnime(
    val items: List<AnimeSummary>,
    val page: Int,
    val hasNext: Boolean
)

data class SearchResult(
    val query: String,
    val total: Int,
    val items: List<AnimeSummary>
)

data class ScheduleDay(
    val key: String,
    val label: String,
    val items: List<AnimeSummary>
)

data class SavedAnime(
    val slug: String,
    val title: String,
    val coverUrl: String?,
    val status: String?,
    val type: String?,
    val addedAt: Long
)

data class HistoryEntry(
    val animeSlug: String,
    val animeTitle: String,
    val coverUrl: String?,
    val episodeSlug: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val completed: Boolean,
    val updatedAt: Long
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val hasProgress: Boolean
        get() = positionMs > 0L && durationMs > 0L
}
