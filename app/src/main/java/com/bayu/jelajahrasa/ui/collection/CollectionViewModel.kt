package com.bayu.jelajahrasa.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.mapper.toEntity
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto
import com.bayu.jelajahrasa.data.repository.AuthRepository
import com.bayu.jelajahrasa.data.repository.MovieRepository
import com.bayu.jelajahrasa.model.CollectionType
import com.bayu.jelajahrasa.model.Mood
import com.bayu.jelajahrasa.model.determineMood
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class CollectionUiState(
    val selectedType: CollectionType = CollectionType.WATCHLIST,
    val rawMovies: List<MovieEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val watchlistCount: Int = 0,
    val watchedCount: Int = 0,
    val favoriteCount: Int = 0,
    val searchQuery: String = "",
    val sortOrder: String = "DESC",
    val selectedMood: Mood? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class CollectionViewModel(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null
    private var operationJob: Job? = null
    private var observingUserId: String? = null
    private var countJobs = mutableListOf<Job>()
    private var observingCountsUserId: String? = null

    private fun observeCounts(userId: String) {
        if (observingCountsUserId == userId && countJobs.isNotEmpty()) return
        observingCountsUserId = userId
        countJobs.forEach { it.cancel() }
        countJobs.clear()

        countJobs.add(
            viewModelScope.launch {
                movieRepository.getCollectionCount(userId, CollectionType.WATCHLIST.value)
                    .collect { count ->
                        _uiState.value = _uiState.value.copy(watchlistCount = count)
                    }
            }
        )
        countJobs.add(
            viewModelScope.launch {
                movieRepository.getCollectionCount(userId, CollectionType.WATCHED.value)
                    .collect { count ->
                        _uiState.value = _uiState.value.copy(watchedCount = count)
                    }
            }
        )
        countJobs.add(
            viewModelScope.launch {
                movieRepository.getCollectionCount(userId, CollectionType.FAVORITE.value)
                    .collect { count ->
                        _uiState.value = _uiState.value.copy(favoriteCount = count)
                    }
            }
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterMovies()
    }

    fun setSortOrder(sortOrder: String) {
        if (_uiState.value.sortOrder == sortOrder) return
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
        loadCollections(_uiState.value.selectedType)
    }

    fun setMoodFilter(mood: Mood?) {
        _uiState.value = _uiState.value.copy(selectedMood = mood)
        filterMovies()
    }

    private fun filterMovies() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val mood = state.selectedMood

        val filtered = state.rawMovies.filter { movie ->
            val matchesQuery = query.isBlank() || movie.title.lowercase().contains(query)
            val matchesMood = mood == null || movie.determineMood() == mood
            matchesQuery && matchesMood
        }

        _uiState.value = state.copy(movies = filtered)
    }

    fun loadCollections(type: CollectionType) {
        val userId = authRepository.currentUser?.uid

        if (userId == null) {
            collectionJob?.cancel()
            collectionJob = null
            observingUserId = null
            observingCountsUserId = null
            countJobs.forEach { it.cancel() }
            countJobs.clear()

            _uiState.value = CollectionUiState(
                selectedType = type,
                errorMessage = "Silakan masuk terlebih dahulu."
            )
            return
        }

        observeCounts(userId)

        collectionJob?.cancel()
        observingUserId = userId

        _uiState.value = _uiState.value.copy(
            selectedType = type,
            isLoading = true,
            errorMessage = null
        )

        collectionJob = viewModelScope.launch {
            movieRepository.getCollections(
                userId = userId,
                collectionType = type.value,
                sortOrder = _uiState.value.sortOrder
            )
                .catch { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage
                            ?: "Gagal memuat koleksi film."
                    )
                }
                .collect { movies ->
                    val currentState = _uiState.value
                    val query = currentState.searchQuery.trim().lowercase()
                    val mood = currentState.selectedMood

                    val filtered = movies.filter { movie ->
                        val matchesQuery = query.isBlank() || movie.title.lowercase().contains(query)
                        val matchesMood = mood == null || movie.determineMood() == mood
                        matchesQuery && matchesMood
                    }

                    _uiState.value = currentState.copy(
                        selectedType = type,
                        rawMovies = movies,
                        movies = filtered,
                        isLoading = false,
                        errorMessage = null
                    )
                }
        }
    }

    fun addToCollection(
        movie: MovieEntity,
        type: CollectionType
    ) {
        val userId = authRepository.currentUser?.uid

        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Silakan masuk terlebih dahulu."
            )
            return
        }

        if (operationJob?.isActive == true) return

        operationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )

            try {
                if (type == CollectionType.WATCHED) {
                    movieRepository.moveToWatched(userId, movie)
                } else {
                    movieRepository.addToCollection(
                        userId = userId,
                        movie = movie,
                        collectionType = type.value
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.localizedMessage
                        ?: "Gagal menambahkan film ke koleksi."
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false
                )
            }
        }
    }

    fun addMovieDetailToCollection(
        movie: MovieDetailDto,
        type: CollectionType
    ) {
        addToCollection(
            movie = movie.toEntity(),
            type = type
        )
    }

    fun removeFromCollection(
        movieId: Int,
        type: CollectionType
    ) {
        val userId = authRepository.currentUser?.uid

        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Silakan masuk terlebih dahulu."
            )
            return
        }

        if (operationJob?.isActive == true) return

        operationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                errorMessage = null
            )

            try {
                movieRepository.removeFromCollection(
                    userId = userId,
                    movieId = movieId,
                    collectionType = type.value
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.localizedMessage
                        ?: "Gagal menghapus film dari koleksi."
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false
                )
            }
        }
    }

    fun checkCollection(
        movieId: Int,
        type: CollectionType,
        onResult: (Boolean) -> Unit
    ) {
        val userId = authRepository.currentUser?.uid

        if (userId == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val exists = movieRepository.isInCollection(
                    userId = userId,
                    movieId = movieId,
                    collectionType = type.value
                )

                onResult(exists)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                onResult(false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    fun addMoviesToCollection(movies: List<MovieEntity>, type: CollectionType) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                if (type == CollectionType.WATCHED) {
                    movieRepository.batchMoveToWatched(userId, movies)
                } else {
                    for (movie in movies) {
                        movieRepository.addToCollection(userId, movie, type.value)
                    }
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.localizedMessage ?: "Gagal menambahkan film ke koleksi."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    fun removeMoviesFromCollection(movieIds: List<Int>, type: CollectionType) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                for (movieId in movieIds) {
                    movieRepository.removeFromCollection(userId, movieId, type.value)
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.localizedMessage ?: "Gagal menghapus film dari koleksi."
                )
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    override fun onCleared() {
        collectionJob?.cancel()
        operationJob?.cancel()
        countJobs.forEach { it.cancel() }
    }
}
