package com.bayu.jelajahrasa.model

import com.bayu.jelajahrasa.data.local.entity.MovieEntity

data class MoviePageResult(
    val movies: List<MovieEntity>,
    val currentPage: Int,
    val totalPages: Int,
    val totalResults: Int
)