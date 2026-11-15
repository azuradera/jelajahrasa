package com.bayu.jelajahrasa.model

data class MovieFilter(
    val genreIds: String? = null,
    val year: Int? = null,
    val minimumRating: Double? = null,
    val maximumRating: Double? = null,
    val originCountry: String? = null,
    val sortBy: String = "popularity.desc"
)