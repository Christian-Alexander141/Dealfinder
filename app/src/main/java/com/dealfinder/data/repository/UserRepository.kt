package com.dealfinder.data.repository

import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    
    /**
     * Get all users
     */
    fun getAllUsers(): Flow<List<User>>
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(id: Long): User?
    
    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): User?
    
    /**
     * Get current user (simplified for demo)
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Register a new user
     */
    suspend fun registerUser(name: String, email: String, userType: UserType): User
    
    /**
     * Update user
     */
    suspend fun updateUser(user: User)
    
    /**
     * Delete user
     */
    suspend fun deleteUser(id: Long)
    
    /**
     * Check if user exists with email
     */
    suspend fun userExists(email: String): Boolean
    
    /**
     * Set current user (for demo purposes)
     */
    suspend fun setCurrentUser(user: User)
    
    /**
     * Clear current user
     */
    suspend fun clearCurrentUser()
}
