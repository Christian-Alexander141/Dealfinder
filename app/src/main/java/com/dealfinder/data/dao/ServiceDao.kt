package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Service
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    
    @Insert
    suspend fun insert(service: Service): Long
    
    @Insert
    suspend fun insertAll(services: List<Service>)
    
    @Update
    suspend fun update(service: Service)
    
    @Query("SELECT * FROM services WHERE id = :id")
    suspend fun getById(id: Long): Service?
    
    @Query("SELECT * FROM services WHERE providerId = :providerId")
    fun getByProvider(providerId: Long): Flow<List<Service>>
    
    @Query("DELETE FROM services WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM services")
    suspend fun deleteAll()
}
