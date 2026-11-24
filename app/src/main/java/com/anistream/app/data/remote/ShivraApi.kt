package com.anistream.app.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints implemented by https://shivraapi.my.id/otd, verified against the
 * live service. There is no separate server, subtitle, audio or "all anime"
 * endpoint, and the documented home endpoint returns 404.
 */
interface ShivraApi {

    @GET("otd/ongoing")
    suspend fun ongoing(@Query("page") page: Int): Response<ResponseBody>

    @GET("otd/completed")
    suspend fun completed(@Query("page") page: Int): Response<ResponseBody>

    @GET("otd/genres")
    suspend fun genres(): Response<ResponseBody>

    @GET("otd/genres/{slug}")
    suspend fun genreAnime(
        @Path("slug") slug: String,
        @Query("page") page: Int
    ): Response<ResponseBody>

    @GET("otd/search")
    suspend fun search(@Query("q") query: String): Response<ResponseBody>

    @GET("otd/schedule")
    suspend fun schedule(): Response<ResponseBody>

    @GET("otd/anime/{slug}")
    suspend fun animeDetail(@Path("slug") slug: String): Response<ResponseBody>

    @GET("otd/episode/{slug}")
    suspend fun episode(@Path("slug") slug: String): Response<ResponseBody>
}
