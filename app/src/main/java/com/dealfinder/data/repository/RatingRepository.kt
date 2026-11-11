package com.dealfinder.data.repository

import com.dealfinder.data.models.VerifiedRating
import kotlinx.coroutines.flow.Flow

interface RatingRepository {
    
    /**
     * Get all ratings for a provider
     */
    fun getRatingsByProvider(providerId: Long): Flow<List<VerifiedRating>>
    
    /**
     * Get average score for a provider
     */
    fun getAverageScore(providerId: Long): Flow<Float?>
    
    /**
     * Get latest rating for a provider
     */
    suspend fun getLatestRating(providerId: Long): VerifiedRating?
    
    /**
     * All VERIFIED ratings (reactive). Unverified ratings are never exposed
     * as independent test results.
     */
    fun getAllVerifiedRatings(): Flow<List<VerifiedRating>>
}
