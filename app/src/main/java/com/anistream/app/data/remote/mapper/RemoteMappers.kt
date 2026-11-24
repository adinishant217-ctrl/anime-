package com.anistream.app.data.remote.mapper

import com.anistream.app.core.common.EpisodeNumbers
import com.anistream.app.data.remote.dto.AnimeDetailDto
import com.anistream.app.data.remote.dto.AnimeSummaryDto
import com.anistream.app.data.remote.dto.EpisodeDetailDto
import com.anistream.app.data.remote.dto.EpisodeDto
import com.anistream.app.data.remote.dto.GenreDto
import com.anistream.app.data.remote.dto.ProviderDto
import com.anistream.app.data.remote.dto.ScheduleItemDto
import com.anistream.app.data.remote.dto.SearchDataDto
import com.anistream.app.data.remote.dto.StreamQualityDto
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.AnimeSummary
import com.anistream.app.domain.model.Episode
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult
import com.anistream.app.domain.model.StreamProvider
import com.anistream.app.domain.model.StreamQuality
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val dayOrder = listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu", "minggu")

fun AnimeSummaryDto.toDomain(): AnimeSummary = AnimeSummary(
    slug = slug,
    title = title,
    coverUrl = cover.cleanUrl(),
    episodeLabel = episode.nullIfBlank(),
    totalEpisodes = total_episode.nullIfBlank(),
    rating = rating.nullIfBlank(),
    day = day.nullIfBlank(),
    date = date.nullIfBlank(),
    status = status.nullIfBlank(),
    studio = studio.nullIfBlank(),
    synopsis = sinopsis.nullIfBlank(),
    genres = genre.toGenreNames()
)

fun GenreDto.toDomain(): Genre = Genre(
    name = name,
    slug = slug.ifBlank { url.orEmpty().trimEnd('/').substringAfterLast('/') }
)

fun AnimeDetailDto.toDomain(): AnimeDetail {
    val resolvedSlug = slug.nullIfBlank()
        ?: url.orEmpty().trimEnd('/').substringAfterLast('/')
    val episodes = episode_list.orEmpty()
        .map { it.toDomain() }
        .sortedBy { it.number }
    return AnimeDetail(
        slug = resolvedSlug,
        title = title,
        japaneseTitle = japanese.nullIfBlank(),
        coverUrl = cover.cleanUrl(),
        score = skor.nullIfBlank(),
        producers = produser.nullIfBlank(),
        type = tipe.nullIfBlank(),
        status = status.nullIfBlank(),
        totalEpisodes = total_episode.nullIfBlank(),
        duration = durasi.nullIfBlank(),
        releaseDate = tanggal_rilis.nullIfBlank(),
        studio = studio.nullIfBlank(),
        synopsis = synopsis.nullIfBlank(),
        genres = genre.toGenreList(),
        episodes = episodes,
        hasBatch = batch.batchSlug() != null
    )
}

fun EpisodeDto.toDomain(): Episode {
    val number = EpisodeNumbers.extract(title)
    return Episode(
        slug = slug,
        title = title,
        number = number,
        date = date.nullIfBlank(),
        url = url.nullIfBlank(),
        displayTitle = EpisodeNumbers.cleanTitle(title, number)
    )
}

fun EpisodeDetailDto.toDomain(): EpisodeDetail = EpisodeDetail(
    slug = slug,
    title = title,
    url = url.nullIfBlank(),
    releaseTime = waktu_rilis.nullIfBlank(),
    previousEpisodeSlug = prev_episode.episodeSlug(),
    nextEpisodeSlug = next_episode.episodeSlug(),
    defaultStreamUrl = defaultstreaming.nullIfBlank(),
    qualities = stream.orEmpty().map { it.toDomain() }.filter { it.providers.isNotEmpty() || it.label.isNotBlank() }
)

fun StreamQualityDto.toDomain(): StreamQuality = StreamQuality(
    label = quality,
    providers = providers.orEmpty().map { it.toDomain() }
)

fun ProviderDto.toDomain(): StreamProvider = StreamProvider(
    name = provider,
    url = url
)

fun SearchDataDto.toDomain(): SearchResult = SearchResult(
    query = query,
    total = total,
    items = list.orEmpty().map { it.toDomain() }
)

fun ScheduleItemDto.toDomain(): AnimeSummary = AnimeSummary(
    slug = slug,
    title = title
)

fun Map<String, List<ScheduleItemDto>>.toScheduleDays(): List<ScheduleDay> {
    val days = entries.map { (key, value) ->
        ScheduleDay(
            key = key,
            label = key.replaceFirstChar { it.uppercase() },
            items = value.map { it.toDomain() }
        )
    }
    val known = days.filter { it.key.lowercase() in dayOrder }
        .sortedBy { dayOrder.indexOf(it.key.lowercase()) }
    val unknown = days.filter { it.key.lowercase() !in dayOrder }.sortedBy { it.key }
    return known + unknown
}

fun JsonElement?.toGenreNames(): List<String> = when (this) {
    is JsonArray -> mapNotNull { element ->
        when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonObject -> (element["name"] as? JsonPrimitive)?.contentOrNull
            else -> null
        }
    }
    else -> emptyList()
}

fun JsonElement?.toGenreList(): List<Genre> = when (this) {
    is JsonArray -> mapNotNull { element ->
        if (element !is JsonObject) return@mapNotNull null
        val name = (element["name"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        val url = (element["url"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        Genre(
            name = name,
            slug = url.trimEnd('/').substringAfterLast('/')
        )
    }
    else -> emptyList()
}

fun JsonElement?.episodeSlug(): String? = when (this) {
    is JsonObject -> (this["slug"] as? JsonPrimitive)?.contentOrNull
    is JsonArray -> firstOrNull()?.let { element ->
        (element as? JsonObject)?.let { (it["slug"] as? JsonPrimitive)?.contentOrNull }
    }
    else -> null
}

fun JsonElement?.batchSlug(): String? = when (this) {
    is JsonObject -> (this["slug"] as? JsonPrimitive)?.contentOrNull
    is JsonArray -> firstOrNull()?.let { element ->
        (element as? JsonObject)?.let { (it["slug"] as? JsonPrimitive)?.contentOrNull }
    }
    else -> null
}

private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.cleanUrl(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
