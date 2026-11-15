package com.bayu.jelajahrasa.data.mapper

import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.remote.dto.MovieDto

fun MovieDto.toEntity(): MovieEntity {
    return MovieEntity(
        movieId = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genreIds = genreIds.joinToString(",")
    )
}

fun MovieEntity.toGenreIds(): List<Int> {
    return genreIds
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { it.toIntOrNull() }
}