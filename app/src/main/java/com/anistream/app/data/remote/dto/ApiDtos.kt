package com.anistream.app.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTOs mirror the ShivraAPI payloads used by anichi-cli. Every field that the
 * service has been observed to omit or type inconsistently is nullable/defaulted.
 */
@Serializable
data class ApiMeta(
    val status: Boolean = false,
    val code: Int = 0,
    val message: String = "",
    val timestamp: String? = null,
    val source: String? = null
)

@Serializable
data class ApiEnvelope<T>(
    val meta: ApiMeta? = null,
    val data: T? = null
)

@Serializable
data class PaginationDto(
    val current_page: Int = 1,
    val per_page: Int = 0
)

@Serializable
data class AnimeSummaryDto(
    val title: String = "",
    val slug: String = "",
    val url: String = "",
    val cover: String? = null,
    val episode: String? = null,
    val total_episode: String? = null,
    val rating: String? = null,
    val day: String? = null,
    val date: String? = null,
    val status: String? = null,
    val studio: String? = null,
    val sinopsis: String? = null,
    val genre: JsonElement? = null
)

@Serializable
data class AnimeListDto(
    val list: List<AnimeSummaryDto>? = null,
    val pagination: PaginationDto? = null
)

@Serializable
data class GenreDto(
    val name: String = "",
    val slug: String = "",
    val url: String? = null
)

@Serializable
data class EpisodeDto(
    val title: String = "",
    val slug: String = "",
    val url: String = "",
    val date: String? = null
)

@Serializable
data class AnimeDetailDto(
    val title: String = "",
    val japanese: String? = null,
    val skor: String? = null,
    val produser: String? = null,
    val tipe: String? = null,
    val status: String? = null,
    val total_episode: String? = null,
    val durasi: String? = null,
    val tanggal_rilis: String? = null,
    val studio: String? = null,
    val genre: JsonElement? = null,
    val synopsis: String? = null,
    val slug: String? = null,
    val url: String? = null,
    val cover: String? = null,
    val batch: JsonElement? = null,
    val episode_list: List<EpisodeDto>? = null
)

@Serializable
data class ProviderDto(
    val provider: String = "",
    val url: String = ""
)

@Serializable
data class StreamQualityDto(
    val quality: String = "",
    val providers: List<ProviderDto>? = null
)

@Serializable
data class EpisodeDetailDto(
    val title: String = "",
    val slug: String = "",
    val url: String? = null,
    val waktu_rilis: String? = null,
    val prev_episode: JsonElement? = null,
    val next_episode: JsonElement? = null,
    val defaultstreaming: String? = null,
    val stream: List<StreamQualityDto>? = null,
    val downloads: JsonElement? = null
)

@Serializable
data class SearchDataDto(
    val query: String = "",
    val total: Int = 0,
    val list: List<AnimeSummaryDto>? = null
)

@Serializable
data class ScheduleItemDto(
    val title: String = "",
    val url: String = "",
    val slug: String = ""
)
