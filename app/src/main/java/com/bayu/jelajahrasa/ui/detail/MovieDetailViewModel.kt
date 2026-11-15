package com.bayu.jelajahrasa.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayu.jelajahrasa.data.remote.dto.MovieDetailDto
import com.bayu.jelajahrasa.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val movie: MovieDetailDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MovieDetailViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    fun loadMovieDetails(movieId: Int) {
        if (movieId <= 0) {
            _uiState.value = MovieDetailUiState(
                errorMessage = "ID film tidak valid."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = MovieDetailUiState(
                isLoading = true
            )

            try {
                val movie = repository.getMovieDetails(movieId)

                _uiState.value = MovieDetailUiState(
                    movie = movie
                )
            } catch (exception: Exception) {
                _uiState.value = MovieDetailUiState(
                    errorMessage = exception.localizedMessage
                        ?: "Gagal memuat detail film."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}