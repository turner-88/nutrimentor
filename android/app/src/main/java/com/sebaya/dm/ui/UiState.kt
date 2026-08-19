package com.sebaya.dm.ui

/** Generic screen-section state so every screen can render loading/error/empty. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

/** Current data if the state is a success, else null (handy for derived values). */
val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success)?.data
