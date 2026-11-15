package com.bayu.jelajahrasa.data.remote.service

import com.bayu.jelajahrasa.data.remote.dto.GenreResponseDto
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto
import com.bayu.jelajahrasa.data.remote.dto.MovieResponseDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class KeywordResponseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("keywords") val keywords: List<KeywordDto>
)

data class KeywordDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

interface TmdbApiService {

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreIds: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("primary_release_year") year: Int? = null,
        @Query("vote_average.gte") minimumRating: Double? = null,
        @Query("vote_average.lte") maximumRating: Double? = null,
        @Query("with_origin_country") originCountry: String? = null,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID"
    ): MovieResponseDto

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "id-ID",
        @Query("include_adult") includeAdult: Boolean = false
    ): MovieResponseDto

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String = "id-ID"
    ): GenreResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "id-ID"
    ): MovieDetailDto

    @GET("movie/{movie_id}/keywords")
    suspend fun getMovieKeywords(
        @Path("movie_id") movieId: Int
    ): KeywordResponseDto
}
