package com.dealfinder.data.repository

import com.dealfinder.data.models.Offer
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    
    /**
     * Get all active offers
     */
    fun getAllActiveOffers(): Flow<List<Offer>>
    
    /**
     * Get offers by provider
     */
    fun getOffersByProvider(providerId: Long): Flow<List<Offer>>
    
    /**
     * Get offer by ID
     */
    suspend fun getOfferById(id: Long): Offer?
    
    /**
     * Get active offers sorted by expiry date
     */
    fun getActiveOffersSortedByExpiry(): Flow<List<Offer>>
    
    /**
     * Get offers for specific provider that are active
     */
    suspend fun getActiveOffersByProvider(providerId: Long): List<Offer>
    
    /**
     * Save ("vormerken") an offer for a user – persisted locally in Room.
     */
    suspend fun saveOffer(userId: Long, offerId: Long)
    
    /**
     * Remove a previously saved offer.
     */
    suspend fun removeSavedOffer(userId: Long, offerId: Long)
    
    /**
     * All offers saved by a user (including expired ones – status is shown in the UI).
     */
    fun getSavedOffers(userId: Long): Flow<List<Offer>>
    
    /**
     * Whether the user already saved this offer.
     */
    suspend fun isOfferSaved(userId: Long, offerId: Long): Boolean
}
