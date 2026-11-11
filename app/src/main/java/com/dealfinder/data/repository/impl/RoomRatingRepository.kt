package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.VerifiedRatingDao
import com.dealfinder.data.models.VerifiedRating
import com.dealfinder.data.repository.RatingRepository
import kotlinx.coroutines.flow.Flow

class RoomRatingRepository(
    private val ratingDao: VerifiedRatingDao
) : RatingRepository {
    
    override fun getRatingsByProvider(providerId: Long): Flow<List<VerifiedRating>> {
        return ratingDao.getByProvider(providerId)
    }
    
    override fun getAverageScore(providerId: Long): Flow<Float?> {
        return ratingDao.getAverageScore(providerId)
    }
    
    override suspend fun getLatestRating(providerId: Long): VerifiedRating? {
        return ratingDao.getLatestRating(providerId)
    }
    
    override fun getAllVerifiedRatings(): Flow<List<VerifiedRating>> {
        return ratingDao.getAllVerified()
    }
}
