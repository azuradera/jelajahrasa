package com.bayu.jelajahrasa.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovieDetailDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("original_title")
    val originalTitle: String,

    @SerializedName("overview")
    val overview: String,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("release_date")
    val releaseDate: String?,

    @SerializedName("runtime")
    val runtime: Int?,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int,

    @SerializedName("genres")
    val genres: List<GenreDto>,

    @SerializedName("tagline")
    val tagline: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("original_language")
    val originalLanguage: String,

    @SerializedName("popularity")
    val popularity: Double,

    @SerializedName("homepage")
    val homepage: String?
)