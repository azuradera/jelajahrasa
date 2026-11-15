package com.bayu.jelajahrasa.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bayu.jelajahrasa.data.repository.AuthRepository
import com.bayu.jelajahrasa.data.repository.MovieRepository

class CollectionViewModelFactory(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            return CollectionViewModel(
                movieRepository,
                authRepository
            ) as T
        }

        throw IllegalArgumentException("ViewModel tidak dikenal.")
    }
}