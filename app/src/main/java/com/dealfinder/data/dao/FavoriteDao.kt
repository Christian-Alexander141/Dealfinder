package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    
    @Insert
    suspend fun insert(favorite: Favorite): Long
    
    @Update
    suspend fun update(favorite: Favorite)
    
    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun getById(id: Long): Favorite?
    
    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getByUser(userId: Long): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites WHERE userId = :userId AND providerId = :providerId")
    suspend fun getByUserAndProvider(userId: Long, providerId: Long): Favorite?
    
    @Query("SELECT COUNT(*) FROM favorites WHERE providerId = :providerId")
    fun getCountByProvider(providerId: Long): Flow<Int>
    
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM favorites WHERE userId = :userId AND providerId = :providerId")
    suspend fun deleteByUserAndProvider(userId: Long, providerId: Long)
    
    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Long)
    
    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
