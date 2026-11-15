package com.bayu.jelajahrasa.data.repository

import androidx.room.withTransaction
import com.bayu.jelajahrasa.data.local.AppDatabase
import com.bayu.jelajahrasa.data.local.entity.CollectionEntity
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.mapper.toEntity
import com.bayu.jelajahrasa.data.remote.MovieRemoteDataSource
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto
import com.bayu.jelajahrasa.model.CollectionType
import com.bayu.jelajahrasa.model.MoviePageResult
import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val database: AppDatabase,
    private val remoteDataSource: MovieRemoteDataSource = MovieRemoteDataSource()
) {

    private val movieDao = database.movieDao()
    private val collectionDao = database.collectionDao()

    suspend fun discoverMovies(
        genreIds: String? = null,
        sortBy: String = "popularity.desc",
        year: Int? = null,
        minimumRating: Double? = null,
        maximumRating: Double? = null,
        originCountry: String? = null,
        page: Int = 1
    ): MoviePageResult {
        val response = remoteDataSource.getMovies(
            genreIds = genreIds,
            sortBy = sortBy,
            year = year,
            minimumRating = minimumRating,
            maximumRating = maximumRating,
            originCountry = originCountry,
            page = page
        )

        val movies = response.results.map { it.toEntity() }

        database.withTransaction {
            movieDao.insertMovies(movies)
        }

        return MoviePageResult(
            movies = movies,
            currentPage = response.page,
            totalPages = response.totalPages.coerceAtMost(500),
            totalResults = response.totalResults
        )
    }

    suspend fun searchMovies(
        query: String,
        page: Int = 1
    ): MoviePageResult {
        val response = remoteDataSource.searchMovies(
            query = query,
            page = page
        )

        val movies = response.results.map { it.toEntity() }

        database.withTransaction {
            movieDao.insertMovies(movies)
        }

        return MoviePageResult(
            movies = movies,
            currentPage = response.page,
            totalPages = response.totalPages.coerceAtMost(500),
            totalResults = response.totalResults
        )
    }

    suspend fun getMovieGenres() =
        remoteDataSource.getGenres()

    suspend fun getMovieDetails(movieId: Int): MovieDetailDto {
        val detail = remoteDataSource.getMovieDetails(movieId, "id-ID")
        if (detail.overview.isBlank()) {
            val englishDetail = remoteDataSource.getMovieDetails(movieId, "en-US")
            if (englishDetail.overview.isNotBlank()) {
                return englishDetail
            }
        }
        return detail
    }

    suspend fun getWatchedMovieIds(userId: String): List<Int> {
        return collectionDao.getCollectionMovieIds(userId, "WATCHED")
    }

    fun getAllMovies(): Flow<List<MovieEntity>> {
        return movieDao.getAllMovies()
    }

    suspend fun addToCollection(
        userId: String,
        movie: MovieEntity,
        collectionType: String
    ) {
        database.withTransaction {
            movieDao.insertMovie(movie)

            collectionDao.insertCollection(
                CollectionEntity(
                    userId = userId,
                    movieId = movie.movieId,
                    collectionType = collectionType
                )
            )
        }
    }

    suspend fun moveToWatched(
        userId: String,
        movie: MovieEntity
    ) {
        database.withTransaction {
            movieDao.insertMovie(movie)
            collectionDao.insertCollection(
                CollectionEntity(
                    userId = userId,
                    movieId = movie.movieId,
                    collectionType = CollectionType.WATCHED.value
                )
            )
            collectionDao.removeCollection(
                userId = userId,
                movieId = movie.movieId,
                collectionType = CollectionType.WATCHLIST.value
            )
        }
    }

    suspend fun batchMoveToWatched(
        userId: String,
        movies: List<MovieEntity>
    ) {
        database.withTransaction {
            for (movie in movies) {
                movieDao.insertMovie(movie)
                collectionDao.insertCollection(
                    CollectionEntity(
                        userId = userId,
                        movieId = movie.movieId,
                        collectionType = CollectionType.WATCHED.value
                    )
                )
                collectionDao.removeCollection(
                    userId = userId,
                    movieId = movie.movieId,
                    collectionType = CollectionType.WATCHLIST.value
                )
            }
        }
    }

    fun getCollections(
        userId: String,
        collectionType: String,
        sortOrder: String = "DESC"
    ): Flow<List<MovieEntity>> {
        return collectionDao.getCollections(userId, collectionType, sortOrder)
    }

    fun getCollectionCount(
        userId: String,
        collectionType: String
    ): Flow<Int> {
        return collectionDao.getCollectionCount(userId, collectionType)
    }

    suspend fun removeFromCollection(
        userId: String,
        movieId: Int,
        collectionType: String
    ) {
        collectionDao.removeCollection(userId, movieId, collectionType)
    }

    suspend fun getMovieKeywords(movieId: Int): List<String> {
        val cachedMovie = movieDao.getMovieById(movieId)
        if (cachedMovie != null && cachedMovie.keywords.isNotBlank()) {
            return cachedMovie.keywords.split(",").filter { it.isNotBlank() }
        }

        val remoteKeywords = remoteDataSource.getKeywords(movieId)
        val keywordsString = remoteKeywords.joinToString(",")
        if (cachedMovie != null) {
            movieDao.updateMovieKeywords(movieId, keywordsString)
        }
        return remoteKeywords
    }

    suspend fun isInCollection(
        userId: String,
        movieId: Int,
        collectionType: String
    ): Boolean {
        return collectionDao.isInCollection(
            userId,
            movieId,
            collectionType
        )
    }

    suspend fun getMovieById(movieId: Int): MovieEntity? {
        return movieDao.getMovieById(movieId)
    }
}