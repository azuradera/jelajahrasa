package com.bayu.jelajahrasa.ui.movie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedRecommendationViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val MODE_NONE = "none"
        const val MODE_FAST = "fast"
        const val MODE_ACCURATE = "accurate"
    }

    private val _recommendationMode = MutableStateFlow(MODE_NONE)
    val recommendationMode = _recommendationMode.asStateFlow()

    fun setMode(mode: String) {
        if (_recommendationMode.value != mode) {
            _recommendationMode.value = mode
        }
    }
}
