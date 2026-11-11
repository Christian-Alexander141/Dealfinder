package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Offer
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface OfferDao {
    
    @Insert
    suspend fun insert(offer: Offer): Long
    
    @Insert
    suspend fun insertAll(offers: List<Offer>)
    
    @Update
    suspend fun update(offer: Offer)
    
    @Query("SELECT * FROM offers WHERE id = :id")
    suspend fun getById(id: Long): Offer?
    
    @Query("SELECT * FROM offers WHERE isActive = 1 AND validUntil > :now")
    fun getAllActive(now: Instant): Flow<List<Offer>>
    
    @Query("SELECT * FROM offers WHERE providerId = :providerId AND isActive = 1 AND validUntil > :now")
    fun getByProvider(providerId: Long, now: Instant): Flow<List<Offer>>
    
    @Query("SELECT * FROM offers WHERE isActive = 1 AND validUntil > :now ORDER BY validUntil ASC")
    fun getActiveSortedByExpiry(now: Instant): Flow<List<Offer>>
    
    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM offers")
    suspend fun deleteAll()
}
