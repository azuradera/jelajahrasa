package com.bayu.jelajahrasa.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bayu.jelajahrasa.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPasswordResetSent: Boolean = false,
    val resetEmailMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authState.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
    }

    fun register(email: String, password: String) {
        authenticate {
            repository.register(email, password)
        }
    }

    fun login(email: String, password: String) {
        authenticate {
            repository.login(email, password)
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun resetPassword(email: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                isPasswordResetSent = false,
                resetEmailMessage = null
            )

            try {
                repository.resetPassword(email)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPasswordResetSent = true,
                    resetEmailMessage = "Tautan atur ulang kata sandi telah dikirim ke $email"
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage
                        ?: "Terjadi kesalahan. Silakan coba kembali."
                )
            }
        }
    }

    fun clearResetState() {
        _uiState.value = _uiState.value.copy(
            isPasswordResetSent = false,
            resetEmailMessage = null,
            errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun authenticate(action: suspend () -> FirebaseUser) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val user = action()

                _uiState.value = _uiState.value.copy(
                    user = user,
                    isLoading = false,
                    errorMessage = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage
                        ?: "Terjadi kesalahan. Silakan coba kembali."
                )
            }
        }
    }
}