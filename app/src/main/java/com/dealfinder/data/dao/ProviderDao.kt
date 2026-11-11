package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Provider
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDao {
    
    @Insert
    suspend fun insert(provider: Provider): Long
    
    @Insert
    suspend fun insertAll(providers: List<Provider>)
    
    @Update
    suspend fun update(provider: Provider)
    
    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: Long): Provider?
    
    @Query("SELECT * FROM providers WHERE isActive = 1 AND isDealfinderPartner = 1")
    fun getAllActivePartners(): Flow<List<Provider>>
    
    @Query("""
        SELECT * FROM providers 
        WHERE isActive = 1 AND isDealfinderPartner = 1
        AND (name LIKE :query OR description LIKE :query)
        """)
    fun searchActivePartners(query: String): Flow<List<Provider>>
    
    @Query("""
        SELECT * FROM providers 
        WHERE isActive = 1 AND isDealfinderPartner = 1
        AND categoryId = :categoryId
        """)
    fun getByCategory(categoryId: Long): Flow<List<Provider>>
    
    @Query("""
        SELECT * FROM providers 
        WHERE isActive = 1 AND isDealfinderPartner = 1
        AND name LIKE :query OR description LIKE :query
        AND categoryId = :categoryId
        """)
    fun searchByCategory(query: String, categoryId: Long): Flow<List<Provider>>
    
    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM providers")
    suspend fun deleteAll()
}
