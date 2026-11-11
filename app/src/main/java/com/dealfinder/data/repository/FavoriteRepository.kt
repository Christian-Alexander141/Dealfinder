package com.dealfinder.data.repository

import com.dealfinder.data.models.Favorite
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    
    /**
     * Get all favorites for a user
     */
    fun getFavoritesByUser(userId: Long): Flow<List<Favorite>>
    
    /**
     * Check if provider is favorited by user
     */
    suspend fun isFavorite(userId: Long, providerId: Long): Boolean
    
    /**
     * Add to favorites
     */
    suspend fun addFavorite(userId: Long, providerId: Long): Long
    
    /**
     * Remove from favorites
     */
    suspend fun removeFavorite(userId: Long, providerId: Long)
    
    /**
     * Get favorite count for a provider
     */
    fun getFavoriteCount(providerId: Long): Flow<Int>
}
