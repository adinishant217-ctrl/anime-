package com.anistream.app.data.remote

import android.content.Context
import com.anistream.app.core.common.ApiException
import com.anistream.app.data.remote.dto.AnimeDetailDto
import com.anistream.app.data.remote.dto.AnimeListDto
import com.anistream.app.data.remote.dto.EpisodeDetailDto
import com.anistream.app.data.remote.dto.GenreDto
import com.anistream.app.data.remote.dto.ScheduleItemDto
import com.anistream.app.data.remote.dto.SearchDataDto
import com.anistream.app.data.remote.mapper.toDomain
import com.anistream.app.data.remote.mapper.toScheduleDays
import com.anistream.app.domain.model.AnimeDetail
import com.anistream.app.domain.model.EpisodeDetail
import com.anistream.app.domain.model.Genre
import com.anistream.app.domain.model.PagedAnime
import com.anistream.app.domain.model.ScheduleDay
import com.anistream.app.domain.model.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

class ShivraAnimeRemoteDataSource(
    context: Context,
    baseUrl: String = BASE_URL
) : AnimeRemoteDataSource {

    private val json = defaultApiJson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(50, TimeUnit.SECONDS)
        .cache(Cache(File(context.cacheDir, "anistream_http"), HTTP_CACHE_BYTES))
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "AniStream/1.0 (Android)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api: ShivraApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
        .create(ShivraApi::class.java)

    override suspend fun fetchOngoing(page: Int): Result<PagedAnime> = paged(page) { api.ongoing(page) }

    override suspend fun fetchCompleted(page: Int): Result<PagedAnime> = paged(page) { api.completed(page) }

    override suspend fun fetchGenres(): Result<List<Genre>> = request({ api.genres() }) { body ->
        decodeApiData<List<GenreDto>>(json, body).map { it.toDomain() }
    }

    override suspend fun fetchGenreAnime(slug: String, page: Int): Result<PagedAnime> =
        paged(page) { api.genreAnime(slug, page) }

    override suspend fun search(query: String): Result<SearchResult> =
        request({ api.search(query) }) { body ->
            decodeApiData<SearchDataDto>(json, body).toDomain()
        }

    override suspend fun fetchSchedule(): Result<List<ScheduleDay>> =
        request({ api.schedule() }) { body ->
            decodeApiData<Map<String, List<ScheduleItemDto>>>(json, body).toScheduleDays()
        }

    override suspend fun fetchAnimeDetail(slug: String): Result<AnimeDetail> =
        request({ api.animeDetail(slug) }) { body ->
            val detail = decodeApiData<AnimeDetailDto>(json, body).toDomain()
            if (detail.slug.isBlank() || detail.title.isBlank()) {
                throw ApiException("The anime service returned incomplete details for this title.")
            }
            detail
        }

    override suspend fun fetchEpisode(slug: String): Result<EpisodeDetail> =
        request({ api.episode(slug) }) { body ->
            val episode = decodeApiData<EpisodeDetailDto>(json, body).toDomain()
            if (episode.title.isBlank()) {
                throw ApiException("The anime service returned an empty episode.")
            }
            episode
        }

    private suspend fun paged(
        page: Int,
        call: suspend () -> Response<ResponseBody>
    ): Result<PagedAnime> = request(call) { body ->
        val data = decodeApiData<AnimeListDto>(json, body)
        val items = data.list.orEmpty().map { it.toDomain() }
        val perPage = data.pagination?.per_page ?: 0
        PagedAnime(
            items = items,
            page = data.pagination?.current_page?.takeIf { it > 0 } ?: page,
            hasNext = if (perPage > 0) items.size >= perPage else items.isNotEmpty()
        )
    }

    private suspend fun <T> request(
        call: suspend () -> Response<ResponseBody>,
        decode: (String) -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = call()
            if (!response.isSuccessful) {
                return@withContext Result.failure(ApiException(apiHttpMessage(response.code())))
            }
            val body = response.body()?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(ApiException(EMPTY_RESPONSE))
            }
            Result.success(decode(body))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    companion object {
        const val BASE_URL = "https://shivraapi.my.id/"
        private const val HTTP_CACHE_BYTES = 25L * 1024L * 1024L
        private const val EMPTY_RESPONSE = "The anime service returned an empty response."
    }
}
