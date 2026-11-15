package com.bayu.jelajahrasa.data.remote

import com.bayu.jelajahrasa.data.remote.dto.GenreResponseDto
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto
import com.bayu.jelajahrasa.data.remote.dto.MovieResponseDto
import com.bayu.jelajahrasa.data.remote.service.TmdbApiService

class MovieRemoteDataSource(
    private val api: TmdbApiService = RetrofitClient.api
) {

    suspend fun getMovies(
        genreIds: String? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
        minimumRating: Double? = null,
        maximumRating: Double? = null,
        originCountry: String? = null,
        page: Int = 1
    ): MovieResponseDto {
        return api.discoverMovies(
            genreIds = genreIds,
            sortBy = sortBy,
            year = year,
            minimumRating = minimumRating,
            maximumRating = maximumRating,
            originCountry = originCountry,
            page = page
        )
    }

    suspend fun searchMovies(
        query: String,
        page: Int = 1
    ): MovieResponseDto {
        return api.searchMovies(
            query = query,
            page = page
        )
    }

    suspend fun getGenres(): GenreResponseDto {
        return api.getMovieGenres()
    }

    suspend fun getMovieDetails(
        movieId: Int,
        language: String = "id-ID"
    ): MovieDetailDto {
        return api.getMovieDetails(movieId, language)
    }

    suspend fun getKeywords(movieId: Int): List<String> {
        return try {
            val response = api.getMovieKeywords(movieId)
            response.keywords.map { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
