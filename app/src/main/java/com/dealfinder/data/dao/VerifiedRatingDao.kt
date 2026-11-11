package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.VerifiedRating
import kotlinx.coroutines.flow.Flow

@Dao
interface VerifiedRatingDao {
    
    @Insert
    suspend fun insert(rating: VerifiedRating): Long
    
    @Insert
    suspend fun insertAll(ratings: List<VerifiedRating>)
    
    @Update
    suspend fun update(rating: VerifiedRating)
    
    @Query("SELECT * FROM verified_ratings WHERE id = :id")
    suspend fun getById(id: Long): VerifiedRating?
    
    @Query("SELECT * FROM verified_ratings WHERE providerId = :providerId")
    fun getByProvider(providerId: Long): Flow<List<VerifiedRating>>
    
    @Query("SELECT AVG(overallScore) FROM verified_ratings WHERE providerId = :providerId AND verificationStatus = 'VERIFIED'")
    fun getAverageScore(providerId: Long): Flow<Float?>
    
    @Query("""
        SELECT * FROM verified_ratings 
        WHERE providerId = :providerId 
        ORDER BY testDate DESC
        LIMIT 1
        """)
    suspend fun getLatestRating(providerId: Long): VerifiedRating?
    
    @Query("DELETE FROM verified_ratings WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM verified_ratings")
    suspend fun deleteAll()
    
    /** Nur verifizierte Testergebnisse – unverifizierte zählen nicht als unabhängige Bewertung. */
    @Query("SELECT * FROM verified_ratings WHERE verificationStatus = 'VERIFIED'")
    fun getAllVerified(): Flow<List<VerifiedRating>>
}
