package com.dealfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import com.dealfinder.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication and registration
 */
class AuthViewModel : ViewModel() {
    
    private val userRepository = AppModule.provideUserRepository()
    private val appPreferences = AppModule.provideAppPreferences()
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    sealed class AuthUiState {
        object Idle : AuthUiState()
        object Loading : AuthUiState()
        data class Success(val userId: Long, val name: String, val email: String, val userType: UserType) : AuthUiState()
        data class Error(val message: String) : AuthUiState()
    }
    
    /**
     * Register a new user
     */
    fun registerUser(name: String, email: String, userType: UserType) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            try {
                // Check if user already exists
                if (userRepository.userExists(email)) {
                    _uiState.value = AuthUiState.Error("Diese E-Mail-Adresse ist bereits registriert")
                    return@launch
                }
                
                // Register new user
                val user = userRepository.registerUser(name, email, userType)
                
                // Set as current user
                appPreferences.setCurrentUser(user.id, user.name, user.email, user.userType)
                
                _uiState.value = AuthUiState.Success(
                    user.id, user.name, user.email, user.userType
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Regestrierung fehlgeschlagen: ${e.message}")
            }
        }
    }
    
    /**
     * Check if user is already registered (for demo purposes)
     */
    suspend fun isUserRegistered(email: String): Boolean {
        return userRepository.userExists(email)
    }
    
    /**
     * Get current user
     */
    suspend fun getCurrentUser(): User? {
        return userRepository.getCurrentUser()
    }
    
    /**
     * Set current user (for navigation purposes)
     */
    fun setCurrentUser(userId: Long, name: String, email: String, userType: UserType) {
        viewModelScope.launch {
            appPreferences.setCurrentUser(userId, name, email, userType)
        }
    }
}
