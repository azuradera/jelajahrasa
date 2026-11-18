package com.bayu.jelajahrasa.ui.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayu.jelajahrasa.ThemePreferences
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.remote.dto.GenreDto
import com.bayu.jelajahrasa.data.repository.MovieRepository
import com.bayu.jelajahrasa.model.Mood
import com.bayu.jelajahrasa.model.MovieFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieUiState(
    val movies: List<MovieEntity> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val selectedMood: Mood? = null,
    val recommendationMode: String = ThemePreferences.MODE_ACCURATE,
    val activeFilter: MovieFilter = MovieFilter(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingGenres: Boolean = false,
    val errorMessage: String? = null,
    val genreErrorMessage: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalResults: Int = 0
)

class MovieViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState = _uiState.asStateFlow()

    private var requestJob: Job? = null
    private var genreJob: Job? = null
    private val shownMovieIds = mutableSetOf<Int>()
    private var currentMoodPage = 1

    fun setRecommendationMode(mode: String) {
        if (_uiState.value.recommendationMode != mode) {
            _uiState.value = _uiState.value.copy(recommendationMode = mode)
            val mood = _uiState.value.selectedMood
            if (mood != null) {
                recommendByMood(mood, isNewSession = true)
            }
        }
    }

    fun recommendByMood(mood: Mood, isNewSession: Boolean = true) {
        requestJob?.cancel()

        if (isNewSession) {
            shownMovieIds.clear()
            currentMoodPage = (1..3).random()
        }

        if (_uiState.value.recommendationMode == ThemePreferences.MODE_FAST) {
            recommendFastByMood(mood)
        } else {
            recommendAccurateByMood(mood)
        }
    }

    private fun recommendFastByMood(mood: Mood) {
        val filter = MovieFilter(
            genreIds = mood.getGenreIds(),
            sortBy = "popularity.desc",
            minimumRating = 5.0
        )

        _uiState.value = _uiState.value.copy(
            selectedMood = mood,
            searchQuery = "",
            activeFilter = filter
        )

        requestJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val page = currentMoodPage
                val result = repository.discoverMovies(
                    genreIds = filter.genreIds,
                    sortBy = filter.sortBy,
                    year = filter.year,
                    minimumRating = filter.minimumRating,
                    page = page
                )

                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val watchedMovieIds = if (userId != null) {
                    repository.getWatchedMovieIds(userId)
                } else {
                    emptyList()
                }

                val validCandidates = result.movies.filter { movie ->
                    !watchedMovieIds.contains(movie.movieId) &&
                    !shownMovieIds.contains(movie.movieId)
                }.shuffled()

                var selectedMovies = validCandidates.take(10)

                if (selectedMovies.size < 10) {
                    val fallbackCandidates = result.movies.filter { movie ->
                        !watchedMovieIds.contains(movie.movieId)
                    }.shuffled()
                    selectedMovies = fallbackCandidates.take(10)
                }

                currentMoodPage += 1
                if (currentMoodPage > 5) currentMoodPage = 1

                shownMovieIds.addAll(selectedMovies.map { it.movieId })

                _uiState.value = _uiState.value.copy(
                    movies = selectedMovies,
                    isLoading = false,
                    isLoadingMore = false,
                    currentPage = 1,
                    totalPages = 1,
                    totalResults = selectedMovies.size
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = exception.localizedMessage
                        ?: "Gagal mengambil rekomendasi suasana hati."
                )
            }
        }
    }

    private fun recommendAccurateByMood(mood: Mood) {
        val sortOptionsList = listOf("vote_average.desc", "popularity.desc", "vote_count.desc")
        val randomSort = sortOptionsList.random()

        val filter = MovieFilter(
            genreIds = mood.getGenreIds(),
            sortBy = randomSort,
            minimumRating = 5.0
        )

        _uiState.value = _uiState.value.copy(
            selectedMood = mood,
            searchQuery = "",
            activeFilter = filter
        )

        requestJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val p1 = currentMoodPage
                val p2 = currentMoodPage + 1
                val p3 = currentMoodPage + 2

                val page1 = repository.discoverMovies(genreIds = filter.genreIds, sortBy = filter.sortBy, year = filter.year, minimumRating = filter.minimumRating, page = p1)
                val page2 = repository.discoverMovies(genreIds = filter.genreIds, sortBy = filter.sortBy, year = filter.year, minimumRating = filter.minimumRating, page = p2)
                val page3 = repository.discoverMovies(genreIds = filter.genreIds, sortBy = filter.sortBy, year = filter.year, minimumRating = filter.minimumRating, page = p3)

                val allCandidates = (page1.movies + page2.movies + page3.movies).distinctBy { it.movieId }

                val scoredMovies = allCandidates.map { movie ->
                    val keywords = repository.getMovieKeywords(movie.movieId)
                    val score = mood.calculateScore(movie, keywords)
                    movie to score
                }

                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val watchedMovieIds = if (userId != null) {
                    repository.getWatchedMovieIds(userId)
                } else {
                    emptyList()
                }

                val validCandidates = scoredMovies.filter { (movie, score) ->
                    !watchedMovieIds.contains(movie.movieId) &&
                    !shownMovieIds.contains(movie.movieId) &&
                    score >= 4.0
                }.sortedByDescending { it.second }

                var selectedMovies = validCandidates.map { it.first }.take(10)

                if (selectedMovies.size < 10) {
                    val fallbackCandidates = scoredMovies.filter { (movie, _) ->
                        !watchedMovieIds.contains(movie.movieId)
                    }.sortedByDescending { it.second }

                    selectedMovies = fallbackCandidates.map { it.first }.take(10)
                }

                currentMoodPage += 3
                if (currentMoodPage > 10) currentMoodPage = 1

                shownMovieIds.addAll(selectedMovies.map { it.movieId })

                _uiState.value = _uiState.value.copy(
                    movies = selectedMovies,
                    isLoading = false,
                    isLoadingMore = false,
                    currentPage = 1,
                    totalPages = 1,
                    totalResults = selectedMovies.size
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = exception.localizedMessage
                        ?: "Gagal mengambil rekomendasi suasana hati."
                )
            }
        }
    }

    fun loadMoreMoodRecommendations() {
        val mood = _uiState.value.selectedMood ?: return
        if (_uiState.value.isLoading) return
        recommendByMood(mood, isNewSession = false)
    }

    fun applyFilters(filter: MovieFilter) {
        _uiState.value = _uiState.value.copy(
            selectedMood = null,
            searchQuery = "",
            activeFilter = filter
        )

        loadMovies(filter)
    }

    fun clearFilters() {
        val filter = MovieFilter()

        _uiState.value = _uiState.value.copy(
            selectedMood = null,
            searchQuery = "",
            activeFilter = filter
        )

        loadMovies(filter)
    }

    private fun loadMovies(
        filter: MovieFilter,
        page: Int = 1,
        append: Boolean = false
    ) {
        requestJob?.cancel()

        requestJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !append,
                isLoadingMore = append,
                errorMessage = null
            )

            try {
                val result = repository.discoverMovies(
                    genreIds = filter.genreIds,
                    sortBy = filter.sortBy,
                    year = filter.year,
                    minimumRating = filter.minimumRating,
                    maximumRating = filter.maximumRating,
                    originCountry = filter.originCountry,
                    page = page
                )

                val currentMovies = _uiState.value.movies

                _uiState.value = _uiState.value.copy(
                    movies = if (append) {
                        (currentMovies + result.movies).distinctBy { it.movieId }
                    } else {
                        result.movies
                    },
                    isLoading = false,
                    isLoadingMore = false,
                    currentPage = result.currentPage,
                    totalPages = result.totalPages,
                    totalResults = result.totalResults
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = exception.localizedMessage
                        ?: "Gagal memuat film."
                )
            }
        }
    }

    fun searchMovies(query: String, page: Int = 1, append: Boolean = false) {
        requestJob?.cancel()

        _uiState.value = _uiState.value.copy(
            selectedMood = null,
            searchQuery = query,
            isLoading = !append,
            isLoadingMore = append,
            errorMessage = null
        )

        requestJob = viewModelScope.launch {
            try {
                val result = repository.searchMovies(query, page)
                val currentMovies = _uiState.value.movies

                _uiState.value = _uiState.value.copy(
                    movies = if (append) {
                        (currentMovies + result.movies).distinctBy { it.movieId }
                    } else {
                        result.movies
                    },
                    isLoading = false,
                    isLoadingMore = false,
                    currentPage = result.currentPage,
                    totalPages = result.totalPages,
                    totalResults = result.totalResults
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = exception.localizedMessage
                        ?: "Gagal mencari film."
                )
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || currentState.currentPage >= currentState.totalPages) {
            return
        }

        val nextPage = currentState.currentPage + 1
        if (currentState.searchQuery.isNotBlank()) {
            searchMovies(currentState.searchQuery, nextPage, append = true)
        } else if (currentState.selectedMood != null) {
            loadMoreMoodRecommendations()
        } else {
            loadMovies(currentState.activeFilter, nextPage, append = true)
        }
    }

    fun loadGenres() {
        if (_uiState.value.genres.isNotEmpty() || _uiState.value.isLoadingGenres) return

        genreJob?.cancel()
        genreJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGenres = true, genreErrorMessage = null)
            try {
                val response = repository.getMovieGenres()
                _uiState.value = _uiState.value.copy(
                    genres = response.genres,
                    isLoadingGenres = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingGenres = false,
                    genreErrorMessage = exception.localizedMessage ?: "Gagal memuat genre."
                )
            }
        }
    }
}
