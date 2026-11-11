package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.FavoriteDao
import com.dealfinder.data.models.Favorite
import com.dealfinder.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class RoomFavoriteRepository(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {
    
    override fun getFavoritesByUser(userId: Long): Flow<List<Favorite>> {
        return favoriteDao.getByUser(userId)
    }
    
    override suspend fun isFavorite(userId: Long, providerId: Long): Boolean {
        return favoriteDao.getByUserAndProvider(userId, providerId) != null
    }
    
    override suspend fun addFavorite(userId: Long, providerId: Long): Long {
        val favorite = Favorite(
            userId = userId,
            providerId = providerId,
            createdAt = Instant.now()
        )
        return favoriteDao.insert(favorite)
    }
    
    override suspend fun removeFavorite(userId: Long, providerId: Long) {
        favoriteDao.deleteByUserAndProvider(userId, providerId)
    }
    
    override fun getFavoriteCount(providerId: Long): Flow<Int> {
        return favoriteDao.getCountByProvider(providerId)
    }
}
