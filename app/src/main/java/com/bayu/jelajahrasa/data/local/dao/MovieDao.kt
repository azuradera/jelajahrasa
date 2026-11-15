package com.bayu.jelajahrasa.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Upsert
    suspend fun insertMovie(movie: MovieEntity)

    @Upsert
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies WHERE movieId = :movieId LIMIT 1")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies ORDER BY title ASC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies")
    suspend fun getAllMoviesList(): List<MovieEntity>

    @Query("UPDATE movies SET keywords = :keywords WHERE movieId = :movieId")
    suspend fun updateMovieKeywords(movieId: Int, keywords: String)
}
