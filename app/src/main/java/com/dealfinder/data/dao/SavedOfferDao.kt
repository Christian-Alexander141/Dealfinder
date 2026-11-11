package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.SavedOffer
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedOfferDao {

    /** Saving twice for the same user/offer is a no-op (unique index). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(savedOffer: SavedOffer): Long

    @Query("DELETE FROM saved_offers WHERE userId = :userId AND offerId = :offerId")
    suspend fun delete(userId: Long, offerId: Long)

    @Query("SELECT COUNT(*) FROM saved_offers WHERE userId = :userId AND offerId = :offerId")
    suspend fun countSaved(userId: Long, offerId: Long): Int

    /**
     * Full offer rows for everything the user saved, newest saved first.
     * Expired offers are included on purpose – the UI shows their status.
     */
    @Query(
        """
        SELECT o.* FROM offers o
        INNER JOIN saved_offers s ON s.offerId = o.id
        WHERE s.userId = :userId
        ORDER BY s.savedAt DESC
        """
    )
    fun getSavedOffersByUser(userId: Long): Flow<List<Offer>>

    @Query("DELETE FROM saved_offers WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    @Query("DELETE FROM saved_offers")
    suspend fun deleteAll()
}
