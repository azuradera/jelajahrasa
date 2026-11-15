package com.bayu.jelajahrasa.data.mapper

import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto

fun MovieDetailDto.toEntity(): MovieEntity {
    return MovieEntity(
        movieId = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genreIds = genres.joinToString(",") { it.id.toString() }
    )
}