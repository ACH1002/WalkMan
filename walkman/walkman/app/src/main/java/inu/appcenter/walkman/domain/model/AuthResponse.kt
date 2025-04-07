package inu.appcenter.walkman.domain.model

sealed interface AuthResponse {
    data object Success: AuthResponse
    data class Error(val message: String?) : AuthResponse
}

