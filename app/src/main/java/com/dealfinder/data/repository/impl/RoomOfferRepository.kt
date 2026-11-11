package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.OfferDao
import com.dealfinder.data.dao.SavedOfferDao
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.SavedOffer
import com.dealfinder.data.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

class RoomOfferRepository(
    private val offerDao: OfferDao,
    private val savedOfferDao: SavedOfferDao
) : OfferRepository {
    
    override fun getAllActiveOffers(): Flow<List<Offer>> {
        return offerDao.getAllActive(Instant.now())
    }
    
    override fun getOffersByProvider(providerId: Long): Flow<List<Offer>> {
        return offerDao.getByProvider(providerId, Instant.now())
    }
    
    override suspend fun getOfferById(id: Long): Offer? = offerDao.getById(id)
    
    override fun getActiveOffersSortedByExpiry(): Flow<List<Offer>> {
        return offerDao.getActiveSortedByExpiry(Instant.now())
    }
    
    override suspend fun getActiveOffersByProvider(providerId: Long): List<Offer> {
        return offerDao.getByProvider(providerId, Instant.now()).first()
    }
    
    override suspend fun saveOffer(userId: Long, offerId: Long) {
        savedOfferDao.insert(SavedOffer(userId = userId, offerId = offerId))
    }
    
    override suspend fun removeSavedOffer(userId: Long, offerId: Long) {
        savedOfferDao.delete(userId, offerId)
    }
    
    override fun getSavedOffers(userId: Long): Flow<List<Offer>> {
        return savedOfferDao.getSavedOffersByUser(userId)
    }
    
    override suspend fun isOfferSaved(userId: Long, offerId: Long): Boolean {
        return savedOfferDao.countSaved(userId, offerId) > 0
    }
}
