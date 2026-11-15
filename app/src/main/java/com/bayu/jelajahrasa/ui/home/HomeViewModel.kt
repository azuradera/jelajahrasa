package com.bayu.jelajahrasa.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.data.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val popularMovies: List<MovieEntity> = emptyList(),
    val currentYearMovies: List<MovieEntity> = emptyList(),
    val topRatedMovies: List<MovieEntity> = emptyList(),
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                val popularResult = repository.discoverMovies(sortBy = "popularity.desc", page = 1)
                
                val currentYearResult = repository.discoverMovies(
                    sortBy = "vote_average.desc",
                    year = currentYear,
                    minimumRating = 6.0,
                    page = 1
                )

                val topRatedResult = repository.discoverMovies(
                    sortBy = "vote_average.desc",
                    minimumRating = 8.0,
                    page = 1
                )

                _uiState.value = HomeUiState(
                    popularMovies = popularResult.movies.take(10),
                    currentYearMovies = currentYearResult.movies.take(10),
                    topRatedMovies = topRatedResult.movies.take(10),
                    currentYear = currentYear,
                    isLoading = false
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage ?: "Gagal memuat film."
                )
            }
        }
    }
}
